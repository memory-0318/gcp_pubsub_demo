package com.middleapp.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.middleapp.GcpPubSubReplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/8/7
 */
@Configuration
@RequiredArgsConstructor
public class ReplierConfig {
    public static final String REQUEST_SUB_ID = "simple-action-request-sub";
    public static final String RESPONSE_TOPIC_ID = "simple-action-response-topic";
    public static final String NOTIFY_BACK_APP_TOPIC_ID = "notify-back-app-topic";

    @Bean
    public GcpPubSubReplier createGcpPubSubReplier(PubSubTemplate pubSubTemplate) {
        return new GcpPubSubReplier(REQUEST_SUB_ID, RESPONSE_TOPIC_ID, NOTIFY_BACK_APP_TOPIC_ID, pubSubTemplate);
    }
}
