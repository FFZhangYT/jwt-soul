package com.github.zkoalas.jwts.util;

import com.github.zkoalas.jwts.provider.Token;
import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.ObjectUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * Token工具类
 */
public class TokenUtil {
    public static final long DEFAULT_EXPIRE = 60 * 60 * 24;  // 默认过期时长,单位秒

    /**
     * 生成token
     */
    public static Token buildToken(String subject) {
        return buildToken(subject, DEFAULT_EXPIRE);
    }

    public static Token buildToken(String subject, long expire) {
        return buildToken(subject, expire, getKey());
    }

    public static Token buildToken(String subject, long expire, Key key) {
        Date expireDate = new Date(new Date().getTime() + 1000 * expire);  // 单位毫秒
        String access_token = toBuildAccessToken(null, subject, expireDate, key);
        Token token = new Token();
        token.setTokenKey(Hex.encodeToString(key.getEncoded()));
        token.setAccessToken(access_token);
        token.setUserId(subject);
        token.setExpireTime(expireDate.getTime());
        return token;
    }

    public static Token toBuildToken(Map<String, Object> claims, String subject, long expire, Key key) {
        Date expireDate = new Date(new Date().getTime() + 1000 * expire);  // 单位毫秒
        String access_token = toBuildAccessToken(claims, subject, expireDate, key);
        Token token = new Token();
        token.setTokenKey(Hex.encodeToString(key.getEncoded()));
        token.setAccessToken(access_token);
        token.setExpireTime(expireDate.getTime());
        return token;
    }


    public static String toBuildAccessToken(Map<String, Object> claims, String subject, Date expireDate, Key key) {
        if(ObjectUtils.isEmpty(claims)){
            claims = Maps.newHashMap();
        }
        String access_token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .signWith(key)
                .setExpiration(expireDate)
                .compact();
        return access_token;
    }
    /**
     * 解析token
     */
    public static String parseToken(String token, String hexKey) {
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(parseHexKey(hexKey)).parseClaimsJws(token);
        return claimsJws.getBody().getSubject();
    }

    /**
     * 生成Key
     */
    public static Key getKey() {
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    /**
     * 生成16进制的key
     */
    public static String getHexKey() {
        return getHexKey(getKey());
    }

    public static String getHexKey(Key key) {
        return Hex.encodeToString(key.getEncoded());
    }

    /**
     * 把16进制的key转成Key
     */
    public static Key parseHexKey(String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            return null;
        }
        SecretKey key = Keys.hmacShaKeyFor(Hex.decode(hexKey));
        return key;
    }

}
