package com.pinyougou.sellergoods.service.impl;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.pinyougou.mapper.TbTypeTemplateMapper;
import com.pinyougou.pojo.TbTypeTemplate;
import com.pinyougou.sellergoods.service.TypeTemplateService;
import com.pinyougou.utils.CommonConstant;
import org.omg.CORBA.MARSHAL;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.pojo.TbItemCatExample;
import com.pinyougou.pojo.TbItemCatExample.Criteria;
import com.pinyougou.sellergoods.service.ItemCatService;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service(timeout = 5000)
public class ItemCatServiceImpl implements ItemCatService {

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Override
	public List<TbItemCat> findByParent(Long parentId) {

		TbItemCatExample itemCatExample = new TbItemCatExample();
		itemCatExample.createCriteria().andParentIdEqualTo(parentId);
		return itemCatMapper.selectByExample(itemCatExample);
	}

	@Autowired
	private RedisTemplate redisTemplate;
//	@Autowired
//	private TbTypeTemplateMapper tbTypeTemplateMapper;
	@Autowired
	private TypeTemplateService typeTemplateService;

	/**
	 * 查询全部
	 *
	 * 分类缓存到redis给搜索使用
	 *
	 * 运行商审核的时候会调用这个方法
	 *
	 */
	@Override
	public List<TbItemCat> findAll() {

		List<TbItemCat> itemCatList = itemCatMapper.selectByExample(null);
		try {
			for (TbItemCat itemCat : itemCatList) {

                //分类缓存到redis给搜索使用
                Long typeId = itemCat.getTypeId();
				System.out.println("typeId------->"+typeId+" catName----->"+itemCat.getName());
				redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_ITEMCATLIST).put(itemCat.getName(), typeId);

                TbTypeTemplate typeTemplate = typeTemplateService.findOne(typeId);
                //TbTypeTemplate typeTemplate = tbTypeTemplateMapper.selectByPrimaryKey(typeId);

                List<Map> brandList = JSON.parseArray(typeTemplate.getBrandIds(), Map.class);
                redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_BRANDLIST).put(typeId+"", brandList);

                List<Map> specList = JSON.parseArray(typeTemplate.getSpecIds(), Map.class);
                redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_SPECLIST).put(typeId+"", specList);

            }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return itemCatList;
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbItemCat> page=   (Page<TbItemCat>) itemCatMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbItemCat itemCat) {
		itemCatMapper.insert(itemCat);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbItemCat itemCat){
		itemCatMapper.updateByPrimaryKey(itemCat);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbItemCat findOne(Long id){
		return itemCatMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			itemCatMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbItemCat itemCat, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbItemCatExample example=new TbItemCatExample();
		Criteria criteria = example.createCriteria();
		
		if(itemCat!=null){			
						if(itemCat.getName()!=null && itemCat.getName().length()>0){
				criteria.andNameLike("%"+itemCat.getName()+"%");
			}
	
		}
		
		Page<TbItemCat> page= (Page<TbItemCat>)itemCatMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}
	
}
