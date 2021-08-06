# 3장 스프링 부트 개발자 도구

2장에서는 스프링 데이털르 사용하는 방법을 알아봤다. 스프링 도구가 번거로운 일을 맡아 처리해주므로 도메인 객체를 정의하고 쿼리를 작성하김나 하면 된다. 뿐만 아니라 요청을 

받아서 데이터를 조회하고 결과를 반환하는 모든 작업이 리액티브하게 수행된다. 이번 3장은 다음 내용을 다룬다

- 코드를 수정할 때 애플리케이션을 자동으로 재시작하는 방법
- 브라우저 새로고침을 자동으로 실행하는 라이브 리로드 사용법
- 프로젝트 리액터 디버깅 도구

## 애플리케이션 시작 시간 단축

코드를 수정할 때마다 변경사항을 애플리케이션에 반영하기 위해 애플리케이션을 계속 재시작 하면서 불편함을 느끼지 않은 개발자는 없을 것이다. WAR파일을 만들고

애플리케이션에 배포까지 해야 하는 상황이라면 애플리케이션 재시작은 정말 오래 걸린다.  스프링 부트가 나오기 한참 전부터 스프링 프레임워크는 무거운 애플리케이션 서버 대신

서블릿 컨테이널르 선택해서 재시작 문제 해결을 시도했었다. 이는 전체 개발 프로세스를 더 가볍고 신속하게 만드는 변화를 이끌어냈다.

하지만 아쉽게도 그런 변화 또한 충분치 않았고 개발 프로세스를 좀 더 개선하는 방법을 찾기 시작했다고 볼 수 있다.

그로 인해 스프링부트가 등장되었다. 스프링 부트가 처음 출시되면서 `내장형 서블릿 컨테이너` 라는 기념비적인 혁신을 이뤄냈다. WAR파일을 만들어서 아파치

톰캣 같은 이미 설치돼 있는 서블릿 컨테이너에 애플리케이션을 배포하는 방식이 아닌 역으로 애플리케이션에 서블릿 컨테이너를 포함하는 방식이다. 서블릿 컨테이너를

애플리케이션에 포함하면서 스프링 부트는 애플리케이션 시작 속도를 높인 것 뿐만 아니라 애플리케이션 배포 개념 자체를 뒤바꿨다. 애플리케이션은 더이상 운영담당자가

설치하고 운영하는 서블릿 컨테이너에 종속되지 않는다. 그대신 어떤 서블릿 컨테이너를 어떻게 사용할지 주체적으로 선택해서 스프링 부트를 통해 지정할 수 있다. 

이제 WAR파일을 서블릿 컨테이너에 배포할 필요 없이, JVM만 설치돼 있으면 어떤 장비에도 JAR파일을 배포해서 서블릿 컨테이너가 포함된 애플리케이션을

실행할 수 있다. 

## 개발자 도구

스프링 부트 개발팀은 내장형 서블릿 컨테이너로 얻은 월계관에 만족하지 않고 `DevTools` 라는 새로운 개발자 도구를 만들어 내었고 이 모듈에 포함된 기능은다음과같다.

- 애플리케이션 재시작과 리로드 자동화
- 환경설정 정보 기본값 제공
- 자동설정 변경사항 로깅
- 정적 자원 제외
- 라이브 리로드 지원

프로젝트에서 DevTools 개발자 도구를 사용하려면 빌드 파일에 다음 의존관계를 추가해야 한다

```groovy
    compileOnly("org.springframework.boot:spring-boot-devtools")
```

`spring-boot-devtools` 는 애플리케이션 시작방법을 몰래 훔쳐본다. `java -jar` 명령이나 클라우드 서비스 제공자가 사용하는 특별한 클래스로더를 통해 애플리케이션이

실행되면, 훔쳐보던 `spring-boot-devtools` 가 이번실행은 상용이구나 라고 판단하고 개발자 도구 기능을 활성화하지 않는다. 하지만 IDE에서 실행되거나 

`spring-boot:run` 명령으로 실행되면, `spring-boot-devtools` 는 이번 실행이 개발모드라고 판단하고 개발자 도구의 모든 기능을 활성화한다.

### 자동 재시작과 리로딩

애플리케이션 개발 속도를 높이기 위해 스프링 부트 개발팀은 사용자 코드 변화를 감지하고 애플리케이션을 재시작하는 기능을 추가했다. 

먼저 애플리케이션을 실행후 브라우저에서 http://localhost:8080에 접속해보자.

현재 HomeController.home()코드는 다음과 같다

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

이제 코드의 itemRepository.findAll() 에 다음과 같이 doOnNext()를 추가해보자

```java
this.itemRepository.findAll().doOnNext(System.out.println)
```

이제 애플리케이션 자동 재시작이 제대로 동작하는지 확인해보자

1. IDE에서 저장 명령이나 빌드 프로젝트 명령을 실행해 스프링 부트에게 재시작하라는 신호를 보낸다
2. 콘솔 출력을 모니터링해서 애플리케이션 재시작이 동작하는지 확인한다
3. 브라우저 페이지를 새로고침해서 상품조회를 재실행하면 콘솔에 상품 정보가 출력된다

### 정적 자원 제외

기본적으로 스프링 부트는 다음과 같은 자원에는 변경이 발생해도 재시작을 하지 않는다.

- /META-INF/maven
- /META-INF/resources
- /resources
- /static
- /public
- /templates

대부분의 웹 기술에서는 정적 자원 변경 내용은 재부팅 없이도 서버에 반영할 수 있으므로, 정적 자원 변경은 애플리케이션 재시작을 유발하지 않는다. 변경사항이 재시작을 유발

하지 않게 하는 경로를 변경하려면 다음과 같이 설정하면 된다.

```properties
spring.devtools.restart.exclude=static/**,public/**
```

이렇게 명시적으로 설정하면 /static, /public 폴더에 있는 자원 변경은 서버 재시작을 유발하지 않는다. 그 외의 위치에 존재하는 모든 자원 변경은 재시작을 유발한다.

`spring.devtools.restart.enabled=false` 로 지정하면 개발자 도구에 의한 재시작 자체를 아예 비활성화 할 수 있다.

### 개발 모드에서 캐시 비활성화

스프링 부트와 통합되는 많은 컴포넌트는 다양한 캐시 수단을 가지고 있다. 예를들어 어떤 템플릿 엔진의 경우에는, 컴파일된 템플릿을 캐시하기도 한다. 이는 상용 운영 환경에서는 편리

한 기능이지만 변경사항을 계속 확인해야 하는 개발과정에서는 불편함을 가중시킨다. 스프링 부트에는 타임리프의 캐시 설정기능이 있다. `spring.thymeleaf.cache` 값을

false로 설정하면 타임리프 캐시 기능을 비활성화 할 수 있다. 이외에도 여러 옵션이 있다. 하지만 이런 설정값을 운영환경, 개발 환경에 따라 활성화/비홀성화하는 것은 이내 번거로운

일이 돼버린다. 스프링 부트 개발자 도구가 이런 불편함을 해소해준다. 빌드 파일에 `spring-boot-devtools` 를 추가해서 IDE나 메이븐으로 애플리케이션을 실행하면 개발모드로

실행이 되고 여러가지 환경설정 정보가 기본으로 정해진 값으로 지정된다. 이 기능을 `속성 기본값 적용 (property default)` 라고 한다.

### 부가적 웹 활동 로깅

스프링 웹플럭스나 스프링 MVC로 만들어진 웹 애플리케이션에서는 `application.properties` 파일에 다음 내용을 추가하면 `web` 로깅 그룹에 대한 로깅을 활성화 할 수 있다

```properties
logging.level.web=DEBUG
```

특정 패키지 내의 모든 파일에 사용된 모든 로깅코드를 변경하거나, 클래스 수준 디버깅 설정을 뒤져보지 않아도 간단하게 web 로깅 그룹의 로그를 출력해서 웹 수준에서 어떤일이

수행되는지 확인할 수 있다. 

### 자동설정에서의 로깅 변경

스프링 부트에서 가장 강력한 기능 중 하나는 `자동설정(autoconfiguration)` 이다. 여러가지 빈의 존재 여부, 환경설정 정보, 클래스패스에 있는 라이브러리 정보를 기준으로 스프링 

부트가 자동으로 여러 정보를 설정하고 애플리케이션을 구성한다. 자동설정 기능에서 기본으로 구성해주는 대로 사용해도 괜찮지만, 때론 어떻게 동작하는지 파악하기 어려울 수도 있다.

자동 설정에 의해 어떤 작업이 수행되는지 확인할 수 있는 기능이 스프링 부트에 포함돼 있지만, 스프링 부트가 애플리케이션을 자동으로 구성하면서 결정한 모든 내용을 보려면

분량이 너무많다. 그래서 스프링 부트 2부터는 자동설정의 기본값과 다르게 설정된 내용만 확인할 수 있도록 변경사항을 관리한다. 예를 들어 어떤 빈을 추가해서 자동설정 기본값과

다르게 구성됐다면 그 달라진 내용만 보여준다

### 라이브 리로드 지원

스프링 부트 개발자 도구에는 `라이브 리로드(LiveReload) 서버`  가 내장돼 있다. 라이브 리로드는 서버가 재시작됐을때 웹 페이지를 새로 로딩하는 단순한 작업을 수행한다

쉽게 말해, 서버가 재시작됐을때 브라우저의 새로고침 붜튼을 자동으로 눌러준다고 생각하면 된다. 아주 단순한 기능 같지만 템플릿이나 웹 핸들러 관련 문제를 해결할 때 라이브

리로드 기능을 사용하면 생산성을 높일 수 있다. 라이브 리로드를 사용하려면 백엔드에서 서버를 실행하고 브라우저에도 [LiveReload](http://livereload.com/extensions)플러그인을 설치해야한다

---



## 리액터 개발자 도구

지금까지 알아본 내용은 모두 스프링부트와 관련된 기능이다. 이제 프로젝트 리액터용 개발자 도구도 살펴보자

### 리액터 플로우 디버깅

리액터 플로우 중에 무언가 잘못된다면 어떻게 대응할 것인가? 스택트레이스를 살펴보면 될까? 아쉽지만 리액터 처리과정은 일반적으로 여러 스레드에 걸쳐 수행될 수 있으므로

스택 트레이스를 통해 쉽게 확인할 수 없다. 리액터는 나중에 `구독(subscription)` 에 의해 실행되는 작업 흐름을 `조립(assemble)` 하는 비동기, 논블로킹연산을 사용한다

그럼 스택 트레이스를 출력하면 어떤 내용이 나을까?  

애플리케이션에서 리액터로 작성하는 일련의 연산은 앞으로 어떤 작업이 수행될지 기록해놓은 조리법이라고 할 수 있다. 스프링 레퍼런스 문서에서는 이를 `조립(assembly)` 라고

부르며, 구체적으로는 람다 함수나 메소드 레퍼런스를 사용해서 작성한 명령 객체를 합쳐놓은 것이라 볼 수 있다. 조리법에 포함된 모든 연산은 하나의 스레드에서 처리될수도 있다

하지만 누군가가 구독하기 전까지는 아무런 일도 발생하지 않는다는 점을 잊지말아야 한다. 조리법에 적힌 내용은 구독이 돼야 실행되기 시작하며, 리액터 플로우를 조립하는 데 사용

된 스레드와 각 단계를 실제 수행하는 스레드가 동일하다는 보장은 없다. 

한가지 골치 아픈 사실이 있는데, 자바 스택 트레이스는 동일한 스레드 내에서만 이어지며, 스레드 경계를 넘어서지 못한다는 점이다. 멀티 스레드를 사용하는 환경에서 예외를 잡아서

스레드 경꼐를 넘어 전달하려면 특별한 조치를 해줘야한다. 이 문제는 `구독` 하는 시점에 시작돼서 작업 흐름의 각 단계가 여러 스레드를 거쳐서 수행될 수 있는 리액티브 코드를 

작성할 때는 훨씬 더 심해진다.

```java
static class SimpleExample {
  public static void main(String[] args){
    ExecutorService executor= Executors.newSingleThreadScheduledExecutor();
    
    List<Integer> source;
    if(new Random().nextBoolean()){
      source=IntStream.range(1,11).boxed()
        .collect(Collectors.toList());
    }else{
      source=Array.asList(1,2,3,4);
    }
    try{
      executor.submit(() -> source.get(5)).get();
    }catch(InterruptedException | ExecutionException e){
      e.printStackTrace();
    }finally{
      executor.shutdown();
    }
  }
}
```

리액터 기반이 아닌 이 명령형 코드는 `ExecutorService` 를 생성하고 긴 List와 짧은 List를 임의로 생성한 후에 List를 생성한 스레드가 아닌 다른 스레드에서 람다식을 통해

List의 5번째 원소를 추출한다. 이 코드는 성공 또는 실패 두가지 경로로 실행된다. 10개의 원소를 가진 List가 생성되면 5번째 원솔르 추출하는데 아무런 문제가 없을 것이고,

4개의 원소를 가진 List가 생성되면 5번째 원소를 추출할 때 ArrayIndexOutOfBoundsException이 발생한다.

스택 트레이스를 보면 FutureTask 에서 ExecutionException이 발생했고 원인은 ArrayIndexOutOfBoundsException 라고 나와있다. 새스레드를 시작한 시점에서

끝이 나고, 새 스레드 시작전에 어느 경로를 타고 리스트가 생성됐는지는 나오지 않는다. 리스트에서 5번째 원소를 가져오는 스레드와 리스트를 생성한 스레드가 다르기 때문이다

명령행 코드가 아니라 리액터 코드로 작성해서 실행되면 어떻게 나올까?

```java
static class ReactorExample{
  public static void main(String[] args){
    Mono<Integer> source;
    if(new Random().nextBoolean()){
      source=Flux.range(1,10).elementAt(5);
    }else{
      source=Flux.just(1,2,3,4).elementAt(5);
    }
    source
      	.subscribeOn(Schedulers.parallel())
      	.block();
  }
}
```

리액터로 작성ㅎ된 코드는 랜덤하게 10개 또는 4개짜리 Flux를 생성하고 Flux.elementAt(5) 를 호출해 5번째 원소를 포함하는 Mono를 반환한 후에 Schedulers.parallel() 를 호출

해서 리액터 플로우가 여러 스레드에서 병렬 실행된다. 리액터로 작성한 코드를 실행해도 스택트레이스에 많은 내용이 출력되지만, 최초의 문제 발생 지점인 Flux 생성지점까지

출력하지는 못하므로 큰 의미가 없다. 이 문제는 리액터가 아니라, 자바의 스택 트레이스 처리에서 기인하는 문제다. 리액터는 스택 트레이스를 통해 가능한 먼곳까지 따라가지만

다른 스레드의 내용까지는 쫓아가지 못한다. 이 한계를 극복하게 해주는 것이 바로 리액터의 `Hooks.onOperatorDebug()` 메소드이다.

```java
static class ReactorExample{
  public static void main(String[] args){
    
    Hooks.onOperatorDebug();
    
    Mono<Integer> source;
    if(new Random().nextBoolean()){
      source=Flux.range(1,10).elementAt(5);
    }else{
      source=Flux.just(1,2,3,4).elementAt(5);
    }
    source
      	.subscribeOn(Schedulers.parallel())
      	.block();
  }
}
```



`Hooks.onOperatorDebug()` 를 호출해서 리액터의 `백트레이싱 (backtracing)` 을 활성화 한 것외에는 기존 코드와 같다. 이대로 실행해보면 이번에는 의미있는

정보들이 출력된다 `Hooks.onOperatorDebug()` 를 호출했을 뿐인데 스택트레이스에 의미있는 정보가 출력되므로 오류를 찾기 훨씬 쉬워졌다.

이 기능은 정말 혁신적이라 할 수 있다. 프로젝트 리액터는 오류 관련 핵심 정보를 스레드 경계를 넘어서 전달할 수 있는 방법을 만들어냈다. 리액터 자체로는 JVM이 지원하지

않으므로 스레드 경계를 넘을 수 없지만, `Hooks.onOperatorDebug()` 를 호출하면 리액터가 처리 흐름 조립 시점에서의 호출부 세부 정보를 수집하고 구독해서 

실행되는 시점에 세부정보를 넘겨준다. 프로젝트 리액터는 완전한 비동기, 논블로킹 도구다. 리액터 플로우에 있는 모든 연산은 다른 스레드에서 실행될 수도 있다.

리액터 개발자 도구 없이 개발자 스스로 리액터 플로우 처리 정보를 스레드마다 넘겨주도록 구현하려면 엄청나게 많은 비용이 들것이다. 리액터는 확장성 있는 애플리케이션을

만들 수 있는 수단을 제공함과 동시에 개발자의 디버깅을 돕는 도구와 함께 제공한다.

### 리액터 플로우 로깅

리액터에서는 실행 후에 디버깅하는 것 외에 실행될 때 로그를 남길 수도 있다. 리액터 코드에 `log.debug()` 를 사용해보면 전통적인 명령행 코드에서와 달리 원하는 대로 

출력하는게 쉽지않다는 것을 알게 된다. 이런 이슈는 리액티브 스트림이 비동기라는 특성 때문이 아니라 리액터가 적용한 함수형 프로그래밍 기법에서 비롯된 문제다.

람다 함수나 메소드레퍼런스를 사용하면 `log.debug()` 문을 사용할 수 있는 위치에 제한을 받는다. 다음코드를 보자

```java
return itemRepository.findById(id)
  									.map(Item::getPrice);
```

이 예제에서 리액터의 map() 연산은 Item::getPrice 라는 메소드 레퍼런스를 이용해 Item 객체에서 price 값을 얻는다. 이렇게 메소드 레퍼런스를 사용하면 코드를 간결하게

줄일 수 있다. 하지만 로그를 찍으렴녀 다음과 같이 작성해야 하므로 간결함을 맇게된다.

```java
return imtepRepository.findById(id)
  										.map(item -> {
                        log.debug("Found item");
                        return item.getPrice();
                      });
```

메소드 레퍼런스를 썻다면 훨씬 간결했을 코드가 여러 행의 람다식을 사용하는 장황한 코드로 바뀌었다. 로그를 찍으려면 이렇게 하는 수밖에 없다.

그저 로깅때문에 간결한 코드를 쓸 수 없다는건 여러모로 심각한 문제다. 다행히 리액터는 이문제에 대한 해법을 제시한다. 리액터 플로우 실행중에 어느단계에 있는지를 알고 싶다면

다음과 같이 작성하면 된다.

```java
Mono<Cart> addItemToCart(String cartId, String itemId) {
		return this.cartRepository.findById(cartId) 
				.log("foundCart") 
				.defaultIfEmpty(new Cart(cartId)) 
				.log("emptyCart") 
				.flatMap(cart -> cart.getCartItems().stream() 
						.filter(cartItem -> cartItem.getItem() 
								.getId().equals(itemId))
						.findAny() 
						.map(cartItem -> {
							cartItem.increment();
							return Mono.just(cart).log("newCartItem");
						}) 
						.orElseGet(() -> {
							return this.itemRepository.findById(itemId) 
									.log("fetchedItem") 
									.map(CartItem::new) 
									.log("cartItem") 
									.map(cartItem -> {
										cart.getCartItems().add(cartItem);
										return cart;
									}).log("addedCartItem");
						}))
				.log("cartWithAnotherItem") 
				.flatMap(this.cartRepository::save) 
				.log("savedCart");
	}
```

이 코드에서는 InventoryService 안에 있는 여러 리액터 연산자 사이에 `log(...)` 문이 여러개 있는 것이 눈에 띄는데 각 `log(...)` 문은 각기 다른 문자열을 포함하고있다.

로그에는 addItemToCart() 메소드가 실행될 때 내부적으로 수행되는 일뿐만 아니라 리액티브 스트림 시그널 흐름도 모두 함께 출력된다. 이미 여러번 강조했지만, 구독하기 전까지

아무것도 실행되지 않는다. 구독은 리액터 플로우에서는 가장 마지막에 발생하지만 로그에서는 맨위에 표시된다. 

### 블록하운드를 사용한 블로킹 코드 검출

지금까지 어렵게 배운 리액티브 프로그래밍은 복도 끝 골방에 있는 외로운 개발자가 블로킹 API를 한 번 호출하면 아무 소용이 없게된다. 단 한사람으로 시스템이 걷잡을 수 없도록

느려지는 위험을 이대로 방치해도 괜찮을까? 절대 안된다. 그럼 블로킹 코드가 소스 어디에도 없고 관련설정도 적절하다는 것을 어떻게 보장할 수 있을까

바로 `블록하운드(BlockHound)` 가 이 중요한 임무를 담당한다. 블록하운드를 사용하기 위해 빌드파일에 다음 내용을 추가한다

```groovy
testCompile 'io.projectreactor.tools:blockhound:$LATEST_RELEASE'
```



블록하운드는 그 자체로는 아무일도 하지 않는다. 하지만 애플리케이션에 적절하게 설정되면 자바 에이전트 API를 이용해 블로킹 메소드를 검출하고, 해당 스레드가 블로킹 메소드 호출

을 허용하는지 검사할 수 있다. 스프링 부트 시작 생명주기에 블록하운드를 등록하자

```java

@SpringBootApplication
public class Chap03Application {
    public static void main(String[] args) {
      	BlockHound.install();
        SpringApplication.run(Chap03Application.class, args);
    }

}
```

`BlockHound.install()` 이 SpringApplication.run(...) 보다 앞에 위치하고 있는것을 눈여겨 보자. 이렇게 하면 스프링 부트 애플리케이션을 시작할 때 블록하운드가

바이트코드를 조작할 수 있게 된다.이제 애플리케이션을 시작하자. 애플리케이션은 정상적으로 시작되지만 http://localhost:8080 에 접속하면 오류가 출력된다.

상세한 스택트레이스에 블록하운드 관련 로그가 추가되었다. 핵심만 보자

![image](https://user-images.githubusercontent.com/40031858/128466639-b1a4136f-9238-4f49-9be7-c5586d36e732.png)

1. 블록하운드가 블로킹 I/O 메소드인 FileInputStream.readBytes() 호출 감지
2. 타임리프가 마크업을 위한 템플릿 처리 수행
3. 스프링 프레임워크 기반 람다 함수에 의해 타임리프 템플릿 엔진 호출

`FileInputStream.readBytes()` 메소드를 JDK 소스 코드에서 살펴보면 일부가 C언어로 구현된 네이티브 메소드임을 알 수 있다. 블록하운드가 네이티브 메소드 호출까지 

가로채서 블로킹 코드를 검출할 수 있다는 점은 상당히 인상적이다 블록하운드 덕분에 `FileInputStream.readBytes()` 호출이 블로킹 코드라는 점을 알게됐다. 

블로킹 코드가 하나라도 포함되면 리액티브 프로그래밍은 제대로 동작하지 않는다. 이문제를 어떻게 해결할까? 선택지는 다음과같다

- 타임리프에 티켓을 생성해서 블로킹 문제를 해결하게한다
- JDK에 테킷을 생성해서 블로킹 문제를 해결하게 한다
- 블로킹 부분을 수용가능하다고 결정하고 블록하운드가 이 부분을 건너뛰게한다

범용적으로 사용되는 JDK 메소드를 허용해서 무분별하게 블로킹 코드가 사용되는 위험을 감수하지말고 허용 범위를 좁혀서 좀더 구체적인 일부 지점만 허용하는 것이 안전하다

타임리프에서 템플릿을 읽는 부분에 사용된 블로킹 호출 부분인 `TemplateEngine.process()` 만 콕 찝어서 허용하면, 다른 곳에서 사용되는 `FileInputStream.readBytes()`

는 여전히 검출 대상이므로 블로킹 코드가 잘못 사용되는 위험을 막을 수 있다. `TemplateEngine.process()` 안에서 템플릿 파일을 읽고 마크다운 지시어를 전부 처리하므로

이부분만 허용 리스트에 추가하는 것이 좋다.

```java
@SpringBootApplication
public class Chap03Application {
    public static void main(String[] args) {
        BlockHound.builder()
                .allowBlockingCallsInside(TemplateEngine.class.getCanonicalName(),"process")
                .install();
        SpringApplication.run(Chap03Application.class, args);
    }

}
```

이제 다시 애플리케이션을 실행후 http://localhost:8080 에 접속해보면 블로킹 코드 호출 관련 오류메시지가 모두 사라졌음을 확인할 수 있다.

## 정리

지금까지 3장에서 배운 내용은 다음과 같다

- 스프링 부트 개발자 도구를 프로젝트에 추가하는 방법
- 소스 코드 변경 시 애플리케이션 자동 재시작
- 개발 모드로 실행 시 캐시 동작을 막는 방법
- 스프링 부트에 내장된 라이브 리로드 기능
- 스레드 경계를 넘는 스택 트레이스 설정
- 리액터의 로깅 연산자를 사용해서 로그 정보와 리액티브 스트림 시그널을 모두 로그에 남기는 방법
- JDK 메소드까지 포함해서 블로킹 코드 호출을 검출하는 블록하운드 사용법

4장 스프링부트 테스트 에서는 스프링 부트 테스트가 어떻게 일급 시민이 되는지 알아보고, 다양한 테스트 수준에서  사용할 수 있는 여러기법을 보자!

