package com.frontapp.controller;

import com.frontapp.service.DynamicTopicPublisherService;
import com.frontapp.service.SimpleActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/7/17
 */
@RestController
@RequiredArgsConstructor
public class PublisherController {
    private final DynamicTopicPublisherService dynamicTopicPublisherService;
    private final SimpleActionService simpleActionService;

    @PostMapping(value = { "publish/topic/{topicId}" })
    public void publishMessage(@PathVariable("topicId") String topicId, @RequestParam("msg") String message) {
        this.dynamicTopicPublisherService.publishMessage(topicId, message);
    }

    @PostMapping(value = { "publish/fixedChannel" })
    public void publishMessageToOutboundChannel(@RequestParam("msg") String message) {
        this.simpleActionService.sendSimpleActionRequest(message);
    }
}
