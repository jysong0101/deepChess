package com.deepchess.service_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevController {

    @GetMapping("/dev")
    public String devPage() {
        // 간단한 인라인 HTML과 CSS로 화면 가운데에 큼직한 구글 로그인 버튼을 그립니다.
        return "<!DOCTYPE html>" +
               "<html lang='ko'>" +
               "<head><meta charset='UTF-8'><title>DeepChess Dev</title></head>" +
               "<body style='display:flex; justify-content:center; align-items:center; height:100vh; background-color:#f4f7f6; font-family:sans-serif; margin:0;'>" +
               "<div style='text-align:center; background:white; padding:50px; border-radius:10px; box-shadow:0 4px 6px rgba(0,0,0,0.1);'>" +
               "<h2 style='margin-bottom:30px; color:#333;'>🛠️ 개발자 테스트 환경</h2>" +
               "<a href='/oauth2/authorization/google' style='padding:15px 30px; background-color:#4285F4; color:white; text-decoration:none; border-radius:5px; font-weight:bold; font-size:1.1em; transition:0.3s;'>" +
               "구글 계정으로 로그인</a>" +
               "</div></body></html>";
    }
}