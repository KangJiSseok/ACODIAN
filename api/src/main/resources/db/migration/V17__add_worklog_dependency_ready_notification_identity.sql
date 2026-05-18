-- Flyway execution source of truth
-- Purpose: prevent duplicate ready notifications for the same parent worklog while leaving existing reminder policies unchanged.
-- Rule: schema-only migration; no seed or data backfill.

CREATE UNIQUE INDEX uq_notification_worklog_dependency_ready
    ON tb_notification (user_id, reference_type, reference_id, notification_type)
    WHERE notification_type = 'WORKLOG_DEPENDENCY_READY';
