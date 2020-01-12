package com.atguigu.gmall.index.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/cates")
    public Resp<List<CategoryEntity>>queryLvl1Catageory(){

        List<CategoryEntity>list=  indexService.queryLvl1Catageory();
        return Resp.ok(list);
    }

    @GetMapping("cates/{pid}")

    public Resp<List<CategoryVo>> queryCategoryWithSub(@PathVariable("pid")Long pid){

      List<CategoryVo> categoryVos= indexService.queryCategoryWithSub(pid);
        return  Resp.ok(categoryVos);
    }




}
