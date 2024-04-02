package org.example.stream;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.model.StatusCodes;
import akka.japi.function.Function;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Author: $USER
 * Date: $DATE $TIME
 * Description:
 */
public class AkkaHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(AkkaHttpServer.class);


    public static void main(String[] args) {

        if (args == null || args.length < 2) {
            args = new String[]{"127.0.0.1", "8088"};
            logger.warn("args is null. used default args [{},{}] to configure.", args[0], args[1]);
        }

        Config config = ConfigFactory.load("clusterconfig.conf");
        ActorSystem system = ActorSystem.create("actorSystem", config);

        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            Http.get(system).newServerAt(host, port).bind((Function<HttpRequest, CompletionStage<HttpResponse>>) param -> {
                RequestEntity entity = param.entity();

                logger.info("url:{},k receive default {}", param.getUri(), entity);

                // 构建响应
                HttpResponse response = HttpResponse.create()
                        .withStatus(StatusCodes.OK)
                        .withEntity("Response data");

                return CompletableFuture.completedFuture(response);
            }).toCompletableFuture();
        } catch (RuntimeException e) {
            system.terminate();
            logger.error(e.getMessage(), e);
        }
    }
}