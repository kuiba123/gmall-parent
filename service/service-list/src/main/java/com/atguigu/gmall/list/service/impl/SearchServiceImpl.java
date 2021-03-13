package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /*
    * 上架商品列表
    * @param skuId:
    * @return: void
    */
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        //查询sku信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            //  skuId
            goods.setId(skuInfo.getId());
            //  skuName
            goods.setTitle(skuInfo.getSkuName());
            //  price
            goods.setPrice(skuInfo.getPrice().doubleValue());

            goods.setCreateTime(new Date());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            //赋值品牌数据
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            if (trademark!=null){
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }

            //  赋值分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            if (categoryView!=null){
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory3Id(categoryView.getCategory3Id());

                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }


            //  赋值平台属性： private List<SearchAttr> attrs; attrId，attrValue，attrName
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            //  获取数据
            if(!CollectionUtils.isEmpty(attrList)){
                //  Function R apply(T t)
                List<SearchAttr> searchAttrList = attrList.stream().map((baseAttrInfo -> {
                    SearchAttr searchAttr = new SearchAttr();
                    //  平台属性Id
                    searchAttr.setAttrId(baseAttrInfo.getId());
                    //  平台属性名称
                    searchAttr.setAttrName(baseAttrInfo.getAttrName());
                    //  平台属性值名称
                    searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
                    return searchAttr;
                })).collect(Collectors.toList());
                //  赋值平台属性集合
                goods.setAttrs(searchAttrList);
            }
            /*//查询sku对应的平台属性
            List<BaseAttrInfo> baseAttrInfoList  = productFeignClient.getAttrList(skuId);
            if (baseAttrInfoList != null) {
                List<SearchAttr> searchAttrList = baseAttrInfoList.stream().map(baseAttrInfo -> {
                    SearchAttr searchAttr = new SearchAttr();
                    searchAttr.setAttrId(baseAttrInfo.getId());
                    searchAttr.setAttrName(baseAttrInfo.getAttrName());
                    //一个sku只对应一个属性值
                    List<BaseAttrValue> baseAttrValueList  = baseAttrInfo.getAttrValueList();
                    searchAttr.setAttrName(baseAttrValueList.get(0).getValueName());
                    return searchAttr;
                }).collect(Collectors.toList());
                goods.setAttrs(searchAttrList);
            }



            //查询品牌
            BaseTrademark baseTrademark = productFeignClient.getTrademark(skuInfo.getTmId());
            if (baseTrademark != null) {
                goods.setTmId(skuInfo.getTmId());
                goods.setTmName(baseTrademark.getTmName());
                goods.setTmLogoUrl(baseTrademark.getLogoUrl());
            }

            //查询分类
            BaseCategoryView baseCategoryView  = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            if (baseCategoryView != null) {
                goods.setCategory1Id(baseCategoryView.getCategory1Id());
                goods.setCategory1Name(baseCategoryView.getCategory1Name());
                goods.setCategory2Id(baseCategoryView.getCategory2Id());
                goods.setCategory2Name(baseCategoryView.getCategory2Name());
                goods.setCategory3Id(baseCategoryView.getCategory3Id());
                goods.setCategory3Name(baseCategoryView.getCategory3Name());
            }
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setId(skuInfo.getId());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());*/
        }


        this.goodsRepository.save(goods);
    }

    /*
    * 下架商品列表
    * @param skuId:
    * @return: void
    */
    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }

    /*
    * 更新热点
    * @param skuId:
    * @return: void
    */
    @Override
    public void incrHotScore(Long skuId) {
        //定义key
        String hotKey = "hotScore";
        //保存数据
        Double hotScore  = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (hotScore % 10 == 0) {
            //更新es
            Optional<Goods> optional  = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {

        //1.通过java代码动态生成dsl语句
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);

        //进行查询操作RestHighLevelClient，获取到查询响应
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //需要将这个响应结果searchResponse封装成SearchResponseVo
        //trademarkList,attrsList,goodsList,total 这四个属性在parseSearchResult方法赋值
        SearchResponseVo responseVo = this.parseSearchResult(searchResponse);

        /*
         private Integer pageSize;//每页显示的内容
         private Integer pageNo;//当前页面
         private Long totalPages;//总页数
         */
        //给部分属性赋值
        responseVo.setPageNo(searchParam.getPageNo());
        responseVo.setPageSize(searchParam.getPageSize());
        //在工作中总结出来的公式
        long totalPages = (responseVo.getTotal() + searchParam.getPageSize() - 1)/searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }

    /*
    * 数据封装
    * @param response:
    * @return: com.atguigu.gmall.model.list.SearchResponseVo
    */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {

        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
        private List<SearchResponseTmVo> trademarkList;
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        private List<Goods> goodsList = new ArrayList<>();
        private Long total;//总记录数
         */

        SearchHits hits = searchResponse.getHits();
        //从聚合中获取品牌信息
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //为了获取到buckets需要转换
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //Function有参数,有返回值
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map((bucket) -> {
            //声明品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            //获取品牌Id
            String keyAsString = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(keyAsString));

            //赋值品牌Name在另一个桶中
            ParsedStringTerms tmNameAgg = ((Terms.Bucket) bucket).getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            //赋值品牌LogoUrl
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());
        //添加品牌的
        searchResponseVo.setTrademarkList(trademarkList);

        //添加平台属性attrAgg属于nested类型
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //转完后获取attrIdAgg
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        //获取对应的平台属性数据
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map((bucket) -> {
            //声明一个平台属性对象
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            //获取平台属性Id
            Number keyAsNumber = ((Terms.Bucket) bucket).getKeyAsNumber();
            searchResponseAttrVo.setAttrId(keyAsNumber.longValue());
            //获取平台属性名称
            ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);
            //获取平台属性值的名称
            ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            //平台属性值名称对应多个数据,需要循环遍历获取到里面的每个key所对应的数据
            List<? extends Terms.Bucket> buckets = attrValueAgg.getBuckets();

            //方式一普通for循环迭代

            //方式二 通过 Terms.Bucket::getKeyAsString 来获取 key
            List<String> values = buckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(values);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());
        searchResponseVo.setAttrsList(attrsList);

        //商品集合goodsList
        SearchHit[] subHits = hits.getHits();
        //声明一个集合来存储Goods
        List<Goods> goodsList = new ArrayList<>();
        //循环遍历
        for (SearchHit subHit : subHits) {
            //是一个Goods.class组成的json字符串
            String sourceAsString = subHit.getSourceAsString();
            //将sourceAsring转为Goods的对象
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            //细节:如果通过关键词检索,获取到高亮字段
            if (subHit.getHighlightFields().get("title")!=null) {
                //说明通过关键词检索的
                Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                //覆盖原来的title
                goods.setTitle(title.toString());
            }
            goodsList.add(goods);
        }
        //赋值商品集合对象
        searchResponseVo.setGoodsList(goodsList);
        //赋值total
        searchResponseVo.setTotal(hits.totalHits);
        return searchResponseVo;
    }

    /*
    *  动态生成dsl语句
    * @param searchParam:
    * @return: org.elasticsearch.action.search.SearchRequest
    */
    private SearchRequest buildQueryDsl(SearchParam searchParam) {

        //构建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建boolQueryBuilder
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //query -- bool -- filter
        //boolQueryBuilder.filter()
        //判断用户是否根据分类Id查询
        if (searchParam.getCategory1Id() != null) {
            //  query -- bool -- filter -- term
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }
        if (searchParam.getCategory2Id() != null) {
            //  query -- bool -- filter -- term
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        if (searchParam.getCategory3Id() != null) {
            //  query -- bool -- filter -- term
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }

        //判断查询条件是否为关键字: 小米手机  小米and手机
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }

        //判断用户是否根据品牌过滤query -- bool -- filter -- term
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            //trademark = 2 : 华为
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                //根据品牌Id过滤
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));
            }
        }

        //通过平台属性值进行过滤
        //判断是否根据了平台属性进行过滤
        //  props=23:4G:运行内存  平台属性Id 平台属性值名称 平台属性名
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            //循环数组
            for (String prop : props) {
                //  prop = 23:4G:运行内存
                //  进行分割：
                String[] split = prop.split(":");
                //数据符合格式
                if (split != null && split.length == 3) {
                    //创建两个bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //构建查询条件
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    //将sub 将subBoolQuery 赋值到boolQuery 中
                    boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));
                    //将boolQuery 赋值给总的boolQueryBuilder
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }

        //调用query方法{query}
        searchSourceBuilder.query(boolQueryBuilder);

        //分页查询
        //表示从第几页查询
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        //默认显示每页三条数据
        searchSourceBuilder.size(searchParam.getPageSize());
        //排序
        //判断 order=1:desc  order=1:asc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            //进行分割  1:desc
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                String field = "";
                //判断数组中第一位，如果1 按照热度排名进行排序，如果2按照价格进行排序 ...
                switch (split[0]){
                    case "1":
                        field="hotScore";
                        break;
                    case "2":
                        field="price";
                        break;
                }
                //  按照这种判断标识进行排序 使用三元表达式进行判断
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                //默认排序规则
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }

        //高亮必须先通过关键字检索
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //聚合aggs
        //设置品牌聚合
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        //设置平台属性聚合 特殊的数据类型nested
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));

        //稍微优化:设置想要的数据字段id , defaultImg，title，price 其他字段在展示的时候设置成null
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);

        //应该返回SearchRequest对象
        //  GET /goods/info/_search ||  GET /goods/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);

        //打印出来的就是dsl语句
        System.out.println("DSL: \t"+ searchSourceBuilder.toString());
        return searchRequest;
    }
}
