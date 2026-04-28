package com.ibank.axwms.domain.file.external;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    @Override
    public String upload(MultipartFile file, String key) {
        log.info("[S3-STUB] upload key={} name={}", key, file.getOriginalFilename());
        return key;
    }

    @Override
    public void delete(String key) {
        log.info("[S3-STUB] delete key={}", key);
    }

    @Override
    public void copy(String sourceKey, String targetKey) {
        log.info("[S3-STUB] copy source={} target={}", sourceKey, targetKey);
    }

    @Override
    public String toPublicUrl(String key) {
        return key;
    }

    @Override
    public List<String> listKeysUploadedBefore(String prefix, Instant cutoff) {
        return List.of();
    }
}
