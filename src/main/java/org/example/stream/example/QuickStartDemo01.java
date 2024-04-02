package org.example.stream.example;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.IOResult;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;

/**
 * Author: wanghao
 * Date: 2024/4/2 10:01
 * Description:
 */
public class QuickStartDemo01 {

    public static void main(String[] args) {

        Config config = ConfigFactory.load("clusterconfig.conf");
        ActorSystem system = ActorSystem.create("actorSystem", config);

        Source<Integer, NotUsed> source = Source.range(1, 100);
        //CompletionStage<Done> done = source.runForeach(i -> System.out.println(i), system);
        Source<BigInteger, NotUsed> factorials = source.scan(BigInteger.ONE, (acc, next) -> acc.multiply(BigInteger.valueOf(next)));
        CompletionStage<IOResult> done = factorials.map(BigInteger::toString).runWith(lineSink("/Users/wanghao/factorial2.txt"), system);
        done.thenRun(system::terminate);
    }

    public static Sink<String, CompletionStage<IOResult>> lineSink(String filename) {
        return Flow.of(String.class)
                .map(s -> ByteString.fromString(s + "\n"))
                .toMat(FileIO.toPath(Paths.get(filename)), Keep.right());
    }
}
