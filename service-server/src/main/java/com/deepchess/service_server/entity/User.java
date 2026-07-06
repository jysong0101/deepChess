package com.deepchess.service_server.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "google_uid", nullable = false, unique = true)
    private String googleUid;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Builder
    public User(String email, String googleUid, String nickname, String profileImage) {
        this.email = email;
        this.googleUid = googleUid;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }
}