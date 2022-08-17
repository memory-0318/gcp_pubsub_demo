package com.middleapp;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.constant.PubSubMdcConstants.*;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/8/6
 */
public class GcpPubSubReplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPubSubReplier.class);

    private final String requestSubscriptionId;
    private final String responseTopicId;
    private final String notifyBackTopicId;

    private final PubSubTemplate pubSubTemplate;

    public GcpPubSubReplier(
        String requestSubscriptionId,
        String responseTopicId,
        String notifyBackAppTopicId,
        PubSubTemplate pubSubTemplate) {
        this.requestSubscriptionId = requestSubscriptionId;
        this.responseTopicId = responseTopicId;
        this.pubSubTemplate = pubSubTemplate;
        this.notifyBackTopicId = notifyBackAppTopicId;

        this.init();
    }

    protected void init() {
        this.pubSubTemplate.subscribe(this.requestSubscriptionId, acknowledgeablePubsubMessage -> {
            this.postRequestMessageReceived(acknowledgeablePubsubMessage);
            ListenableFuture<Void> future = acknowledgeablePubsubMessage.ack();
            try {
                future.get(10, TimeUnit.SECONDS);
                LOGGER.info("Successfully sent ack to request topic");

                this.sendResponseMessage("Success");
                this.notifyBackApp();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("Exception occurred when sending ack");
            }
        });
    }

    public void sendResponseMessage(String payload) {
        Objects.requireNonNull(payload, "The response payload should not be null");

        Map<String, String> currentCtxMap = MDC.getCopyOfContextMap();
        currentCtxMap.put(EVENT_ATTR_INITIATOR_ID, "");
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
            .putAllAttributes(currentCtxMap)
            .setData(ByteString.copyFromUtf8(payload))
            .build();

        ListenableFuture<String> future = this.pubSubTemplate.publish(this.responseTopicId, pubsubMessage);
        try {
            String messageId = future.get(10, TimeUnit.SECONDS);
            currentCtxMap.put(EVENT_ATTR_MESSAGE_ID, messageId);
            this.postResponseMessageSent(messageId, payload, currentCtxMap);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Exception occurred when sending response message");
        }
    }

    public void notifyBackApp() {
        Map<String, String> currentCtxMap = MDC.getCopyOfContextMap();
        currentCtxMap.put(EVENT_ATTR_INITIATOR_ID, MDC.get(EVENT_ATTR_MESSAGE_ID));
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
            .putAllAttributes(currentCtxMap)
            .setData(ByteString.copyFromUtf8("Wake up"))
            .build();

        ListenableFuture<String> future = this.pubSubTemplate.publish(this.notifyBackTopicId, pubsubMessage);

        try {
            Map<String, String> notifyEventCtxMap = new HashMap<>(currentCtxMap);
            String messageId = future.get(10, TimeUnit.SECONDS);
            notifyEventCtxMap.put(EVENT_ATTR_MESSAGE_ID, messageId);

            try {
                Map<String, String> tempCtxMap = MDC.getCopyOfContextMap();
                MDC.setContextMap(notifyEventCtxMap);
                String logText = String.format("Notified back app: %s",
                    new ObjectMapper().writeValueAsString(notifyEventCtxMap));
                LOGGER.info(logText);
                MDC.setContextMap(tempCtxMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Exception occurred when notifying back app");
        }
    }

    protected void postRequestMessageReceived(BasicAcknowledgeablePubsubMessage acknowledgeablePubsubMessage) {
        PubsubMessage pubsubMessage = acknowledgeablePubsubMessage.getPubsubMessage();
        Map<String, String> messageAttributesMap = pubsubMessage.getAttributesMap();

        MDC.put(EVENT_ATTR_CONVERSATIONAL_ID,
            messageAttributesMap.getOrDefault(EVENT_ATTR_CONVERSATIONAL_ID, MDC.get(MDC_FIELD_TRACE_ID)));
        MDC.put(MDC_FIELD_TRACE_ID,
            messageAttributesMap.getOrDefault(EVENT_ATTR_CONVERSATIONAL_ID, MDC.get(MDC_FIELD_TRACE_ID)));
        MDC.put(EVENT_ATTR_REQUESTOR_ID, pubsubMessage.getMessageId());
        MDC.put(EVENT_ATTR_MESSAGE_ID, pubsubMessage.getMessageId());
        MDC.put(EVENT_ATTR_INITIATOR_ID, "");

        this.logRequestMessage(pubsubMessage);
    }

    protected void logRequestMessage(PubsubMessage pubsubMessage) {
        try {
            String logText = String.format("Received request: %s",
                new ObjectMapper().writeValueAsString(MDC.getCopyOfContextMap()));
            LOGGER.info(logText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void postResponseMessageSent(String messageId, String payload, Map<String, String> copyOfOriginalCtxMap) {
        try {
            Map<String, String> tempCtx = MDC.getCopyOfContextMap();
            MDC.setContextMap(copyOfOriginalCtxMap);
            String logText = String.format("Sent response: %s",
                new ObjectMapper().writeValueAsString(copyOfOriginalCtxMap));
            LOGGER.info(logText);
            MDC.setContextMap(tempCtx);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
