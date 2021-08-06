package com.saechimdaeki.chap03;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;

    Mono<Cart> addToCart(String cartId, String id){
        return this.cartRepository.findById(cartId)
                .defaultIfEmpty(new Cart(cartId))
                .flatMap(cart -> cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getItem()
                .getId().equals(id)).findAny()
                .map(cartItem -> {
                    cartItem.increment();
                    return Mono.just(cart);
                }).orElseGet(()-> this.itemRepository.findById(id)
                                .map(CartItem::new)
                                .doOnNext(cartItem ->
                                        cart.getCartItems().add(cartItem))
                                .map(cartItem -> cart)))
                .flatMap(this.cartRepository::save);
    }
}
