package com.frontapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.constant.PubSubMdcConstants.*;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/8/4
 */
public class GcpPubSubRequestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPubSubRequestor.class);

    private final String requestTopicId;
    private final String responseSubscriptionId;
    private final PubSubTemplate pubSubTemplate;

    public GcpPubSubRequestor(String requestTopicId, String responseSubscriptionId, PubSubTemplate pubSubTemplate) {
        this.requestTopicId = requestTopicId;
        this.responseSubscriptionId = responseSubscriptionId;
        this.pubSubTemplate = pubSubTemplate;

        this.init();
    }

    protected void init() {
        this.pubSubTemplate.subscribe(this.responseSubscriptionId, message -> {
            this.postResponseMessageReceived(message);
            this.logResponseMessage(message);
            ListenableFuture<Void> future = message.ack();
            try {
                future.get(10, TimeUnit.MINUTES);
                LOGGER.info("Successfully sent ack to response");
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("Exception occurred when sending ack to response");
            }
        });
    }

    public void sendRequestMessage(String payload) {
        Objects.requireNonNull(payload, "The request payload should not be null");

        this.preRequestMessageSent();
        Map<String, String> copyOfOriginalCtxMap = MDC.getCopyOfContextMap();
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
            .putAllAttributes(MDC.getCopyOfContextMap())
            .setData(ByteString.copyFromUtf8(payload))
            .build();

        ListenableFuture<String> future = this.pubSubTemplate.publish(this.requestTopicId, pubsubMessage);
        //        future.addCallback(
        //            messageId -> {
        //                copyOfOriginalCtxMap.put(MDC_FIELD_SPAN_ID, MDC.get(MDC_FIELD_SPAN_ID));
        //                copyOfOriginalCtxMap.put(EVENT_ATTR_MESSAGE_ID, messageId);
        //                copyOfOriginalCtxMap.put(EVENT_ATTR_REQUESTOR_ID, messageId);
        //                this.postRequestMessageSent(messageId, payload, copyOfOriginalCtxMap);
        //            },
        //            ex -> LOGGER.error("Exception occurred when publish request message")
        //        );
        try {
            String messageId = future.get(10, TimeUnit.SECONDS);
            copyOfOriginalCtxMap.put(EVENT_ATTR_MESSAGE_ID, messageId);
            this.postRequestMessageSent(messageId, payload, copyOfOriginalCtxMap);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            //            throw new RuntimeException(e);
            LOGGER.error("Exception occurred when publish request message", e);
        }
    }

    protected void preRequestMessageSent() {
        Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
        MDC.put(EVENT_ATTR_CONVERSATIONAL_ID, mdcCtxMap.get(MDC_FIELD_TRACE_ID));
    }

    protected void postRequestMessageSent(String messageId, String payload, Map<String, String> copyOfOriginalCtxMap) {
        MDC.setContextMap(copyOfOriginalCtxMap);
        MDC.put(EVENT_ATTR_MESSAGE_ID, messageId);
        MDC.put(EVENT_ATTR_REQUESTOR_ID, messageId);
        MDC.put(EVENT_ATTR_INITIATOR_ID, "");

        try {
            String logText = String.format("Sent request: %s",
                new ObjectMapper().writeValueAsString(MDC.getCopyOfContextMap()));
            LOGGER.info(logText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void postResponseMessageReceived(BasicAcknowledgeablePubsubMessage acknowledgeablePubsubMessage) {
        PubsubMessage message = acknowledgeablePubsubMessage.getPubsubMessage();
        Map<String, String> messageAttributesMap = message.getAttributesMap();

        MDC.put(EVENT_ATTR_CONVERSATIONAL_ID,
            messageAttributesMap.getOrDefault(EVENT_ATTR_CONVERSATIONAL_ID, MDC.get(MDC_FIELD_TRACE_ID)));
        MDC.put(MDC_FIELD_TRACE_ID,
            messageAttributesMap.getOrDefault(MDC_FIELD_TRACE_ID, MDC.get(MDC_FIELD_TRACE_ID)));
        MDC.put(EVENT_ATTR_INITIATOR_ID, "");
        MDC.put(EVENT_ATTR_REQUESTOR_ID, messageAttributesMap.getOrDefault(EVENT_ATTR_REQUESTOR_ID, ""));
        MDC.put(EVENT_ATTR_MESSAGE_ID, message.getMessageId());
    }

    protected void logResponseMessage(BasicAcknowledgeablePubsubMessage acknowledgeablePubsubMessage) {
        try {
            String logText = String.format("Received response: %s",
                new ObjectMapper().writeValueAsString(MDC.getCopyOfContextMap()));
            LOGGER.info(logText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
