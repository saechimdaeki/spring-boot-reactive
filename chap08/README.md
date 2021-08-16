# 8장 스프링 부트 R소켓

앞서 7장에서는 2개의 컴포넌트를 연결하기 위해 비동기 메시징을 사용하는 방법을 살펴봤다. 이번에는 리액티브 스트림 프로그래밍을 지원하기 위해

바닥부터 새로 만들고 있는 프로토콜인 `R소켓` 을 알아볼 차례다. 이번 8장에서는 다음과 같은 내용을 배운다

- 요청-응답 클라이언트/서버를 구성하는 방법
- 양쪽 모두에서 트래픽을 발생시킬 수 있는 양방향 서비스 구성

## R소켓 소개

지금까지 이 책을 통해 배우고 있는 모든 것은 결국 현재 자원을 잘 활용해서 더 높은 확장성을 가진 리액티브 애플리케이션을 만드는 방법이다.

그중에서도 배압(backpressure)은 리액티브 스트림의 근간을 이루는 핵심 개념이며, 이를 통해 확장성 있는 애플리케이션을 만들 수 있다.

배압을 이해하고 나면 무언가를 조회하기 위해 네트워크 경계를 넘나들어야 하는데 주로 HTTP를 기반으로 하는 원격기술을 사용한다.

하지만 HTTP는 리액티브하지 않다. HTTP는 "하남나 더 보내줘", "남은 것 모두다 보내줘" 같은 메시지를 표현할 수도 이해할 수도 없다.

HTTP는 요청-응답 패러다임에 뿌리를 두고 있다. 사람들은 HTTP를 통해 단순한 요청-응답을 넘어서 터널로 서로 연결하는 방법을 고민해왔고,

그 첫번째 해답은 클라이언트가 서버에게 요청을 보낸 후에 즉각적인 대답을 기대하지않고, 오래 기다리더라도 언제든 서버가 데이터를 보낼 준비가 됐을

때 서버로부터 응답을 받으면 응답을 처리하고 바로 새로운 요청을 서버에게 보내서 또 오래 기다리는 식으로 연결 지속성을 확보하는 롱 폴링이다.

이방식은 기대한 대로 동작은 했지만 자원을 점유한다는 한계가 있었고, 이는 웹소켓의 등장을 앞당겼다. 웹소켓도 OSI7 계층에 위치하며 2011년 표준화된

최신 프로토콜이다. 웹소켓은 요청-응답방식의 HTTP와는 다르게 양방향이다. 웹소켓은 가볍고 양방향 비동기 통신을 지원하지만 배압 개념이 없으므로

리액티브하지 않다. 7장에서 살펴본 것처럼 API가 비동기 방식이라고 해서 전체 과정이 리액티브한 것은 아니다. 그래서 진정한 해결책이 나오려면 새 

프로토콜이 필요하다. 리액티브 스트림을 근간으로 하는 새로운 프로토콜, 바로 `R소켓` 이다.

## 리액티브 프로토콜 탄생.

`R소켓` 은 HTTP, 웹소켓과 마찬가지로 OSI 7계층 프로토콜이다. R소켓은 웹소켓, TCP, 애런 등 여러가지 프로토콜 위에서 동작하도록 설계됐다. 정리하자면

웹소켓은 아주 가볍고 유연해서 R소켓이 필요로 하는 모든것을 지원한다. TCP는 OSI4계층에 위치하는 강력한 프로토콜이다. HTTP는 TCP의 연결관리를

사용해서 TCP위에서 동작한다. R소켓도 TCP를 사용해서 장애내성(fault-tolerant)과 확장성을 가진 리액티브 연결을 만들 수 있다.

애런은 UDP위에서 동작하는 메시징 프로토콜이다. UDP는 신뢰성 있는 연결을 필요로 하지않는 프로토콜이다. 리액터 애플리케이션은 작업부하 사이를

오가는 워커 스레드를 사용하므로, 작업 부하가 여러 가지 메시지로부터 만들어진다는 사실은 어렵지 않게 유추할 수 있다.

## R소켓 패러다임

R소켓은 당연하지만 소켓을 전제로 한다. 소켓은 연결을 맺고, 데이터를 송수신하는 데 신뢰성이 입증된 방식이다. R소켓은 단순히 연결에 사용되는 채널에

다른 API를 추가한 것이라고 이해할 수 있다. 하지만 먼저 다음과 같은 R소켓의 패러다임을 이해하는 것이 중요하다

- 요청-응답 request-response (1개의 스트림)
- 요청-스트림 request-stream (다수의 유한한 스트림)
- 실행 후 망각 fire-and-forget (무응답)
- 채널 channel (양방향)

### 요청-응답

최신프로토콜인 R소켓을 소개하는데 HTTP의 요청-응답 패러다임을 첫 번째로 언급하는 것이 조금 역설적이기도 하다. 하지만 실제로 통신에서 일반적으로

필요한 요구사항의 80%는 이 요청-응답 방식으로 해결할 수 있다. 요청-응답을 수행하려면 여러가지 작업을 수행해야한다. 원격 서비스에 데이터를 요청하고,

새 데이터를 전송하고 확정을 기다리는 등 여러 작업이 요청-응답을 통해 수행된다. HTTP는 오직 요청-응답 방식만 지원한다는 점이 문제라고 할 수 있다.

그래서 이를 보완할 수 있는 전략이 필요하다

### 요청-스트림

요청-스트림 방식은 한번의 요청을 보내고 스트림 형태로 응답을 계속 받을 수 있으므로 좀 더 효율적인 요청 방식이라고 할 수 있다. 롱폴링이나 코멧은

응답을 받을 때마다 처리를 하고 응답을 받은 후에 다시 요청을 보내는 일을 반복해야 한다. 이렇게 비슷한 처리 작업을 위해 요청-응답을 반복하는 것은

많은 오버헤드를 유발한다. 응답을 기다리면서 스레드가 점유되므로 트래픽이 많은 상황에서는 지연이 발생하는 주요 원인인 되기도 한다.

R소켓은 채널을 열고 요청을 보낸 후에 스레드를 점유하지 않고 스트림 형태로 응답을 받을 수 있다. 

주식 종목의 가격 정보를 요청하고 변화되는 주식 가격을 스트림 형태로 계속 응답받는 상황이 요청- 스트림 방식의 대표적인 사례라고 할 수 있다.

개발자는 가격 변동 정보가 언제 도착하는지 신경쓸 필요 없으며 가격이 변동될 때마다 언제든지 받을 수 있다. 물론 그렇다고 해서 서버가 10만개의

가격 정보 업데이트를 한번에 전송해도 된다는 뜻은 아니다. R소켓을 바탕으로 리액티브 프로그래밍을 사용하면 "다음 10개 정보를 보내줘"라고 요청을

보낼 수 있다.

### 실행 후 망각

실행 후 망각은 요청을 보내고 나서 응답은 신경쓰지 않는 뒤끝 없는 방식이지만 별로 대단해 보이지는 않을 수도 있다. 하지만 실행 후 망각은 단순한

응답 무시라고 할 수 만은 없다. 비동기 전송 방식으로 요청-응답을 주고받아본 적이 있다면, 응답은 원래의 요청과 연관돼야 한다는 점을 알고 있을 것이다.

그래서 연관 ID를 사용하기도 하는데, 이 과정에서 여러가지 복잡성이 생겨난다. 간과해서는 안 될 것은 지금 배우고 있는것은 리액티브 스트림이라는 것이다.

어떤 것도 스레드를 점유해서는 안된다. R소켓은 모든 요청이 응답 결과를 항상 필요로 하는것은 아니라는 점을 충분히 활용하면서 앞에서 언급한 연관성

유지에 의해 발생하는 오버헤드를 제거할 수 있다.

### 채널

R소켓의 세 가지 패러다임에서는 요청을 보내서 클라이언트와 요청을 처리하는 서버가 등장한다. 클라이언트와 서버는 다음과 같은 세 가지 선택지를

가지고 있다.

- 응답 대기
- 응답 대기 안함
- 무한 응답 대기

어느 쪽이든 요청을 보내는 것은 클라이언트라는 사실에는 변함이 없다.

채널 패러다임은 이런 틀을 깨고 진정한 메시지 지향 양방향 통신 채널을 실현한다. 채널의 어느 쪽이든 상대방에게 메시지를 전송할 수 있고, 양쪽 모두

리액티브 메시지 리스너를 반드시 등록해야한다. 7장에서 레빗엠큐를 리액터와 함께 사용하기 위한 설정이 단순하지는 않았다. 하지만 R소켓을 사용하면

그런 설정이 전혀 필요 없다. 게다가 R소켓에는 배압 기능도 포함돼 있다.

## R소켓 서버 생성

R소켓 서버와 R소켓 클라이언트를 만들려면 2개의 독립된 애플리케이션이 필요하다. 먼저 R소켓 서버를 만들어보자.

```groovy
    implementation 'org.springframework.boot:spring-boot-starter-rsocket'
```

이 의존관계를 통해 다음 기능이 프로젝트에 추가된다.

- RScoekt Core와 Transport Netty
- Reactor Netty
- Spring Messaging
- Jackson

세 가지 주요 요소는 다음과 같다

1. `R소켓`: 자바로 구현된 R소켓 프로토콜

2. `리액터 네티`:네티는 리액티브 메시지 관리자 역할도 충분히 수행할 수 있다. 리액터로 감싸져서 더 강력한 서버로 만들어졌다.

3. `스프링+잭슨`: 메시지가 선택되고 직렬화되며 전송되고 역직렬화되고 라우팅되는 것은 프로토콜의 리액티브 속성만큼이나 중요하다. 스프링의

   입증된 메시지 처리 아키텍쳐와 잭슨을 함께 사용하는 사례는 무수히 많으며 현장에서 충분히 검증됐다

이제 메시지를 받아서 처리하는 R소켓 서비스인 RSocketService 클래스를 작성해보자.

```java
@Service
@RequiredArgsConstructor
public class RSocketService {   
    private final ItemRepository repository;
}
```

이 클래스에 요청-응답, 실행 후 망각, 요청-스트림 처리 로직을 작성하기 전에 먼저 생각해볼 것이 있다. 이벤트가 어떻게 발생하기를 바라는가? 단순히 

Item객체를 몽고디비에 저장하는것은 어렵지 않게 만들 수 있다. 누군가 채널에 요청을 보내서 현재 존재하는 Item객체목록을 조회하는것도 만들 수 있다.

더 역동성을 추가해보자면 새로운 Item객체가 저장되면 스트림 갱신을 받도록 약속한 사람들에게 자동으로 정보를 제공하게 만들 수도 있다. 이런 방식은

리액티브 스트림 프로그래밍을 통해 가능해진다. 단순히 가능해지는 정도가 아니라 효율적이고 확장성이있다.

필요한 것은 새 Item을 계속 추가할 수 있는 Flux다. `FluxProcessor` 는 Item이 들어올 때마다 Flux에 추가할 수 있다. 그리고 이 Flux에 관심 있는

누구든지 구독을 통해 스트림 트래픽을 받아갈 수 있다. `FluxProcessor` 클래스의 코드를 작성하는 것은 그렇게 어렵지 않다. 가장 중요한 것은 코드가

아니라 요구사항 정의다.

- 가장 최근 메시지만 보내야 한다면 `EmitterProcessor`가 필요
- 최근 N개의 메시지를 보관하고 새로운 구독자에게 N개의 메시지를 모두 보내야 한다면 `ReplyProcessor` 가 필요하다.
- 단 하나의 컨슈머만을 대상으로 한다면 `UnicastProcessor` 가 필요하다.

8장에서는 누군가 구독했을 때 최근 메시지만 보내는 처리기가 예제로서 가장 적합하므로 `EmitterProcessor` 를 주로 다룬다

```java
@Service
public class RSocketService {

    private final ItemRepository repository;
    
    private final EmitterProcessor<Item> itemProcessor;
    private final FluxSink<Item> itemSink;
    // Deprecated인 EmitterProcess의 대체 구현, 위 2행 코드 대신 다음 코드로 대체
    
    private final Sinks.Many<Item> itemsSink;
    
    public RSocketService(ItemRepository repository){
        this.repository=repository;
        this.itemProcessor=EmitterProcessor.create();
        this.itemSink=this.itemProcessor.sink();
        
        // Deprecated인 EmitterProcess의 대체 구현, 위 2행 코드 대신 다음 코드로 대체
        this.itemsSink=Sinks.many().multicast().onBackpressureBuffer();
    }

}
```

- `EmitterProcessor` 에 새 Item을 추가하려면 진입점이 필요하며 이를 싱크라고 한다. sink()메소드를 호출해서 얻을 수 있다.

`EmitterProessor` 는 단지 Flux를 상속받은 특별한 버전의 Flux라는 점을 꼭 기억해두자. 리액티브 스트림이 사용되는 곳 어디에나 `EmitterProcessor`

를 전달할 수 있고, 새 Item객체를 주입하는 동안 구독하게 할 수 있다. RSocketService에 추가되는 아래 내용은 itemProcessor와 itemSink를 적절하게

가용하는 방법을 보여준다. 먼저 R소켓 요청-응답을 처리해보자.

```java
@MessageMapping("newItems.request-response")
    public Mono<Item> processNewItemsViaRSocketRequestResponse(Item item){
        return this.repository.save(item)
                .doOnNext(this.itemSink::next);
        
        //Deprecated 인 FluxProcessor, EmitterProcessor의 대체 구현
        // .doOnNext(savedItem -> this.itemsSink.tryEmitNext(savedItem));
    }
```

- 스프링 메시징의 @MessageMapping 애노테이션은 도착지가 newItems.request-response로 지정된 R소켓 메시지를 이 메소드로 라우팅

- 스프링 메시징은 메

- 시지가 들어오기를 리액티브하게 기다리고 있다가 메시지가 들어오면 메시지 본문을 인자로 해서 save() 메소드를 호출한다.

  반환 타입은 도메인 객체를 Item을 포함하는 리액터 타입이며, 이는 요청하는 쪽에서 예상하는 응답 메시지 시그니처와 일치한다.

전체 플로우는 결국 `Mono<Item>` 으로 귀결되며 R소켓은 적절한 배압 신호를 사용해서 메시지를 보낸 요청자에게 `Mono<Item>` 정보를 반환한다. 

상대적으로 단순한 흐름처럼 보이짐나 많은 설명이 필요하다. 전반적인 흐름은 도메인 객체 정보를 받아서, 처리한 후에, 그대로 반환하거나 부가적인 기능과

함께 원래 호출자에게 반환하는 것이다. 개념 자체는 단순해 보이는데 리액티브 플로우를 공유하려면 더 많은 설계상의 고민이 필요하다. 이런 고민은 저장된

도메인 객체를 다른 Flux응답을 기다리고 있는 구독자에게 공유하려고 할 때도 필요하다. 

이번엔 요청-스트림 방식을 알아보자. 요청-스트림은 여러개의 Item을 Flux에 담아 반환한다

```java
@MessageMapping("newItems.request-stream")
    public Flux<Item> findItemsViaRSocketRequestStream(){
        return this.repository.findAll()
                .doOnNext(this.itemSink::next);

        //Deprecated 인 FluxProcessor, EmitterProcessor의 대체 구현
        // .doOnNext(this.itemsSink::tryEmitNext);
    }
```

저장 대신 조회이고, Mono 대신 Flux에 담아 반환한다는 것 외에는 요청-응답 방식과 거의 같다. 최종 클라이언트의 요청을 받아서 회신하는 R소켓 클라이

언트 쪽에서는 R소켓 서버로부터 회신받은 Flux에 여러가지 연산과 배압을 적용해서 최종 클라이언트의 요구사항에 맞게 데이터를 제공할 수 있다.

실행 후 망각을 정의할 때도 아주 비슷한 흐름으로 전개된다

```java
@MessageMapping("newItems.fire-and-forget")
    public Mono<Void> processNewItemsViaRSocketFireAndForget(Item item){
        return this.repository.save(item)
                .doOnNext(this.itemSink::next)
                //Deprecated 인 FluxProcessor, EmitterProcessor의 대체 구현
                // .doOnNext(savedItem -> this.itemsSink.tryEmitNext(savedItem))
                .then();
    }
```

실행 후 망각에서 유일하게 다른점은 두 가지이다.

- 라우트
- 반환타입

라우트는 예제 수준에서는 요청-응답 방식과 그렇게 많이 다르지는 않다. 실제 애플리케이션이었다면 라우트 경로는 아마도 newItems.save와 비슷했을것이다

하지만 반환타입은 요청-응답 방식과 완전히 다르다. `Mono<Item>` 을 반환하는 대신에 `Mono<Void>` 를 반환한다 '실행 후 망각' 이므로 데이터를 반환할

필요가 없기 때문이다. 그렇다고 해서 반환타입이 void인 것은 아니다. 왜냐하면 리액티브 스트림 프로그래밍에서는 적어도 데이터 신호를 받을수 있는

수단은 반환해야 하기 때문이다. 리액티브 프로그래밍에서 데이터를 반환할 필요가 없을 때는 `Mono<Void>` 를 반환하면 딱 맞는다. `Mono<Void>` 는

반환할 수 있는 가장 기본적인 타입이며 리액티브 스트림 프로그래밍 규격도 준수한다.

리액터에서는 `then()` 연산자를 사용하면 Mono에 감싸져 있는 데이터를 사용하지 않고 버릴 수 있다. 실제로 데이터는 사용되지 않고 리액티브 

스트림의 제어 신호만 남아있다. 이제 마지막 시나리오에서 채널을 열고 새 Item객체 플로우를 받아서 처리하는 방법을 알아보자

```java
@MessageMapping("newItems.monitor")
    public Flux<Item> monitorNewItems(){
        return this.itemProcessor;
        //Deprecated 인 FluxProcessor, EmitterProcessor의 대체 구현
        // return this.itemsSink.asFlux();
    }
```

- 예제에서는 요청으로 들어오는 데이터가 없지만, 클라이언트가 요청에 데이터를 담아 보낼 수도 있다. 쿼리나 필터링처럼 클라이언트가 원하는 것을

  요청 데이터에 담아 보낼 수 도 있다. 그래서 반환타입은 다른방식에처럼 Mono가 아니라 Flux다

- 실제 반환되는 것은 단순히 EmitterProcessor다. EmitterProcessor도 Flux이므로 반환 타입에 맞는다. EmitterProcessor에는 입수, 저장, 발행

  된 Item객체들이 들어 있다. 이메소드를 구독하는 여러 주체들은 모두 EmitterProcessor에 담겨있는 Item객체들의 복사본을 받게 된다.

이런 내용을 웹페이지를 통해 전달할 수도있고, 감사 시스템에 연결할수도 있다. 사용처는 무궁무진하다. 앞서 세가지 예제에서 간과하지 말아야 할 것은

스프링 메시징과 스프링 부트의 자동설정 핸들러 덕분에 잭슨 직렬화/역직렬화와 메시지 라우팅을 쉽게 처리할 수 있었다는 점이다. 주변 준비 내용에 

지나치게 집중하지 말아야 핵심적인 애플리케이션 아키텍처를 구상할 수 있다. 서버쪽 연산 정의를 마쳤으면 이제 R소켓 트래픽을 받아내는 코드와 연결해야 

한다. 다음 단계는 서비스가 R소켓 연결을 어떻게 소비하는지 결정하는 일이다. 먼저 properties를 다음과 같이 설정하자

```properties
server.port=9000

spring.rsocket.server.port=7000

spring.rsocket.server.transport=tcp

```

이렇게하면 네티는 9000포트에서 실행되고 7000포트에서 R소켓 트래픽을 기다린다.  

몽고디비도 필요하므로 다른 터미널에서 다음 명령어를 쳐 몽고디비를 실행하자.

```bash
$ docker run -p 27017-27019:27017-27019 mongo
```

## R소켓 클라이언트 생성

이제 R소켓 클라이언트 프로젝트를 만들어보자. R소켓 클라이언트는 외부로부터 HTTP요청을 받아서 R소켓 연결을 통해 백엔드 서버로 요청을 전달한다.

그래서 HTTP요청을 받을 수 있는 웹플럭스 컨트롤러가 필요하다. 웹소켓을 사용해서 브라우저와 백엔드 서버가 통신하는 것만큼 복잡하지는 않겠지만

가능한 한 쉽게 이해할 수 있는 예제로 시작해보자.

```java
@RestController
public class RSocketController {

    private final Mono<RSocketRequester> requester;

    public RSocketController(RSocketRequester.Builder builder){
        this.requester=builder
                .dataMimeType(APPLICATION_JSON)
                .metadataMimeType(parseMediaType(MESSAGE_RSOCKET_ROUTING.toString()))
                .connectTcp("localhost",7000)
                .retry(5)
                .cache();
    }
}
```

- `Mono<RSocketRequester>` 는 스프링 프레임워크의 `RSocketRequester` 를 리액터로 감싼것이다. Mono를 사용하므로 R소켓에 연결된 코드는

  새 클라이언트가 구독할 때마다 호출된다.

- 스프링 부트는 RSocketRequesterAutoConfiguration 정책 안에서 자동으로 RSocketRequester.Builder qlsdmf aksemfdjwnsek.

그런데 RScoketRequester는 무엇일까? 자바 공식문서에 따르면 R소켓에 무언가를 보낼 때 사용하는 얇은 포장재와 같다. 결국 R소켓의

API는 프로젝트 리액터를 사용한다. R소켓에 스프링의 메시징 패러다임은 포함하지 않았다. `RSocketRequester` 를 사용해야 스프링 프레임워크와

연동된다. 이렇게 하면 도착지를 기준으로 메시지를 라우팅할 수 있다. 그리고 보너스로 트래픽의 인코딩/디코딩도 쉽게할 수 있다. 

`RSocketRequester` 를 사용하지 않으면 클라이언트와 서버 양쪽의 모든 R소켓 연결에서 데이터를 직접 관리해야 한다. 그런데 왜 Mono로 감쌀까?

리액터의 Mono패러다임은 연결을 R소켓 연결 세부정보를 포함하는 지연 구조체로 전환한다. 아무도 연결하지 않으면 R소켓은 열리지 않는다. 누군가

구독을 해야 세부정보가 여러 구독자에게 공유될 수 있다. 하나의 R소켓만으로 모든 구독자에게 서비스할 수 있다는 점도 중요하다. R소켓을 구독자마다

1개씩 만들 필요가 없다. 대신에 하나의 R소켓 파이프에 대해 구독자별로 하나씩 연결을 생성한다. 이렇게 준비과정을 마쳐야 R소켓이 네트워크를 통해

오가는 데이터 프레임을 리액티브하게 전송하고 배압을 처리하는데 집중할 수 있다. 스프링 프레임워크는 데이터 인코딩/디코딩과 라우팅을 담당할 수 있다

리액터는 요청 처리 전 과정을 지연 방식으로 수행할 수 있어서 자원 효율성을 높일 수 있다. 모든 준비 과정이 말 그대로 프로젝트 리액터를 사용해서 

진행되므로 R소켓과 스프링 웹플럭스를 함께 사용하기 위해 투박한 편법을 쓸 필요가 없다

## 웹플럭스 요청을 R소켓 요청-응답으로 전환

```java
		@PostMapping("/items/request-response")
    Mono<ResponseEntity<?>> addNewItemUsingRSocketRequestResponse(@RequestBody Item item){
        return this.requester
                .flatMap(rSocketRequester -> rSocketRequester
                .route("newItems.request-response")
                .data(item)
                .retrieveMono(Item.class))
                .map(savedItem -> ResponseEntity.created(
                        URI.create("/items/request-response")
                ).body(savedItem));
    }
```

- `Mono<RSocketRequester>` 에 flatMap()을 적용해서 이 요청을 newItems.request-response로 라우팅할 수 있다.

스프링 웹플럭스와 R소켓 API가 모두 프로젝트 리액터를 사용하는 덕분에 둘을 아주 매끄럽게 함게 사용할 수 있다. 둘은 하나의 플로우 안에서

체이닝으로 연결될 수 있어서 HTTP웹 요청을 받아서 R소켓 연결에 전달하고 응답을 받아서 클라이언트에 리액티브하게 반환할 수 있다.

요청-응답 서비스와 클라이언트는 테스트하기도 편리하다.

```java
@SpringBootTest
@AutoConfigureWebTestClient
public class RSocketTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ItemRepository repository;

    @Test
    void verifyRemoteOperationsThroughRSocketRequestResponse() throws InterruptedException{

        //데이터 초기화
        this.repository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        // 새 Item 생성
        this.webTestClient.post().uri("/items/request-response")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Item.class)
                .value(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                });

        Thread.sleep(500);

        this.repository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                }).verifyComplete();
    }
}
```

요청-스트림 테스트는 요청-응답 테스트와는 다르다. 특히 검증 로직 첫 부분에 StepVerifier가 나오지 않고 returnResult(),getResponseBody() 를 통해

일단 플로우에서 빠져나온 다음에 StepVerifier를 사용해서 검증을 시작하는 용법을 잘 기억해두자

## 웹플럭스 요청을 R소켓 실행후 망각으로 전환

다음 코드를 보면 R소켓의 실행 후 망각 패러다임 사용법을 알 수 있다.

```java
@PostMapping("/items/fire-and-forget")
    Mono<ResponseEntity<?>> addNewItemUsingRSocketFireAndForget(@RequestBody Item item){
        return this.requester
                .flatMap(rSocketRequester -> rSocketRequester
                .route("newItems.fire-and-forget").data(item).send())
                .then(Mono.just(
                        ResponseEntity.created(
                                URI.create("/items/fire-and-forget")
                        ).build()
                ));
    }
```

함수형 프로그래밍에서 비어 있는 Void를 무시하는것은 map()이나 flatMap()이나 마찬가지다. 그래서 `Mono<Void>` 를 map()이나 flatMap()을 사용해서

다른 것으로 전환하는 것은 불가능하다. R소켓 클라이언트와 서버가 실행 후 망각을 제대로 처리했는지 확인하는 테스트 케이스를 작성하자

```java
 @Test
    void verifyRemoteOperationsThroughRSocketFireAndForget() throws InterruptedException {

        this.repository.deleteAll() 
                .as(StepVerifier::create) 
                .verifyComplete();

        this.webTestClient.post().uri("/items/fire-and-forget") 
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99)) 
                .exchange() 
                .expectStatus().isCreated() 
                .expectBody().isEmpty(); 

        Thread.sleep(500); 

        this.repository.findAll() 
                .as(StepVerifier::create) 
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                }) 
                .verifyComplete();
    }
```

## 웹플럭스 요청을 R소켓 채널로 전환

R소켓에서 일대일로 통신하는 세 가지 패러다임을 테스트해봤다. 이제 R소켓의 양방향 채널 지원을 테스트해볼 차례다. 다음 코드는 이벤트 흐름을 구독할

수 있는 단일 메시지를 전송하는 예제다.

```java
@GetMapping(value = "/items",produces = TEXT_EVENT_STREAM_VALUE)
    Flux<Item> liveUpdateds(){
        return this.requester
        .flatMapMany(rSocketRequester -> rSocketRequester.route("newItems.monitor")
        .retrieveFlux(Item.class));        
    }
```

이제 클라이언트 애플리케이션을 재실행하고 터미널에서 curl -v localhost:8080/items를 실행하면, 클라이언트로부터의 결과를 기다린다. 

앞에서 작성한 두 가지 테스트 케이스를 실행하면 이 터미널에 Item결과가 표시되는 것을 볼 수 있다.

![image](https://user-images.githubusercontent.com/40031858/129509483-5c60b0d4-3118-4f38-a2fe-ee367012fb85.png)

curl명령을 실행하면 R소켓 클라이언트가 Content-Type 헤더 값이 text/event-stream인 스트림을 응답한다. 스트림 응답을 받으면 curl은 전체

결과를 모두 가져올 수 있을 때까지 기다렸다가 모두 받은 후 실행을 종료한느 방식으로 동작하지 않고, 결괏값이 생길 때마다 결과를 화면에 표시하고 

실행을 종료하지 않고 추가로 응답을 받을 수 있는 대기상태로 남는다.

## 정리

지금까지 8장에서 다룬 내용은 다음과 같다

-  네 가지 R소켓 패러다임 : 요청-응답, 요청-스트림, 실행 후 망각, 채널
- 네티를 웹 컨테이너로 사용하고 TCP를 전송 프로토콜로 사용하는 R소켓 서버 생성
- 웹 요청을 R소켓을 통해 전달하는 R소켓 클라이언트. 설정
- 스프링 포트폴리오와 리액터를 활용해서 기능적 코드와 전송 프로토콜인 R소켓을 매끄럽게 연동하는 방법

이어서 9장 '스프링 부트 애플리케이션 보안' 에서는 시스템 접근 권한을 가진 사람에게만 허가 받은 행위를 허용하고, 그 외의 행위는

불허하는 방법을 알아본다

