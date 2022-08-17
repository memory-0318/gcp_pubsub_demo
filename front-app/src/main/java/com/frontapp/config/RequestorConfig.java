package com.frontapp.config;

import com.frontapp.GcpPubSubRequestor;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/8/7
 */
@Configuration
@RequiredArgsConstructor
public class RequestorConfig {
    //    public static final String PROPERTY_SIMPLE_ACTION_REQUEST_TOPIC_ID = "simple-action-request.topic-id";
    public static final String PROPERTY_SIMPLE_ACTION_RESPONSE_SUB_ID = "simple-action-response.subscription-id";

    private final Environment environment;

    @Bean
    public GcpPubSubRequestor createGcpPubSubRequestor(PubSubTemplate pubSubTemplate) {
        //        String requestorTopicId = this.environment.getProperty(PROPERTY_SIMPLE_ACTION_REQUEST_TOPIC_ID);
        String requestorTopicId = "simple-action-request-topic";
        //        String responseSubId = this.environment.getProperty(PROPERTY_SIMPLE_ACTION_RESPONSE_SUB_ID);
        String responseSubId = "simple-action-response-sub";
        return new GcpPubSubRequestor(requestorTopicId, responseSubId, pubSubTemplate);
    }
}
