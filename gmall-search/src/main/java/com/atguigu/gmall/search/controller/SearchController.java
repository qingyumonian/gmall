package com.atguigu.gmall.search.controller;


import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public Resp<SearchResponseVo> search(SearchParam searchParam) throws IOException {
        SearchResponseVo searchResponseVo = searchService.search(searchParam);

        return  Resp.ok(searchResponseVo);
    }

}
