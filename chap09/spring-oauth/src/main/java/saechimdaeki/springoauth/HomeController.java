package saechimdaeki.springoauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;


@Controller
@RequiredArgsConstructor
public class HomeController {

    private final InventoryService inventoryService;


    @GetMapping
    Mono<Rendering> home(
                          @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                          @AuthenticationPrincipal OAuth2User oauth2User) {
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items", this.inventoryService.getInventory())
                .modelAttribute("cart", this.inventoryService.getCart(cartName(oauth2User))
                        .defaultIfEmpty(new Cart(cartName(oauth2User))))

                // 인증 상세 정보 조회는 조금 복잡하다.
                .modelAttribute("userName", oauth2User.getName())
                .modelAttribute("authorities", oauth2User.getAuthorities())
                .modelAttribute("clientName",
                        authorizedClient.getClientRegistration().getClientName())
                .modelAttribute("userAttributes", oauth2User.getAttributes())
                .build());
    }

    @PostMapping("/add/{id}")
    Mono<String> addToCart(@AuthenticationPrincipal OAuth2User oauth2User, @PathVariable String id) {
        return this.inventoryService.addItemToCart(cartName(oauth2User), id)
                .thenReturn("redirect:/");
    }

    @DeleteMapping("/remove/{id}")
    Mono<String> removeFromCart(@AuthenticationPrincipal OAuth2User oauth2User, @PathVariable String id) {
        return this.inventoryService.removeOneFromCart(cartName(oauth2User), id) //
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

    private static String cartName(OAuth2User oAuth2User) {
        return oAuth2User.getName() + "'s Cart";
    }
}
