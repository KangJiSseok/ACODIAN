package com.ibank.axwms.domain.file.external;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 오브젝트 스토리지에 저장할 파일의 key(경로) 를 생성한다.
 * URL 이 아닌 불변 식별자만을 만들며, 실제 접근 URL 은 호출 시점에 별도로 조립한다.
 */
@Component
public class FilePathGenerator {

    private static final String DEFAULT_EXTENSION = "bin";

    /**
     * 업무 첨부 파일 key 를 생성한다. 형식은 {@code worklog/{yyyy}/{MM}/{dd}/{uuid}.{ext}} 로
     * 날짜 파티셔닝과 UUID 충돌 방지를 동시에 확보한다.
     *
     * @param worklogId    업무 ID. 현재 경로에는 포함되지 않지만 향후 파티셔닝 전략 변경 대비용 파라미터로 유지한다.
     * @param originalName 업로드 원본 파일명
     * @return 스토리지에 저장할 key
     */
    public String generate(Long worklogId, String originalName) {
        LocalDate today = LocalDate.now();
        return "worklog/%d/%02d/%02d/%s.%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extensionOf(originalName)
        );
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return DEFAULT_EXTENSION;
        }
        int dot = originalName.lastIndexOf('.');
        if (dot <= 0 || dot == originalName.length() - 1) {
            return DEFAULT_EXTENSION;
        }
        return originalName.substring(dot + 1).toLowerCase();
    }
}
