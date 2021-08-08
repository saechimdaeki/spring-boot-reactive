package com.saechimdaeki.chap05;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class) //@ExtendWith는 테스트 핸들러를 지정할 수 있는 JUNIT5 API이다. 스프링에 특화된 테스트
//기능을 사용할 수 있게 해준다.
public class InventoryServiceUnitTest {

    InventoryService inventoryService;

    @MockBean private ItemRepository itemRepository;
    @MockBean private CartRepository cartRepository;

    @BeforeEach
    void setUp(){
        //테스트 데이터 정의
        Item sampleItem=new Item("item1","TV tray","Alf TV tray",19.99);
        CartItem sampleCartItem = new CartItem(sampleItem);
        Cart sampleCart = new Cart("My Cart", Collections.singletonList(sampleCartItem));

        //협력자와의 상호작용 정의
        when(cartRepository.findById(anyString())).thenReturn(Mono.empty());
        when(itemRepository.findById(anyString())).thenReturn(Mono.just(sampleItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(sampleCart));

        inventoryService=new InventoryService(itemRepository,cartRepository);
    }

    @Test
    void addItemToEmptyCartShouldProduceOneCartItem(){
        inventoryService.addItemToCart("My Cart","item1")
                .as(StepVerifier::create)
                .expectNextMatches(cart -> {
                    assertThat(cart.getCartItems()).extracting(CartItem::getQuantity)
                            .containsExactlyInAnyOrder(1);

                    assertThat(cart.getCartItems()).extracting(CartItem::getItem)
                            .containsExactly(new Item("item1","TV tray","Alf TV tray",19.99));

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void alternativeWayToTest(){
        StepVerifier.create(
                inventoryService.addItemToCart("My Cart","item1"))
                .expectNextMatches(cart -> {
                    assertThat(cart.getCartItems()).extracting(CartItem::getQuantity)
                            .containsExactlyInAnyOrder(1);

                    assertThat(cart.getCartItems()).extracting(CartItem::getItem)
                            .containsExactly(new Item("item1","TV tray","Alf TV tray",19.99));

                    return true;
                })
                .verifyComplete();
    }
}
