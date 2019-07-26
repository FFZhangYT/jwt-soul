package com.github.zkoalas.jwts.provider;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;
import com.github.zkoalas.jwts.util.TokenUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * jdbc存储token的实现
 */
@Slf4j
public class JdbcTokenStore implements TokenStore {
    private final JdbcTemplate jdbcTemplate;
    private RowMapper<Token> rowMapper = new TokenRowMapper();

    private static final String UPDATE_FIELDS = "access_token, user_id, permissions, roles, refresh_token, expire_time";

    private static final String BASE_SELECT = "select token_id, " + UPDATE_FIELDS + ", create_time, update_time from oauth_token";

    private static final String SQL_SELECT_BY_TOKEN = BASE_SELECT + " where user_id = ? and access_token = ?";

    private static final String SQL_SELECT_BY_USER_ID = BASE_SELECT + " where user_id = ? order by create_time";

    private static final String SQL_INSERT = "insert into oauth_token (" + UPDATE_FIELDS + ") values (?,?,?,?,?,?)";

    private static final String SQL_UPDATE = "update oauth_token set " + UPDATE_FIELDS.replaceAll(", ", "=?, ") + "=? where token_id = ?";

    private static final String SQL_UPDATE_PERMS = "update oauth_token set permissions = ? where user_id = ?";

    private static final String SQL_UPDATE_ROLES = "update oauth_token set roles = ? where user_id = ?";

    private static final String SQL_DELETE = "delete from oauth_token where user_id = ? and access_token = ?";

    private static final String SQL_DELETE_BY_USER_ID = "delete from oauth_token where user_id = ?";

    private static final String SQL_SELECT_KEY = "select token_key from oauth_token_key";

    private static final String SQL_INSERT_KEY = "insert into oauth_token_key (token_key) values (?)";

    public JdbcTokenStore(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource required");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public String getTokenKey() {
        String tokenKey = null;
        try {
            tokenKey = jdbcTemplate.queryForObject(SQL_SELECT_KEY, String.class);
        } catch (EmptyResultDataAccessException e) {
        }
        if (tokenKey == null || tokenKey.trim().isEmpty()) {
            tokenKey = TokenUtil.getHexKey();
            jdbcTemplate.update(SQL_INSERT_KEY, tokenKey);
        }
        return tokenKey;
    }


    public Token createNewToken(String userId, String[] permissions, String[] roles) {
        return createNewToken(userId, permissions, roles, TokenUtil.DEFAULT_EXPIRE);
    }

    public Token createNewToken(String userId, String[] permissions, String[] roles, long expire) {
        String tokenKey = getTokenKey();
        log.debug("-------------------------------------------");
        log.debug("构建token使用tokenKey：" + tokenKey);
        log.debug("-------------------------------------------");
        Token token = TokenUtil.buildToken(userId, expire, TokenUtil.parseHexKey(tokenKey));
        token.setPermissions(permissions);
        token.setRoles(roles);
        if (storeToken(token) > 0) {
            if (Config.getInstance().getMaxToken() != null && Config.getInstance().getMaxToken() != -1) {
                List<Token> userTokens = findTokensByUserId(userId);
                if (userTokens.size() > Config.getInstance().getMaxToken()) {
                    for (int i = 0; i < userTokens.size() - Config.getInstance().getMaxToken(); i++) {
                        removeToken(userId, userTokens.get(i).getAccessToken());
                    }
                }
            }
            return token;
        }
        return null;
    }

    public int storeToken(Token token) {
        List<Object> objects = getFieldsForUpdate(token);
        return jdbcTemplate.update(SQL_INSERT, listToArray(objects));
    }

    public Token findToken(String userId, String access_token) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_BY_TOKEN, rowMapper, userId, access_token);
        } catch (EmptyResultDataAccessException e) {
        }
        return null;
    }

    public List<Token> findTokensByUserId(String userId) {
        try {
            return jdbcTemplate.query(SQL_SELECT_BY_USER_ID, rowMapper, userId);
        } catch (EmptyResultDataAccessException e) {
        }
        return null;
    }

    public int removeToken(String userId, String access_token) {
        return jdbcTemplate.update(SQL_DELETE, userId, access_token);
    }

    public int removeTokensByUserId(String userId) {
        return jdbcTemplate.update(SQL_DELETE_BY_USER_ID, userId);
    }

    public int updateRolesByUserId(String userId, String[] roles) {
        Object[] objects = new Object[2];
        try {
            objects[0] = JSON.toJSONString(roles);
        } catch (Exception e) {
            e.printStackTrace();
        }
        objects[1] = userId;
        return jdbcTemplate.update(SQL_UPDATE_ROLES, objects);
    }

    public int updatePermissionsByUserId(String userId, String[] permissions) {
        Object[] objects = new Object[2];
        try {
            objects[0] = JSON.toJSONString(permissions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        objects[1] = userId;
        return jdbcTemplate.update(SQL_UPDATE_PERMS, objects);
    }

    private List<Object> getFieldsForUpdate(Token token) {
        List<Object> objects = new ArrayList();
        objects.add(token.getAccessToken());
        objects.add(token.getUserId());
        String permJson = null;
        try {
            permJson = JSON.toJSONString(token.getPermissions());
        } catch (Exception e) {
            e.printStackTrace();
        }
        objects.add(permJson);
        String roleJson = null;
        try {
            roleJson = JSON.toJSONString(token.getRoles());
        } catch (Exception e) {
            e.printStackTrace();
        }
        objects.add(roleJson);
        objects.add(token.getTokenKey());
        objects.add(token.getRefreshToken());
        objects.add(token.getExpireTime());
        return objects;
    }

    private Object[] listToArray(List<Object> list) {
        if (list == null) {
            return null;
        }
        Object[] objects = new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            objects[i] = list.get(i);
        }
        return objects;
    }


    private static class TokenRowMapper implements RowMapper<Token> {

        @Override
        public Token mapRow(ResultSet rs, int rowNum) throws SQLException {
            int token_id = rs.getInt("token_id");
            String access_token = rs.getString("access_token");
            String user_id = rs.getString("user_id");
            String permissions = rs.getString("permissions");
            String roles = rs.getString("roles");
             String token_key = rs.getString("token_key");
            String refresh_token = rs.getString("refresh_token");
            Date expire_time = rs.getDate("expire_time");
            Date create_time = rs.getDate("create_time");
            Date update_time = rs.getDate("update_time");
            Token token = new Token();
            token.setTokenId(token_id);
            token.setAccessToken(access_token);
            token.setUserId(user_id);
            token.setTokenKey(token_key);
            token.setRefreshToken(refresh_token);
            token.setExpireTime(expire_time.getTime());
            token.setCreateTime(create_time.getTime());
            token.setUpdateTime(update_time.getTime());
            if (permissions != null) {
                try {
                    token.setPermissions(listToArray(JSON.parseArray(permissions,String.class)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (roles != null) {
                try {
                    token.setRoles(listToArray(JSON.parseArray(permissions,String.class)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return token;
        }

        private String[] listToArray(List<String> list) {
            if (list == null) {
                return null;
            }
            String[] objects = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                objects[i] = list.get(i);
            }
            return objects;
        }
    }
}
