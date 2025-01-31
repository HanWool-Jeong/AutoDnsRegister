package com.example.AutoDnsRegister.controller;

import com.example.AutoDnsRegister.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OAuthController {

    private final TokenService tokenService;

    @Autowired
    public OAuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/OAuth")
    public String oAuth() {
        return "redirect:" + tokenService.makeGoogleOAuthURL();
    }

    @GetMapping("/redirect")
    @ResponseBody
    public String redirect(@RequestParam String code) {
        tokenService.saveAccessToken(code);
        return "리다이렉트됨~!";
    }

}
