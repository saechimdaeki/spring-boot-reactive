package saechimdaeki.springsecurity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;


@Controller
@RequiredArgsConstructor
public class HomeController {

    private final InventoryService inventoryService;

    @GetMapping
    Mono<Rendering> home(Authentication auth) {
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items", this.inventoryService.getInventory())
                .modelAttribute("cart", this.inventoryService.getCart(cartName(auth))
                        .defaultIfEmpty(new Cart(cartName(auth))))
                .modelAttribute("auth", auth)
                .build());
    }

    @PostMapping("/add/{id}")
    Mono<String> addToCart(Authentication auth, @PathVariable String id) {
        return this.inventoryService.addItemToCart(cartName(auth), id)
                .thenReturn("redirect:/");
    }

    @DeleteMapping("/remove/{id}")
    Mono<String> removeFromCart(Authentication auth, @PathVariable String id) {
        return this.inventoryService.removeOneFromCart(cartName(auth), id)
                .thenReturn("redirect:/");
    }

    @PostMapping
    @ResponseBody
    Mono<Item> createItem(@RequestBody Item newItem) {
        return this.inventoryService.saveItem(newItem);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    Mono<Void> deleteItem(@PathVariable String id) {
        return this.inventoryService.deleteItem(id);
    }

    private static String cartName(Authentication auth) {
        return auth.getName() + "'s Cart";
    }
}
