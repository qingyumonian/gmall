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
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MiwidXNlck5hbWUiOiJ0aGVraW5nIiwiZXhwIjoxNTgwNTYzMzgyfQ.ZMFe5TXbK6rCc9UgM5eXzvVHC_UasLkkVj51SOU4MRdK78_pb1120mkflbjYiRur9Erxw8S9hCofw9LRu9czgexzIfqZa1BMjQ2ujTWCqYxWOukU5Siqdo35tlStI4O51rm3vsAV7dazSJNkyQqdrhScZf0pk1qzDZdtXiPQ2U6Jv6JfxrCELq3c30TlVbGIpTHqrDz-J44eWDrv4t_VcqE2BKvWFteEVur8M77tZqx2q5a83-9E4Wk_MW0UDDP5slGKUmlBqBUidan9O-iIiALNsCGqkMVL6O3Ug023qBSBsw0GCYfcC4UdeRGDu7QxFcLTA0TKKaV-THAajxOPrg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("userName"));
    }
}