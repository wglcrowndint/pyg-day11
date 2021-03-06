package com.pinyougou.manager.controller;
import java.util.Arrays;
import java.util.List;

import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.search.service.SearchService;
import entity.Goods;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import entity.Result;
/**
 * controller
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

	@Reference
	private GoodsService goodsService;
	@Reference
	private SearchService searchService;
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findAll")
	public List<TbGoods> findAll(){			
		return goodsService.findAll();
	}
	
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findPage")
	public PageResult  findPage(int page,int rows){			
		return goodsService.findPage(page, rows);
	}
	
	/**
	 * 增加
	 * @param goods
	 * @return
	 */
	@RequestMapping("/add")
	public Result add(@RequestBody Goods goods){
		try {
			goodsService.add(goods);
			return new Result(true, "增加成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "增加失败");
		}
	}

	@RequestMapping("updateStatus")
	public Result updateStatus(Long ids[], String status) {
		Result result = new Result();
		try {
			goodsService.updateStatus(ids, status);
			result.setSuccess(true);
			result.setMessage("提交成功");
		} catch (Exception e) {
			e.printStackTrace();
			result.setMessage("提交失败");
			return result;
		}

		//如果商品审核通过把对应的商品项添加到索引库
		try {
			if ("2".equals(status)) {
				List<TbItem> itemList = goodsService.findAuditedStatusItems(ids);
				searchService.importItemList(itemList);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("更新索引库失败");
		}

		return result;
	}
	
	/**
	 * 修改
	 * @param goods
	 * @return
	 */
	@RequestMapping("/update")
	public Result update(@RequestBody Goods goods){
		try {
			goodsService.update(goods);
			return new Result(true, "修改成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "修改失败");
		}
	}	
	
	/**
	 * 获取实体
	 * @param id
	 * @return
	 */
	@RequestMapping("/findOne")
	public Goods findOne(Long id){
		return goodsService.findOne(id);		
	}
	
	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@RequestMapping("/delete")
	public Result delete(Long [] ids){
		try {
			goodsService.delete(ids);
			//删除索引库
			searchService.deleteByGoodsIds(Arrays.asList(ids));

			return new Result(true, "删除成功"); 
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "删除失败");
		}
	}
	
		/**
	 * 查询+分页
	 * @param goods
	 * @param page
	 * @param rows
	 * @return
	 */
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbGoods goods, int page, int rows  ){

//		String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
//		goods.setSellerId(sellerId);
		return goodsService.findPage(goods, page, rows);		
	}
	
}
