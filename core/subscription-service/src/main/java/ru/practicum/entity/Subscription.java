package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "subscribes")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_user_id")
    Long follower;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followed_to_user_id")
    Long followedTo;

    public Subscription(Long follower, Long followedTo) {
        this.follower = follower;
        this.followedTo = followedTo;
    }
}
