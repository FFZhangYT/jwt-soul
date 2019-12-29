package com.github.zkoalas.jwts.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.github.zkoalas.jwts.util.TokenUtil;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * redis存储token的实现
 */
@Slf4j
public class RedisTokenStore implements TokenStore {
    private static final String KEY_TOKEN_KEY = "oauth_token_key";
    private static final String KEY_PRE_TOKEN = "oauth_token:";
    private static final String KEY_PRE_PERM = "oauth_prem:";
    private static final String KEY_PRE_ROLE = "oauth_role:";
    private static final String KEY_PRE_ROLE_IDS = "oauth_role_ids:";

    private StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getTokenKey() {
        String tokenKey = redisTemplate.opsForValue().get(KEY_TOKEN_KEY);
        if (tokenKey == null || tokenKey.trim().isEmpty()) {
            tokenKey = TokenUtil.getHexKey();
            redisTemplate.opsForValue().set(KEY_TOKEN_KEY, tokenKey);
        }
        return tokenKey;
    }



    public Token createNewToken(String userId, String[] permissions, String[] roles, String[] roleIds) {
        return createNewToken(userId, permissions, roles, roleIds , TokenUtil.DEFAULT_EXPIRE);
    }

    public Token createNewToken(String userId, String[] permissions, String[] roles, String[] roleIds, long expire) {
        String tokenKey = getTokenKey();
        log.debug("-------------------------------------------");
        log.debug("构建token使用tokenKey：" + tokenKey);
        log.debug("-------------------------------------------");
        Token token = TokenUtil.buildToken(userId, expire, TokenUtil.parseHexKey(tokenKey));
        token.setPermissions(permissions);
        token.setRoles(roles);
        token.setRoleIds(roleIds);
        if (storeToken(token) > 0) {
            if (null != Config.getInstance().getMaxToken() && -1 != Config.getInstance().getMaxToken() ) {
                Long userTokenSize = redisTemplate.opsForList().size(KEY_PRE_TOKEN + userId);
                if (userTokenSize > Config.getInstance().getMaxToken()) {
                    for (int i = 0; i < userTokenSize - Config.getInstance().getMaxToken(); i++) {
                        redisTemplate.opsForList().leftPop(KEY_PRE_TOKEN + userId);
                    }
                }
            }
            return token;
        }
        return null;
    }

    public int storeToken(Token token) {
        // 存储access_token
        redisTemplate.opsForList().rightPush(KEY_PRE_TOKEN + token.getUserId(), token.getAccessToken());
        // 存储权限
        if(!ObjectUtils.isEmpty(token.getPermissions())) {
            String permKey = KEY_PRE_PERM + token.getUserId();
            redisTemplate.delete(permKey);
            redisTemplate.opsForSet().add(permKey, token.getPermissions());
        }
        // 存储角色
        if(!ObjectUtils.isEmpty(token.getRoles())) {
            String roleKey = KEY_PRE_ROLE + token.getUserId();
            redisTemplate.delete(roleKey);
            redisTemplate.opsForSet().add(roleKey, token.getRoles());
        }
        //存储角色id
        if(!ObjectUtils.isEmpty(token.getRoleIds())) {
            String roleIdsKey = KEY_PRE_ROLE_IDS + token.getUserId();
            redisTemplate.delete(roleIdsKey);
            redisTemplate.opsForSet().add(roleIdsKey, token.getRoleIds());
        }
        return 1;
    }

    @Override
    public Token findToken(String userId, String access_token) {
        if (userId != null && !userId.trim().isEmpty()) {
            List<String> accessTokens = redisTemplate.opsForList().range(KEY_PRE_TOKEN + userId, 0, -1);
            for (int i = 0; i < accessTokens.size(); i++) {
                if (accessTokens.get(i).equals(access_token)) {
                    Token token = new Token();
                    token.setUserId(userId);
                    token.setAccessToken(access_token);
                    token.setPermissions(setToArray(redisTemplate.opsForSet().members(KEY_PRE_PERM + userId)));
                    token.setRoles(setToArray(redisTemplate.opsForSet().members(KEY_PRE_ROLE + userId)));
                    token.setRoleIds(setToArray(redisTemplate.opsForSet().members(KEY_PRE_ROLE_IDS + userId)));
                    return token;
                }
            }
        }
        return null;
    }

    public List<Token> findTokensByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        List<Token> tokens = new ArrayList<Token>();
        List<String> accessTokens = redisTemplate.opsForList().range(KEY_PRE_TOKEN + userId, 0, -1);
        if (accessTokens != null || accessTokens.size() > 0) {
            String[] perms = setToArray(redisTemplate.opsForSet().members(KEY_PRE_PERM + userId));
            String[] roles = setToArray(redisTemplate.opsForSet().members(KEY_PRE_ROLE + userId));
            String[] roleIds = setToArray(redisTemplate.opsForSet().members(KEY_PRE_ROLE_IDS + userId));
            for (int i = 0; i < accessTokens.size(); i++) {
                Token token = new Token();
                token.setAccessToken(accessTokens.get(i));
                token.setUserId(userId);
                token.setPermissions(perms);
                token.setRoles(roles);
                token.setRoleIds(roleIds);
                tokens.add(token);
            }
        }
        return tokens;
    }

    public int removeToken(String userId, String access_token) {
        redisTemplate.opsForList().remove(KEY_PRE_TOKEN + userId, 0, access_token);
        return 1;
    }

    public int removeTokensByUserId(String userId) {
        redisTemplate.delete(KEY_PRE_TOKEN + userId);
        return 1;
    }

    public int updateRolesByUserId(String userId, String[] roles) {
        String roleKey = KEY_PRE_ROLE + userId;
        redisTemplate.delete(roleKey);
        redisTemplate.opsForSet().add(roleKey, roles);
        return 1;
    }

    @Override
    public int updateRoleIdsByUserId(String userId, String[] roleIds) {
        String roleKey = KEY_PRE_ROLE_IDS + userId;
        redisTemplate.delete(roleKey);
        redisTemplate.opsForSet().add(roleKey, roleIds);
        return 1;
    }

    public int updatePermissionsByUserId(String userId, String[] permissions) {
        String permKey = KEY_PRE_PERM + userId;
        redisTemplate.delete(permKey);
        redisTemplate.opsForSet().add(permKey, permissions);
        return 1;
    }

    private String[] setToArray(Set<String> set) {
        if (set == null) {
            return null;
        }
        return set.toArray(new String[set.size()]);
    }
}
