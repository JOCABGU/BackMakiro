package com.example.TigoStarSystem.auth.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;

import java.time.OffsetDateTime;

public class AuthSession {
    private final String token;
    private final AuthLoginResponse usuario;
    private final OffsetDateTime expira;

    public AuthSession(String token, AuthLoginResponse usuario, OffsetDateTime expira) {
        this.token = token;
        this.usuario = usuario;
        this.expira = expira;
    }

    public String getToken() {
        return token;
    }

    public AuthLoginResponse getUsuario() {
        return usuario;
    }

    public OffsetDateTime getExpira() {
        return expira;
    }
}
