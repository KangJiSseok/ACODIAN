package com.ibank.axwms.domain.organization.user.scheduler;

import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TempImageCleanupScheduler {

    private final ObjectStoragePort objectStoragePort;

    public TempImageCleanupScheduler(
            @Qualifier("profileImageS3ObjectStorageAdapter") ObjectStoragePort objectStoragePort
    ) {
        this.objectStoragePort = objectStoragePort;
    }

    @Scheduled(cron = "0 20 22 * * *")
    public void deleteOrphanedTempImages() {
        String prefix = "temp/";
        Instant cutoff = Instant.now().minus(0, ChronoUnit.DAYS);

        List<String> keys = objectStoragePort.listKeysUploadedBefore(prefix, cutoff);
        int deleted = 0;
        for (String key : keys) {
            try {
                objectStoragePort.delete(key);
                deleted++;
            } catch (RuntimeException e) {
                log.warn("temp 파일 삭제 실패 key={}", key, e);
            }
        }
        log.info("temp 프로필 이미지 정리 완료 deleted={} total={}", deleted, keys.size());
    }
}
