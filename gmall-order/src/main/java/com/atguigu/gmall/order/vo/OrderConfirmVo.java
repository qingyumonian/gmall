package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    //收货地址
    private List<MemberReceiveAddressEntity> addresses;

    private List<OrderItemVo>orderItemVos;

    private Integer bounds;

    private String orderToken;//防止重复提交
}
