package com.deepchess.service_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MainController {

    @GetMapping("/")
    public Map<String, String> serverCheck() {
        return Map.of("message", "server on");
    }
}