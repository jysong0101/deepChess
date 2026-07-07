package com.deepchess.service_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
    public String home() {
        return "<!DOCTYPE html>" +
               "<html lang='ko'>" +
               "<head><meta charset='UTF-8'><title>DeepChess</title></head>" +
               "<body style='display:flex; justify-content:center; align-items:center; height:100vh; background-color:#2c3e50; font-family:sans-serif; margin:0;'>" +
               "<div style='text-align:center;'>" +
               "<h1 style='color:white; margin-bottom:30px;'>♟️ DeepChess Server 🟢 ON</h1>" +
               
               "<div style='display:flex; gap:20px; justify-content:center;'>" +
               // 클릭 시 localStorage에 'new' 저장 후 이동
               "<a href='#' onclick='localStorage.setItem(\"mode\", \"new\"); window.location.href=\"/dev\";' style='padding:15px 30px; background-color:#27ae60; color:white; text-decoration:none; border-radius:5px; font-weight:bold; font-size:1.2em; transition:0.3s; box-shadow:0 4px 6px rgba(0,0,0,0.3);'>" +
               "♟️ 새 게임 분석</a>" +
               
               // 클릭 시 localStorage에 'import' 저장 후 이동
               "<a href='#' onclick='localStorage.setItem(\"mode\", \"import\"); window.location.href=\"/dev\";' style='padding:15px 30px; background-color:#f39c12; color:white; text-decoration:none; border-radius:5px; font-weight:bold; font-size:1.2em; transition:0.3s; box-shadow:0 4px 6px rgba(0,0,0,0.3);'>" +
               "📁 기보 불러오기</a>" +
               "</div>" +
               
               "<p style='color:#bdc3c7; margin-top:20px; font-size:0.9em;'>(어느 버튼을 누르든 구글 로그인 후 통합 분석 보드로 이동합니다)</p>" +
               "</div></body></html>";
    }
}