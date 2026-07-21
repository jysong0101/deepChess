package com.deepchess.service_server.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.UserRepository;

import lombok.RequiredArgsConstructor;
// ... 상단 import 생략 ...

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String googleUid = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String nickname = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture");

        // DB에 없으면 신규 가입(저장), 있으면 기존 정보 조회
        User user = userRepository.findByGoogleUid(googleUid)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .googleUid(googleUid)
                                .nickname(nickname)
                                .profileImage(profileImage)
                                .isProfileSet(false) // 💡 신규 유저는 프로필 설정 '미완료' 상태로 생성
                                .build()
                ));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"
        );
    }
}