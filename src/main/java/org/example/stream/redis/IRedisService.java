package org.example.stream.redis;

import java.util.List;

/**
 * Author: wanghao
 * Date: 2024/2/22 11:54
 * Description:
 */
public interface IRedisService {

    Long zadd(String key, double score, String member);

    List<String> getZrangeSortdSet(String key, Long startSize, Long endSize);

    public Long zcard(final  String key);
}
