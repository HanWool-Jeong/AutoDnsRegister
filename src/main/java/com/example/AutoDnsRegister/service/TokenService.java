package com.example.AutoDnsRegister.service;

public interface TokenService {

    public String makeGoogleOAuthURL();

    public void saveAccessToken(String code);

}
