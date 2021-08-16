package saechimdaeki.rsocket_server;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.*;

@Controller
public class RSocketService {

    private final ItemRepository repository;
    private final EmitterProcessor<Item> itemProcessor;

    private final FluxSink<Item> itemSink;

    //  Deprecated인 FluxProcessor, EmitterProcessor의 대체 구현
	private final Sinks.Many<Item> itemsSink;

    public RSocketService(ItemRepository repository) {
        this.repository = repository;
        this.itemProcessor = EmitterProcessor.create();
        this.itemSink = this.itemProcessor.sink();
        //  Deprecated인 FluxProcessor, EmitterProcessor의 대체 구현
		this.itemsSink = Sinks.many().multicast().onBackpressureBuffer();

    }

    @MessageMapping("newItems.request-response")
    public Mono<Item> processNewItemsViaRSocketRequestResponse(Item item) {
        return this.repository.save(item)
                .doOnNext(this.itemSink::next);
        //  Deprecated인 FluxProcessor, EmitterProcessor의 대체 구현
//				.doOnNext(savedItem -> this.itemsSink.tryEmitNext(savedItem));
    }

    @MessageMapping("newItems.request-stream")
    public Flux<Item> findItemsViaRSocketRequestStream() {
        return this.repository.findAll()
                .doOnNext(this.itemSink::next);
        //  Deprecated인 FluxProcessor, EmitterProcessor의 대체 구현
//				.doOnNext(this.itemsSink::tryEmitNext);
    }

    @MessageMapping("newItems.fire-and-forget")
    public Mono<Void> processNewItemsViaRSocketFireAndForget(Item item) {
        return this.repository.save(item)
                .doOnNext(this.itemSink::next)
                //  Deprecated인 FluxProcessor, EmitterProcessor의 대체 구현
//				.doOnNext(savedItem -> this.itemsSink.tryEmitNext(savedItem))
                .then();
    }

    @MessageMapping("newItems.monitor")
    public Flux<Item> monitorNewItems() {
        return this.itemProcessor;
        //  Deprecated인 FluxProcessor, EmitterProcessor의 대체 구현
//		return this.itemsSink.asFlux();
    }
}
