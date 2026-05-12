package com.ibank.axwms.domain.file.external;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 인프라 추상화. 구현체는 S3 등 실제 오브젝트 스토리지에 대응한다.
 * 도메인 서비스는 이 포트만 의존해 스토리지 교체나 스텁 대체가 가능하다.
 */
public interface ObjectStoragePort {

    /**
     * 주어진 key 위치에 파일을 저장하고 저장에 사용된 key 를 반환한다.
     * key 는 호출측이 미리 생성해 넘긴다. 반환값은 보상 삭제·엔티티 저장에 재사용된다.
     *
     * @param file 업로드 대상 파일
     * @param key  스토리지 내부 식별자(경로)
     * @return 저장에 사용된 key
     */
    String upload(MultipartFile file, String key);

    /**
     * 업로드된 객체를 key 로 삭제한다. DB 저장 실패 시 보상 용도로 호출한다.
     *
     * @param key 삭제할 객체의 스토리지 key
     */
    void delete(String key);

    /**
     * 소스 key 의 객체를 대상 key 로 복사한다.
     * 두 key 모두 스토리지 내부 상대 key 형식을 사용한다.
     */
    void copy(String sourceKey, String targetKey);

    /**
     * 주어진 상대 key 에 대한 공개 접근 URL 을 반환한다.
     * 업로드 없이 key 만으로 URL 을 미리 계산할 때 사용한다.
     */
    String toPublicUrl(String key);

    /**
     * 공개 접근 URL 을 storage 상대 key 로 복원한다.
     * 구현체가 URL 규칙을 알 수 없거나 자기 저장소 URL 이 아니면 빈 값을 반환한다.
     */
    default Optional<String> toStorageKey(String publicUrl) {
        return Optional.empty();
    }

    /**
     * prefix 하위에서 cutoff 이전에 업로드된 객체의 상대 키 목록을 반환한다.
     * 반환된 키는 delete(key) 에 그대로 전달할 수 있는 상대 키 형식이다.
     *
     * @param prefix 조회할 경로 접두사 (예: "temp/")
     * @param cutoff 이 시각보다 이전에 업로드된 객체만 반환
     */
    List<String> listKeysUploadedBefore(String prefix, Instant cutoff);
}
