package com.saechimdaeki.chap03;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ItemRepository extends ReactiveCrudRepository<Item,String> {
    Flux<Item> findByNameContaining(String partialName);

}
