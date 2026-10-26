package com.ibank.axwms.domain.worklog.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibank.axwms.domain.worklog.dto.InternalGraphRagWorklogQueryApiDto;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class GraphRagWorklogSearchClientTest {

    private static final String QUERY_PATH = "/ai/graph/worklogs-v1/query";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private ExecutorService executorService;
    private String baseUrl;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("GraphRAG TEAM_IDS 요청이면 scopeMode와 allowedTeamIds를 함께 직렬화한다")
    void GraphRAG_TEAM_IDS_요청이면_scopeMode와_allowedTeamIds를_함께_직렬화한다() throws Exception {
        InternalGraphRagWorklogQueryApiDto.Request request =
                InternalGraphRagWorklogQueryApiDto.Request.forTeamIds("정산 업무 알려줘", List.of(21L, 22L));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.get("query").asText()).isEqualTo("정산 업무 알려줘");
        assertThat(json.get("scopeMode").asText()).isEqualTo("TEAM_IDS");
        assertThat(json.get("allowedTeamIds")).hasSize(2);
        assertThat(json.get("allowedTeamIds").get(0).asLong()).isEqualTo(21L);
        assertThat(json.get("allowedTeamIds").get(1).asLong()).isEqualTo(22L);
    }

    @Test
    @DisplayName("GraphRAG ALL 요청이면 scopeMode만 직렬화하고 allowedTeamIds를 생략한다")
    void GraphRAG_ALL_요청이면_scopeMode만_직렬화하고_allowedTeamIds를_생략한다() throws Exception {
        InternalGraphRagWorklogQueryApiDto.Request request =
                InternalGraphRagWorklogQueryApiDto.Request.forAll("전체 정산 업무 알려줘");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.get("query").asText()).isEqualTo("전체 정산 업무 알려줘");
        assertThat(json.get("scopeMode").asText()).isEqualTo("ALL");
        assertThat(json.has("allowedTeamIds")).isFalse();
    }

    @Test
    @DisplayName("GraphRAG TEAM_IDS 요청은 빈 팀 목록을 내부 계약 위반으로 차단한다")
    void GraphRAG_TEAM_IDS_요청은_빈_팀_목록을_내부_계약_위반으로_차단한다() {
        assertThatThrownBy(() -> InternalGraphRagWorklogQueryApiDto.Request.forTeamIds("정산", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("teamIds must not be empty");
    }

    @Test
    @DisplayName("GraphRAG query 경로로 scopeMode TEAM_IDS 요청을 전송한다")
    void GraphRAG_query_경로로_scopeMode_TEAM_IDS_요청을_전송한다() throws Exception {
        AtomicReference<String> receivedPath = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        startServer(exchange -> {
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJsonResponse(exchange, 200, """
                    {
                      "answer": "팀 업무 검색 결과입니다.",
                      "references": [],
                      "mode": "local",
                      "internalOnly": true
                    }
                    """);
        });
        GraphRagWorklogSearchClient client = client(Duration.ofSeconds(1));

        client.queryWorklogs(InternalGraphRagWorklogQueryApiDto.Request.forTeamIds("정산 업무 알려줘", List.of(21L, 22L)));

        assertThat(receivedPath.get()).isEqualTo(QUERY_PATH);
        assertThat(receivedBody.get()).contains("\"scopeMode\":\"TEAM_IDS\"");
        assertThat(receivedBody.get()).contains("\"allowedTeamIds\":[21,22]");
    }

    @Test
    @DisplayName("GraphRAG query 경로로 scopeMode ALL 요청을 전송하고 allowedTeamIds를 생략한다")
    void GraphRAG_query_경로로_scopeMode_ALL_요청을_전송하고_allowedTeamIds를_생략한다() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        startServer(exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJsonResponse(exchange, 200, """
                    {
                      "answer": "전체 업무 검색 결과입니다.",
                      "references": [],
                      "mode": "local",
                      "internalOnly": true
                    }
                    """);
        });
        GraphRagWorklogSearchClient client = client(Duration.ofSeconds(1));

        client.queryWorklogs(InternalGraphRagWorklogQueryApiDto.Request.forAll("전체 결산 업무 알려줘"));

        assertThat(receivedBody.get()).contains("\"scopeMode\":\"ALL\"");
        assertThat(receivedBody.get()).doesNotContain("allowedTeamIds");
    }

    @Test
    @DisplayName("GraphRAG query 성공 응답을 내부 DTO로 역직렬화한다")
    void GraphRAG_query_성공_응답을_내부_DTO로_역직렬화한다() throws Exception {
        startServer(exchange -> respondJson(exchange, 200, """
                {
                  "answer": "결산 보고서 작성 업무가 있습니다.",
                  "references": [
                    {"referenceId": "ref-a", "filePath": "worklog://502"}
                  ],
                  "mode": "local",
                  "internalOnly": true
                }
                """));
        GraphRagWorklogSearchClient client = client(Duration.ofSeconds(1));

        InternalGraphRagWorklogQueryApiDto.Response response = client.queryWorklogs(request());

        assertThat(response.answer()).isEqualTo("결산 보고서 작성 업무가 있습니다.");
        assertThat(response.references()).hasSize(1);
        assertThat(response.references().getFirst().referenceId()).isEqualTo("ref-a");
        assertThat(response.internalOnly()).isTrue();
    }

    @Test
    @DisplayName("AI HTTP 오류는 시맨틱 검색 502용 BusinessException으로 변환하고 원인 예외를 보존한다")
    void AI_HTTP_오류는_시맨틱_검색_502용_BusinessException으로_변환하고_원인_예외를_보존한다() throws Exception {
        startServer(exchange -> respondJson(exchange, 504, "{\"detail\":\"GRAPHRAG_QUERY_TIMEOUT\"}"));
        GraphRagWorklogSearchClient client = client(Duration.ofSeconds(1));

        Throwable thrown = catchThrowable(() -> client.queryWorklogs(request()));

        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasCauseInstanceOf(RestClientResponseException.class);
        assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
    }

    @Test
    @DisplayName("read timeout은 시맨틱 검색 502용 BusinessException으로 변환하고 원인 예외를 보존한다")
    void read_timeout은_시맨틱_검색_502용_BusinessException으로_변환하고_원인_예외를_보존한다() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                respondJson(exchange, 200, "{\"answer\":\"late\",\"references\":[],\"mode\":\"local\",\"internalOnly\":true}");
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        });
        GraphRagWorklogSearchClient client = client(Duration.ofMillis(50));

        Throwable thrown = catchThrowable(() -> client.queryWorklogs(request()));

        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
    }

    private void startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(QUERY_PATH, handler);
        executorService = Executors.newSingleThreadExecutor();
        server.setExecutor(executorService);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ai";
    }

    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();
        writeJsonResponse(exchange, status, body);
    }

    private static void writeJsonResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    private GraphRagWorklogSearchClient client(Duration readTimeout) {
        return new GraphRagWorklogSearchClient(new AiWorklogSearchProperties(
                true,
                baseUrl,
                Duration.ofSeconds(1),
                readTimeout
        ));
    }

    private static InternalGraphRagWorklogQueryApiDto.Request request() {
        return InternalGraphRagWorklogQueryApiDto.Request.forTeamIds("결산 업무 알려줘", List.of(21L, 22L));
    }
}
