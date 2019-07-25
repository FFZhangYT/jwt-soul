package org.yt.jwts.provider;


import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yt.jwts.util.TokenUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LocalTokenStore {

    private String secretKey;
    private String md5Key;
    private Long expiration;


    protected final Log logger = LogFactory.getLog(this.getClass());
    public LocalTokenStore(String secretKey, String md5Key, Long expiration) {
        this.secretKey = secretKey;
        this.md5Key = md5Key;
        this.expiration = expiration;
    }

    public String getTokenKey() {
        return TokenUtil.getHexKey(Keys.hmacShaKeyFor(secretKey.getBytes()));
    }

    public Token createNewToken(String subject) {
        String tokenKey = getTokenKey();
        logger.debug("-------------------------------------------");
        logger.debug("构建token使用tokenKey：" + tokenKey);
        logger.debug("-------------------------------------------");
        Map<String, Object> claims = new HashMap();
        claims.put(md5Key, getRandomString(6));
        Token token = TokenUtil.toBuildToken(claims,subject, expiration, TokenUtil.parseHexKey(tokenKey));
        return token;
    }

    public <T> T parseToken(String accessToken,Class<T> clazz) throws IOException {
        String clazzJson = TokenUtil.parseToken(accessToken, getTokenKey());
        return JSON.parseObject(clazzJson,clazz);
    }


    public String parseToken(String accessToken){
        return TokenUtil.parseToken(accessToken, getTokenKey());
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
