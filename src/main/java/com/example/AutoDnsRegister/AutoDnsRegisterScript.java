package com.example.AutoDnsRegister;

import com.example.AutoDnsRegister.data.TokenJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class AutoDnsRegisterScript {

    private static TokenJDBC tokenJDBC;

    private static String previousIp, currentIp, cloudflareToken, zoneId;
    private static String clientId, clientSecret, googleRefreshToken, googleAccessToken;

    private static final String[] targetDnsRecords = {"hanwool.cc", "www.hanwool.cc"};
    private static ArrayList<JSONObject> targetDnsObject;

    private static void checkInternetAndIp() throws Exception {
        try {
            currentIp = WebClient.create("https://ifconfig.me/ip")
                    .get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.out.println("checkInternetAndIp");
            throw e;
        }
    }

    private static void checkIfCloudflareTokenValid() throws Exception {
        try {
            tokenJDBC = new TokenJDBC();
            cloudflareToken = tokenJDBC.getCloudflareToken();
            WebClient.create("https://api.cloudflare.com/client/v4/user/tokens/verify")
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cloudflareToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.out.println("checkIfCloudflareTokenValid");
            throw e;
        }
    }

    private static void getDnsZoneId() throws Exception {
        try {
            String result = WebClient.create("https://api.cloudflare.com/client/v4/zones")
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cloudflareToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            zoneId = jsonArray.getJSONObject(0).getString("id");
        } catch (Exception e) {
            System.out.println("getDnsZoneId");
            throw e;
        }
    }

    private static void getDnsTargetRecords() throws Exception {
        try {
            String result = WebClient.create(String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records", zoneId))
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cloudflareToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            targetDnsObject = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String name = jsonArray.getJSONObject(i).getString("name");
                if (Arrays.asList(targetDnsRecords).contains(name)) {
                    targetDnsObject.add(jsonArray.getJSONObject(i));
                }
            }
        } catch (Exception e) {
            System.out.println("getDnsTargetRecords");
            throw e;
        }
    }

    private static boolean isIpChanged() {
        previousIp = targetDnsObject.get(0).getString("content");
        return previousIp.equals(currentIp);
    }

    private static void updateDns() throws Exception {
        try {
            targetDnsObject.parallelStream().forEach(jsonObject -> {
                WebClient.create(String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s",
                                zoneId, jsonObject.getString("id")))
                        .method(HttpMethod.PATCH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cloudflareToken)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .bodyValue(String.format("{ \"content\": \"%s\"}", currentIp))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            });
        } catch (Exception e) {
            System.out.println("updateDns");
            throw e;
        }
    }

    private static void refreshGoogleAccessToken() throws Exception {
        try {
            CompletableFuture<Void> clientIdFuture = CompletableFuture.runAsync(() -> {
                try {
                    clientId = tokenJDBC.getGoogleClientId();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            CompletableFuture<Void> clientSecretFuture = CompletableFuture.runAsync(() -> {
                try {
                    clientSecret = tokenJDBC.getGoogleClientSecret();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            CompletableFuture<Void> googleRefreshTokenFuture = CompletableFuture.runAsync(() -> {
                try {
                    googleRefreshToken = tokenJDBC.getGoogleRefreshToken();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            CompletableFuture.allOf(clientIdFuture, clientSecretFuture, googleRefreshTokenFuture).join();

            String formBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&grant_type=" + "refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(googleRefreshToken, StandardCharsets.UTF_8);

            String result = WebClient.create("https://oauth2.googleapis.com/token")
                    .post()
                    .header(HttpHeaders.CONTENT_TYPE,"application/x-www-form-urlencoded")
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONObject jsonObject = new JSONObject(result);
            googleAccessToken = jsonObject.getString("access_token");
            tokenJDBC.updateGoogleAccessToken(googleAccessToken);
        } catch (Exception e) {
            System.out.println("refreshGoogleApiAccessToken");
            throw e;
        }
    }

    private static void sendEmail() throws Exception {
        try {
            String body = String.format("이전: %s<br>현재: %s", previousIp, currentIp);
            String to = "soink366739@gmail.com";
            byte[] encodedBytes = Base64.getEncoder().encode("아이피 변경 알림".getBytes(StandardCharsets.UTF_8));
            String subject = new String(encodedBytes, StandardCharsets.US_ASCII);
            String emailContent = "Content-Type: text/html; charset=UTF-8\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "to: " + to + "\r\n" +
                    "subject: =?UTF-8?B?" + subject + "?=\r\n" +
                    "\r\n" + body;
            String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(emailContent.getBytes(StandardCharsets.UTF_8));

            WebClient.create("https://www.googleapis.com/gmail/v1/users/me/messages/send")
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessToken)
                    .bodyValue(String.format("{\"raw\":\"%s\"}", encodedEmail))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.out.println("sendEmail");
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            checkInternetAndIp();
            checkIfCloudflareTokenValid();
            getDnsZoneId();
            getDnsTargetRecords();
            if (!isIpChanged()) {
                updateDns();
                refreshGoogleAccessToken();
                sendEmail();
                System.out.println("아이피 변경됨 " + previousIp + " => " + currentIp);
            } else {
                System.out.println("아이피 안 바뀜");
            }
            tokenJDBC.close();
        } catch (Exception e) {
            System.out.println("에러발생!");
            e.printStackTrace();
        }
    }

}
