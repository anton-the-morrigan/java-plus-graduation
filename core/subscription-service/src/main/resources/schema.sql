CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    follower_user_id BIGINT NOT NULL,
    followed_to_user_id BIGINT NOT NULL,
    CONSTRAINT uk_subscription UNIQUE (follower_user_id, followed_to_user_id)
);