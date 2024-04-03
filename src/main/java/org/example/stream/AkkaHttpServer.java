package org.example.stream;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.example.stream.http.HttpApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

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
            HttpApp app = new HttpApp();
            Http.get(system)
                    .newServerAt(host, port)
                    .bind(app.createRoute())
                    .thenApply(binding -> binding.addToCoordinatedShutdown(Duration.ofSeconds(10), system));
            logger.info("Server online at http://{}:{}/", host, port);
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            system.terminate();
        }
    }
}