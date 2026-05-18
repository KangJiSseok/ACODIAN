package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogLightIndexDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WorklogLightIndexClientTest {

    @Test
    @DisplayName("Light v3 index endpoint에 worklogIds 배열 body를 POST로 전송한다")
    void post_worklog_ids_to_light_v3_index_endpoint() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        CountDownLatch requestArrived = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ai/light/worklogs-v3/index", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            requestArrived.countDown();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            WorklogLightIndexClient client = new WorklogLightIndexClient(new AiWorklogLightIndexProperties(
                    true,
                    "http://localhost:" + port + "/ai",
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(1)
            ));

            client.requestIndex(TriggerWorklogLightIndexDto.Request.of(501L));

            assertThat(requestArrived.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(method.get()).isEqualTo("POST");
            assertThat(path.get()).isEqualTo("/ai/light/worklogs-v3/index");
            assertThat(body.get()).isEqualTo("{\"worklogIds\":[501]}");
        } finally {
            server.stop(0);
        }
    }
}
