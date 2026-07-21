package com.deepchess.service_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

import com.deepchess.service_server.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    // 💡 추가됨: OAuth2 요청 URL을 커스텀하기 위해 필요한 빈(Bean)
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/h2-console/**", "/login/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                // 💡 추가된 부분: 구글 로그인 창으로 넘어갈 때 우리가 만든 커스텀 리졸버를 타도록 설정
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestResolver(customAuthorizationRequestResolver())
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            );

        return http.build();
    }

    // 💡 계정 선택창을 강제로 띄우기 위한 커스텀 리졸버 메서드
    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        // 구글에 요청을 보낼 때 'prompt=select_account' 파라미터를 강제로 끼워 넣습니다.
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> params.put("prompt", "select_account")));

        return resolver;
    }
}