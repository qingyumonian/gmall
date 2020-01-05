package com.atguigu.gmall.pms.feign;


import com.atguigu.core.bean.Resp;

import com.atguigu.gmall.pms.Vo.SaleVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("sms-service")
public interface GmallSmsClient {

    @PostMapping("sms/skubounds/sales")
    public Resp<Object> saveSales(@RequestBody SaleVo saleVo);
}
