# 7장 스프링 부트 메시징

앞서 6장에서는 스프링 부트를 사용해서 API 서버를 만드는 방법을 심도 있게 살펴봤다. 요청을 입력받아서 원천 데이터를 전통적인 JSON 형태로

반환하는 서버를 만들었고, 하이퍼미디어의 진정한 힘을 제대로 활용할 수 있는 스프링 HATEOAS를 사용해서 구 버전 호환성을 지닌 API서버도 

만들어봤다. 이제 메시지 연결을 활용해서 컴포넌트 사이의 결합도를 낮추도록 아키텍쳐를 고도화해보자. 비동기 메시징은 동일한 애플리케이션 안에

있는 컴포넌트들 또는 네트워크를 통해 연결된 여러 마이크로서비스에 분산돼 있는 컴포넌트들을 연결하는 좋은 수단이 될 수 있다.

이번 7장에서 다룰 내용은 다음과 같다

- 스프링 부트에서 지원하는 다양한 메시징 솔루션
- 스프링 부트에서 직접 지원하지는 않지만 스프링 포트폴리오에서 지원하는 다양한 메시징 솔루션
- AMQP를 자세히 알아보고, 스프링 AMQP와 프로젝트 리액터를 활용해 웹 계층과 백엔드의 결합관계 해소

## 메시징 솔루션 선택

메시징 솔루션은 JMS, 아파치 카프카, AMQP, 레디스, 젬파이어, 아파치 지오드 등 매우 다양하다. 이 솔루션들은 공통점도 많지만 저마다

다른 관심사에 최적화돼 있다. 어떤 솔루션이 어떤 시나리오에 맞는지는 시나리오에 따라 모두 다르므로 이책에서는 다루지 않는다. 

대신에, 메시징을 활용하고 리액티브 스트림 프로그래밍에 적절히 통합하는 방법을 배운다.

## 익숙한 패턴을 사용한 문제 해결

개별 솔루션에 대한 얘기를 이어가기 전에 자바의 복잡도 감소가 스프링 포트폴리오의 핵심 특징이라는 점을 이해해야한다. 스프링의 설계 목표는

무엇보다도 애플리케이션을 만드는 방법을 단순화하는 것이다. 이를 달성하는 가장 강력한 수단 중 하나가 `템플릿 패턴이다`. 이 템플릿 패턴은 아주

강력해서 GOF디자인 패턴 책에도 포함돼있다. 본질적으로 `템플릿` 이란 특정 API의 모든 복잡성을 가장 단순한 연산으로 축약하는 것을 의미한다.

템플릿 중 가장 대표적인 것은 `JdbcTemplate`이다. JDBC를 직접 사용하면 개발자가 쿼리문 작성, DB연결 관리를 모두 신경 써야 하고, 2백여 개의

쿼리를 작성한 후에는 혹시 `ResultSet`을 닫지 않고 종료 처리한 것이 있는지 하나하나 살펴봐야 한다. 

스프링 프레임워크는 `JdbcTemplate`을 만들었다. `JdbcTemplate` 을 활용하면 몇가지 연산만 사용해서 데이터 조회와 수정을 처리할 수 있다.

개발자는 SQL쿼리문과 쿼리 결과 처리 방법만 제공하면 된다. DB 연결, 커서, 결과 구성 , ResultSet 닫기 등은 모두 스프링이 알아서 대신 처리해준다

결국 JdbcTemplate이 자원 관리를 맡아서 실수 없이 처리하고 개발자는 고객 요구사항에 집중할 수 있다. 템플릿 패턴은 너무 강력해서 MailSender,

JndiTemplate, HibernateTemplate, JdoTemplate 등 여러 영역에서 두루 사용되고 있다. 그리고 다음과 같은 비동기 메시징에서도 사용된다

- `JMS`: 자바 표준 메시징 API, 스프링 프레임워크는 JMS 브로커를 사용한 메시지 송신과 수신을 쉽게 처리할 수있도록 JmsTemplate과

  DefaultMessageListenerContainer를 제공한다

- `아파치 카프카`: 빠른 속도로 대세로 자리 잡고있는 브로커, 스프링 아파치 카프카는 아파치 카프카를 사용한 메시지 송신과 수신을 쉽게 처리할 수 

  있도록 KafkaTemplate과 KafkaMessageListenerContainer를 제공한다

- `레빗엠큐(RabbitMQ)`:높은 처리량과 강한 회복력이 특징인 메시지 브로커, 스프링 AMQP는 레빗엠큐를 사용한 메시지 송신과 수신을 쉽게

  처리할 수 있도록 AmqpTemplateSimpleMessageListenerContainer를 제공한다

- `레디스(Redis)`: 빠른 속도를 무기로 가장 널리사용되는 브로커. 스프링 데이터 레디스는 레디스를 사용하는 메시지 송신과 수신을 쉽게 처리할 수 

  있도록 RedisMessageListenerContainer를 제공한다

직접 구현하면 복잡할 수 있는 API를 획기적으로 단순화한 발행-구독(pub-sub) 관련 유틸리티를 사용하면 쉽게 메시지를 발행할 수 있게 해주고, 

메시지를 받아서 처리하는 메시지 리스너도 쉽게 등록할 수 있게 해준다

## 손쉬운 테스트

AMQP 브로커인 레빗엠큐를 사용하는 테스트를 중심으로 메시지 처리 방법을 알아보자. [`테스트컨테이너`](https://testcontainers.org/) 는 도커를 활용하는 자바 테스트 지원

라이브러리다. 테스트컨테이너는 도커에서 실행될 수만 있다면, 어떤 데이터베이스나 메시지 브로커, 서드파티 시스템도 테스트용으로 쉽게 쓸

수 있다. 테스트가 종료되면 테스트에 사용됐던 여러 컨테이너자원도 남김없이 깔끔하게 종료된다. 그래서 테스트를 실행할 때마다 아주 쉽게

깨끗한 상태의 레빗엠큐를 실행하고 사용할 수 있다 테스트컨테이너를 사용하려면 테스트컨테이너 BOM(Bill of Materials) 파일을 가져와야한다

```groovy
    implementation 'org.testcontainers:testcontainers-bom:1.15.3'
```

build.gradle 파일에 위와 같이 BOM 파일을 지정해주면 모든 테스트컨테이너 모듈의 올바른 버전을 한번에 지정할 수 있다. BOM파일 정보를 지정한

다음에는 레빗엠큐 테스트에 필요한 테스트 스코프 의존관계를 추가하자

```groovy
testImplementation "org.testcontainers:rabbitmq:1.15.3"
testImplementation "org.testcontainers:junit-jupiter:1.15.3"
```

테스트컨테이너 레빗엠큐 모듈은 도커관리를 담당하는 핵심 의존 라이브러리와 레빗엠큐를 활성화하는 모듈을 포함하고 있다. 

테스트컨테이너는 현재 Junit4기준으로 만들어져있다. 그래서 스프링 부트 2.3부터 표준으로 사용되는 Junit5와 함께 사용하려면 테스트컨테이너의

`junit-jupiter` 모듈도 추가해야한다. 테스트가 종료되면 별도로 신경쓰지 않아도 테스트에 사용된 컨테이너도 함께 종료된다.

## 테스트컨테이너 사용 테스트

테스트컨테이너를 사용할 준비를 마쳤다. 이제 테스트 대상 시스템의 어떤 동작을 테스트할지 먼저 정해보자. 지금까지 비동기 메시지 솔루션에

대해 얘기해왔는데, 웹 컨트롤러에서 새로운 Item객체 생성 요청을 받아서 레빗엠큐를 통해 메시지로 전달하는 과정을 구현해보자. 메시지를

받아서 몽고디비에 저장하는 서비스도 함께 구현할 것이다. 메시지를 매개체로 사용하는 이 단순한 개념은 여러방식으로 응용해서 얼마든지

재사용할 수 있다. 예를 들어, 웹 컨트롤러 대신 다른것으로 대체할 수도있다. 그렇게 해도 메시지는 레빗앰큐를 통해 전송된다. 

또는 메시지를 전송하는 API를 직접 호출하게 할 수도 있다. 일단 처음에 시도하려 했던 것부터 시작해보자. 동기적인 웹 요청을 받아서

비동기 메시지로 바꾸는 웹컨트롤러를 만들어보자. 

```java
@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@ContextConfiguration
public class RabbitTest {

    @Container static RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine");

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ItemRepository repository;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry){
        registry.add("spring.rabbitmq.host",container::getContainerIpAddress);
        registry.add("spring.rabbitmq.port",container::getAmqpPort);
    }
}
```

- @SpringBootTest 애노테이션은 자동설정, 환경설정 값 읽기, 내장 웹 컨테이너 등 테스트를 위핸 애플리케이션 구동에 필요한 모든것을 활성화한다.

  기본적으로 실제 운영환경이 아니라 실제 운영환경을 흉내 낸 가짜(mock) 환경을 사용한다

- @AutoConfigureWebTestClient를 붙여서 테스트용으로 사용하는 webClient인 WebTestClient를 자동설정한다.

- @Testcontainers는 Junit5에서 제공하는 애노테이션이며 테스트컨테이너를 테스트에 사용할 수 있게 해준다.

- @ContextConfiguration은 지정한 테스트 실행 전에 먼저 애플리케이션 컨텍스트에 로딩해준다.

- @DynamicPropertySource는 자바 8의 함수형 인터페이스인 Supplier를 사용해서 환경설정 내용을 Environment에 동적으로 추가한다.

  container::getContainerIpAddress와 container::getAmqpPort 메소드 핸들을 사용해서 테스트컨테이너에서 실행한 레빗엠큐 브로커의

  호스트 이름과 포트 번호를 가져온다. 이렇게하면 레빗엠큐 연결 세부정보를 테스트컨테이너에서 읽어와서 스프링 AMQP에서 사용할 수 있도록

  스프링 부트 환경설정 정보에 저장한다.

테스트를 작성하기전 알아둬야 할 것이 있다. 지금까지 프로젝트 리액터를 사용하는 테스트에서는 StepVerifier를 사용해서 비동기 처리 흐름을 쉽게

테스트할 수 있었고, 지연 효과를 흉내낼수도 있었다. 하지만 레빗엠큐를 사용하는 테스트에서는 RabbitVerifier 같은것이 없어서 Thread.sleep()을

사용해야 한다. 

## 테스트 케이스 구성

웹 컨트롤러 초안을 만들기 전에 먼저 웹 컨트롤러가 처리해야할 일을 먼저 나열해보자

1. 새 Item 객체를 생성하기 위해 Item 데이터가 담겨있는 HTTP POST요청을 받는다.
2.  Item 데이터를 적절한 메시지로 변환한다
3. Item 생성 메시지를 브로커에게 전송한다

메시지를 받는 브로커 쪽에서 해야 할 일은 다음과 같다

1. 새 메시지를 받을 준비를 하고 기다린다.
2. 새 메시지가 들어오면 꺼내서
3. 몽고디비에 저장한다

잊지 말아야 할 것은 개발자가 직접하든프레임워크에 위임하든 구독을 해야 동작한다는 점이다. 이제 실제 테스트 케이스를 작성해보자.

물론 실제 테스트 대상이 구현되지 않은 상태이므로 테스트는 실패한다

```java
@Test
    void verifyMessagingThroughAmqp() throws InterruptedException {
        this.webTestClient.post().uri("/items")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody();

        Thread.sleep(1500L);

        this.webTestClient.post().uri("/items")
                .bodyValue(new Item("Smurf TV tray","nothing important",29.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody();

        Thread.sleep(2000L);

        this.repository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches( item -> {
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                })
                .expectNextMatches(item -> {
                    assertThat(item.getName()).isEqualTo("Smurf TV tray");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(29.99);
                    return true;
                })
                .verifyComplete();
    }
```

먼저 알아둘 것은 이테스트가 실제 레빗엠큐 브로커를 대상으로 수행된다는 점이다. 아직 Item 데이터를 받아서 메시지로 변환하고 브로커에 보내서 몽고

디비에 저장하는 로직이 구현돼 있지 않으므로 테스트를 실행하면 물론 실패한다. 이제부터 로직을 구현해서 테스트를 통과시킬 것이다.

스프링은 자바의 복잡성을 낮추는 것을 목표로 한다고 말했었다. 스프링 AMQP는 널리 사용되는 메시징 프로토콜인 AMQP를 스프링 방식으로

사용할 수 있게 해준다. 스프링 AMQP를 사용하기 위해 프로젝트 빌드파일에 의존관계를 추가하자

```groovy
implementation 'org.springframework.boot:spring-boot-starter-amqp'
```

이제 POST요청을 리액티브 방식으로 처리할 수 있는 스프링 웹플럭스 REST 컨트롤러를 작성하자.

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class SpringItemController {
    private final AmqpTemplate template;
    
    @PostMapping("/items")
    Mono<ResponseEntity<?>> addNewItemUsingSpringAmqp(@RequestBody Mono<Item> item){
        return item.subscribeOn(Schedulers.boundedElastic())
                .flatMap(content -> {
                    return Mono.fromCallable( () -> {
                        this.template.convertAndSend(
                                "hacking-spring-boot", "new-items-spring-amqp",content
                        );
                        return ResponseEntity.created(URI.create("/items")).build();
                    });
                });
    }
}
```

레빗엠큐가 `블로킹 API` 를 호출한다는게 사실일까? 그렇다. 레빗엠큐가 비동기 메시징 시스템이긴 하지만 많은 레빗엠큐 API는 작업 수행 중 현재 

스레드를 블록한다. 이 미묘한 차이를 이해하는 것이 중요하다. 결국에는 비동기 처리 과정으로 되돌아가더라도 어떤 API가 현재 스레드를 블로킹한다면 

블로킹 API다. 이 예제에서는 긴 시간동안 블로킹하지 않으므로 큰 문제가 되지 않을 것 같지만 이런 블로킹에 의해 발생하는 지연이 쌓이고 쌓이면,

나중에 무시 못할 부담이 될 수 있다. 그래서 프로젝트 리액터에서는 이 문제를 해결할 방법을 만들어 뒀다.

## 스케줄러를 사용해서 블로킹 API 감싸기

리액터는 스레드에 대해 알지 못한다. 리액터의 API를 사용할 때 멀티스레드 프로그래밍을 반드시 활용해야 하는 것은 아니다. 리액터를 사용할 때는

AMQP 예제에서 살펴본 것처럼 여러 단계의 작업 절차를 만들게된다. 리액터는 `스케줄러(Scheduler)` 를 통해 개별 수행 단계가 어느 스레드에서

실행될지 지정할 수 있다. 한 개의 스레드만을 사용하더라도 비동기 논블로킹 코드를 작성할 수 있다. 한 개의 스레드가 작업을 수행할 수 있을 때,

다시 말하면 스레드가 시스템 자원의 가용성에 반응할 준비가 돼있을때 개별 수행 단계를 실행하는 방식을 사용하면 가능하다. 하나의 작업 단계가

완료되면 스레드는 리액터의 작업 코디네이터에게 반환되고 다음에 어떤 작업 단계를 실행할지 결정된다. 모든 작업이 이처럼 개별 단계가 완료될

때마다 스케줄러에게 스레드를 반환하는 패러다임으로 수행될 수 있다면, 스레드의 숫자는 전통적인 `멀티스레드 프로그래밍` 에서만큼 중요하지는 않

게된다. 작업 수행 단계 중에 블로킹 API가 호출이 포함된다면 리액터에게 알려서 블로킹 API를 별도의 스레드에서 호출하게 해야 의도하지 않은

스레드 낭비를 방지할 수 있다. 리액터는 다음과 같이 여러 방법으로 스레드를 사용할 수 있다.

- `Scheduler.immediate()` : 현재 스레드

- `Schedulers.single()` : 재사용 가능한 하나의 스레드. 현재 수행중인 리액터 플로우뿐만 아니라 호출되는 모든 작업이 동일한 하나의

  스레드에서 실행된다

- `Schedulers.newSingle()` : 새로 생성한 전용 스레드

- `Schedulers.boundedElastic()`: 작업량에 따라 스레드 숫자가 늘어나거나 줄어드는 신축성 있는 스레드풀

- `Schedulers.parallel()` : 병렬 작업에 적합하도록 최적화된 고정 크기 워커 스레드풀

- `Schedulers.fromExecutorService()`: ExecuterService 인스턴스를 감싸서 재사용

리액터 플로우에서 스케줄러를 변경하는 방법은 두가지다.

- `publishOn()`: 호출되는 시점 이후로는 지정한 스케줄러를 사용한다. 이 방법을 사용하면 사용하는 스케줄러를 여러번 바꿀수있다

- `subscribeOn()`: 플로우 전 단계에 걸쳐 사용되는 스케줄러를 지정한다. 플로우 전체에 영향을 미치므로 publishOn()에 비해

  영향 범위가 더 넓다.

addNewItemUsingAmqp() 안에서 subscribeOn(Schedulers.boundedElastic()) 이 호출되고 있다. 이렇게 하면 블룅 호출을 처리할 수

있는 신축성 있는 스레드 풀을 사용할 수 있다. 이 신축성 스레드 풀은 별도의 스레드 풀이므로 블로킹 API 호출이 있더라도 다른 리액터 플로우에

블로킹 영향을 전파하지 않는다. 앞에서 설명한 것처럼 subscribeOn()을 호출하는 위치는 중요하지 않다. 리액터 플로우에서 subscribeOn()이 어디에

위치하든 해당 플로우 전체가 subscribeOn()으로 지정한 스레드에서 실행된다. 다만 나중에 publishOn() 으로 스레드를 다시 지정하면, 지정한 지점

이후부터는 publishOn()으로 새로 지정한 스레드에서 리액터 플로우가 실행된다.

## 컨슈머 작성

웹플럭스 컨트롤러에 메시지 프로듀서가 단정하게 작성돼 있으므로 이제 레빗엠큐 컨슈머를 만들어야한다. 스프링 AMQP에는 컨슈머를 만들 수 있는

여러 방법이 준비돼 있다. 가장 단순한 방식은 `AmqpTemplate.receive(queueName)` 이지만 가장 좋은 방식이라고 할 순 없다. 특히 부하가

많은 상황에서는적합하지 않다. 더 많은 메시지를 `폴링(polling)` 방식으로 처리할 수도 있고 콜백을 등록해서 처리할 수도 있지만 

`@RabbitListener` 를 사용하는 것이 가장 유연하고 편리하다

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class SpringAmqpItemService {
    
    private final ItemRepository repository;
    
    @RabbitListener(
            ackMode = "MANUAL",
            bindings = @QueueBinding(
                    value = @Queue,
                    exchange = @Exchange("hacking-spring-boot"),
                    key="new-items-spring-amqp"))
    public Mono<Void> processNewItemsViaSpringAmqp(Item item){
        log.info("Consuming => {}",item);
        return this.repository.save(item).then();
    }
}
```

- @RabbitListener가 붙은 메소드는 스프링 AMQP 메시지 리스너로 등록되어 메시지를 소비할 수 있다

- @QueueBinding은 큐를 익스체인지에 바인딩하는 방법을 지정한다

- @Queue는 임의의 지속성 없는 익명 큐를 생성한다. 특정 큐를 바인딩하려면 @Queue의 인자로 큐의 이름을 지정한다

  Durable, exclusive, autoDelete 같은 속성값도 지정할 수 있다

- @Exchange는 이 큐와 연결될 익스체인지를 지정한다. 예제에서는 hacking-spring-boot 익스체인지를 큐와 연결한다.

  익스체인지의 다른 속성값을 설정할 수도 있다

- key는 라우팅 키를 지정한다

- @RabbitListener에서 지정한 내용에 맞는 메시지가 들어오면 processNewItemsViaSpringAmqp(Item item) 이 실행되며,

  메시지에 들어있는 Item 데이터는 item 변수를 통해 전달된다

메시지 내용은 길지 않은데 스프링 AMQP사용을 위한 애노테이션에 대해 설명할 내용이 많다. 스프링 AMQP는 비동기 메시지를 여러가지

방법으로 소비할 수 있다. @RabbitListener 애노테이션을 사용하는 방법이 가장 직관적이다. 이름있는 큐를 사용할 수 도 있고 위와같이

익명 큐를 사용할 수도 있다.

`@RabbitListener` 애노테이션을 메소드에 붙이면 스프링 AMQP가 가능한 한 가장 효율적인 캐시 및 폴링 메커니즘을 적용하고 백그라운드에서

리스너를 등록한다. 스프링 AMQP를 사용하면 자바의 `Serialzable` 인터페이스를 사용해서 직렬화를 처리할 수 있다. 지금까지 작성한 메시지를 

`Serializable` 을 구현하도록 변경하면 그리 어렵지 않게 직렬화 할 수 있지만 최선의 방법이라고 할 수는 없다.

다른 대안으로는 POJO 객체를 JSON 같은 문자열 표현으로 변환하고 문자열을 바이트 배열로 만들어서 네트워크를 통해 전송하는 방법이 있다.

스프링에서 JSON 직렬화를 담당하는 잭슨 라이브러리를 사용하는 방법은 아주 간단하다. 다음과 같이 빈을 하나 등록하면 된다

```java
 	 @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
```

이렇게 빈으로 등록하면 스프링 프레임워크의 MessageConverter가 자동으로 활성화된다. MessageConverter는 POJO객체를 JSON으로 전환하거나

JSON을 POJO객체로 전환하는 역할을 담당한다.  이제 테스트를 실행시 성공적으로 수행함을 나타낸다.

지금까지 스프링을 활용해서 AMQP 메시징을 사용하는 기본적인 방법을 알아봤다. 단순하고 강력하다. 그리고 필요에 맞춰 고쳐서 사용하기도 쉽다.

## 정리

7장에서는 레빗엠큐를 사용하는 방법을 알아봤다. 메시지 브로커를 설치하는데 몇시간이 넘는 긴 시간을 들이는 대신에 테스트컨테이너를 사용해서

쉽게 메시징 솔류션을 사용할 수 있었다. VM웨어의 탄주 애플리케이션 서비스 같은 클라우드 솔루션을 선정하면 아주 간단하게 메시징 기반 애플리케이션

을 상용 배포할 수 있다. 하지만 7장에서는 비동기 메시징 솔루션의 일반적인 개념에 대해 더 자세히 알아보는 것을 목표로 했다. 비동기 메시징 솔루션은 

레빗엠큐 말고도 다양하다. JMS, 액티브엠큐, 카프카 등 많은 선택지가 있다.

하지만 핵심 개념은 다음과 같이 동일하다

- 블로킹 API는 감싸서 별도의 스레드실행
- 하나의 메시지 발행
- 하나 혹은 둘 이상의 컨슈머가 메시지 소비
- 스프링 포트폴리오에 포함된 RabbitTemplate, RabbitMessageTemplate, AmqpTemaplte, JmsTemplate, KafkaTemplate 등 다양한 템플릿 활용

스프링 AMQP 사용법을 알게되면 다음 프로젝트에서 어떤 메시징 솔루션을 사용하더라도 어렵지 않게 적응할 수 있을 것이다.

지금까지 7장에서 배운내용은 다음과 같다

- 테스트컨테이너, 레빗엠큐, 스프링 AMQP설정
- 웹과 백엔드가 예상대로 동작하는지 검증하는 테스트 작성
- 동기적 웹 요청을 받아서 처리하는 웹플럭스 컨트롤러 작성
- 블로킹 API 호출부를 감싸서 리액터의 엘라스틱 스레드 풀에서 실행
- RabbitTemplate을 사용해서 비동기 메시지 브로커를 통해 메시지 전송
- @RabbitListener를 사용해서 레빗엠큐 리스너를 설정하고, 전송받은 메시지 소비

이어서 8장 '스프링 부트 R소켓' 에서는 유연성 있는 리액티브 프로토콜인 R소켓을 사용해서 서비스 사이에 통신하는 방법을 알아본다.

