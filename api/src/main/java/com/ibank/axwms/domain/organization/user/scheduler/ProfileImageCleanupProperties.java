package com.ibank.axwms.domain.organization.user.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.profile-image.cleanup")
public record ProfileImageCleanupProperties(
        String cron,
        String tempPrefix,
        long retentionDays
) {
}
