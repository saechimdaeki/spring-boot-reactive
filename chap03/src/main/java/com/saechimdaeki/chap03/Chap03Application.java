package com.saechimdaeki.chap03;

import org.thymeleaf.TemplateEngine;
import reactor.blockhound.BlockHound;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Chap03Application {
    public static void main(String[] args) {
        BlockHound.builder()
                .allowBlockingCallsInside(TemplateEngine.class.getCanonicalName(),"process")
                .install();
        SpringApplication.run(Chap03Application.class, args);
    }

}
