package com.ibank.axwms.domain.organization.user.service;

import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import com.ibank.axwms.domain.file.external.S3StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * 프로필 이미지 저장소 전용 S3 어댑터.
 * 공용 ObjectStoragePort 와 분리해 개발자별 base-prefix 분리한다.
 */
@Slf4j
@Component
public class ProfileImageS3ObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public ProfileImageS3ObjectStorageAdapter(S3Client s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /** 파일을 S3에 업로드하고 포트 계약대로 상대 storage key 를 반환한다. */
    @Override
    public String upload(MultipartFile file, String key) {
        String qualifiedKey = qualify(key);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(qualifiedKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return key;
        } catch (IOException | RuntimeException exception) {
            log.error("프로필 이미지 S3 업로드 실패 bucket={} key={} name={}",
                    properties.bucket(), qualifiedKey, file.getOriginalFilename(), exception);
            throw new IllegalStateException("프로필 이미지 S3 업로드에 실패했습니다. key=" + qualifiedKey, exception);
        }
    }


    /** S3 오브젝트를 삭제한다. 스케줄러가 temp 파일 정리 시 호출한다. */
    @Override
    public void delete(String key) {
        String qualifiedKey = qualify(key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(qualifiedKey)
                    .build());
        } catch (RuntimeException exception) {
            log.error("프로필 이미지 S3 삭제 실패 bucket={} key={}", properties.bucket(), qualifiedKey, exception);
            throw new IllegalStateException("프로필 이미지 S3 삭제에 실패했습니다. key=" + qualifiedKey, exception);
        }
    }

    /** 버킷 내에서 sourceKey → targetKey 로 복사한다. promote() 의 temp → final 위치 이동에 사용된다. */
    @Override
    public void copy(String sourceKey, String targetKey) {
        String qualifiedSource = qualify(sourceKey);
        String qualifiedTarget = qualify(targetKey);
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(properties.bucket()).sourceKey(qualifiedSource)
                    .destinationBucket(properties.bucket()).destinationKey(qualifiedTarget)
                    .build());
        } catch (RuntimeException exception) {
            log.error("프로필 이미지 S3 복사 실패 source={} target={}", qualifiedSource, qualifiedTarget, exception);
            throw new IllegalStateException("프로필 이미지 S3 복사에 실패했습니다. source=" + qualifiedSource, exception);
        }
    }

    /** S3 에 업로드하지 않고 key 만으로 공개 URL 을 미리 계산한다. */
    @Override
    public String toPublicUrl(String key) {
        return buildPublicUrl(qualify(key));
    }

    /** prefix 하위에서 cutoff 이전에 업로드된 객체의 상대 키 목록을 반환한다. */
    @Override
    public List<String> listKeysUploadedBefore(String prefix, Instant cutoff) {
        String qualifiedPrefix = qualify(prefix);
        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(properties.bucket())
                        .prefix(qualifiedPrefix)
                        .build()
        );
        return response.contents().stream()
                .filter(obj -> obj.lastModified().isBefore(cutoff))
                .map(obj -> unqualify(obj.key()))
                .toList();
    }

    /** basePrefix 를 key 앞에 붙여 개발자별 S3 네임스페이스를 분리한다. */
    private String qualify(String key) {
        String prefix = properties.basePrefix();
        if (!StringUtils.hasText(prefix)) {
            return key;
        }
        return prefix + "/" + key;
    }

    /** qualify() 역연산: absoluteKey 에서 basePrefix/ 접두사를 제거해 상대 키를 반환한다. */
    private String unqualify(String absoluteKey) {
        String prefix = properties.basePrefix();
        if (!StringUtils.hasText(prefix)) {
            return absoluteKey;
        }
        String prefixWithSlash = prefix + "/";
        return absoluteKey.startsWith(prefixWithSlash)
                ? absoluteKey.substring(prefixWithSlash.length())
                : absoluteKey;
    }

    /** publicBaseUrl 과 key 를 결합해 공개 접근 URL 을 만든다. */
    private String buildPublicUrl(String qualifiedKey) {
        String base = properties.publicBaseUrl();
        return base.endsWith("/") ? base + qualifiedKey : base + "/" + qualifiedKey;
    }
}
