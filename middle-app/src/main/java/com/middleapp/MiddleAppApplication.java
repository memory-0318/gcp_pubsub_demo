package com.middleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @author Brian Su <brian.su@tpisoftware.com>
 * @description:
 * @date: 2022/7/17
 */
@SpringBootApplication
public class MiddleAppApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MiddleAppApplication.class);
        application.run();
    }
}
