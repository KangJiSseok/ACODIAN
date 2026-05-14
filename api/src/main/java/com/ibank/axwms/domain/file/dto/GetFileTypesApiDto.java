package com.ibank.axwms.domain.file.dto;

import com.ibank.axwms.domain.file.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GetFileTypesApiDto {

    @Schema(description = "파일 형식 조회 응답 항목")
    public record Response(
            @Schema(description = "파일 목록 필터에 사용할 FileType 코드", example = "PDF")
            FileType fileType,
            @Schema(description = "저장소와 DB 에 기록되는 소문자 확장자", example = "pdf")
            String extension
    ) {
        /**
         * enum 이름은 API 필터 값으로, 확장자는 저장 vocabulary 로 분리해 클라이언트 선택지에 함께 제공한다.
         */
        public static Response from(FileType fileType) {
            return new Response(fileType, fileType.dbExtension());
        }

        /**
         * enum 선언 순서를 그대로 유지해 Swagger 설명과 화면 선택지의 노출 순서가 흔들리지 않게 한다.
         */
        public static List<Response> all() {
            return Arrays.stream(FileType.values())
                    .map(Response::from)
                    .toList();
        }
    }
}
