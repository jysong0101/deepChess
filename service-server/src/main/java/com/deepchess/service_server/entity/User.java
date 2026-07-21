package com.deepchess.service_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    // 💡 추가: 닉네임 설정 등 초기 프로필 설정이 완료되었는지 확인하는 플래그
    @Column(name = "is_profile_set", nullable = false)
    private boolean isProfileSet;

    @Builder
    public User(String email, String googleUid, String nickname, String profileImage, boolean isProfileSet) {
        this.email = email;
        this.googleUid = googleUid;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.isProfileSet = isProfileSet;
    }

    // 💡 추가: 닉네임을 변경하고 프로필 설정을 완료 처리하는 편의 메서드
    public void updateNicknameAndCompleteProfile(String newNickname) {
        this.nickname = newNickname;
        this.isProfileSet = true;
    }
}