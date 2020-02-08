package com.atguigu.gmall.order.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        OrderConfirmVo orderConfirmVo= orderService.confirm();
        return Resp.ok(orderConfirmVo);
    }


    @PostMapping("submit")
    public Resp<Object>submit(@RequestBody OrderSubmitVo orderSubmitVo){

        orderService.submit(orderSubmitVo);

      return   Resp.ok(null);
    }
}
