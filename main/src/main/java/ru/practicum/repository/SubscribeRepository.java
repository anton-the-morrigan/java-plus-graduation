package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.entity.Subscribe;

import java.util.List;

public interface SubscribeRepository extends JpaRepository<Subscribe, Long> {
    void deleteByFollower_IdIsAndFollowedTo_Id(Long followerUserId, Long followedToUserId);

    //List<Subscribe> findSubscribesByFollower_IdIs(Long followerId);

    @Query("""
            select followedTo.id
            from Subscribe
            where follower.id = ?1""")
    List<Long> findFollowedUsersIds(Long followerUserId);
}
