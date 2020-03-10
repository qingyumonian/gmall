package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient highLevelClient;     //es的客户端

    public SearchResponseVo search(SearchParam searchParam) throws IOException {


        SearchResponse search = highLevelClient.search(new SearchRequest(new String[]{"goods"}, buildDSL(searchParam)), RequestOptions.DEFAULT);
        System.out.println(search.toString());//输出结果集

        SearchResponseVo responseVo = parseSearchResult(search);  //接收根据结果集，封装数据
        //设置分页参数
        responseVo.setPageNum(searchParam.getPageNum());
        responseVo.setPageSize(searchParam.getPageSize());
        return responseVo;
    }

    //数据的解析封装
    private SearchResponseVo parseSearchResult( SearchResponse search){
        SearchResponseVo responseVo = new SearchResponseVo();

        //查询结果集的封装
        SearchHits hits = search.getHits();
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList=new ArrayList<>();
        for (SearchHit hitsHit : hitsHits) {
            //获取_source反序列化成Goods
            String goodsJSON = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(goodsJSON, Goods.class);
            //获取高亮结果集，覆盖普通skuTitle
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField skuTitle = highlightFields.get("skuTitle");
            goods.setSkuSubTitle(skuTitle.getFragments()[0].toString());
            goodsList.add(goods);
        }
        responseVo.setProducts(goodsList);

        //解析品牌分类的聚合结果集
        Map<String, Aggregation> aggregationMap = search.getAggregations().asMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        SearchResponseAttrVO brandVO = new SearchResponseAttrVO();
        brandVO.setProductAttributeId(null);
        brandVO.setName("品牌");
        //获取聚合中的桶
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){

            //把每个桶转换成JSon字符串
            List<String> brandValues = buckets.stream().map(bucket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ((Terms.Bucket) bucket).getKeyAsNumber());
                ParsedStringTerms brandIdNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().asMap().get("brandNameAgg");
                map.put("name", brandIdNameAgg.getBuckets().get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());


            brandVO.setValue(brandValues);
            responseVo.setBrand(brandVO);
        }

        //解析分类的聚合结果集

        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        SearchResponseAttrVO categoryVO = new SearchResponseAttrVO();
        brandVO.setProductAttributeId(null);
        brandVO.setName("分类");
        //获取聚合中的桶
        List<? extends Terms.Bucket>  categoryBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryBuckets)){

            //把每个桶转换成JSon字符串
            List<String> categoryValues = categoryBuckets.stream().map(bucket -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ((Terms.Bucket) bucket).getKeyAsNumber());
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().asMap().get("categoryNameAgg");
                map.put("name", categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());


            categoryVO.setValue(categoryValues);
            responseVo.setCatelog(categoryVO);
        }



        //解析规格参数聚合结果集
        ParsedNested attrsAgg = (ParsedNested)aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> idAggBuckets = attrIdAgg.getBuckets();
        List<SearchResponseAttrVO> attrVOS = idAggBuckets.stream().map(bucket -> {
            SearchResponseAttrVO attrVO = new SearchResponseAttrVO();
            attrVO.setProductAttributeId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
            //获取规格参数名自聚合
            ParsedStringTerms attrNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            attrVO.setName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            //获取给个参数值自聚合，解析出规格参数值集合
            ParsedStringTerms attrValueAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
            List<String> values = valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVO.setValue(values);
            return attrVO;
        }).collect(Collectors.toList());
        responseVo.setAttrs(attrVOS);

        //总记录数
        responseVo.setTotal(search.getHits().getTotalHits());
        return  responseVo;
    }


    //封装约束参数并查询数据
    private SearchSourceBuilder buildDSL(SearchParam searchParam) {

        String key = searchParam.getKey();

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        if (StringUtils.isEmpty(key)) {
            return sourceBuilder;
        }
        //1.构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1品牌名称的过滤
        boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", key).operator(Operator.OR));
        //1.2构建过滤条件
        //1.2.1品牌的过滤
        Long[] brandId = searchParam.getBrand();
        if (brandId != null && brandId.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        //1.2.2分类的过滤
        Long[] catelog3 = searchParam.getCatelog3();
        if (catelog3 != null && catelog3.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", catelog3));
        }

        //1.2.3规格属性的过滤
        String[] props = searchParam.getProps();
        if (props != null && props.length != 0) {
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                if (split.length != 2 || split == null) {
                    continue;
                }
                //创建嵌套查询
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //创建嵌套查询中的子查询
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                //构建子查询的过滤条件
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                subBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", split[1].split("-")));
                //将子查询加入到嵌套查询中去
                boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                //将嵌套查询加入query中去
                boolQueryBuilder.filter(boolQuery);

            }
        }

        //1.2.4价格区间
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Double priceFrom = searchParam.getPriceFrom();
        if (priceFrom != null) {
            rangeQueryBuilder.gte(priceFrom);
        }
        Double priceTo = searchParam.getPriceTo();
        if (priceTo != null) {
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);

        sourceBuilder.query(boolQueryBuilder);

        //2.构建排序
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                String field = null;
                switch (split[0]) {
                    case "1":
                        field = "sale";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                sourceBuilder.sort(field, StringUtils.equals("asc", split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }


        //3.构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("skuTitle").preTags("<em>").postTags("</em>"));
        //5.构建聚合
        //5.1品牌的聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));
        //5.2分类的聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        //5.3规格的聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));
        System.out.println(sourceBuilder.toString());  //输出查询的语句集
        return sourceBuilder;
    }


}
