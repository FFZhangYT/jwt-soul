# jwt-soul

[![](https://jitpack.io/v/FFZhangYT/Jwt-soul.svg)](https://jitpack.io/#FFZhangYT/Jwt-soul)

### Introduction
    Jwt-soul encapsulates jjwt

### Dependent mode

    <dependency>
        <groupId>com.github.FFZhangYT</groupId>
        <artifactId>Jwt-soul</artifactId>
        <version>Tag</version>
    </dependency>
    
	<repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
	</repositories>

### Use in SpringMVC

```xml
<beans>
    <!-- Interceptor configuration -->
    <mvc:interceptors>
        <mvc:interceptor>
            <mvc:mapping path="/api/**" />
            <mvc:exclude-mapping path="/api/login" />
            <bean class="org.yt.jwts.TokenInterceptor">
                <property name="tokenStore" ref="tokenStore" />
                <property name="maxToken" value="3" /> <!-- Maximum number of token per user -->
            </bean>
        </mvc:interceptor>
    </mvc:interceptors>
    
    <!-- Here you can choose JdbcTokenStore and RedisTokenStore -->
    <bean id="tokenStore" class="org.yt.jwts.provider.JdbcTokenStore">
        <constructor-arg name="dataSource" ref="dataSource" />
    </bean>
    
    <bean id="tokenStore" class="org.yt.jwts.provider.RedisTokenStore">
        <constructor-arg name="redisTemplate" ref="stringRedisTemplate" />
    </bean>
</beans>
```

<br>


### Log in to issue token

```java
@RestController
public class LoginController {
    @Autowired
    private TokenStore tokenStore;
    
    @PostMapping("/login")
    public ResultMap login(String account, String password, HttpServletRequest request) {
        // Your verification logic.
        // ......
        // Issue token
        Token token = tokenStore.createNewToken(userId, permissions, roles);
        return ResultMap.ok("login success").put("access_token",token.getAccessToken());
    }
}
```

The default expiration time for token is one day, setting the expiration time method in seconds:


    Token token = tokenStore.createNewToken(userId, permissions, roles, 60*60*24*30);


<br>

### Use comments or code to restrict permissions
1.Using comments:
```text
// System permissions are required to access
@RequiresPermissions("system")

// System and front permissions are required to access, logical can not write, the default is AND
@RequiresPermissions(value={"system","front"}, logical=Logical.AND)

// System or front permissions are required to access
@RequiresPermissions(value={"system","front"}, logical=Logical.OR)

// You need a admin or user role to access
@RequiresRoles(value={"admin","user"}, logical=Logical.OR)
```
> Comments can only be added to Controller's methods.

<br>

2.The way you use code:
```text
//Do you have system permissions
SubjectUtil.hasPermission(request, "system");

//Do you have system or front permissions.
SubjectUtil.hasPermission(request, new String[]{"system","front"}, Logical.OR);

//Is there a admin or user role.
SubjectUtil.hasRole(request, new String[]{"admin","user"}, Logical.OR)
```

<br>

### Front-end delivery token
Put it in the parameter and pass it with 'access_ token':
```javascript
$.get("/xxx", { access_token: token }, function(data) {

});
```
Put it in header and pass it with 'Authorization', 'Bearer': 
```javascript
$.ajax({
    url: "/xxx", 
    beforeSend: function(xhr) {
        xhr.setRequestHeader("Authorization", 'Bearer '+ token);
    },
    success: function(data){ }
});
```

<br>

## Matters needing attention
### Exception handling
&emsp;Jwt-soul throws exceptions when token authentication fails and does not have permissions, and the framework defines several exceptions：
    
| Exception             | Description             | Description error message                             |
|:----------------------|:------------------------|:------------------------------------------------------|
| ErrorTokenException   | Token validation failed | Error message "Authentication failed", error code 401 |
| ExpiredTokenException | Token has expired       | Error message "login expired", error code 402         |
| UnauthorizedException | No permissions          | Error message "No access", error code 403             |

&emsp;It is recommended that you use an exception processor to catch an exception and return json data to the foreground：

```xml
<bean id="exceptionHandler" class="com.xxx.ExceptionHandler" />
```

```java
public class ExceptionHandler implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object obj, Exception ex) {
        if(ex instanceof TokenException){
            writerJson(response, ((TokenException) ex).getCode(), ex.getMessage());
        } else {
            writerJson(response, 500, ex.getMessage());
            ex.printStackTrace();
        }
        return new ModelAndView();
    }

    private void writerJson(HttpServletResponse response, int code, String msg) {
        response.setContentType("application/json;charset=UTF-8");
        try {
            PrintWriter out = response.getWriter();
            out.write("{\"code\":"+code+",\"msg\":\""+msg+"\"}");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

<br>

### Take the initiative to invalidate token：
```java
public class XX {
    @Autowired
    private TokenStore tokenStore;
    
    public void xx(){
        // Remove a user's token
        tokenStore.removeToken(userId, access_token);
        
        // Remove all token of the user
        tokenStore.removeTokensByUserId(userId);
    }
}
```

<br>

### Update the list of roles and permissions
&emsp;Modified roles and permissions for users need to update roles and permissions in the framework synchronously:
```java
public class XX {
    @Autowired
    private TokenStore tokenStore;
    
    public void xx(){
        // Update the user's list of roles
        tokenStore.updateRolesByUserId(userId, roles);
        
        // Update the user's permission list
        tokenStore.updatePermissionsByUserId(userId, permissions);
    }
}
```

<br>

### Gets the current user information
```text
Token token = SubjectUtil.getToken(request);
```   

<br>

### RedisTokenStore needs to integrate redis

1.SpringMvc integrated Redis:
```xml
<beans>
    <bean id="poolConfig" class="redis.clients.jedis.JedisPoolConfig">
        <property name="maxIdle" value="300" />
        <property name="maxTotal" value="600" />
        <property name="maxWaitMillis" value="1000" />
        <property name="testOnBorrow" value="true" />
    </bean>
    
    <bean id="jedisConnectionFactory" class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory">
        <property name="hostName" value="127.0.0.1" />
        <property name="password" value="" />
        <property name="port" value="6379" />
        <property name="poolConfig" ref="poolConfig" />
    </bean>
    
    <bean id="stringRedisTemplate" class="org.springframework.data.redis.core.StringRedisTemplate">
        <property name="connectionFactory" ref="jedisConnectionFactory" />
    </bean>
</beans>
```

<br>

### JdbcTokenStore needs to import SQL
&emsp;Using JdbcTokenStore requires importing SQL, to configure dataSource.

<br>