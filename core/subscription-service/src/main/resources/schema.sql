-- =============================================================
-- Подписки пользователей на других пользователей:
--   - follower_user_id - id пользователя, кто подписался
--   - followed_to_user_id - id пользователя, на кого подписался
-- =============================================================
CREATE TABLE IF NOT EXISTS subscribes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    follower_user_id BIGINT NOT NULL,
    followed_to_user_id BIGINT NOT NULL,
    FOREIGN KEY (follower_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (followed_to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_subscription UNIQUE (follower_user_id, followed_to_user_id)
);