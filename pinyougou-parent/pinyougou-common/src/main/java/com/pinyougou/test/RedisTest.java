package com.pinyougou.test;

import com.pinyougou.utils.CommonConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by crowndint on 2018/10/19.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring/applicationContext-redis.xml")
public class RedisTest
{
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testValueOpsSet() {

        redisTemplate.boundValueOps("name").set("itheima");
    }

    @Test
    public void testValueOps2() {

        Object value = redisTemplate.boundValueOps("name").get();
        redisTemplate.delete("name");
        System.out.println(value);
    }

    @Test
    public void testSetOps1() {

        redisTemplate.boundSetOps("nameSet").add("小泽");
        redisTemplate.boundSetOps("nameSet").add("龙泽");
        redisTemplate.boundSetOps("nameSet").add("波多");
    }

    @Test
    public void testSetOps2() {

        Object randomMember = redisTemplate.boundSetOps("nameSet").randomMember();
        System.out.println(randomMember);
        System.out.println("---------------");
        redisTemplate.delete("nameSet");
        Set members = redisTemplate.boundSetOps("nameSet").members();
        for (Object member : members) {
            System.out.println(member);
        }
    }

    @Test
    public void testListOps1() {

        BoundListOperations listOps = redisTemplate.boundListOps("listOps");
        listOps.leftPush("aaa");
        listOps.leftPush("bbb");
        listOps.leftPush("ccc");

    }

    @Test
    public void testListOps2() {

        BoundListOperations listOps = redisTemplate.boundListOps("listOps");
        List list = listOps.range(0, 100);
        for (Object o : list) {
            System.out.println(o);
        }
        System.out.println("------");
        Object o = listOps.index(2);
        System.out.println(o);
        System.out.println("------");
        Object topValue = listOps.rightPop();
        System.out.println(topValue);
    }

    @Test
    public void testMapOps1() {

        BoundHashOperations boundHashOps = redisTemplate.boundHashOps("hashOps");
        boundHashOps.put("1", "aaa");
        boundHashOps.put("2", "bbb");
        boundHashOps.put("3", "ccc");
    }

    @Test
    public void testMapOps2() {

        BoundHashOperations hashOperations = redisTemplate.boundHashOps("hashOps");
        Set keys = hashOperations.keys();
        for (Object key : keys) {
            System.out.print(key+" ");
        }
        List values = hashOperations.values();
        for (Object value : values) {
            System.out.print(value+"  ");
        }
        System.out.println("----");
        Object o = hashOperations.get("1");
        System.out.println(o);
        redisTemplate.delete("hashOps");
    }


    @Test
    public void testTypeCat() {

        Long typeId  = (Long) redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_ITEMCATLIST).get("进口皮鞭");
        List<Map> brandList = (List<Map>) redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_BRANDLIST).get(typeId);
        List<Map> specList = (List<Map>) redisTemplate.boundHashOps(CommonConstant.REDIS_CACHE_SPECLIST).get(typeId);
    }




}
