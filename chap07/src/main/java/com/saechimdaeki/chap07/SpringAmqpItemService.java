package com.saechimdaeki.chap07;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpringAmqpItemService {

    private final ItemRepository repository;

    @RabbitListener(
            ackMode = "MANUAL",
            bindings = @QueueBinding(
                    value = @Queue,
                    exchange = @Exchange("hacking-spring-boot"),
                    key="new-items-spring-amqp"))
    public Mono<Void> processNewItemsViaSpringAmqp(Item item){
        log.info("Consuming => {}",item);
        return this.repository.save(item).then();
    }
}
