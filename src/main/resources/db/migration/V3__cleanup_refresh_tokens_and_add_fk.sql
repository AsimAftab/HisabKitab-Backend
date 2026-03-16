-- Remove orphaned refresh tokens before enforcing the foreign key.
DELETE FROM refresh_tokens rt
WHERE NOT EXISTS (
    SELECT 1
    FROM users u
    WHERE u.id = rt.user_id
);

-- Add the FK only when it does not already exist.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_refresh_tokens_user_id'
    ) THEN
        ALTER TABLE refresh_tokens
            ADD CONSTRAINT fk_refresh_tokens_user_id
            FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;
    END IF;
END $$;
