package com.atguigu.gmall.auth.config;

import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "jwt.token")
@Data
public class JwtProperties {

    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private Integer expireTime;
    private String cookieName;


    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            if (!pubFile.exists() || !priFile.exists()) {
                //创建公钥私钥
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
             publicKey = RsaUtils.getPublicKey(pubKeyPath);
             privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

