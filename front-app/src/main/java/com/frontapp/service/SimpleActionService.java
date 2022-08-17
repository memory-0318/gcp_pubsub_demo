package com.frontapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontapp.GcpPubSubRequestor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/7/17
 */
@Service
@RequiredArgsConstructor
public class SimpleActionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleActionService.class);
    private final ObjectMapper objectMapper;

    private final GcpPubSubRequestor requestor;

    public void sendSimpleActionRequest(String message) {
        this.requestor.sendRequestMessage(message);
    }
    //    public void sendSimpleActionRequest(String message) {
    //        if (StringUtils.isEmpty(message)) {
    //            throw new RuntimeException("Message should not be null or empty string.");
    //        }
    //
    //        MessageWrapper<String> eventMessage = MessageWrapper.<String>newBuilder()
    //            .setHeader(MessageHeader.newBuilder()
    //                .setConversationalId(MDC.get(StackdriverTraceConstants.MDC_FIELD_TRACE_ID))
    //                .setInitiatorId(null)
    //                .build())
    //            .setBody(new MessageBody<>(message)).build();
    //
    //        String eventMessageString = null;
    //        try {
    //            eventMessageString = this.objectMapper.writeValueAsString(eventMessage);
    //        } catch (JsonProcessingException e) {
    //            throw new RuntimeException("Exception occurred when casting MessageWrapper to text.");
    //        }
    //        this.messagingGateway.sendSimpleActionRequest(eventMessageString);
    //    }
}
