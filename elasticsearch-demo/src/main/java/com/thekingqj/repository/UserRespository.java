package com.thekingqj.repository;

import com.thekingqj.pojo.User;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UserRespository  extends ElasticsearchRepository<User,Long> {

    public List<User> findByAgeBetween(Integer age1, Integer age2);

    @Query("{\n" +
            "    \"range\": {\n" +
            "      \"age\": {\n" +
            "        \"gte\": \"?0\",\n" +
            "        \"lte\": \"?1\"\n" +
            "      }\n" +
            "    }\n" +
            "  }")
    public List<User> findByNative(Integer age1, Integer age2);

}
