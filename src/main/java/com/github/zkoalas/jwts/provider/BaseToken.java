package com.github.zkoalas.jwts.provider;


public interface BaseToken {

    /**
     * 用户id编号
     * @return
     */
    String getUserId();
    /**
     * 用户权限
     * @return
     */
     String[] getPermissions();

    /**
     *  用户角色
     * @return
     */
    String[] getRoles();
}
