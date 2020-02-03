package com.atguigu.gmall.ums.controller;

import java.util.Arrays;
import java.util.Map;


import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jdk.nashorn.internal.codegen.MethodEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;




/**
 * 会员
 *
 * @author saber
 * @email lxf@theking.com
 * @date 2020-01-02 17:34:57
 */
@Api(tags = "会员 管理")
@RestController
@RequestMapping("ums/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    /**
     *  数据的校验（手机号、用户名的唯一性）
     * @param data
     * @param type
     * @return
     */
    @GetMapping("check/{data}/{type}")
    public Resp<Object> checkData(@PathVariable("data")String data,@PathVariable("type")Integer type){

        Boolean b=memberService.checkData(data,type);

        return  Resp.ok(b);
    }

    @PostMapping("code")
    public Resp<Object> sendCode(@RequestParam("phone")String phone){

        memberService.sendCode(phone);
        return  Resp.ok(null);
    }

    /**
     * 用户的注册
     * @param memberEntity
     * @param code
     * @return
     */
    @PostMapping("register")
    public Resp<Object>register(MemberEntity memberEntity,@RequestParam("code")String code){
        this.memberService.register(memberEntity,code);
        return Resp.ok(null);
    }

    @GetMapping("query")
    public Resp<MemberEntity> queryUser(@RequestParam("username")String username,
                                  @RequestParam("password")String password){
        MemberEntity memberEntity= memberService.queryUser(username,password);
        return  Resp.ok(memberEntity);
    }

    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ums:member:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = memberService.queryPage(queryCondition);

        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{id}")
    @PreAuthorize("hasAuthority('ums:member:info')")
    public Resp<MemberEntity> info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return Resp.ok(member);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('ums:member:save')")
    public Resp<Object> save(@RequestBody MemberEntity member){
		memberService.save(member);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ums:member:update')")
    public Resp<Object> update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ums:member:delete')")
    public Resp<Object> delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return Resp.ok(null);
    }

}
