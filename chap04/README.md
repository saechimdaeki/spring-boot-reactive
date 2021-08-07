# 4장 스프링 부트 테스트

앞서 3장에서는 개발 속도를 높이기 위해 스프링 부트 개발자 도구를 활용하는 방법을 알아봤다. 리액터 팀은 리액티브 애플리케이션을 만들 수 있도록

`블록하운드(BlockHound)` 를 비롯해 다양한 개발자 도구를 만들었다. 4장에서는 한 걸음 더 나아가 애플리케이션을 리액티브하게 테스트 하는 방법을 알아본다.

테스트 대상, 테스트 커버리지 달성 목표, 단위 테스트와 통합 테스트 개념 등 전통적인 코드에 적용했던 표준은 리액티브 코드에도 적용돼야 한다는 관점에서 보면

리액티브 코드 테스트든, 전통적인 코드 테스트든 사실 다를게 없다. 하지만 '구독하기 전까지는 아무 일도 일어나지 않는다' 라는 리액터의 확고한 규칙이

적용돼야 한다는 점에서라면, 리액티브 코드 테스트와 전통적인 코드 테스트는 다르다. 이런 차이에서 오는 난관을 바르게 극복할 수 있도록 리액터 팀에서

여러가지 도구를 만들었다. 4장에서 다룰 내용은 다음과 같다.

- 리액티브 단위 테스트 작성 방법
- 스프링 부트의 내장 컨테이너 테스트 기능
- 단위 테스트와 통합 테스트의 중간에 위치하는 슬라이스 테스트활용
- 테스트 도구를 활용한 블로킹 코드 검출

## 리액티브 단위 테스트 작성

테스트 중 가장 단순하고 빠르며 쉬운 테스트는 `단위 테스트`다. 여기서 말하는 단위란 자바에서는 하나의 클래스라고 볼 수 있다. 테스트 대상 클래스가 의존하는

다른 협력 클래스의 실제 인스턴스 대신 가짜 인스턴스의 스텁(stub) 을 사용해서 협력 클래스는 테스트 대상에서 제외하고, 오직 테스트 대상 클래스만의 기능을

테스트하고 검증하는 것을 단위 테스트라고 정의한다. 

스프링 부트는 테스트가 얼마나 중요한지 잘 알고 있기에, 테스트에 필요한 여러 도구를 쉽게 사용할 수 있도록 스타터(starter) 를 제공한다. 그러므로 빌드 파일에

`spring-boot-starter-test` 를 추가하기만 하면 다음과 같은 테스트 라이브러리가 자동으로 추가된다.

- 스프링 부트 테스트(Spring Boot Test)
- 제이슨 패스(JsonPath)
- 제이유닛(Junit 5)
- 어서트제이( AssertJ)
- 모키토(Mockito)
- 제이슨어서트(JSONassert)
- 스프링테스트(Spring Test)

상호작용이 적을수록 단위 테스트는 단순해진다. 예를들어, 전체 애플리케이션의 근간을 이루는 도메인 객체가 아마도 가장 테스트하기 쉬울 것이다. 비즈니스 로직 없이 

단순한 값 검사로직만 사용하는 빈약한 도메인 모델 방식을 사용하든, 비즈니스 로직을 풍부하게 담고 있는 도메인 모델을 사용하든, 이 도메인 계층은 다른계층에 대한

의존 관계가 없어야 한다. 결국 다른 계층에 존재하는 협력자가 없어서 상호작용이 적은 도메인 객체가 테스트하기 가장 쉽다.

```java
public class ItemUnitTest {

    @Test
    void itemBasicsSHouldWork(){
        Item sampleItem=new Item("item1","TV tray", "Alf TV tray", 19.99);

        // ASSERTJ 를 사용한 값 일치 테스트
        assertThat(sampleItem.getId()).isEqualTo("item1");
        assertThat(sampleItem.getName()).isEqualTo("TV tray");
        assertThat(sampleItem.getDescription()).isEqualTo("Alf TV tray");
        assertThat(sampleItem.getPrice()).isEqualTo(19.99);
        assertThat(sampleItem.toString()).isEqualTo(
                "Item(id=item1, name=TV tray, description=Alf TV tray, price=19.99)");

        Item sampleItem2=new Item("item1","TV tray","Alf TV tray",19.99);
        assertThat(sampleItem).isEqualTo(sampleItem2);
    }
}
```

도메인 객체는 테스트하기가 그렇게 어렵지 않다. 하지만 다른 컴포넌트와 상호작용하는 코드를 테스트할 때는 조금 복잡해진다 InventoryService 클래스는 비즈니스 로직도

포함하고 있고 레포지토리에서 가져온 외부 컬렉션과도 상호작용해야 한다. 뿐만 아닌 서비스는 프로젝트 리액터 덕분에 사용할 수 있게 된 비동기, 논블로킹 플로우가 사용되는

첫번째 지점이기도하다. 비동기, 논블로킹 코드는 어떻게 테스트해야 할까? 

걱정할 필요는 없다. 스프링 부트와 프로젝트 리액터에서는 비동기, 논블로킹 코드도 JUnit을 통해 테스트할 수 있다. 

InventoryService.addItemCart(...) 메소드를 통해 비동기, 논블로킹 코드를 테스트하는 방법을 알아보자.

```java
@ExtendWith(SpringExtension.class) //@ExtendWith는 테스트 핸들러를 지정할 수 있는 JUNIT5 API이다. 스프링에 특화된 테스트
//기능을 사용할 수 있게 해준다.
public class InventoryServiceUnitTest {
}
```

테스트의 대상이 되는 클래스를 `CUT(class under test)` 라고 한다. 테스트 클래스를 선언하고 나면 무엇을 테스트하고, 무엇을 테스트하지 않을지를 분별하는 것이 중요하다.

서비스 클래스의 단위 테스트라면 테스트 대상 서비스 바깥에 존재하는 것은 모두 협력자라는 이름을 붙여서 목(mock) 객체를 만들거나 스텁을 만들어서 테스트 대상에서 제외한다

InventoryServiceUnitTest 는 다음과 같이 2개의 가짜 협력자가 필요하다.

```java
InventoryService inventoryService;
    
@MockBean private ItemRepository itemRepository;
@MockBean private CartRepository cartRepository;
```

ItemRepository와 CartRepository는 테스트 대상 클래스인 InventoryService에 주입되는 협력자다. 그래서 둘은 테스트 대상이 아니므로 가짜 객체를 만들어서 테스트에 사용하며

가짜 객체를 만들고 스프링 빈으로 등록하기 위해 `@MockBean` 애노테이션을 붙인다. 스프링 부트 테스트는 이 `@MockBean` 애노테이션을 보면 모키토(Mockito) 를 사용해서 

가짜 객체를 만들고 이를 애플리케이션 컨텍스트에 빈으로 추가한다

스프링 부트가 제공하는 `@MockBean` 애노테이션은 두가지 핵심 기능을 포함한다. 첫번째는 코드 작성 시간 단축이다. `@MockBean` 은 다음코드를 직접 작성하는 것과 같다

```java
@BeforeEach
void setUp(){
  itemRepository=mock(ItemRepository.class);
  cartRepository=mock(CartRepository.class);
}
```

어차피 2개의 가짜 협력자를 위해 두 줄의 코드를 작성하기는 마찬가지지만 `@MockBean` 을 사용하는 것이 훨씬 간결하고 이미 알고있는 협력자의 타입 정보를 더 잘활용한다.

두번째 기능은 좀더 미묘한 수준의 기능인데 `@MockBean` 이 협력자를 더 눈에 띄게 잘드러나게 해준다는 것이다. 이제 테스트 준비를 해보자

```java
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
```



테스트 대상 클래스의 협력자가 리액티브하다면 테스트에 사용할 가짜 협력자는 리액터 타입을 반환해야한다. 이부분이 익숙치 않아 과부하를 유발할 수도 있다. 모든것을

Mono.just(...) 나 Flux.just(...) 로 감싸는것은 귀찮지만, 이를 피하려면 리액터용 별도의 모키토 API를 사용해야 한다. 그러나 이 모키토 API를 사용하면 블록하운드가 잘못 사용된

블로킹 코드를 검출하기가 매우 어려워질 수 있다. 가짜 객체를 사용할 때 유의해야 할 점은 테스트 대상 클래스 안에 있는 알고리즘을 테스트해야한다는 점이다. 무심코 작성하다보면

테스트 대상 클래스가 아니라 가짜 객체를 테스트하는 코드가 만들어지는 상황을 심심치않게 마주하게된다.

이제 실제 테스트 코드를 작성해보자

```java
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
```

리액티브 코드를 테스트할 때 핵심은 기능만을 검사하는 것이 아닌 리액티브 스트림 시그널도 함께 검사해야한다는 점이다. 리액티브 스트림은 `onSubscribe` , `onNext` , `onError`

,`onComplete` 를 말한다. 예제 테스트 코드는 `onNext` 와 `onComplete` 시그널을 모두 검사한다. `onNext` 와 `onComplete` 가 모두 발생하면 성공경로라고 부른다.

구독하기 전까지는 아무일도 일어나지 않는다. 그렇다면 누가 구독을하는 걸까? 바로 `StepVerifier` 다. 결괏값을 얻기 위해 블로킹 방식으로 기다리는 대신에 리액터의 테스트 

도구가 대신 구독을하고 값을 확인할 수 있게 해준다. 값을 검증할 수 있는 적절한 함수를 `expectNextMatches(...)` 에 람다식 인자로 전달해주고, `verifyComplete()` 를

호출해서 `onComplete` 시그널을 확인하면 의도한 대로 테스트가 동작했음이 보장된다. 예전 테스트코드에서는 탑레벨 방식이라 부르는 패러다임을 사용했다. 

먼저 리액터 기반 함수를 최상위에서 호출하고 다음에 `as(StepVerifier::create)` 를 이어서 호출하고 있다. 그런데 이방법만이 아닌 동일한 테스트 코드를 방식을바꾸어

다음과 같이 작성할수도있다.

```java
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
```

이방식은 `StepVerifier.create(inventoryService.addItemToCart("My Cart", "item1"))` 로시작한다. 단순히 바깥에 명시적으로 드러난 행이 아니라 메소드의 인자

까지 뒤져봐야 무엇이 테스트되는지를 알 수 있으므로 별로 좋아보이지 않는다. 따라서 리액터의 as() 연산자를 사용해서 테스트 대상 메소드 결괏값을  `StepVerifier` 로 

흘려보내는 탑 레벨 방식으로 작성하면 테스트 코드의 의도가 더 분명히 드러난다

## 내장 컨테이너 테스트 실행

지금까지 도메인 객체테스트와 서비스 테스트를 작성했다. 하짐나 좀더 넓은 범위를 테스트할 필요도 있다. 예를 들어, 웹 컨트롤러가 백엔드 서비스와 바르게 협력하고 있는지

확인하는 것은 더 중요하다. 이와 같이 전 계층을 아우르는 종단 간 테스트는 대체로 값비싼 테스트 환경을 구성해야 한다. 그래서 애플리케이션에 변경이 발생할 때마다 종단 간

테스트를 수행하려면 비용이 많이든다. 하지만 너무 걱정할 필요는 없다. 스프링 부트는 완전한 기능을 갖춘 내장 웹 컨테이너를 임의의 포트에 연결해서 구동할 수 있다. 테스트

케이스는 목(mock) 나 스텁 같은 가짜 협력자와 협력할 필요 없이 실제 애플리케이션에서와 마찬가지 방식으로 생성되고 실행되는 진짜 애플리케이션 구성요소와 협력할수

있다. 다음 테스트케이스를 보자.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class LoadingWebSiteIntegrationTest {

    @Autowired
    WebTestClient client;
    
    @Test
    void test(){
        client.get().uri("/").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(exchangeResult -> {
                    assertThat(exchangeResult.getResponseBody()).contains("<a href=\"/add");
                });
    }
}
```

`@SpringBootTest` 는 스프링 부트가 실제 애플리케이션을 구동하게 만든다. `WebEnvironment.RANDOM_PORT` 는 테스트할 때 임의의 포트에 내장컨테이너를 바인딩한다.

`@AutoCOnfigureWebTestClient` 는 애플리케이션 요청을 날리는 `WebTestClient` 인스턴스를 생성한다.

이렇게 결과에 따라 더많은 검증을 추가할 수도있다. 예를 들어 jsoup 같은 라이브러리를 사용해서 HTML 응답 테스트를 파싱하고 특정 텍스트 패턴을 검사하면 테스트를 단순화

할 수 있다. JSON응답은 JsonPath나 JSONassert로 검증할 수 있다. 

테스트 목적으로 내장 웹컨테이너를 실행하는 것도 비용이 든다. 스프링 부트 덕분에 아주 쉽게 접근할 수 있게 되긴 했지만, 여전히 무거우므로 첫 테스트를 내장 웹 컨테이너

통합 테스트로 시작하는 것은 적절하지 않다. 그보다는 다음과 같이 복합적인 테스트 전략을 가져가는 것이 좋다.

- null 값 처리를 포함한 도메인 객체 테스트
- 가짜 협력자를 활용해서 모든 비즈니스 로직을 검사하는 서비스 계층 테스트
- 내장 웹 컨테이너를 사용하는 약간의 종단 간 테스트

종단 간 테스트에는 무슨 문제가 있길래 약간만 수행하는 것일까? 테스트 범위가 넓어질 수록 테스트는 깨지기 쉽다. 도메인 객체를 새로 변경했다면 그 도메인 객체에 대한 

단위 테스트가 영향을 받고, 그 도메인 객체를 사용하는 서비스 테스트도 영향을 받게 된다. 하지만 아주 큰 대규모 변경이 아니라면 도메인 객체 변경은 종단 간 테스트

케이스에는 거의 영향을 미치지 않을 것이다. 하지만 서비스 계층에 대한 변경은 서비스 계층뿐 아니라 해당 서비스 계층을 거쳐가는 종단간 테스트에도 영향을 미친다.

따라서 넓은 범위를 대상으로 하는 테스트케이스를 너무 많이 작성하면 코드를 변경할 때마다 함께 변경해야 할 테스트 케이스도 많아지며 이는 관리비용 증가로 이어진다

## 스프링 부트 슬라이스 테스트

스프링 부트가 제공하는 멋진 내장 컨테이너를 활용한 종단 간 테스트를 만들어볼 생각은 했겠지만 그 기대가 여지없이 무너졌다. 

그렇다면 단위테스트와 종단 간 통합테스트 중간 정도에 해당하는 테스트는 없을까? 다행스럽게도 `슬라이스 테스트(slice test)` 라고 있다.

스프링 부트에는 다음과 같은 다양한 테스트 지원 기능이 준비돼있다

- @AutoConfigureRestDocs
- @DataJdbcTest
- @DataJpaTest
- @DataLdapTest
- @DataMongoTest
- @DataNeo4jTest
- @DataRedisTest
- @JdbcTest
- @JooqTest
- @JsonTest
- @RestClientTest
- @WebFluxTest
- @WebMvcTest

스프링 부트의 슬라이스 테스트 기능을 활용하면 다음과 같이 몽고디비 테스트를 작성할 수 있다.

```java
@DataMongoTest
public class MongoDbSliceTest {

    @Autowired ItemRepository repository;
    
    @Test
    void itemRepositorySavesItems(){
        Item sampleItem=new Item("name","description",1.99);
        
        repository.save(sampleItem)
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("name");
                    assertThat(item.getDescription()).isEqualTo("description");
                    assertThat(item.getPrice()).isEqualTo(1.99);
                    
                    return true;
                })
                .verifyComplete();
    }
}
```

`@DataMongoTest` 는 스프링 부트 긴으 중 스프링 데이터 몽고디비 활용에 초점을 둔 몽고디비 테스트 관련 기능을 활성화하며, `@ExtendWith({SpringExtension.class})` 를

포함하고 있으므로 JUnit5기능을 사용할 수 있다.

이 몽고디비 슬라이스 테스트는 스프링 데이터 몽고디비 관련 모든 기능을 사용할 수 있게 하고 그 외에 `@Component` 애노테이션이 붙어있는 다른 `빈(Bean)` 정의를 무시한다.

테스트를 마무리하기 위해 스프링 웹플럭스 컨트롤러에 초점을 맞춘 테스트를 하나 만들어보자.

```java
@WebFluxTest(HomeController.class)
public class HomeControllerSliceTest {
    
    @Autowired
    private WebTestClient client;
    
    @MockBean
    InventoryService inventoryService;
    
    @Test
    void homePage(){
        when(inventoryService.getInventory()).thenReturn(Flux.just(
                new Item("id1","name1","desc1",1.99),
                new Item("id2","name2","desc2",9.99)
        ));
        when(inventoryService.getCart("My Cart"))
                .thenReturn(Mono.just(new Cart("My Cart")));
        
        client.get().uri("/").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(exchangeResult -> {
                    assertThat(exchangeResult.getResponseBody()).contains("action=\"/add/id1\"");
                    assertThat(exchangeResult.getResponseBody()).contains("action=\"/add/id2\"");
                });
    }
}
```

`@WebFluxTest(HomeController.class)` 는 이 테스트 케이스가 `HomeController` 에 국한된 스프링 웹플럭스 슬라이스 테스트를 사용하도록 설정한다.

## 블록하운드 사용 단위 테스트

3장에서 보았던 것 처럼 애플리케이션 메인 함수 시작부분에 블록하운드를 직어넣어 사용하는 방식은 블록하운드를 실제 운영환경에서도 활성화하므로, 학습용으로는 괜찮지만

실무에서는 적합한 전략은 아니다. 블록하운드를 테스트 환경에서만 사용하는 것이 더낫다. 블록하운드를 Junit과 함께 사용하려면 다음과 같이 빌드파일을 추가하자.

```groovy
implementation 'io.projectreactor.tools:blockhound-junit-platform:1.0.6.RELEASE' // 2021- 08- 07 기준 현재 gradle에서 오류가있다고함
```

이제 블록하운드가 Junit플랫폼의 `TestExecutionListener` 를 지원하므로 테스트 메소드에 사용된 블로킹 코드 호출을 검출할 수 있게 됐다. 

테스트 케이스 관점에서 블록하운드는 정확히 어떤것을 검출하는 것일까? 이해를 돕기위해 예제를 보자

```java
 
    @Test
    void threadSleepIsABlockingCall(){
        Mono.delay(Duration.ofSeconds(1))
                .flatMap(tick -> {
                    try{
                        Thread.sleep(10);
                        return Mono.just(true);
                    }catch (InterruptedException e){
                        return Mono.error(e);
                    }
                }).as(StepVerifier::create)
                .verifyComplete();
    }
```



블록하운드가 테스트케이스에도 연동되어 블로킹 코드를 검출해낸다. 블록하운드는 어떤 것을 검출해낼까? 전부는 아니지만 주요한 몇가지는 다음과 같다

- java.lang.Thread#sleep()
- 여러가지 Socket 및 네트워크 연산
- 파일 접근 메소드 일부

검출될 수 있는 전체 메소드 목록은 BlockHound클래스 안에 있는 Builder 클래스의 blokcingMethods 해시맵에서 확인할 수 있다. 이제 이해를 돕기위해 실제사례를 보자.

```java
Mono<Cart> addItemToCart(String cartId, String itemId){
  Cart myCart=this.cartRepository.findById(cartId)
    	.defaultIfEmpty(new Cart(cartId))
    .block();
  
  return myCart.getCartItems().stream()
    		.filter(cartItem -> cartItem.getItem().getId().eqals(itemId))
    		.findAny()
    		.map(cartItem -> {
          cartItem.increment();
          return Mono.just(myCart);
        })
    .orElseGet(() -> this.itemRepository.findById(itemId)
              .map(item -> new CartItem(item))
              .map(cartItem -> {
                myCart.getCartItems().add(cartItem);
                return myCart;
              }))
    		.flatMap(cart -> this.cartRepository.save(cart));
}
```

가장 먼저 cartRepository.findById() 를 호출해서 장바구니 정보를 가져온다. 편리한 defaultIfEmpty() 패턴을 사용했지만 마지막에 block() 을 호출해서 값을 얻을 때까지

블로킹 방식으로 기다린다. 이렇게 잘못 작성된 코드를 찾기만 한다면 수정은 쉽다. 하지만 찾는 것이 쉽지 않다. 날마자 이렇게 코드 한줄 한줄 검사할 수 없고 이럴 때 필요한

것이 블록하운드이다. 테스트 대상 서비스 외에 모든 것을 가짜 객체를 생성해 사용하는 테스트 케이스를 만들어서 확인해보자.

```java

@ExtendWith(SpringExtension.class)
public class BlockHoundIntegrationTest {

    AltInventoryService inventoryService;
    
    @MockBean ItemRepository itemRepository;
    @MockBean CartRepository cartRepository;
    
}
```

이제 가짜 객체를 생성해보자

```java
 @BeforeEach
    void setUp(){
        // 테스트 데이터 정의
        Item sampleItem = new Item("item1", "TV tray", "Alf TV tray", 19.99);
        CartItem sampleCartItem = new CartItem(sampleItem);
        Cart sampleCart = new Cart("My Cart", Collections.singletonList(sampleCartItem));
        
        //협력자와의 가짜 상호작용 정의
        when(cartRepository.findById(anyString()))
                .thenReturn(Mono.<Cart> empty().hide());
        
        when(itemRepository.findById(anyString())).thenReturn(Mono.just(sampleItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(sampleCart));
        inventoryService=new AltInventoryService(itemRepository,cartRepository);
    }
```

1. 테스트 케이스에 사용할 데이터를 정의한다. 
2. 협력자와의 가짜 상호작용을 정의한다
3. 비어있는 결과를 리액터로부터 감춘다

비어있는 결과를 리액터로부터 감춘다?? 

cartRepository.findItemById() 는 Mono.empty() 를반환한다. Mono.empty() 는 MonoEmpty 클래스의 싱글턴 객체를 반환한다. 리액터는 이런 인스턴스를 감지하고 런타임에서

최적화한다. block() 호출이 없으므로 블록하운드는 아무것도 호출하지 않고 지나간다. 이것은 리액터 문제가 아닌 시나리오 문제다. 개발자는 장바구니가 없을때도 문제없이

처리하기를 바랐지만, 리액터는 필요하지 않다면 블로킹 호출을 친절하게 알아서 삭제한다. 테스트 관점에서 이처럼 블로킹 호출이 알아서 제거되는 문제를 해결하려면 

MonoEmpty를 숨겨서 리액터의 최적화 루틴한테 걸리지 않게 해야한다. 리액터 자바독문서에는 이 연산을 설명하고있다

```markdown
"Mono.hide() 의 주목적은 진단을 정확하게 수행하기 위해식별성 기준 최적화를 방지하는것이다"
```

이제 실제 테스트 케이스를 작성하자

```java
 		@Test
    void blockHoundShouldTrapBlockingCall(){
        Mono.delay(Duration.ofSeconds(1))
                .flatMap(tick -> inventoryService.addItemToCart("My Cart", "item1"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(throwable -> {
                    assertThat(throwable).hasMessageContaining(
                            "block()/blockFirst()/blockLast() are blocking"
                    );
                });
    }
```

테스트 메소드는 명시적인 블로킹 호출을 포함하고 있으므로 예외가 발생하고, 예외가 발생할 것을 예상하고 VerifyErrorSatisfies() 를 호출해서 발생한 예외의 메시지를 단언하는

테스트는 성공한다. 일반적인 테스트 케이스는 블로킹 코드가 없다는 것을 검증하는 것이 목적이고, 실행 중 오류 없이 완료될 것을 예상하므로 `verifyComplete()` 를 호출해야함.

## 정리

지금까지 4장에서 다룬 내용은 다음과 같다

- StepVerifier를 사용해서 리액티브 테스트 작성
- 리액티브 스트림보다 하부 계층에 위치하는 도메인 객체를 간단하게 테스트
- @MockBean을 사용해서 만든 가짜 협력자와 StepVerifier를 사용해서 리액티브 서비스 테스트
- 리액티브 결과뿐 아니라 complete와 error같은 리액티브 스트림 시거늘도 검증
- 스프링 부트를 사용해서 완전한 기능을 갖춘 웹컨테이너 실행
- @WebFluxTest나 @DataMongoTest를 사용해서 애플리케이션의 일부 계층만 더 빠르게 테스트할 수 있는 슬라이스 테스트
- 리액터 블록하운드 모듈을 사용해 블로킹 코드 검출

5장 스프링부트 운영 에서는 서비스 운영 관리를 위해 스프링 부트가 제공하는 기능을알아보고 애플리케이션을 상용환경에 배포한 후 Day2운영을 다루는 방법을알아보자
