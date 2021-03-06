/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.saechimdaeki.chap03;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;


@Controller
@RequiredArgsConstructor
public class HomeController {

	private final ItemRepository itemRepository;
	private final CartRepository cartRepository;
	private final CartService cartService;

	@GetMapping
	Mono<Rendering> home() {
		return Mono.just(Rendering.view("home.html")
				.modelAttribute("items",
						this.itemRepository.findAll().doOnNext(System.out::println))
				.modelAttribute("cart",
						this.cartRepository.findById("My Cart")
								.defaultIfEmpty(new Cart("My Cart")))
				.build());
	}

	@PostMapping("/add/{id}")
	Mono<String> addToCart(@PathVariable String id) {
		return this.cartService.addToCart("My Cart",id)
				.thenReturn("redirect:/");
	}


	@PostMapping
	Mono<String> createItem(@ModelAttribute Item newItem) {
		return this.itemRepository.save(newItem)
				.thenReturn("redirect:/");
	}

	@DeleteMapping("/delete/{id}")
	Mono<String> deleteItem(@PathVariable String id) {
		return this.itemRepository.deleteById(id)
				.thenReturn("redirect:/");
	}
}
