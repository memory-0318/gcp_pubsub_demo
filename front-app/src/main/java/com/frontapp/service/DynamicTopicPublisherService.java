package com.frontapp.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.logging.StackdriverTraceConstants;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/7/17
 */
@Service
@RequiredArgsConstructor
public class DynamicTopicPublisherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTopicPublisherService.class);

    private final GcpProjectIdProvider gcpProjectIdProvider;

    public void publishMessage(String topicId, String message) {
        TopicName topicName = this.createTopicName(this.gcpProjectIdProvider.getProjectId(), topicId);

        ByteString data = this.createByteStringData(message);
        String globalTxnId = MDC.get(StackdriverTraceConstants.MDC_FIELD_TRACE_ID);
        String parentTxnId = "";
        String localTxnId = MDC.get(StackdriverTraceConstants.MDC_FIELD_SPAN_ID);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
            .putAttributes("GlobalTxnID", globalTxnId)
            .putAttributes("ParentTxnID", parentTxnId)
            .putAttributes("LocalTxnID", localTxnId)
            .setData(data)
            .build();

        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName)
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Exception occurred when creating Pub/Sub publisher.", e);
        }

        ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
        try {
            String messageId = messageIdFuture.get();

            String logText = String.format("Topic: %s, MsgID: %s, GlobalTxnID: %s, ParentTxnID: %s, LocalTxnID: %s",
                topicId, messageId, globalTxnId, parentTxnId, localTxnId);
            LOGGER.info(logText);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Exception occurred when getting message id.", e);
        } finally {
            this.shutdownPublisher(publisher);
        }
    }

    private TopicName createTopicName(String projectId, String topicId) {
        return TopicName.of(projectId, topicId);
    }

    private ByteString createByteStringData(String message) {
        return ByteString.copyFromUtf8(message);
    }

    private void shutdownPublisher(Publisher publisher) {
        try {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Exception occurred when waiting publisher termination.", e);
        }
    }
}
