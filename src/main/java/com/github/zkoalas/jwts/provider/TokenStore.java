package com.github.zkoalas.jwts.provider;

import java.util.List;

/**
 * 操作token的接口
 */
public interface TokenStore extends BaseTokenStore{

    Token createNewToken(String userId, String[] permissions, String[] roles);

    Token createNewToken(String userId, String[] permissions, String[] roles, long expire);

    int storeToken(Token token);

    List<Token> findTokensByUserId(String userId);

    int removeToken(String userId, String access_token);

    int removeTokensByUserId(String userId);

    int updateRolesByUserId(String userId, String[] roles);

    int updatePermissionsByUserId(String userId, String[] permissions);
}
