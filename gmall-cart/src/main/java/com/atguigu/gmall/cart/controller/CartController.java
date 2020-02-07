package com.atguigu.gmall.cart.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("cart")
public class CartController {

//    @GetMapping("test")
//    public String test(){
//        System.out.println(LoginInterceptor.getUserInfo());
//        return  "xxx";
//    }

    @Autowired
    private CartService cartService;

    @PostMapping
    public Resp<Object> addCart(@RequestBody Cart cart){

        cartService.addCart(cart);

        return  Resp.ok(null);

    }

    @GetMapping
    public Resp<List<Cart>> queryCarts(){

       List<Cart> carts= cartService.queryCatrs();

       return Resp.ok(carts);
    }

    @PostMapping("update")
    public Resp<Object> updateCart(@RequestBody Cart cart){

        cartService.updateCart(cart);
        return Resp.ok(null);
    }
    @PostMapping("check")
    public Resp<Object> checkCart(@RequestBody Cart cart){

        cartService.checkCart(cart);
        return Resp.ok(null);
    }

    @PostMapping("delete/{skuId}")
    public Resp<Object>deleteCart(@PathVariable("skuId") Long skuId){

        cartService.deleteCart(skuId);
        return Resp.ok(null);
    }

    @GetMapping("{userId}")
    public Resp<List<Cart>>queryCheckedCarts(@PathVariable("userId")Long userId){

        List<Cart>carts= cartService.queryCheckedCarts(userId);
        return Resp.ok(carts);
    }



}
