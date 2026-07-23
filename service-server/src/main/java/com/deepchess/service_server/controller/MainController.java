package com.deepchess.service_server.controller;

import java.io.IOException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deepchess.service_server.dto.request.NicknameRequest;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor // 💡 UserRepository 주입을 위해 추가
public class MainController {

    private final UserRepository userRepository;

    @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
    public String home(@AuthenticationPrincipal OAuth2User oAuth2User) {
        boolean isLoggedIn = (oAuth2User != null);
        User user = null;

        if (isLoggedIn) {
            String googleUid = oAuth2User.getAttribute("sub");
            user = userRepository.findByGoogleUid(googleUid).orElse(null);
            
            // 💡 [핵심 로직] 로그인했지만 프로필 설정이 끝나지 않은 신규 유저라면? -> 닉네임 설정창 반환!
            if (user != null && !user.isProfileSet()) {
                return renderSignupPage(user);
            }
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html lang='ko'>")
            .append("<head><meta charset='UTF-8'><title>DeepChess</title></head>")
            .append("<body style='display:flex; justify-content:center; align-items:center; height:100vh; background-color:#2c3e50; font-family:\"Pretendard\", -apple-system, sans-serif; margin:0;'>")
            .append("<div style='text-align:center; background:#34495e; padding:50px 40px; border-radius:16px; box-shadow:0 10px 25px rgba(0,0,0,0.3); width:380px;'>");

        html.append("<h1 style='color:white; margin-top:0; margin-bottom:15px; font-size: 2.8em; letter-spacing: -1px;'>♟️ DeepChess</h1>");

        if (!isLoggedIn) {
            // [상태 1] 비로그인
            html.append("<p style='color:#bdc3c7; margin-bottom:40px; font-size:1.1em; line-height:1.6;'>AI 기반 체스 기보 분석 및<br>시뮬레이션 플랫폼입니다.</p>")
                .append("<a href='/oauth2/authorization/google' style='display:flex; justify-content:center; align-items:center; padding:15px; background-color:#ffffff; color:#333; text-decoration:none; border-radius:8px; font-weight:bold; font-size:1.1em; transition:0.2s; box-shadow:0 4px 6px rgba(0,0,0,0.1); width:100%; box-sizing:border-box;'>")
                .append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 48' width='24px' height='24px' style='margin-right:12px;'>")
                .append("<path fill='#EA4335' d='M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.7 17.74 9.5 24 9.5z'/>")
                .append("<path fill='#4285F4' d='M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z'/>")
                .append("<path fill='#FBBC05' d='M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z'/>")
                .append("<path fill='#34A853' d='M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z'/>")
                .append("<path fill='none' d='M0 0h48v48H0z'/></svg>")
                .append("구글 계정으로 로그인</a>");
        } else {
            // [상태 2] 로그인 됨 (프로필 설정 완료된 유저)
            html.append("<p style='color:#2ecc71; margin-bottom:30px; font-size:1.15em;'>반갑습니다, <strong>").append(user.getNickname()).append("</strong>님!</p>")
                .append("<div style='display:flex; flex-direction:column; gap:15px;'>")
                .append("<a href='#' onclick='localStorage.setItem(\"mode\", \"new\"); window.location.href=\"/board.html\";' style='display:block; padding:15px; background-color:#27ae60; color:white; text-decoration:none; border-radius:8px; font-weight:bold; font-size:1.2em; transition:0.2s; box-shadow:0 4px 6px rgba(0,0,0,0.2);'>")
                .append("♟️ 새 게임 분석</a>")
                .append("<a href='#' onclick='localStorage.setItem(\"mode\", \"import\"); window.location.href=\"/board.html\";' style='display:block; padding:15px; background-color:#f39c12; color:white; text-decoration:none; border-radius:8px; font-weight:bold; font-size:1.2em; transition:0.2s; box-shadow:0 4px 6px rgba(0,0,0,0.2);'>")
                .append("📁 기보 불러오기</a>")
                // 💡 추가된 마이페이지(보관함) 이동 버튼 (파란색 계열 적용)
                .append("<a href='/mypage.html' style='display:block; padding:15px; background-color:#2980b9; color:white; text-decoration:none; border-radius:8px; font-weight:bold; font-size:1.2em; transition:0.2s; box-shadow:0 4px 6px rgba(0,0,0,0.2);'>")
                .append("🗄️ 내 기보 보관함</a>")
                .append("</div>")
                .append("<div style='margin-top: 30px; padding-top: 20px; border-top: 1px solid #4a6278;'>")
                .append("<a href='/logout' style='display:inline-block; padding:12px; background-color:#7f8c8d; color:white; text-decoration:none; border-radius:8px; font-weight:bold; font-size:1em; transition:0.2s; width:100%; box-sizing:border-box;'>")
                .append("🚪 로그아웃</a>")
                .append("</div>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    // 💡 신규 유저에게 보여줄 닉네임 설정 폼 (HTML)
    private String renderSignupPage(User user) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html lang='ko'>")
            .append("<head><meta charset='UTF-8'><title>DeepChess - 프로필 설정</title></head>")
            .append("<body style='display:flex; justify-content:center; align-items:center; height:100vh; background-color:#2c3e50; font-family:\"Pretendard\", -apple-system, sans-serif; margin:0;'>")
            .append("<div style='text-align:center; background:#34495e; padding:50px 40px; border-radius:16px; box-shadow:0 10px 25px rgba(0,0,0,0.3); width:380px;'>")
            
            .append("<h1 style='color:white; margin-top:0; margin-bottom:15px; font-size: 2.5em; letter-spacing: -1px;'>♟️ 환영합니다!</h1>")
            .append("<p style='color:#bdc3c7; margin-bottom:30px; font-size:1.05em; line-height:1.6;'>DeepChess에서 사용할<br>멋진 닉네임을 설정해 주세요.</p>")
            
            // 폼 데이터를 /api/users/nickname 으로 POST 전송
            .append("<form action='/api/users/nickname' method='POST' style='display:flex; flex-direction:column; gap:15px;'>")
            // 기본값으로 구글에서 가져온 이름을 띄워줌
            .append("<input type='text' name='nickname' value='").append(user.getNickname()).append("' placeholder='닉네임 입력' style='padding:15px; border-radius:8px; border:none; outline:none; font-size:1.1em; text-align:center; font-weight:bold; color:#2c3e50;' required>")
            .append("<button type='submit' style='padding:15px; background-color:#3498db; color:white; border:none; border-radius:8px; font-weight:bold; font-size:1.2em; cursor:pointer; transition:0.2s; box-shadow:0 4px 6px rgba(0,0,0,0.2);'>")
            .append("🚀 시작하기</button>")
            .append("</form>")
            
            .append("</div></body></html>");
        return html.toString();
    }

    // 💡 닉네임 설정 폼 제출 시 닉네임을 DB에 업데이트하는 API
    @PostMapping("/api/users/nickname")
    public void setNickname(@AuthenticationPrincipal OAuth2User oAuth2User, 
                            @ModelAttribute NicknameRequest request,
                            HttpServletResponse response) throws IOException {
        if (oAuth2User != null && request.hasNickname()) {
            String googleUid = oAuth2User.getAttribute("sub");
            User user = userRepository.findByGoogleUid(googleUid).orElse(null);
            
            if (user != null) {
                user.updateNicknameAndCompleteProfile(request.trimmedNickname());
                userRepository.save(user);
            }
        }
        // 프로필 업데이트 완료 후 메인 메뉴('/')로 새로고침
        response.sendRedirect("/");
    }
}
