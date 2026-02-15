CREATE UNIQUE INDEX uq_enrollment_event_account_active
    ON enrollment (event_id, account_id)
    WHERE deleted_at IS NULL;
