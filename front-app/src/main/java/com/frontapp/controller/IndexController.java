package com.frontapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/7/18
 */
@RestController
public class IndexController {
    @GetMapping
    public String showHelloWorld() {
        return "Hello World";
    }
}
