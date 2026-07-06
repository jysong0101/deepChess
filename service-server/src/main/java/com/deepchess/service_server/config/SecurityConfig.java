package com.deepchess.service_server.config;

import com.deepchess.service_server.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 테스트 편의를 위해 CSRF 보안 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            
            // H2 콘솔이 iframe을 사용하므로 화면이 깨지지 않도록 설정
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            
            // 접근 권한 제어
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/h2-console/**", "/login/**", "/error").permitAll() // 누구나 접근 가능
                .anyRequest().authenticated() // 그 외 모든 요청은 로그인이 필요함
            )
            
            // OAuth2 로그인 기능 활성화 및 서비스 연결
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/", true) // 로그인 성공 시 메인 화면으로 리다이렉트
            );

        return http.build();
    }
}