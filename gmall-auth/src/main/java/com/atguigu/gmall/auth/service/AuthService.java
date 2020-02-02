package com.atguigu.gmall.auth.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.exception.UmsException;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.auth.fegin.GmallUmsClient;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.auth.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties({JwtProperties.class})
public class AuthService {

   @Autowired
   private GmallUmsClient umsClient;

   @Autowired
   private JwtProperties jwtProperties;

    public String accredit(String username, String password) {
       //1远程调用
        Resp<MemberEntity> memberEntityResp = umsClient.queryUser(username, password);
        MemberEntity memberEntity = memberEntityResp.getData();

        //2.判断是否为空
        if(memberEntity==null){
            throw new UmsException("用户名或密码错误");
        }
        try {
            //3.生成JWT
            Map<String ,Object>map =new HashMap<>();
            map.put("id",memberEntity.getId());
            map.put("userName",memberEntity.getUsername());
            return JwtUtils.generateToken(map,jwtProperties.getPrivateKey(),jwtProperties.getExpireTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  null;

    }
}
