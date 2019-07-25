package org.yt.jwts.provider;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Token implements Serializable {

    private String accessToken; //token

    private String tokenKey; //生成token时的key

    private Integer tokenId;  // 自增主键

    private String userId;  // 用户id

    private String[] permissions;  // 用户权限

    private String[] roles;  // 用户角色

    private String refreshToken;  // 暂时没用

    private Long expireTime;  // 过期时间

    private Long createTime;

    private Long updateTime;
}
