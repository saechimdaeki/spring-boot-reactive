package saechimdaeki.springsecurity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ItemRepository extends ReactiveCrudRepository<Item, String> {

    Mono<Item> findByName(String name);

}
