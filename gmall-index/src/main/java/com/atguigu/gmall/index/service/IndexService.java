package com.atguigu.gmall.index.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {


    @Autowired
    private GmallPmsClient pmsClient;

    public List<CategoryEntity> queryLvl1Catageory() {
        Resp<List<CategoryEntity>> listResp = pmsClient.queryLevelOrCid(1, null);
        List<CategoryEntity> data = listResp.getData();
        return  data;

    }

    public List<CategoryVo> queryCategoryWithSub(Long pid) {
        Resp<List<CategoryVo>> listResp = pmsClient.queryCategoryWithSub(pid);
        return listResp.getData();
    }
}
