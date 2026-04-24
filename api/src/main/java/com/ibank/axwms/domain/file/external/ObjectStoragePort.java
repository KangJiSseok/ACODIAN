package com.ibank.axwms.domain.file.external;

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
}
