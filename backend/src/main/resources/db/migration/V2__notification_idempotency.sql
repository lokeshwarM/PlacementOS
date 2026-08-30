ALTER TABLE notifications ADD CONSTRAINT uq_notifications_idempotency UNIQUE (student_id, placement_drive_id, notification_type, channel);
