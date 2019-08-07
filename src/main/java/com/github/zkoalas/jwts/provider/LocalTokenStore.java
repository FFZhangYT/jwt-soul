package com.github.zkoalas.jwts.provider;


import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import com.github.zkoalas.jwts.util.TokenUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
public class LocalTokenStore implements BaseTokenStore {

    private String secretKey;
    private String md5Key;
    private Long expiration;


    public LocalTokenStore(String secretKey, String md5Key, Long expiration) {
        this.secretKey = secretKey;
        this.md5Key = md5Key;
        this.expiration = expiration;
    }

    @Override
    public String getTokenKey() {
        return TokenUtil.getHexKey(Keys.hmacShaKeyFor(secretKey.getBytes()));
    }


    @Override
    public Token findToken(String subject, String access_token) {
        Token token = JSON.parseObject(subject, Token.class);
        token.setAccessToken(access_token);
        return token;
    }

    public <S extends BaseToken> Token createNewToken(S obj) {
        String subject = JSON.toJSONString(obj);
        String tokenKey = getTokenKey();
        log.debug("-------------------------------------------");
        log.debug("构建token使用tokenKey：" + tokenKey);
        log.debug("-------------------------------------------");
        Map<String, Object> claims = new HashMap();
        claims.put(md5Key, getRandomString(6));
        Token token = TokenUtil.toBuildToken(claims,subject, expiration, TokenUtil.parseHexKey(tokenKey));
        return token;
    }


    public static String getRandomString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
}
