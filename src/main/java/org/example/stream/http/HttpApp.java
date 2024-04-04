package org.example.stream.http;

import akka.Done;
import akka.NotUsed;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.japi.function.Function;
import akka.japi.tuple.Tuple3;
import akka.stream.*;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.stream.data.dto.ResourceInfoDTO;
import org.example.stream.redis.IRedisService;
import org.example.stream.redis.impl.RedisServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletionStage;

/**
 * Author: wanghao
 * Date: 2024/4/2 18:13
 * Description:
 */
public class HttpApp extends BasicRoute {

    private static final Logger logger = LoggerFactory.getLogger(HttpApp.class);

    private static Map<Integer, BigDecimal> act2Score = new HashMap<>();

    private final IRedisService redisService = new RedisServiceImpl();

    static {
        act2Score.put(1,new BigDecimal("0.5"));
        act2Score.put(2,new BigDecimal("0.3"));
        act2Score.put(3,new BigDecimal("-0.5"));
    }

    public Route createRoute() {
        return extractRequest(request ->
                extractMaterializer(materializer ->
                        extractClientIP(remoteAddress -> {
                            // 取request header中的X-Forwarded-For信息
                            String ip = remoteAddress.getAddress().map(InetAddress::getHostAddress).orElse("");
                            if (ip.isEmpty()) {
                                ip = "127.0.0.1";
                            }
                            return concat(
                                    kPath(request, materializer, ip),
                                    testPath(request, materializer),
                                    test2Path(request, materializer)
                            ).orElse(complete(StatusCodes.get(404)));
                        })));
    }

    public Route kPath(HttpRequest request, Materializer materializer, String ip) {
        return path("k", () ->
                post(() -> {
                    // 获取post请求的参数
                    RequestEntity requestEntity = request.entity();
                    CompletionStage<Done> future;
                    if (requestEntity.isDefault()) {
                        logger.info("k receive default {}", requestEntity);
                        requestEntity
                                .toStrict(REQUEST_ENTITY_TIMEOUT, materializer)
                                .thenCompose(strict -> createStreamGraph(strict, ip).run(materializer));

                        requestEntity
                                .toStrict(REQUEST_ENTITY_TIMEOUT, materializer)
                                .thenCompose(strict -> createAnotherStreamGraph(strict).run(materializer));

                    } else {
                        createStreamGraph(requestEntity, ip).run(materializer);
                        createAnotherStreamGraph(requestEntity);
                    }

                    return complete(StatusCodes.get(204));
                })
        );
    }

    public Route testPath(HttpRequest request, Materializer materializer) {
        return path("test", () ->
                post(() -> {
                    RequestEntity entity = request.entity();
                    RunnableGraph<CompletionStage<List<Object>>> streamGraph = createTestStreamGraph(entity);
                    CompletionStage<List<Object>> run = streamGraph.run(materializer);

                    run.thenAccept(result -> {
                        // 在这里处理您的结果
                        logger.info("Result:" + result);
                    }).exceptionally(ex -> {
                        // 处理异常情况
                        ex.printStackTrace();
                        return null;
                    });

                    return complete(StatusCodes.get(204));
                })
        );
    }

    public Route test2Path(HttpRequest request, Materializer materializer) {
        return path("test2", () ->
                post(() -> {
                    RequestEntity entity = request.entity();
                    RunnableGraph<CompletionStage<Object>> streamGraph = createTestStreamGraphV2(entity);
                    CompletionStage<Object> run = streamGraph.run(materializer);
                    run.thenAccept(result -> {
                        Long count = redisService.zcard("sss");
                        // 在这里处理您的结果
                        logger.info("Result:{},{}",result,count);
                    }).exceptionally(ex -> {
                        // 处理异常情况
                        ex.printStackTrace();
                        return null;
                    });
                    return complete(StatusCodes.get(204));
                })
        );
    }

    private RunnableGraph<CompletionStage<Done>> createStreamGraph(RequestEntity requestEntity, String ip) {
        return parseRequestEntity(requestEntity)
                .toMat(Sink.ignore(), Keep.right());
    }

    private RunnableGraph<CompletionStage<Done>> createAnotherStreamGraph(RequestEntity requestEntity) {
        return parseRequestEntity(requestEntity)
                .toMat(Sink.ignore(), Keep.right());
    }

    private RunnableGraph<CompletionStage<Object>> createTestStreamGraphV2(RequestEntity requestEntity) {
        Source<ByteString, Object> originalSource = requestEntity.getDataBytes();

        Flow<ByteString, ResourceInfoDTO, NotUsed> flow = Flow.of(ByteString.class)
                .map((Function<ByteString, ResourceInfoDTO>) param -> {
                    String str = param.utf8String();
                    logger.info("receive data:{}", str);
                    ResourceInfoDTO dto = JSON.parseObject(str, ResourceInfoDTO.class);

                    JSONObject jsonObject = JSON.parseObject(str);
                    //用户行为过滤：收听，订阅，取消订阅
                    Integer act = jsonObject.getInteger("act");
                    if (act == 1 || act == 2 || act == 3) {
                        //padding 用户行为得分
                        dto.setScore(act2Score.get(act));
                        //padding 判断用户是否登录
                        Long userId = jsonObject.getLong("userId");
                        if (userId != null && userId > 0) {
                            //已登录
                            dto.setLoginStatus(1);
                            dto.setUserAccount(userId.toString());
                        } else {
                            //未登录
                            dto.setLoginStatus(2);
                            dto.setUserAccount(jsonObject.getString("deviceId"));
                        }
                        //padding 判断处于哪个时间段
                        dto.setStage(getStageByNow());
                        return dto;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull);

        Source<ResourceInfoDTO, Object> source = originalSource.via(flow);

        Sink<Object, CompletionStage<Object>> sink = Sink.head();

        RunnableGraph<CompletionStage<Object>> runnableGraph = RunnableGraph.fromGraph(GraphDSL.create(sink, (builder, out) -> {

            Outlet<ResourceInfoDTO> outlet = builder.add(source).out();

            builder.from(outlet)
                    .to(out);
            return ClosedShape.getInstance();
        }));

        return runnableGraph;
    }

    private Integer getStageByNow() {
        Integer stage = null;
        LocalDateTime localDateTime = LocalDateTime.now();
        String yyyyMMdd = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time1 = yyyyMMdd + " 00:00:00";
        String time2 = yyyyMMdd + " 06:00:00";
        String time3 = yyyyMMdd + " 10:00:00";
        String time4 = yyyyMMdd + " 16:00:00";
        String time5 = yyyyMMdd + " 20:00:00";
        String time6 = yyyyMMdd + " 23:59:59";

        LocalDateTime localDateTime1 = LocalDateTime.parse(time1, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localDateTime2 = LocalDateTime.parse(time2, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localDateTime3 = LocalDateTime.parse(time3, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localDateTime4 = LocalDateTime.parse(time4, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localDateTime5 = LocalDateTime.parse(time5, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localDateTime6 = LocalDateTime.parse(time6, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (localDateTime.isAfter(localDateTime1) && localDateTime.isBefore(localDateTime2)) {
            stage = 4;
        } else if (localDateTime.isAfter(localDateTime2) && localDateTime.isBefore(localDateTime3)) {
            stage = 1;
        } else if (localDateTime.isAfter(localDateTime3) && localDateTime.isBefore(localDateTime4)) {
            stage = 2;
        } else if (localDateTime.isAfter(localDateTime4) && localDateTime.isBefore(localDateTime5)) {
            stage = 3;
        } else if (localDateTime.isAfter(localDateTime5) && localDateTime.isBefore(localDateTime6)) {
            stage = 4;
        }
        return stage;
    }


    private RunnableGraph<CompletionStage<List<Object>>> createTestStreamGraph(RequestEntity requestEntity) {
        Source<ByteString, Object> originalSource = requestEntity.getDataBytes();

        Flow<ByteString, ResourceInfoDTO, NotUsed> flow = Flow.of(ByteString.class).map((Function<ByteString, ResourceInfoDTO>) param -> {
            String str = param.utf8String();
            logger.info("receive data:{}", str);
            return JSON.parseObject(str, ResourceInfoDTO.class);
        });
        Source<ResourceInfoDTO, Object> objectSource = originalSource.via(flow);


        Sink<Object, CompletionStage<List<Object>>> sink = Sink.seq();
        Flow<ResourceInfoDTO, Long, NotUsed> flow1 = Flow.of(ResourceInfoDTO.class).map(elem -> elem.getResourceId());
        //Flow<Long, Long, NotUsed> flow2 = Flow.of(Long.class).map(elem -> elem + 10);
        Flow<ResourceInfoDTO, Long, NotUsed> flow3 = Flow.of(ResourceInfoDTO.class).map(elem -> elem.getResourceId() + 10);
        Flow<Long, Long, NotUsed> flow4 = Flow.of(Long.class).map(elem -> elem + 20);

        RunnableGraph<CompletionStage<List<Object>>> runnableGraph = RunnableGraph.fromGraph(GraphDSL.create(sink, (builder, out) -> {
            UniformFanOutShape<ResourceInfoDTO, ResourceInfoDTO> bcast = builder.add(Broadcast.create(2));
            UniformFanInShape<Long, Long> merge = builder.add(Merge.create(2));

            Outlet<ResourceInfoDTO> source = builder.add(objectSource).out();

            builder.from(source)
                    .viaFanOut(bcast)
                    .via(builder.add(flow1))
                    .viaFanIn(merge)
                    .via(builder.add(flow4))
                    .to(out);
            builder.from(bcast).via(builder.add(flow3)).viaFanIn(merge);
            return ClosedShape.getInstance();
        }));

        return runnableGraph;
    }
}
