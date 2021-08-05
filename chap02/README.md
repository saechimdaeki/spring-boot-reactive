# 2장 스프링 부트를 활용한 데이터 액세스

2장에서 다룰 내용은 다음과 같다

- 리액티브 데이터 스토어의 요건
- 이커머스 애플리케이션  도메인 객체 정의
- 객체를 저장하고 조회할 레포지토리 생성
- 상기 내용을 서비스에 적용

## 리액티브 데이터 스토어 요건

데이터를 데이터베이스에 밀어 넣기 전에, 먼저 리액티브 프로그래밍의 핵심 요건을 이해해야한다. 리액티브 프로그래밍을 사용하려면 모든 과정이 리액티브여야 한다.

웹 컨트롤러를 리액티브 방식으로 동작하게 만들고 서비스 계층도 리액티브 방식으로 동작하게 만들었는데, 블로킹 방식으로 연결되는 데이터베이스를 호출하면 리액티브는 무너진다

블로킹 방식으로 데이터베이스를 호출한 스레드는 응답을 받을때까지 다른 작업을 하지 못한 채 기다려야 한다. 리액터 기반 애플리케이션은 많은 수의 스레드를 가지고 있지 않으므로

(많은 수의 스레드는 사실 필요하지 않다), 데이터베이스 호출 후 블로킹되는 스레드가 많아지면 스레드가 모두 고갈돼서 결국 전체 애플리케이션이 데이터베이스로부터 결과를 기다리면서

아무런 일도 할 수 없는 상태가 되어 망가지게 된다. 이런 레거시 블로킹 코드를 따로 감싸고 격리해서 문제를 해결할 수도 있지만 이렇게 하면 리액티브의 장점을 잃게된다. 

리액티브 프로그래밍과 관련해 흔히 떠올리는 선입견 중 하나는 리액티브가 태생적으로 빠르다는 주장이다. 이는 물론 사실이아니다. 작업을 수행하는 단일 스레드의 처리 속도 기준으로

보면 리액티브 프로그래밍은 여러가지 오버헤드를 수반하므로 성능 저하가 발생한다.

작업량을 대규모로 늘려야 하는 상황이 아니라면, 방금 설명처럼 리액티브 방식의 오버헤드로 인해 시스템은 더 느리게 동작한다. 리액티브가 태생적으로 빠를 수밖에 없다는 선입견과

오히려 리액티브가 더 느리다는 현실은 서로 상충되는 것처럼 보인다. 사례를 통해 확인하자

```markdown
### 똑같이 화물을 나른다고 해도 소형차와 대형 트럭은 차이가 있다. 대형 트럭이 소형차보다 훨씬 많은 화물을 실어 나를 수 있는것은 분명하다.

### 대형트럭이 소형차보다 훨씬 많은 화물을 실어 나를 수 있는것은 분명하지만 대형 트럭의 주행속도는 그다지 빠르지 않다. 트럭이 화물 운송의 주요 수단으로

### 사용되는 이유는 빠른 소형차보다 훨씬 많은 양의 화물을 실어 나를 수 있기 때문이다. 물건 하나만 나르는데 트럭을 사용하는 것은 낭비다.

### 트럭은 운송량이 크다는게 장점이므로 트럭에 화물을 가득 채워야 트럭의 장점을 활용할 수 있게 된다. 리액티브 프로그래밍도 마찬가지이다.

### 사용자 수가 적고 데이터도 많지 않다면 불필요한 오버헤드를 감수하면서 리액티브를 사용하는 것은 낭비다. 하지만 웹에서 대규모의 트래픽이 발생하고

### 백엔드에서 대용량의 데이터를 처리하는 환경에서는 리액티브 프로그래밍의 장점이 빛을 발하게 된다. 즉 리액티브 프로그래밍에서 스레드는 어떤 작업이

### 끝날 때까지 블로킹되어 기다리지 않고 다른 작업을 수행할 수 있다. 리액티브 런타임은 요청과 응답을 조율핵서 시스템 자원이 허용하는 한도 내에서

### 스레드 사용 효율을 극대화한다.
```

따라서 리액티브가 제대로 동작하려면 데이터베이스도 리액티브하게 동작해야 한다. 그렇지 않으면 앞선 설명처럼 블로킹으로 인해 시스템은 결국 멈춰버릴 것이다. 

그렇다면 어떤 데이터베이스가 이와 같은 최신형 리액티브 프로그래밍을 지원하고 있을까?

- 몽고디비
- 레디스
- 아파치 카산드라
- 엘라스틱서치
- 네오포제이
- 카우치베이스

이렇게 나열한 리액티브 지원 데이터베이스 목록에 관계형 데이터베이스는 단 하나도 포함되어 있지않다. 이유는 무엇일까? 자바에서 관계형 데이터베이스를 사용할 때 어떤

기술이 사용되는지 생각해보자

- JDBC
- JPA
- Jdbi
- JOOQ

결국 JPA와 JDBC는 무슨 문제가 있길래 이를 기반으로 연결되어 사용되는 관계형 데이터베이스가 리액티브 애플리케이션에서 사용되지 못하는 걸까? 

JPA와 JDBC는 블로킹 API다. 트랜잭션을 시작하는 메시지를 전송하고, 쿼리를 포함하는 메시지를 전송하고, 결과가 나올 때 클라이언트에게 스트리밍해주는 개념 자체가 없다. 

모든 데이터베이스 호출은 응답을 받을때까지 블로킹되어 기다려야 한다. JDBC나 JPA를 감싸서 리액티브 스트림 계층에서 사용할 수 있게해주는 반쪽솔루션도 존재하지만

이런 솔루션은 일반적으로 숨겨진 내부 스레드풀을 사용해서 동작한다.  JPA같은 블로킹 API앞에 스레드 풀을 두고 여러 스레드를 사용하는 방식은 일반적으로 

포화 지점에 도달하게 된다. 이 지점을 지나면 스레드 풀은 새 요청이 들어와도 받아서 처리할 스레드가 없으므로 스레드 풀 자체도 블로킹된다. 리액티브 프로그래밍에서 모든 것은

리액티브해야하며, 일부라도 리액티브하지 않고 블로킹된다면 애플리케이션은 제대로 동작하지 않는다. 

이제 다음의 의존관계들을 가진 스프링 프로젝트를 생성하자

- Data MongoDB Reactive
- Flapdoodle
- Thymeleaf
- WebFlux

`spring-boot-starter-mongodb-reactive` 는 스프링 데이터 몽고디비를 포함하고 있으며, 특히 리액티브 버전이 들어있다. 스프링 데이터 몽고디비는 리액티브 스트림을 완벽히

지원하며 아주 쉽게 리액티브 방식으로 협업할 수 있게 해준다. 빌드 파일에 추가된 두번째 의존관계인 `de.flapdoodle.embed.mongo` 는 내장형 몽고디비 도구다. 테스트에 주로

사용하며 애플리케이션 초기 설계 단계에서 데이터 스토어로 사용할 수 있다.

먼저 판매상품부터 정의해보자.

```java
@Data
public class Item {
    private  @Id String id;
    private String name;
    private double price;
    
    private Item(){}

    public Item(String name, double price) {
        this.name = name;
        this.price = price;
    }
}
```

스프링 데이터 몽고디비를 사용하므로 어떤 필드를 몽고디비의 `ObjectId` 값으로 사용할지 결정해야한다. `ObjectId` 는 모든 몽고디비 컬렉션에 있는 `_id` 필드로 사용된다.

스프링 데이터 커먼즈에서 제공하는 `@Id` 애노테이션을 사용해 특정필드를 `ObjectId` 필드로 지정한다. 이제 구매할 상품(Item) 을 장바구니에 추가하는 작업을 모델링해보자.

```java
@Data
public class CartItem {
    private Item item;
    private int quantity;
    
    private CartItem(){}

    public CartItem(Item item) {
        this.item = item;
        this.quantity=1;
    }
}
```

그리고 마지막 도메인 객체는 구매상품(CartItem) 을 담는 장바구니(Cart) 이다.

```java
@Data
public class Cart {
    
    private @Id String id;
    private List<CartItem> cartItems;

    private Cart() { }

    public Cart(String id) {
        this(id, new ArrayList<>());
    }

    public Cart(String id, List<CartItem> cartItems) {
        this.id = id;
        this.cartItems = cartItems;
    }
}
```

장바구니(Cart) 객체는 유일한 식별자를 가지고 있으며, 구매 상품(CartItem)을 저장할 자바 컬렉션도 가지고있다. 생성자는 두가지가 있다. 하나는 파라미터 없는 기본생성자이며

`private` 로 선언해서 외부에서 호출되지 못하게 돼 있다. 다른하나는 `public` 이며 장바구니의 모든 속성을 파라미터로 가지고 있다.

## 레포지토리 만들기 

업계에서는 NoSQL 데이터 스토어를 표준화하는 방법을 찾기 위해 다양한 시도를 해왔지만 아직까지 성공한 사례가 없다. 이유는 모든 NoSQL 엔진이 각기 다르며 저마다의

특징과 장단점이 있고 상충되는 부분이 존재하기 때문이다. 그렇다면 스프링 데이터는 이문제를 어떻게 해결하는 것일까?

스프링이 가진 가장 강력한 패러다임 중의 하나는 `JdbcTemplate`, `RestTemplate`, `JmsTemplate` 같은 `템플릿 패턴` 이다. 템플릿이라는 이름이 붙은 이다양한 도구는

타입 안전 방식으로 연산을 처리하고, 다루기 복잡하고 귀찮은 것들을 추상화해서 데이터베이스 등 협력 대상과의 상호작용이 바르게 동작하도록 보장한다.

가장 간단한 사례로 `JdbcTemplate` 을 사용하면 개발자가 데이터베이스 연결을 직접 열고 닫지않아도 된다. 

스프링 데이터에는 여러가지 데이터 스토어별 맞춤형 템플릿이 있다. 예를들어, 몽고디비용으로는 `MongoTemplate` 과 `ReactiveMongoTemplate` 이 제공된다. 오직 하나의 

데이터 스토어만을 위한 맞춤형 템플릿으로 해당 데이터 스토어의 특정적인 고유한 연산도 문제없이 지원하며, 결국 해당 데이터베이스의 풍부한 기능을 모두활용할수있다.

또한 저장, 조회, 삭제 같은연산은 단순하며 거의 모든 데이터베이스에서 지운한다. 이런 단순한 연산을 처리하기 위해 몽고디비의 가장 깊숙한 면을 알아볼 필요는 없다.

몽고디비가 아닌 다른 데이터베이스를 사용해도 저장, 조회, 삭제같은 단순하고 공통적인 연산은 추상화해서 표준화된 방식으로 접근하면 편리하다.

이 추상화를 담당하는 계층이 바로 `레포지토리` 다. 레포지토리는 만들기도 전혀 어렵지않다. 판매상품(Item) 에 대한 레포지토리를 만들어보자.

```java
public interface ItemRepository extends ReactiveCrudRepository<Item,String> {
}
```

`ItemRepository` 는 스프링데이커커먼즈에 포함된 `ReactiveCrudRepository` 를 상속받고있다. `ItemRepository` 코드에서 알 수 있는 내용은 다음과 같다.

- 첫번째 제네릭 파라미터는 레포지토리가 젖아하고 조회하는 타입을 의미한다. `ItemRepository` 의 첫 번째 제네릭 파라미터는 Item이며 이는 `ItemRepository` 가 

  Item을 저장하고 조회하는 역할을 맡는다는 의미다.

- 두번 째 제네릭 파라미터인 String은 저장되는 데이터의 식별자의 타입이 String이라는 의미다

- `ItemRepository` 자체로는 인터페이스라서 아무런 구현코드도 포함돼있지않다.

스프링 데이터에서 제공하는 인터페이스를 상속받아서 Item 전용 인터페이스를 만들었다. 상속 받아 만든  `ItemRepository` 는 저장할 데이터 타입과 식별자 타입을 강제하는 역할

을 한다. 부모 인터페이스인 `ReactiveCrudRepository` 로부터 상속받는 메소드는 다음과 같다

- save(), saveAll()
- findById(), findAll(), findAllById()
- existsById()
- count()
- deleteById(),delete(),deleteAll()

여러가지 풍부한 CRUD 연산이 망라돼 있으며, 실제 구현 코드를 작성하지 않아도 메소드를 사용할 수 있다. 위에 나열된 메소드 외에도 메소드 이름을 잘 조합하면 다양한 쿼리문 대신

할 수도 있다. 눈여겨봐야 할 것은 모든 메소드의 반환타입이 `Mono` 나 `Flux` 둘 중하나라는 점이다. 이 부분이 매우 중요한데 `Mono` 나 `Flux` 를 구독하고 있다가 몽고디비가 

데이터를 제공할 준비가 됐을때 데이터를 받을 수 있게 된다. 그리고 이 메소드 중 일부는 리액티브 스트림의 `Publisher` 타입을 인자로 받을 수 있다.

## 테스트 데이터 로딩

`ItemRepository` 를 사용해서 새로운 판매상품을 저장하려면 아마 다음과 같이 코드를 작성할 것이다

```java
ItemRepository.save(new Item("Alf alarm clock",19.99));
```

이 코드에는 문제가 있다. 왜일까?

`ReactiveCrudRepository.save()` 는 `Mono<T>` 를 반환하고, `ReactiveCrudRepository` 를 상속받은 `ItemRepository` 는 앞에서 살펴본 것처럼 Item타입의 데이터를

다루므로 `ItemRepository.save()` 는 `Mono<Item>` 을 반환한다. 간단히 말해 위의 코드는 아무일도 하지않는다는 점이 문제다. 일을 하게하려면 다음과 같이 구독을해야한다

```java
ItemRepository
  .save(new Item("Alf alarm clock",19.99))
  .subscribe();
```

구독했으니 문제가없을 것이다. 그런데 아쉽지만 여전히 문제가 남아있다.

따라서 애플리케이션 시작 시점에 어떤 작업을 하려면 다소 맥빠지는 감이 없진 않지만 블로킹 버전의 스프링 데이터 몽고디비를 사용하는 편이 좋다. 하지만 속상할 필요는없는게

늘 그런것이 아닌 애플리케이션 시작 시점에서만 발생하는 이슈이며 지금처럼 테스트 데이터를 로딩하는 테스트 환경구성에서는 약간의 블로킹 코드를 사용해도 문제되지 않는다.

물론 블로킹 코드는 실제 운영환경에선 절대 사용하면 안된다.

```java
public interface BlockingItemRepository extends CrudRepository<Item,String> {
}
```

이제 데이터를 로딩하는 클래스를 만들자

```java
@Component
public class RepositoryDatabaseLoader {
    @Bean
    CommandLineRunner initialize(BlockingItemRepository repository){
        return args -> {
            repository.save(new Item("Alf alarm clock",19.99));
            repository.save(new Item("Smurf TV tray",24.99));
        };
    }
}
```

```markdown
# 참고
### `CommandLineRunner` 는 애플리케이션이 시작된 후에 자동으로 실행되는 트굿한 스프링 부트 컴포넌트로서, run() 메소드 하나만 가지고 있는

### 함수형 인터페이스이다. 애플리케이션에서 사용되는 모든 컴포넌트가 등록되고 활성화된 이후에 run() 메소드가 자동으로 실행되는 것이 보장된다.
```

이렇게 블로킹으로 동작하게하면 애플리케이션 시작 시 네티와 충돌할 위험은 없다. 그런데 이러한 블로킹 레포지토리의 대안은 없을까?? 블로킹 레포지토리 사용 가능성을 낮추려면

아예 만들지를 말아야 한다. 우선 `BlockingItemRepository` 와 이를 사용하는 `RepositoryDatabaseLoader` 를 제거하자. 그럼 데이터는 어떻게 로딩하나?

`MonhoTemplate` 을 사용하면 된다. 스프링 부트와 스프링 데이터 몽고디비 자동설정 기능 덕분에 `MongoTemplate` 과  `ReactiveMongoTemplate` 을 모두 사용할 수 있다.

`MongoTemplate` 은 블로킹 버전이고 `ReactiveMongoTemplate` 은 비동기, 논블로킹 버전이다.

블로킹 레포지토리를 사용하지 않고 블로킹 방식으로 데이터를 로딩하려면 다음과 같이 TemplateDatabaseLoader 클래스를 만들면 된다

```java
@Component
public class TemplateDatabaseLoader {

    @Bean
    CommandLineRunner initialize(MongoOperations mongo) {
        return args -> {
            mongo.save(new Item("Alf alarm clock", 19.99));
            mongo.save(new Item("Smurf TV tray", 24.99));
        };
    }
}
```



그런데 `MongoOperations` 는 무엇일까? 수년 전에 스프링 팀은 `JdbcTemplate` 에서 일부를 추출해서 `JdbcOperations` 라는 인터페이스를 만들었다.

인터페이스를 사용하면 계약과 세부 구현 내용을 분리할 수 있다. 이 패턴은 스프링 포트폴리오에서 사용하는 거의 모든 템플릿에서 사용되고 있다. 따라서 애플리케이션과

몽고디비의 결합도를 낮추려면 `MongoOperations` 인터페이스를 사용하는 것이 좋다. 또한 Cart객체 관리를 위한 리액티브 레포지토리를 만들자 

```java
public interface CartRepository extends ReactiveCrudRepository<Cart,String> {
}
```

## 장바구니 보여주기

HomeController를 만들어 새로만든 레포지토리를 주입하자. itemRepository와 cartRepository를 사용할 수 있게 됐으므로 판매 상품목록과 장바구니를 더 상세하게

보여 줄 수 있다.

```java
	@GetMapping
	Mono<Rendering> home() { 
		return Mono.just(Rendering.view("home.html")
				.modelAttribute("items",
						this.itemRepository.findAll())
				.modelAttribute("cart",
						this.cartRepository.findById("My Cart")
								.defaultIfEmpty(new Cart("My Cart")))
				.build());
	}
```

 `Flux<Item>` 을 반환하는 itemRepository.findAll()과 `Mono<Cart>` 를 타임리프 같은 리액티브 스트림 호환 템플릿 엔진에 제공하면, 아주 약간의 HTML만 home.html에

추가해서 데이터를 화면에 렌더링 할 수 있다. 현재 판매 상품 목록을 표시하기 위해 `<table>` 요소를 HTML에 추가하자

```html
<h2>Inventory Management</h2>
    <table>
        <thead>
            <tr>
                <th>Id</th>
                <th>Name</th>
                <th>Price</th>
            </tr>
        </thead>
        <tbody>
        <tr th:each="item : ${items}">
            <td th:text="${item.id}"></td>
            <td th:text="${item.name}"></td>
            <td th:text="${item.price}"></td>
            <td>
                <form method="post" th:action="@{'/add/' + ${item.id}}">
                    <input type="submit" value="Add to Cart" />
                </form>
            </td>
            <td>
                <form th:method="delete" th:action="@{'/delete/' + ${item.id}}">
                    <input type="submit" value="Delete"/>
                </form>
            </td>
        </tr>
        </tbody>
    </table>
```

HTML은 GET, POST 두 가지 요청 방식을 지원한다. 다른 방식을 사용하려면 약간의 추가 작업이 더 필요한데 타임리프에서는 `th:method="delete"` 를 써서 간단히 DELETE

요청을 보낼 수 있다. `th:method="delete"` 가 실제 HTML로 렌저링될 때는, `<input type="hidden" name="_method" value="delete"/>` 와 같이 변환되고

이 hidden 값을 POST요청으로 전송한다. 스프링 웹플럭스에는 @DeleteMapping 애노테이션이 붙은 컨트롤러 메소드로 요청을 전달하는 특수 필터가 포함돼있다.

이 필터는 기본으로 활성화돼 있지 않으며 다음 설정을 properties에 추가하면 활성화할 수 있다.

```properties
spring.webflux.hiddenmethod.filter.enabled=true
```

스프링 부트의 `속성 기반 자동설정` 덕분에 별도의 자바 코드 없이 속성 설정만으로 쉽게 활성화 할 수 있다. 이제 쇼핑 장바구니를 표시하기 위한 HTML요소를 추가하자.

```html
<h2>My Cart</h2>
    <table>
        <thead>
            <tr>
                <th>Id</th>
                <th>Name</th>
                <th>Quantity</th>
            </tr>
        </thead>
        <tbody>
        <tr th:each="cartItem : ${cart.cartItems}">
            <td th:text="${cartItem.item.id}"></td>
            <td th:text="${cartItem.item.name}"></td>
            <td th:text="${cartItem.quantity}"></td>
        </tr>
        </tbody>
    </table>
```



![image](https://user-images.githubusercontent.com/40031858/128197978-d05138eb-c6c3-4907-87f3-a453f1b66df9.png)

## 장바구니에 상품 담기

이제 장바구니에 상품을 담아보자. 장바구니 담기는 이커머스 시스템에서 필수 기능이다. 해야할 작업을 구체화하면 다음과 같다

- 현재 장바구니를 조회하고, 없으면 비어있는 새 장바구니 생성
- 장바구니에 담은 상품이 이미 장바구니에 있던 상품이라면 수량만 1 증가시키고, 기존에 없던 상품이라면 상품정보 표시후 수량을 1로 표시
- 장바구니 저장

장바구니에 담은 상품을 실제로 데이터베이스에 반영하려면 add/{itemId} 를 처리할 코드를 작성해야 한다

```java
@PostMapping("/add/{id}")
	Mono<String> addToCart(@PathVariable String id) {
		return this.cartRepository.findById("My Cart")
				.defaultIfEmpty(new Cart("My Cart"))
				.flatMap(cart -> cart.getCartItems().stream()
						.filter(cartItem -> cartItem.getItem()
								.getId().equals(id))
						.findAny()
						.map(cartItem -> {
							cartItem.increment();
							return Mono.just(cart);
						})
						.orElseGet(() -> {
							return this.itemRepository.findById(id)
									.map(CartItem::new)
									.map(cartItem -> {
										cart.getCartItems().add(cartItem);
										return cart;
									});
						}))
				.flatMap(this.cartRepository::save)
				.thenReturn("redirect:/");
	}
```

## 서비스 추출

바로 위의 addToCart() 메소드를 보면 상당히 복잡하고 양도 많다. 코드양이 왜저렇게 많을까? 이유는 임시 변수가 없고 상태를 표시하는 중간 단계가 없기 때문이다. 

이런 변수와 상태를 빼내고 나면 어떤 작업을 실제로 수행하는 연산만 남게된다. 각 행은 들어온 입력값에 대한 연산을 수행하고 다음 단계로 넘길 뿐이다.

이렇게 하면 웹 컨트롤러가 무거워지는 것은 시간 문제일 것이다. 스프링 부트 프로젝트 리드인 필웹은 비즈니스 로직이 아닌 웹 요청 처리만 컨트롤러가 담당하도록

만드는 것을 추천한다. 장바구니를 조회하고 상품을 담는 기능은 서비스로 추출해야한다.

```java
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
```

상품을 장바구니에 담는 핵심 과정을 서비스로 옮겨서 컨트롤러를 단순화 할 수 있었다. 추출된 CartService를 주입후 사용하면 addToCart() 메소드는 다음과 같이 간결해진다

```java
@PostMapping("/add/{id}")
	Mono<String> addToCart(@PathVariable String id) {
		return this.cartService.addToCart("My Cart",id)
				.thenReturn("redirect:/");
	}
```

![image](https://user-images.githubusercontent.com/40031858/128200532-34dc382d-89cf-451a-bc2b-21522ba7ea55.png)

이제 Add to Cart 버튼을 누르면 상품이 카트에 담기고 수량이 증가함을 볼 수 있다.

## 데이터베이스 쿼리 

실제 이커머스 서비스에서는 데이터베이스로 id로 조회하는 기능만있는게 아니다. 오히려 전체상품을 보여주고 고객에게 상품을 고르게 하는 것이 일바넞ㄱ이다.

그뿐만 아닌 고객에게 검색어를 입력하게 하고 그에맞는 상품목록을 보여주는 기능이 필요하다. 이제 검색기능을 만들어보자.

```java
public interface ItemRepository extends ReactiveCrudRepository<Item,String> {
    Flux<Item> findByNameContaining(String partialName);
}
```

이 레포지토리는 고객이 입력한 검색어가 이름에 포함된 상품을 반환하는 메소드를 가지고 있다. 메소드 이름에 포함된 `containing` 이라는 일종의 키워드가 그 기능을 담당하며

개발자과 관련 구현코드를 직접 작성할 필요가 없다.  Item객체에 description 등 몇가지 필드가 더있다고 가정하고 스프링 데이터의 메소드 이름규칙을 알아보자

```java
@Data
public class Item {
    private  @Id String id;
    private String name;
  	private String description;
    private double price;
  	private String distributiorRegion;
  	private Date releaseDate;
  	private int availableUnits;
  	private Point location;
  	private boolean active;
}
```

상품의 여러 속성을 기준으로 쿼리문을 자동으로 만들어주는 메소드 이름규칙은 다음과 같다.



| 쿼리 메소드                                      | 설명                                                         |
| :----------------------------------------------- | :----------------------------------------------------------- |
| findByDescription(...)                           | description 값이 일치하는 데이터 질의                        |
| findByNameAndDescription(...)                    | Name 값과 description 값이 모두 일치하는 데이터 질의         |
| findByNameAndDistributorRegion(...)              | Name 값과 distributorRegion 값이 모두 일치하는 데이터 질의   |
| findTop10ByName(...) 또는 findFirst10ByName(...) | Name 값이 일치하는 첫 10개의 데이터 질의                     |
| findByNameIgnoreCase(...)                        | name값이 대소문자 구분 없이 일치하는 데이터 질의             |
| findByNameAndDescriptionAllIgnoreCase(...)       | name값과 description값 모두 대소문자 구분 없이 일치하는 데이터 질의 |
| findByNameOrderByDescriptionAsc(...)             | name값이 일치하는 데이터를 description 값 기준 오름차순으로 정렬한 데이터 질의 |
| findByReleaseDateBefore(Date date)               | releaseDate 값이 date보다 이전인 데이터 질의                 |
| findByReleaseDateAfter(Date date)                | releaseDate 값이 date이후인 데이터 질의                      |

| findByAvailableUnitsGreaterThan(int units)      | availableUnits 값이 units 보다 큰 데이터 질의                |
| :---------------------------------------------- | :----------------------------------------------------------- |
| findByAvailableUnitsGreaterThanEqual(int units) | availableUnits 값이 units 보다 크거나 같은 데이터 질의       |
| findByAvailableUnitsLessThan(int units)         | availableUnits 값이 units 보다 작은 데이터 질의              |
| findByAvailableUnitsLessThanEqual(int units)    | availableUnits 값이 units 보다 작거나 같은 데이터 질의       |
| findByAvailableUnitsBetween(int from, int to)   | availableUnits값이 from과 to 사이에 있는 데이터 질의         |
| findByAvailableUnitsIn(Collection unitss)       | availableUnits값이 unitss 컬렉션에 포함돼있는 데이터 질의    |
| findByAvailableUnitsNotIn(Collection unitss)    | availableUnits값이 unitss 컬렉션에 포함돼있지 않은 데이터 질의 |
| findByNameNotNull() 또는 findByNameIsNotNull()  | name 값이 null이 아닌 데이터 질의                            |
| findByNameNull() 또는 findByNameIsNull()        | name값이 null인 데이터 질의                                  |
| findByNameLike(String f)                        | name값이 문자열 f를 포함하는 데이터 질의                     |

| findByNameNotLike(String f) 또는 findByNameIsNotLike(String f) | name값이 문자열 f를 포함하지 않는 데이터 질의                |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| findByNameStartingWith(String f)                             | name값이 문자열 f로 시작하는 데이터 질의                     |
| findByNameEndingWith(String f)                               | name값이 문자열 f로끝나는 데이터 질의                        |
| findByNameNotContaing(String f)                              | name값이 문자열 f를포함하지 않는 데이터 질의                 |
| findByNameRegex(String pattern)                              | name값이 pattern으로 표현되는 정규 표현식에 해당하는 데이터 질의 |
| findByLocationNear(Point p, Distance max)                    | location값이 p지점 기준 거리 max이내에서 가장 가까운 순서로 정렬된 데이터 질의 |
| findByLocationNear(Point p, Distance min, Distance max)      | location값이 p 지점 기준 거리 min이상 Max이내에서 가장 가까운 순서로 정렬된 데이터 질의 |
| findByLocationWithin(Circle c)                               | location값이 원 영역 c 안에 포함돼있는 데이터 질의           |
| findByLocationWithin(Box b)                                  | location값이 직사각형 영역 b안에 포함돼 있는 데이터 질의     |
| findByActiveIsTrue()                                         | active값이 true인 데이터 질의                                |

| findByActiveIsFalse()           | Active 값이 false인 데이터 질의                |
| ------------------------------- | ---------------------------------------------- |
| findByLocationExists(boolean e) | location 속성의 존재 여부 기준으로 데이터 질의 |

위의 표는 몽고디비 레포지토리에서 지원하는 키워드가 나열돼 있는데 몽고디비 레포지토리 메소드가 지원하는 반환 타입은 다음과 같다`

- `Item(또는 자바 기본 타입)`
- `Iterable<Item>`
- `Iterator<Item>`
- `Collection<Item>`
- `List<Item>`
- `Optional<Item>` (자바 8 또는 Guava)
- `Optional<Item>` (스칼라 또는 Vavr)
- `Stream<Item>`
- `Future<Item>`
- `CompletableFuture<Item>`
- `ListenableFuture<Item>`
- `@Async Future<Item>`
- `@Async CompletableFuture<Item>`
- `@Async ListenableFuture<Item>`
- `Slice<Item>`
- `Page<Item>`
- `GeoResult<Item>`
- `GeoResults<Item>`
- `GeoPage<Item>`
- `Mono<Item>`
- `Flux<Item>`

반환 타입과 메소드 이름 키워들르 잘 조합하면 직접 코드를 작성하지 않고도 다양한 쿼리문을 스프링 데이터가 자동으로 만들어내게 할 수 있다.

## 쿼리문 자동 생성 메소드로 충분하지 않을 때 

메소드 이름 규칙만으로는 작성하기 어려운 커스텀 쿼리는 어떻게 구현해야 할까? 이럴때는 쿼리문을 직접 작성해야한다

```java
@Query("{ 'name' : ?0 'age' : ?1}")
Flux<Item> findItemForCustomerMonthlyReport(String name, int age);

@Query(sort="{ 'age' : -1}")
Flux<Item> findSortedStuffForWeeklyReport();
```

스프링 데이터의 `@Query` 애노테이션이 붙어 있는 메소드는 레포지토리 메소드 이름 규칙에 의해 자동으로 생성되는 쿼리문 대신에 `@Query` 내용으로 개발자가 직접 명시한쿼리문을

사용한다. 반환타입(`Flux<Item>`)은 변환 과정에서 사용된다

## Example쿼리

쿼리 자동생성과 직접 작성 쿼리를 사용하면 많은 일을 해낼 수 있다. 하지만 필터링 기능을 추가한다면 어떻게 해야할까? 고객에게 여러 필드에 대한 검색 기능을 제공하려면 어떻게

해야 할 까? name값을 기준으로 검색하는 기능을 추가한다고 가정해보자 . `findByName(String name)` 메소드를 추가하기만 해도 된다. 그렇다면 이번에는 name값이 전부

일치하는 검색이 아닌 부분일치하는 데이터도 포함해야한다면 어떻게할까? `findByNameContaining(String partialName)` 메소드를 사용하면 된다.

그렇다면 description도 검색조건에 포함해야 한다면? `findByNameAndDescription(String name, String description)` 을 사용하면 된다. 그렇다면

name, description 일치 검색뿐아니라 대소문자 구분없이 부분 일치하는 데이터도 검색결과에 포함해야 한다면? 

`findByNameContainingAndDescriptionContainingAllIgnoreCase(String partialName, String partialDescription)` 을사용하면 된다.

이쯤되면 요구사항이 추가될때마다 메소드 이름은 점점 더 복잡해짐을 알 수 있다. 일단 지금까지 요구사항을 반영한 코드는 다음과같다

```java
//name 검색
Flux<Item> findByNameContaingIgnoreCase(String partialName);

//description 검색
Flux<Item> findByDescriptionContainingIgnoreCase(String partialName);

//name AND description 검색
Flux<Item> findByNameContainingAndDescriptionContainingAllIgnoreCase(String partialName, String partialDesc);

//name OR description 검색
Flux<Item> findByNameContainingOrDescriptionContainingAllIgnoreCase(String partialName, String partialDesc);
```

이 레포지토리 메소드를 사용하는 서비스는 다음과 같다

```java
Flux<Item> search(String partialName, String partialDescription, boolean useAnd){
  if(partialName != null){
    if(partialDescription != null){
      if(useAnd){
        return repository.findByNameContainingAndDescriptionContainingAllIgnoreCase(partialName,partialDescription);
      }else{
				return repository.findByNameContainingOrDescriptionContainingAllIgnoreCase(partialName, partialDescription);
      }
    }else{
      return repository.findByNameContaining(partialName);
    }
  }else{
    if(partialDescription != null){
      return repository.findByDescriptionContainingIgnoreCase(partialDescription);
    }else{
      return repository.findAll();
    }
  }
}
```

이렇게 입력값 null 여부와 AND/OR 선택이라는 악몽 같은 분기 로직에 따라 어렵게 쿼리 메소드를 찾아가고 있다. 요구사항이 추가될 수록 유지관리성은 점점 악화될 것이 

분명해 보인다. 더 나은방법은 없을까? 바로 `Example 쿼리 (Query by Example)` 가 우리를 구원할것이다.

`Example` 쿼리를 사용해 여러 조건을 조립해서 스프링데이터에 전달하면 스프링 데이터는 필요한 쿼리문을 만들어준다. 그래서 조건이 추가될 때마다 계속 복잡해지는

코드를 유지 관리할 필요가 없다.

`Example` 쿼리를 사용하려면 먼저 `ReactiveQueryByExampleExecutor<T>` 를 상속받아야 한다.

새 레포지토리를 하나 정의해보자.

```java
public interface ItemByExampleRepository extends ReactiveQueryByExampleExecutor<Item> {
}
```

기나긴 이름을 가진 쿼리 메소드가 이제 없다. 어떻게 된 것일까? 스프링 데이터의 `ReactiveQueryByExampleExecutor` 를 보면 이해할 수 있다.

```java
public interface ReactiveQueryByExampleExecutor<T>{
  <S extends T> Mono<S> findOne(Example<S> var1);
  <S extends T> Flux<S> findAll(Example<S> var1);
  <S extends T> Flux<S> findAll(Example<S> var1, Sort var2);
  <S extends T> Mono<Long> count(Exmaple<S> var1);
  <S extends T> Mono<Boolean> exists(Example<S> var1);
}
```

`Example` 타입의 파라미터를 인자로 받아서 검색을 수행하고 하나 또는 그이상의 `T` 타입 값을 반환한다. 정렬 옵션도 줄 수 있고 검색 결과 개수를 세거나 데이터 존재 여부를 반환

하는 메소드도 있다. 이 API는 너무 단순해 보이기도 한다. 앞서 본 search() 코드를  `Example` 쿼리 방식으로 바꿔보면 강력한 효과를 금방 알수 있을 것이다. 이를 변경해보자.

```java
Flux<Item> searchByExample(String name, String description, boolean useAnd){
  Item item=new Item(name, description, 0.0);
  
  ExampleMatcher matcher= (useAnd)
    ? ExampleMatcher.matchingAll()
    : ExampleMatcher.matchingAny()
      	.withStringMatcher(StringMatcher.CONTAINING)
      	.withIgnoreCase()
      	.withIgnorePaths("price");
  
  	Example<Item> probe=Example.of(item,matcher);
  
  	return exampleRepository.findAll(probe);
}
```



`Example` 쿼리는 아주 가벼워 보이지만 기능은 막강하다. 주어진 요구사항을 모두 충족할 뿐만 아니라 향후 검색 조건 필드가 추가되더라도 어렵지 않게 수용할 수 있다.

이제  `Example` 쿼리를 사용한 검색 서비스를 웹 컨트롤러에 연결해보자. 스프링 웹플럭스 덕분에 아주 쉽게 연결할 수 있다.

```java
@GetMapping("/search")
Mono<Rendering> search(
	@RequestParam(required=false) String name,
	@RequestParam(required=false) String description,
	@RequestParam boolean useAnd) {
  return Mono.just(Rendering.view("home.html")
         .modelAttribute("results", inventoryService.searchByExample(name,description,useAnd))
                  .build());
}
```

## 평문형 연산

몽고디비 쿼리를 보통 문장 같은 형식으로 사용할 수 있는 `평문형연산(fluent operation)` 을 알아보자. 평문형 API는 여러가지 메소드 이름으로 연쇄적으로 연결해서 보통

문장처럼 작성할 수 있다. 스프링 데이터 몽고디비에서는 `FluentMongoOperations` 의 리액티브 버전인 `ReactiveFluentMongoOperations` 를 통해 평문형

연산 기능을 사용할 수 있다. 

```java
Flux<Item> searchByFluentExample(String name, String description){
  return fluentOperations.query(Item.class)
    	.matching(query(where("TV tray").is(name).and("Sumrf").is(description)))
    	.all();
}
```

이 평문형 API는 몽고디비에서 `{ $and: [ {name: 'TV tray'}, {description: 'Smurf' }]}` 를 입력해서 쿼리하는 것과 같다. 평문형 API에서는 앞서 봤던

`Example` 사용을 포함해서 옵션이 많다 일부 옵션의 사용법을 보자.!

```java
Flux<Item> searchByFluentExample(String name, String description, boolean useAnd){
  Item item=new Item(name, descrption, 0.0);
  
  ExampleMatcher matcher= (useAnd)
    ? ExampleMatcher.matchingAll()
    : ExampleMatcher.matchingAny()
      	.withStringMatcher(StringMatcher.CONTAINING)
      	.withIgnoreCase()
      	.withIgnorePaths("price");
  
  return fluentOperations.query(Item.class)
    	.matching(query(byExample(Example.of(item,matcher))))
    	.all();
}
```

## 트레이드 오프 

지금까지 스프링 데이터 몽고디비를 알아보면서 다음 내용을 배웠다

- 표준  CRUD 메소드 (findAll, findById)
- 메소드 이름 기반 쿼리 (findByNameContaing)
- Example 쿼리
- MongoOperrations
- @Query 애노테이션 사용 쿼리
- 평문형 APi

지금까지 알아본 다양한 사용법에는 다음과 같은 장단점이 있다

| 쿼리방법              | 장점                                                         | 단점                                                         |
| --------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 표준  CRUD 메소드     | - 미리 정의돼 있음<br />- 소스 코드로 작성돼 있음<br />- 리액터 타입을 포함해서 다향한 변환 타입 지원<br />- 데이터 스토어 간 호환성 | - 1개 또는 전부에만 사용 가능<br />- 도메인 객체별로 별도의 인터페이스 작성 필요 |
| 메소드 이름 기반 쿼리 | - 직관적<br />- 쿼리 자동 생성<br />- 리액터 타입을 포함한 다양한 반환 타입 지원<br />- 여러 데이터 스토어에서 모두 지원 | - 도메인 객체마다 레포지토리 작성 필요<br />- 여러 필드와 조건이 포함된 복잡한 쿼리에 사용하면 메소드 이름이 매우 길어지고 불편 |
| Example 쿼리          | - 쿼리 자동생성 <br />- 모든 쿼리 조건을 미리 알 수 없을때 유용<br />- JPA,레디스(Redis) 에서도 사용가능 | - 도메인 객체마다 레포지토리 작성 필요                       |
| MongoOperations       | - 데이터 스토어에 특화된 기능까지 모두 사용가능<br />- 도메인 객체마다 별도의 인터페이스 작성 불필요 | - 데이터 스토어에 종속적                                     |
| @Query 사용 쿼리      | - 몽고QL 사용 가능<br />- 긴 메소드 이름 불필요<br />- 모든 데이터 스토어에서 사용 가능 | - 데이터 스토어에 종속적                                     |
| 평문형  API           | - 직관적<br />- 도메인 객체마다 별도의 인터페이스 작성 불필요 | - 데이터 스토어에 종속적<br />- JPA와 레디스에서도 사용할 수 있지만 호환은 안됨 |

실제 프로젝트 상황에 맞게 데이터 스토어 독립성과 데이터 스토어 최적성 사이에서 올바른 선택을 하는 것이 중요하다.

## 정리

지금까지 2장에서 배운 내용은 다음과같다

- 완전한 리액티브 데이터 스토어에 필요한 요건
- 이커머스 애플리케이션 도메인 객체 정의
- 객체 저장 및 조회에 사용할 레포지토리 생성
- 커스텀 쿼리를 작성하는 여러 가지 방식
- 앞에서 다룬 모든 내용을 서비스에 옮겨 담아서 웹 계층과 분리하는 방법

3장 `스프링 부트 개발자 도구` 에서는 개발자 편의성을 위해 준비된 여러가지 도구를 살펴보고 이를 리액티브 애플리케이션에 적용해 최고의 효과를 얻는 방법을 알아보자
