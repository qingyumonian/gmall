package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "goods",type = "info",shards = 2,replicas = 2)
public class Goods {

    @Id
    private Long skuId;
    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String skuTitle;
    @Field(type =FieldType.Keyword,index = false)
    private String skuSubTitle;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Keyword,index = false)
    private String defaultImage;

    @Field(type = FieldType.Long)
    private Long sale;
    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Boolean)
    private Boolean store;

    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String  brandName;
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;
    @Field(type = FieldType.Nested)//嵌套
    private List<SearchAttrValue> attrs;
}
