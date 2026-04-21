package com.ibank.axwms.global.config;

import com.ibank.axwms.global.response.ApiResponse;
import com.ibank.axwms.global.response.ResponseEnvelopePolicy;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc 스펙 생성기는 컨트롤러 반환 타입만 보고 스키마를 만들기 때문에
 * 런타임에 {@code GlobalResponseAdvice} 가 씌우는 {@link ApiResponse} 봉투가 Swagger UI 에 반영되지 않는 괴리가 생긴다.
 * 여기서는 OperationCustomizer 로 2xx 응답 스키마를 {success, data, timestamp} 봉투로 교체해 실제 curl 응답과 문서를 일치시킨다.
 * 래핑 대상 판정은 런타임 advice 와 동일한 {@link ResponseEnvelopePolicy} 에 위임한다.
 */
@Configuration
public class OpenApiConfig {

    private static final String SUCCESS_STATUS_PREFIX = "2";

    /**
     * springdoc 이 모든 엔드포인트의 {@link Operation} 을 만든 직후 호출해주는 후처리 훅을 등록한다.
     * 각 엔드포인트마다 한 번씩 호출되며, 래핑 대상이면 2xx 응답 스키마를 봉투로 교체한 operation 을 반환한다.
     */
    @Bean
    public OperationCustomizer apiResponseEnvelopeCustomizer() {
        return (operation, handlerMethod) -> {
            if (ResponseEnvelopePolicy.shouldWrap(handlerMethod)) {
                wrapSuccessResponses(operation);
            }
            return operation;
        };
    }

    /**
     * 한 operation 이 선언한 응답 코드들 중 2xx 성공 응답만 골라 봉투로 교체한다.
     * 4xx/5xx 는 GlobalExceptionHandler 가 별도의 error 봉투 형식으로 변환하므로 여기서 건드리지 않는다.
     * 상태 코드 키를 접두 "2" 로 판정해 200/201/202/204 등 성공 범주를 한 번에 포괄한다.
     */
    private void wrapSuccessResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return;
        }
        for (Map.Entry<String, io.swagger.v3.oas.models.responses.ApiResponse> entry : responses.entrySet()) {
            if (!entry.getKey().startsWith(SUCCESS_STATUS_PREFIX)) {
                continue;
            }
            wrapAllMediaTypes(entry.getValue());
        }
    }

    /**
     * 한 응답이 가진 모든 미디어 타입의 schema 를 봉투로 교체한다.
     * 컨트롤러가 {@code produces} 를 명시하지 않으면 springdoc 이 {@code *} / {@code *} 와일드카드 키로 content 를 만들기 때문에,
     * {@code application/json} 하나만 조회하면 래핑이 누락되는 케이스가 생긴다. 이를 막기 위해 미디어 타입과 무관하게 전체 values() 를 순회한다.
     */
    private void wrapAllMediaTypes(io.swagger.v3.oas.models.responses.ApiResponse response) {
        if (response == null) {
            return;
        }
        Content content = response.getContent();
        if (content == null) {
            return;
        }
        for (MediaType mediaType : content.values()) {
            if (mediaType == null || mediaType.getSchema() == null) {
                continue;
            }
            mediaType.schema(buildEnvelopeSchema(mediaType.getSchema()));
        }
    }

    /**
     * 원본 반환 타입 schema 를 감싸는 {success, data, timestamp} 봉투 schema 를 조립한다.
     * data 자리에 원본 schema 를 그대로 끼워 넣기 때문에 {@code #/components/schemas/...} 참조가 살아 있어 컴포넌트 재등록이 필요 없다.
     * 성공 응답에서는 error 필드가 {@code @JsonInclude(NON_NULL)} 로 인해 직렬화에 포함되지 않으므로 문서에서도 생략한다.
     * 세 필드를 required 로 지정해 Swagger UI 가 non-null 계약으로 표시하고 클라이언트 코드 제너레이터가 같은 계약으로 타입을 만든다.
     */
    private Schema<?> buildEnvelopeSchema(Schema<?> dataSchema) {
        ObjectSchema envelope = new ObjectSchema();
        envelope.addProperty("success", new BooleanSchema()._default(Boolean.TRUE));
        envelope.addProperty("data", dataSchema);
        envelope.addProperty("timestamp", new DateTimeSchema());
        envelope.setRequired(List.of("success", "data", "timestamp"));
        return envelope;
    }
}
