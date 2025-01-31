package com.example.AutoDnsRegister.service.impl;

import com.example.AutoDnsRegister.data.TokenJDBC;
import com.example.AutoDnsRegister.service.TokenService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenServiceImpl implements TokenService {

    private final TokenJDBC tokenJDBC;
    private final String clientId, clientSecret;
    private final String redirectUrl =  "http://localhost:8080/redirect";
    private final WebClient webClient;

    @Autowired
    public TokenServiceImpl(TokenJDBC tokenJDBC, WebClient.Builder webClientBuilder) throws Exception {
        this.tokenJDBC = tokenJDBC;
        this.clientId = this.tokenJDBC.getGoogleClientId();
        this.clientSecret = this.tokenJDBC.getGoogleClientSecret();
        this.webClient = webClientBuilder.baseUrl("https://oauth2.googleapis.com/token").build();
    }

    private static String encodeParam(Map<String, String> params) {
        StringBuilder paramBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            paramBuilder.append(encodedKey).append("=").append(encodedValue).append("&");
        }

        paramBuilder.deleteCharAt(paramBuilder.length() - 1);
        return paramBuilder.toString();
    }

    @Override
    public String makeGoogleOAuthURL() {
        String oAuthEndpoint = "https://accounts.google.com/o/oauth2/v2/auth";
        Map<String, String> params = new HashMap<>();
        params.put("client_id", this.clientId);
        params.put("redirect_uri", redirectUrl);
        params.put("response_type", "code");
        params.put("scope", "https://www.googleapis.com/auth/gmail.send");
        params.put("access_type", "offline");
        return oAuthEndpoint + "?" + encodeParam(params);
    }

    @Override
    @Transactional
    public void saveAccessToken(String code) {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", this.clientId);
        params.put("client_secret", this.clientSecret);
        params.put("code", code);
        params.put("redirect_uri", this.redirectUrl);
        params.put("grant_type", "authorization_code");

        Mono<String> responseStr = webClient.post()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(encodeParam(params))
                .retrieve()
                .bodyToMono(String.class);

        responseStr.subscribe(jsonStr -> {
            JSONObject jsonObject = new JSONObject(jsonStr);
            try {
                tokenJDBC.updateGoogleAccessToken(jsonObject.getString("access_token"));
                tokenJDBC.updateGoogleRefreshToken(jsonObject.getString("refresh_token"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, error -> {
            System.out.println("에러발생");
            error.printStackTrace();
        });
    }
}
