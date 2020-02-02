package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.exception.UmsException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;



@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<MemberEntity> queryWrapper = new QueryWrapper<>();
        switch (type){

            case 1:queryWrapper.eq("username",data);break;
            case 2:queryWrapper.eq("mobile",data);break;
            case 3:queryWrapper.eq("email",data);break;
            default:
                return null;
        }
        int count = count(queryWrapper);
        return count==0;
    }

    @Override
    public void register(MemberEntity memberEntity, String code) {

        //校验验证码是否正确
        String redisCode = redisTemplate.opsForValue().get("code:" + memberEntity.getMobile());
        if(!StringUtils.equals(redisCode,code)){
            throw new UmsException("用户名或密码错误");
        }
        //生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        //加盐加密
        String s = DigestUtils.md5Hex(memberEntity.getPassword() + salt);
        memberEntity.setPassword(s);
        //保存用户信息
        memberEntity.setSalt(salt);
        memberEntity.setLevelId(1L);
        memberEntity.setSourceType(1);
        memberEntity.setIntegration(1000);
        memberEntity.setGrowth(1000);
        memberEntity.setStatus(1);
        memberEntity.setCreateTime(new Date());
        this.save(memberEntity);
        //删除验证码
        redisTemplate.delete("code"+memberEntity.getMobile());
    }

    @Override
    public MemberEntity queryUser(String username, String password) {

        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
        if(memberEntity==null){
            return null;
        }
        String salt = memberEntity.getSalt();
        String s = DigestUtils.md5Hex(password + salt);

        if(!StringUtils.equals(s,memberEntity.getPassword())){
            return null;
        }

        return  memberEntity;
    }

}