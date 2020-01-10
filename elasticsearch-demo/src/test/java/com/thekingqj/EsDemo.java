package com.thekingqj;


import ch.qos.logback.core.net.SyslogOutputStream;
import com.alibaba.fastjson.JSON;
import com.thekingqj.repository.UserRespository;
import lombok.AllArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import com.thekingqj.pojo.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
public class EsDemo {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private UserRespository userRespository;

    @Autowired
    private RestHighLevelClient highLevelClient;
    @Test
    public void test1(){

        //创建索引
        this.restTemplate.createIndex(User.class);
        //创建映射
        this.restTemplate.putMapping(User.class);

    }

    @Test
    public void document(){
       // User save = userRespository.save(new User(1L, "蔡徐坤，唱跳rap篮球", 22, "123456"));
//        List<User> users = new ArrayList<>();
//        users.add(new User(1l, "柳岩", 18, "123456"));
//        users.add(new User(2l, "范冰冰", 19, "123456"));
//        users.add(new User(3l, "李冰冰", 20, "123456"));
//        users.add(new User(4l, "锋哥", 21, "123456"));
//        users.add(new User(5l, "小鹿", 22, "123456"));
//        users.add(new User(6l, "韩红", 23, "123456"));
//        this.userRespository.saveAll(users);

//        this.userRespository.deleteById(6L);
//        System.out.println(this.userRespository.findById(1L));
//        userRespository.findByAgeBetween(19, 25).forEach(System.out::println);
//        userRespository.findByNative(19,20).forEach(System.out::println);

//        this.userRespository.search(QueryBuilders.rangeQuery("age").gte(19).lte(20)).forEach(System.out::println);

//        Page<User> age = this.userRespository.search(QueryBuilders.rangeQuery("age").gte(19).lte(20), PageRequest.of(0, 2));
//        System.out.println(age.getTotalElements());
//        System.out.println(age.getTotalPages());
//        age.getContent().forEach(System.out::println);

        //初始化自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.rangeQuery("age").gte(19).lte(22));
        queryBuilder.withSort(SortBuilders.fieldSort("age").order(SortOrder.DESC));
        queryBuilder.withPageable(PageRequest.of(0,2));
        queryBuilder.withHighlightBuilder(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        queryBuilder.addAggregation(AggregationBuilders.terms("passwordAgg").field("password"));


        AggregatedPage<User> page = (AggregatedPage)this.userRespository.search(queryBuilder.build());
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        page.getContent().forEach(System.out::println);
        ParsedStringTerms trems=(ParsedStringTerms)page.getAggregation("passwordAgg");
        trems.getBuckets().forEach(bucket->{
           System.out.println(bucket.getKeyAsString());
       });
    }





    //ES原声客户端
    @Test
    public void search(){
        //初始化自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("name","冰冰").operator(Operator.AND));
        queryBuilder.withSort(SortBuilders.fieldSort("age").order(SortOrder.DESC));
        queryBuilder.withPageable(PageRequest.of(0,2));
        queryBuilder.withHighlightBuilder(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        queryBuilder.addAggregation(AggregationBuilders.terms("passwordAgg").field("password"));

        restTemplate.query(queryBuilder.build(),response ->{
            SearchHit[] hits = response.getHits().getHits();
            System.out.println(hits.length);

            if(hits!=null){
                for (SearchHit hit : hits) {
                    String userJson = hit.getSourceAsString();
                    User user = JSON.parseObject(userJson, User.class);
                    System.out.println(user);
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    HighlightField highlightField = highlightFields.get("name");
                    user.setName(highlightField.getFragments()[0].toString());
                    System.out.println(user);
                }
            }

            //获取聚合结果集
            Map<String, Aggregation> asMap = response.getAggregations().getAsMap();
            ParsedStringTerms passwordAgg = (ParsedStringTerms)asMap.get("passwordAgg");
            passwordAgg.getBuckets().forEach(bucket-> System.out.println(bucket.getKeyAsString()));
            return  null;
        });
    }


    @Test
    public  void highLevelClient() throws IOException {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("name","冰冰").operator(Operator.AND));
        sourceBuilder.sort("age",SortOrder.DESC);
        sourceBuilder.from(0);
        sourceBuilder.size(2);
        sourceBuilder.highlighter(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        sourceBuilder.aggregation(AggregationBuilders.terms("passwordAgg").field("password")
                                  .subAggregation(AggregationBuilders.avg("ageAgg").field("age")));
        SearchResponse response = highLevelClient.search(new SearchRequest(new String[]{"user"}, sourceBuilder), RequestOptions.DEFAULT);

        SearchHit[] hits = response.getHits().getHits();
        System.out.println(hits.length);

        if(hits!=null){
            for (SearchHit hit : hits) {
                String userJson = hit.getSourceAsString();
                User user = JSON.parseObject(userJson, User.class);
                System.out.println(user);
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("name");
                user.setName(highlightField.getFragments()[0].toString());
                System.out.println(user);
            }
        }

        //获取聚合结果集
        Map<String, Aggregation> asMap = response.getAggregations().getAsMap();
        ParsedStringTerms passwordAgg = (ParsedStringTerms)asMap.get("passwordAgg");
        passwordAgg.getBuckets().forEach(bucket-> System.out.println(bucket.getKeyAsString()));
    }


}
