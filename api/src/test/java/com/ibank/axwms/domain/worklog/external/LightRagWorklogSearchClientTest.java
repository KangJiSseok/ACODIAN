package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.InternalLightRagWorklogQueryApiDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LightRagWorklogSearchClientTest {

    private static final String QUERY_PATH = "/ai/light/worklogs-v3/query";

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
    @DisplayName("시맨틱 검색 기본 read timeout은 AI LightRAG query timeout보다 길다")
    void 시맨틱_검색_기본_read_timeout은_AI_LightRAG_query_timeout보다_길다() {
        AiWorklogSearchProperties properties = new AiWorklogSearchProperties(true, null, null, null);

        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(180));
    }

    @Test
    @DisplayName("LightRAG query 성공 응답을 내부 DTO로 역직렬화한다")
    void LightRAG_query_성공_응답을_내부_DTO로_역직렬화한다() throws Exception {
        startServer(exchange -> respondJson(exchange, 200, """
                {
                  "answer": "결산 보고서 작성 업무가 있습니다.",
                  "references": [
                    {"referenceId": "ref-a", "filePath": "worklog://502"}
                  ],
                  "mode": "mix",
                  "internalOnly": true
                }
                """));
        LightRagWorklogSearchClient client = client(Duration.ofSeconds(1));

        InternalLightRagWorklogQueryApiDto.Response response = client.queryWorklogs(request());

        assertThat(response.answer()).isEqualTo("결산 보고서 작성 업무가 있습니다.");
        assertThat(response.references()).hasSize(1);
        assertThat(response.references().getFirst().referenceId()).isEqualTo("ref-a");
        assertThat(response.internalOnly()).isTrue();
    }

    @Test
    @DisplayName("AI HTTP 오류는 시맨틱 검색 502용 BusinessException으로 변환하고 원인 예외를 보존한다")
    void AI_HTTP_오류는_시맨틱_검색_502용_BusinessException으로_변환하고_원인_예외를_보존한다() throws Exception {
        startServer(exchange -> respondJson(exchange, 504, "{\"detail\":\"LIGHTRAG_QUERY_TIMEOUT\"}"));
        LightRagWorklogSearchClient client = client(Duration.ofSeconds(1));

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
                respondJson(exchange, 200, "{\"answer\":\"late\",\"references\":[],\"mode\":\"mix\",\"internalOnly\":true}");
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        });
        LightRagWorklogSearchClient client = client(Duration.ofMillis(50));

        Throwable thrown = catchThrowable(() -> client.queryWorklogs(request()));

        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
    }

    /** 테스트마다 독립된 로컬 HTTP 서버를 띄워 실제 RestClient timeout/HTTP 오류 변환을 검증한다. */
    private void startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(QUERY_PATH, handler);
        executorService = Executors.newSingleThreadExecutor();
        server.setExecutor(executorService);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ai";
    }

    /** 성공/오류 응답을 같은 JSON 형식으로 내려 RestClient의 status 분기만 테스트한다. */
    private static void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    /** 테스트 서버 주소와 요청별 read timeout을 주입한 실제 client를 만든다. */
    private LightRagWorklogSearchClient client(Duration readTimeout) {
        return new LightRagWorklogSearchClient(new AiWorklogSearchProperties(
                true,
                baseUrl,
                Duration.ofSeconds(1),
                readTimeout
        ));
    }

    /** query 본문은 로그에 남기지 않으므로 권한 팀 목록만 포함해 직렬화 경로를 확인한다. */
    private static InternalLightRagWorklogQueryApiDto.Request request() {
        return InternalLightRagWorklogQueryApiDto.Request.of("결산 업무 알려줘", List.of(21L, 22L));
    }
}
