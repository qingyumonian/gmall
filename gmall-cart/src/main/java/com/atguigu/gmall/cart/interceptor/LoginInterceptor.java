package com.atguigu.gmall.cart.interceptor;

import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

//目的是获取用户的登录状态（userId/userKey），不是拦截登录
@EnableConfigurationProperties({JwtProperties.class})
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

   private  static final ThreadLocal<UserInfo> THREAD_LOCAL=new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfo userInfo = new UserInfo();
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, jwtProperties.getUserKey());
        if(StringUtils.isEmpty(userKey)){
            //如果为空则制作一个
            CookieUtils.setCookie(request,response,jwtProperties.getUserKey(), UUID.randomUUID().toString(),jwtProperties.getExpireTime());
        }
        userInfo.setUserKey(userKey);
        if(StringUtils.isEmpty(token)){
           //将userInfo传递到后续的业务
            THREAD_LOCAL.set(userInfo);
            return true;
        }

        try {
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            Long id = Long.valueOf(infoFromToken.get("id").toString());
          userInfo.setUserId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //将userInfo传递到后续的业务
        THREAD_LOCAL.set(userInfo);
        return true;
    }


    public static UserInfo getUserInfo(){
        return  THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //防止内存泄漏  线程池：请求结束不代表线程结束
        THREAD_LOCAL.remove();
    }
}
