package com.example.AutoDnsRegister;

import com.example.AutoDnsRegister.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


@SpringBootTest
public class URLEncodingTest {

    @Autowired
    private TokenService tokenService;

    private static String encodeParam(Map<String, String> params) {
        StringBuilder paramBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                String encodedKey = URLEncoder.encode(entry.getKey(), "UTF-8");
                String encodedValue = URLEncoder.encode(entry.getValue(), "UTF-8");
                paramBuilder.append(encodedKey).append("=").append(encodedValue).append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        paramBuilder.deleteCharAt(paramBuilder.length() - 1);
        return paramBuilder.toString();
    }

     @Test
     void testParamEncodingPrint() {
         Map<String, String> params = new HashMap<>();
         params.put("client_id", "1234");
         params.put("client_secret", "abcd");
         params.put("code", "yeah~");
         params.put("redirect_uri", "http://localhost:8080/redirect");
         params.put("grant_type", "authorization_code");
         System.out.println(encodeParam(params));
     }

    @Test
    void testURLEncodingPrint() {
        System.out.println(tokenService.makeGoogleOAuthURL());
    }

}
