package org.example.stream.http;

import akka.http.javadsl.server.AllDirectives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: wanghao
 * Date: 2024/4/2 18:11
 * Description:
 */
public class BasicRoute extends AllDirectives {

    private static final Logger logger = LoggerFactory.getLogger(BasicRoute.class);

    public static final long REQUEST_ENTITY_TIMEOUT = 60 * 1000;


}
