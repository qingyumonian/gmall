package com.atguigu.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.config.AlipayTemplate;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

  @Autowired
  private AlipayTemplate alipayTemplate;

    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        OrderConfirmVo orderConfirmVo= orderService.confirm();
        return Resp.ok(orderConfirmVo);
    }


    /***
     * 递交订单后应该弹出一个支付页面
     * @param orderSubmitVo
     * @return
     */
    @PostMapping("submit")
    public Resp<Object>submit(@RequestBody OrderSubmitVo orderSubmitVo){

        OrderEntity orderEntity = orderService.submit(orderSubmitVo);
        if(orderEntity!=null){
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount(orderEntity.getTotalAmount().toString());
            payVo.setSubject("11111");
            payVo.setBody("222222");
            try {
                String pay = alipayTemplate.pay(payVo);
                System.out.println(pay);
            } catch (AlipayApiException e) {
                e.printStackTrace();
            }
        }
        return   Resp.ok(orderEntity);
    }

    /**
     * 支付包，支付成功的异步回调
     * @return
     */
    @PostMapping("pay/success")
    public Resp<Object>paySuccess(){

        return Resp.ok(null);
    }
}
