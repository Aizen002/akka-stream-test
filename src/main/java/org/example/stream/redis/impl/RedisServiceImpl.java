package org.example.stream.redis.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.stream.redis.IRedisService;
import org.example.stream.util.ConstUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Author: wanghao
 * Date: 2024/2/22 11:54
 * Description:
 */
public class RedisServiceImpl implements IRedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);

    public static JedisPool jedisPool;

    static {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(2000);
        poolConfig.setMaxIdle(600);
        poolConfig.setMaxWaitMillis(10000);
        if (StringUtils.isNotBlank(ConstUtils.REDIS_SECRET)) {
            jedisPool = new JedisPool(poolConfig, ConstUtils.REDIS_SECRET, ConstUtils.REDIS_HOST, Integer.parseInt(ConstUtils.REDIS_PORT), 1000);
        } else {
            jedisPool = new JedisPool(poolConfig, ConstUtils.REDIS_HOST, Integer.parseInt(ConstUtils.REDIS_PORT), 1000);
        }
    }

    @Override
    public Long zadd(final String key, final double score, final String member) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.zadd(key, score, member);
        } finally {
            release(jedis);
        }
    }

    @Override
    public Long zcard(final String key) {

        if (key == null || key.equals("")) {
            return 0L;
        }
        long start = System.currentTimeMillis();

        Long count = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            count = jedis.zcard(key);
        } catch (Exception e) {
            logger.error("zcard redis error !Key:" + key + "," + key, e);
        } finally {
            release(jedis);
        }
        long end = System.currentTimeMillis();
        if ((end - start) > 100) {
            logger.error(
                    "zadd redis slow　key:" + key + " ......................... time:" + (end - start));
        }
        return count;
    }

    @Override
    public List<String> getZrangeSortdSet(String key, Long startSize, Long endSize) {
        long start = System.currentTimeMillis();
        List<String> list = new ArrayList<>();
        Jedis jedis = null;
        try {
            jedis = getJedis();
            Set<String> set = jedis.zrange(key, startSize, endSize);
            for (String s : set) {
                if (StringUtils.isNotEmpty(s)) {
                    list.add(s);
                }
            }
        } catch (Exception e) {

            logger.error("getZrangeSortdSet redis error !Key:" + key, e);
        } finally {
            release(jedis);
        }
        long end = System.currentTimeMillis();
        if ((end - start) > 100) {
            logger.error("getZrangeSortdSet redis slow　key:" + key + " ......................... time:" + (end - start));
        }
        return list;
    }

    private Jedis getJedis() {
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            logger.info("getJedis error,host:{},port:{}", ConstUtils.REDIS_HOST, ConstUtils.REDIS_PORT);
            throw e;
        }
    }

    private void release(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
            //logger.info("jedis release");
        }
    }

    public static void main(String[] args) {
        IRedisService redisService = new RedisServiceImpl();
        Long liveId = 910415L;
        String testKey = "test:liveId:" + liveId;

//        for (int i = 1; i <= 100; i++) {
//            //commonRedisDao.incrBy(seqKey,1);
//            //Long score = Long.valueOf(commonRedisDao.get(seqKey));
//            redisService.zadd(testKey, 1, i + "");
//        }

        List<String> list = redisService.getZrangeSortdSet(testKey, 99L, 99L);
        System.out.println(list.size() + "," + list);
        list = redisService.getZrangeSortdSet(testKey, 0L, 19L);
        System.out.println(list.size() + "," + list);
        list = redisService.getZrangeSortdSet(testKey, 0L, 9L);
        System.out.println(list.size() + "," + list);
        list = redisService.getZrangeSortdSet(testKey, 10L, 19L);
        System.out.println(list.size() + "," + list);
        System.out.println("---------------------");
        for (int i = 0; i < 10; i++) {
            list = redisService.getZrangeSortdSet(testKey, 0L, 19L);
            System.out.println(list.size() + "," + list);
        }

    }


}
