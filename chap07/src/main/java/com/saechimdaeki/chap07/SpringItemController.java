package com.saechimdaeki.chap07;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SpringItemController {
    private final AmqpTemplate template;

    @PostMapping("/items")
    Mono<ResponseEntity<?>> addNewItemUsingSpringAmqp(@RequestBody Mono<Item> item){
        return item.subscribeOn(Schedulers.boundedElastic())
                .flatMap(content -> {
                    return Mono.fromCallable( () -> {
                        this.template.convertAndSend(
                                "hacking-spring-boot", "new-items-spring-amqp",content
                        );
                        return ResponseEntity.created(URI.create("/items")).build();
                    });
                });
    }
}
