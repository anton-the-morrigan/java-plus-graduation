package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.entity.Subscription;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    void deleteByFollower_IdIsAndFollowedTo_Id(Long followerUserId, Long followedToUserId);

    @Query("""
            select followedTo.id
            from Subscribe
            where follower.id = ?1""")
    List<Long> findFollowedUsersIds(Long followerUserId);
}
