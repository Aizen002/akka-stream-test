package org.example.stream.http;

import akka.NotUsed;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.server.AllDirectives;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: wanghao
 * Date: 2024/4/2 18:11
 * Description:
 */
public class BasicRoute extends AllDirectives {

    private static final Logger logger = LoggerFactory.getLogger(BasicRoute.class);

    public static final long REQUEST_ENTITY_TIMEOUT = 60 * 1000;

    public Source<List<String>, NotUsed> parseRequestEntity(RequestEntity requestEntity) {
        Source<ByteString, Object> source = requestEntity.getDataBytes();

        Flow<ByteString, List<String>, NotUsed> flow = Flow.fromFunction(bs -> {
            List<String> result = new ArrayList<>();
            String str = bs.utf8String();
            logger.info("parse byteString flow {}", str);
            try {

            }catch (Exception e){
                logger.error(e.getMessage(), e);
            }
            return result;
        });
        return source.viaMat(flow, Keep.right());
    }


}
