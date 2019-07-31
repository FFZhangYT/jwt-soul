package com.github.zkoalas.jwts.provider;


/**
 * 所有store的最高父接口
 */
public interface BaseTokenStore {

    String getTokenKey();

    Token findToken(String subject, String access_token);
}
