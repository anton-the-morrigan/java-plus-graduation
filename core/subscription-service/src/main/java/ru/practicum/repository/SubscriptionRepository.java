package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.entity.Subscription;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    void deleteByFollowerAndFollowedTo(Long followerUserId, Long followedToUserId);

    @Query("""
            select followedTo
            from Subscription
            where follower = ?1""")
    List<Long> findFollowedUsersIds(Long followerUserId);
}
