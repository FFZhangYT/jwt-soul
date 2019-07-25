package org.yt.jwts;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.yt.jwts.annotation.Logical;
import org.yt.jwts.annotation.RequiresPermissions;
import org.yt.jwts.annotation.RequiresRoles;
import org.yt.jwts.exception.ErrorTokenException;
import org.yt.jwts.exception.ExpiredTokenException;
import org.yt.jwts.exception.UnauthorizedException;
import org.yt.jwts.provider.Config;
import org.yt.jwts.provider.LocalTokenStore;
import org.yt.jwts.provider.Token;
import org.yt.jwts.provider.TokenStore;
import org.yt.jwts.util.SubjectUtil;
import org.yt.jwts.util.TokenUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.math.BigInteger;

/**
 * 拦截器
 */
@Slf4j
public class TokenInterceptor extends HandlerInterceptorAdapter {

    private Integer storeType;
    private TokenStore tokenStore;
    private LocalTokenStore localTokenStore;

    public TokenInterceptor(TokenStore tokenStore, Integer maxToken,Integer storeType) {
        setTokenStore(tokenStore);
        setMaxToken(maxToken);
        this.storeType = storeType;
    }

    public TokenInterceptor(LocalTokenStore localTokenStore,Integer storeType) {
        this.localTokenStore = localTokenStore;
        this.storeType = storeType;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public void setMaxToken(Integer maxToken) {
        Config.getInstance().setMaxToken(maxToken);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String access_token = request.getParameter("access_token");
        if (access_token == null || access_token.trim().isEmpty()) {
            access_token = request.getHeader("Authorization");
            if (access_token != null && access_token.length() >= 7) {
                access_token = access_token.substring(7);
            }
        }
        if (access_token == null || access_token.trim().isEmpty()) {
            throw new ErrorTokenException("token不能为空");
        }
        String subject;
        if (ObjectUtils.isEmpty(storeType) || storeType == BigInteger.ZERO.intValue()) {
            try {
                String tokenKey = localTokenStore.getTokenKey();
                log.debug("-------------------------------------------");
                log.debug("开始解析token：" + access_token);
                log.debug("使用tokenKey：" + tokenKey);
                TokenUtil.parseToken(access_token, tokenKey);
            } catch (ExpiredJwtException e) {
                log.debug("token已过期");
                throw new ExpiredTokenException();
            } catch (Exception e) {
                log.debug(e.getMessage());
                throw new ErrorTokenException();
            }
        }else {
            try {
                String tokenKey = tokenStore.getTokenKey();
                log.debug("-------------------------------------------");
                log.debug("开始解析token：" + access_token);
                log.debug("使用tokenKey：" + tokenKey);
                subject = TokenUtil.parseToken(access_token, tokenKey);
            } catch (ExpiredJwtException e) {
                log.debug("token已过期");
                throw new ExpiredTokenException();
            } catch (Exception e) {
                log.debug(e.getMessage());
                throw new ErrorTokenException();
            }
            Token token = tokenStore.findToken(subject, access_token);
            if (token == null) {
                log.debug("token不在系统中");
                throw new ErrorTokenException();
            }
            // 检查权限
            if (handler instanceof HandlerMethod) {
                Method method = ((HandlerMethod) handler).getMethod();
                if (method != null) {
                    if (!checkPermission(method, token) || !checkRole(method, token)) {
                        throw new UnauthorizedException();
                    }
                }
            }
            request.setAttribute(SubjectUtil.REQUEST_TOKEN_NAME, token);
            log.debug("-------------------------------------------");
        }
        return super.preHandle(request, response, handler);
    }

    private boolean checkPermission(Method method, Token token) {
        RequiresPermissions annotation = method.getAnnotation(RequiresPermissions.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(RequiresPermissions.class);
            if (annotation == null) {
                return true;
            }
        }
        String[] requiresPermissions = annotation.value();
        Logical logical = annotation.logical();
        return SubjectUtil.hasPermission(token, requiresPermissions, logical);
    }

    private boolean checkRole(Method method, Token token) {
        RequiresRoles annotation = method.getAnnotation(RequiresRoles.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(RequiresRoles.class);
            if (annotation == null) {
                return true;
            }
        }
        String[] requiresRoles = annotation.value();
        Logical logical = annotation.logical();
        return SubjectUtil.hasRole(token, requiresRoles, logical);
    }
}
