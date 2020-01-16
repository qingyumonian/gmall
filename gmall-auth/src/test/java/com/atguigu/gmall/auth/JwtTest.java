package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;


import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
	private static final String pubKeyPath = "C:\\Users\\saber\\Desktop\\shop\\code\\rsa\\rsa.pub";

    private static final String priKeyPath = "C:\\Users\\saber\\Desktop\\shop\\code\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "fdsdfsdfs131345sdf");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1NzkwOTQ2NTd9.FyjWL7q4ZSXiJ7840lwItZmCAtbywWUBiSb_wadfrrk6tIDk3kA3f6W6k8FxMgIgY-5xRFWR2dISmUf3m3iPoobFyzMt_-w1TNnczpiHhrlifRXg-91Ja2gjr_Rqasuq3sNuG1FAP3osgLyrbfGn3XfrmN76MqIfZwZ6o3rhVyp4SPYHwp0W-MxWs99ycu4XAxxqIct9nbG49xNcEWi_Jnn3O9HplILVprpbpq5kOtZYCwnaSZCE9WLBuUZk9Swk8SN0gsc6Xr_2Rty-1-uPr07I68Rqih0Ka4FTBIKd0XrAKRs7hxFB_KnSLUnTnBFUSL80_haTjgMFh5uEhrqoQQ";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}