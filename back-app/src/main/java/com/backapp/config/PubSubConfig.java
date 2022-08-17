package com.backapp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.pubsub.v1.PubsubMessage;
import com.sun.jdi.VMMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.constant.PubSubMdcConstants.*;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/7/27
 */
@Configuration
public class PubSubConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubConfig.class);
    public static final String NOTIFY_BACK_APP_CHANNEL = "notify-back-app-channel";
    public static final String NOTIFY_BACK_APP_SUBSCRIPTION = "notify-back-app-sub";

    @Bean(name = NOTIFY_BACK_APP_CHANNEL)
    public MessageChannel createNotifyBackAppChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter createNotifyBackAppChannelAdapter(
        @Qualifier(NOTIFY_BACK_APP_CHANNEL) MessageChannel messageChannel,
        PubSubTemplate pubSubTemplate
    ) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
            NOTIFY_BACK_APP_SUBSCRIPTION);
        adapter.setOutputChannel(messageChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);

        return adapter;
    }

    @ServiceActivator(inputChannel = NOTIFY_BACK_APP_CHANNEL)
    public void createNotifyBackAppMsgHandler(
        String payload,
        @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage acknowledgeablePubsubMessage
    ) {
        PubsubMessage message = acknowledgeablePubsubMessage.getPubsubMessage();
        Map<String, String> messageAttributesMap = message.getAttributesMap();
        MDC.put(EVENT_ATTR_CONVERSATIONAL_ID, messageAttributesMap.get(EVENT_ATTR_CONVERSATIONAL_ID));
        MDC.put(MDC_FIELD_TRACE_ID, messageAttributesMap.get(MDC_FIELD_TRACE_ID));
        MDC.put(EVENT_ATTR_INITIATOR_ID, messageAttributesMap.getOrDefault(EVENT_ATTR_INITIATOR_ID, ""));
        MDC.put(EVENT_ATTR_MESSAGE_ID, message.getMessageId());

        try {
            String logText = String.format("Notified: %s",
                new ObjectMapper().writeValueAsString(MDC.getCopyOfContextMap()));
            LOGGER.info(logText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ListenableFuture<Void> ackFuture = acknowledgeablePubsubMessage.ack();
        try {
            ackFuture.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Exception occurred when sending ack");
        }
    }
}
