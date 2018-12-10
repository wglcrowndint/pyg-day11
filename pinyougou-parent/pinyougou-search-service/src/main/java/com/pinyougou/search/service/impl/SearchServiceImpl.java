package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.SearchService;
import com.pinyougou.utils.CommonConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by crowndint on 2018/10/22.
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private HttpSolrServer httpSolrServer;
    @Autowired
    private SolrTemplate solrTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> search(Map<String, Object> searchMap)  {
        HashMap<String, Object> searchResult = new HashMap<>();
        try {
            String keywords = (String) searchMap.get("keywords");
            if (StringUtils.isEmpty(keywords)) {
                keywords = "*";
            }
            SolrQuery query = new SolrQuery("item_keywords:"+keywords);

            //关键字索索高亮显示及分页、过滤
            searchByHighlightAndPage(searchResult, searchMap, query);

            //查询按照关键字进行分组
            groupByKeywords(searchResult, query);

            //查询brandList
            getBrandListAndSpecListFromRedis(searchResult, searchMap);


        } catch (SolrServerException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    private void getBrandListAndSpecListFromRedis(HashMap<String, Object> searchResult, Map<String, Object> searchMap) {

        String category = (String) searchMap.get("category");
        if (StringUtils.isEmpty(category)) {
            List<String> categoryList = (List<String>) searchResult.get("categoryList");
            if (categoryList!=null&&!categoryList.isEmpty()) {
                category = categoryList.get(0);
            }
        }

        if (StringUtils.isEmpty(category)) {
            return;
        }

        Long typeId  = (Long) redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_ITEMCATLIST).get(category);
        List<Map> brandList = (List<Map>) redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_BRANDLIST).get(typeId+"");
        List<Map> specList = (List<Map>) redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_SPECLIST).get(typeId+"");

        searchResult.put("brandList", brandList);
        searchResult.put("specList", specList);
    }

    //执行分组查询
    private void groupByKeywords(HashMap<String, Object> searchResult, SolrQuery query) throws SolrServerException {

        List<String> categoryList = new ArrayList<>();
        query.setParam("group", true);//是否分组
        query.setParam("group.field", "item_category");//分组的域
        QueryResponse response = httpSolrServer.query(query);
        GroupResponse groupResponse = response.getGroupResponse();
        List<GroupCommand> values = groupResponse.getValues();
        if (values != null) {
            for (GroupCommand groupCommand : values) {
                for (Group group : groupCommand.getValues()) {
                    SolrDocumentList hits = group.getResult();
                    for (SolrDocument document : hits) {
                        String category = (String) document.getFieldValue("item_category");
                        categoryList.add(category);
                    }
                }
            }
        }
        searchResult.put("categoryList", categoryList);
    }

    private void searchByHighlightAndPage(HashMap<String, Object> searchResult, Map<String, Object> searchMap, SolrQuery query) throws SolrServerException {
        //高亮显示
        query.setHighlight(true);
        //高亮显示的域
        query.addHighlightField("item_title");
        //高亮显示的前缀
        query.setHighlightSimplePre("<em style='color:red'>");
        //高亮显示的后缀
        query.setHighlightSimplePost("</em>");

        //过滤
        String category = (String) searchMap.get("category");
        if (StringUtils.isNotBlank(category)) {
            query.addFilterQuery("item_category:"+category);
        }

        //按照品牌过滤
        String  brand = (String) searchMap.get("brand");
        if (StringUtils.isNotBlank(brand)) {
            query.addFilterQuery("item_brand:"+brand);
        }

        //按照规格过滤
        Map<String, String> spec = (Map<String, String>) searchMap.get("spec");
        if (spec!=null&&!spec.isEmpty()) {
            for(Map.Entry<String, String> entry : spec.entrySet()) {
                query.addFilterQuery("item_spec_"+entry.getKey()+":"+entry.getValue());
            }
        }

        //按照价格过滤
        String price = (String) searchMap.get("price");
        if(StringUtils.isNotBlank(price)) {
            //1000-1500
            String[] split = price.split("-");
            query.addFilterQuery("item_price:["+split[0]+" TO " +split[1]+"]");
        }

        //排序
        String sortField = (String) searchMap.get("sortField");
        String sort = (String) searchMap.get("sort");
        if (StringUtils.isNotBlank(sortField)&&StringUtils.isNotBlank(sort)) {
            query.addSort(sortField, "1".equals(sort)? SolrQuery.ORDER.asc: SolrQuery.ORDER.desc);
        }

        //分页
        Integer pageNo = (Integer) searchMap.get("pageNo");
        Integer pageSize = (Integer) searchMap.get("pageSize");
        if (pageNo==null||pageNo<1) {
            pageNo = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
        query.setStart((pageNo-1)*pageSize);
        query.setRows(pageSize);


        System.out.println(query);
        QueryResponse queryResponse = httpSolrServer.query(query);
        SolrDocumentList solrDocumentList = queryResponse.getResults();
        long numFound = solrDocumentList.getNumFound();

        ArrayList<TbItem> itemList = new ArrayList<>();
        for (SolrDocument solrDocument : solrDocumentList) {
            TbItem item = new TbItem();
            String id = (String) solrDocument.get("id");
            String title = (String) solrDocument.get("item_title");
            Double item_price = (Double) solrDocument.get("item_price");
            String image = (String) solrDocument.get("item_image");
            String item_category = (String) solrDocument.get("item_category");
            String item_brand = (String) solrDocument.get("item_brand");

            //取高亮显示
            Map<String, Map<String, List<String>>> highlighting = queryResponse.getHighlighting();
            List<String> list = highlighting.get(solrDocument.get("id")).get("item_title");
            //判断是否有高亮内容
            if (null != list) {
                title = list.get(0);
            }

            item.setId(Long.parseLong(id));
            item.setTitle(title);
            item.setPrice(new BigDecimal(item_price));
            item.setImage(image);
            item.setCategory(item_category);
            item.setBrand(item_brand);

            itemList.add(item);
        }

        searchResult.put("total", numFound);
        searchResult.put("rows", itemList);
        //计算页数
        int totalPages = (int) Math.ceil(numFound*1.0/pageSize);
        searchResult.put("totalPages", totalPages);

    }


    @Override
    public void deleteByGoodsIds(List<Long> goodsId) {
        SimpleQuery query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsid").in(goodsId);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }

    @Override
    public void importItemList(List itemList) {
        System.out.println(itemList);
        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();
    }


}
