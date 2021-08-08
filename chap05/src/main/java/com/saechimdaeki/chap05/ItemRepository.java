
package com.saechimdaeki.chap05;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ItemRepository extends ReactiveCrudRepository<Item, String> {

    Flux<Item> findByNameContaining(String partialName);

    @Query("{ 'name' : ?0, 'age' :  }")
    Flux<Item> findItemsForCustomerMonthlyReport();

    @Query(sort = "{ 'age' : -1", value = "{ 'name' : 'TV tray', 'age' : }")
    Flux<Item> findSortedStuffForWeeklyReport();


    Flux<Item> findByNameContainingIgnoreCase(String partialName);


    Flux<Item> findByDescriptionContainingIgnoreCase(String partialName);


    Flux<Item> findByNameContainingAndDescriptionContainingAllIgnoreCase(String partialName, String partialDesc);


    Flux<Item> findByNameContainingOrDescriptionContainingAllIgnoreCase(String partialName, String partialDesc);

}
