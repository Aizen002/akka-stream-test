package org.example.stream.http;

import akka.Done;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import akka.stream.javadsl.RunnableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.CompletionStage;

/**
 * Author: wanghao
 * Date: 2024/4/2 18:13
 * Description:
 */
public class HttpApp extends BasicRoute{

    private static final Logger logger = LoggerFactory.getLogger(HttpApp.class);

    public Route createRoute() {
        return extractRequest(request ->
                extractMaterializer(materializer ->
                        extractClientIP(remoteAddress -> {

                            // 取request header中的X-Forwarded-For信息
                            final String ip = remoteAddress.getAddress().map(InetAddress::getHostAddress).orElse("");
                            if (ip.isEmpty()) {
                                logger.error("request ip is empty");
                                return complete(StatusCodes.get(500));
                            }
                            return concat(
                                    kPath(request, materializer, ip)
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

                        future = requestEntity
                                .toStrict(REQUEST_ENTITY_TIMEOUT, materializer)
                                .thenCompose(strict -> createStreamGraph(strict, ip).run(materializer));

                    } else {
                        future = createStreamGraph(requestEntity, ip).run(materializer);
                    }

                    return complete(StatusCodes.get(204));
                })
        );
    }

    private RunnableGraph<CompletionStage<Done>> createStreamGraph(RequestEntity requestEntity, String ip) {
        return null;
    }
}
