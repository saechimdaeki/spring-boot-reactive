# 1장 스프링 부트 웹 애플리케이션 만들기

### 스프링 부트란 무엇인가

스프링 부트는 스프링 포트폴리오를 신속하게, 미리 정의된 방식으로, 이식성 있게, 실제 서비스 환경에 사용할 수 있도록 조립해놓은 것이다

- `신속성` -

  의존관계를 포함해서 애플리케이션의 여러 요소에 기반한 의사결정을 신속히 적용할 수 있게 해주므로 애플리케이션 개발 속도를 높이는데 도움

- `미리 정의된 방식` -

  스프링 부트를 어떻게 사용할지 구성을 정해주면, 그 구성에 따른 가정을 통해 미리 정의된 방식으로 기본적인 설정값이 자동으로 지정된다.

  이 기본 설정값은 여러 피드백을 거쳐 확립됐으며 커뮤니티에서도 널리 사용되고 있다.

- `이식성` - 

  사실상 자바의 표준 도구라고 할 수 있는 스프링 프레임워크 기반으로 만들어져 있어서, JDK가 있는곳이라면 스프링 부트 애플리케이션은

  어디에서나 실행될 수 있다. 특정한 인증을 받은 애플리케이션 서버나 특정 벤더 제품을 필요로 하지 않으므로, 스프링 부트를 사용해 

  애플리케이션을 만들고, 스프링 부트의 도구를 사용해 패키지를 만들면 어디든 배포해서 실행할 수 있다.

- `실제 서비스 환경에 사용 가능` -

  스프링부트는 의심할 바 없는 완성품이다. 아주 작은 부분에만 사용해야한다는 제약도 물론 없으며 실제로도 정말로 광범위하게 사용되고 있다.

  자바로 만들어진 가장 큰 서비스 중 하나인 [넷플릭스 블로그](https://netflixtechblog.com/netflix-oss-and-spring-boot-coming-full-circle-4855947713a0) 에서 다양한 사례를 볼 수 있다.

### 리액티브 프로그래밍 소개

요즈음에는 클라우드 환경에서 애플리케이션을 운영하는 것이 보편화되고 있어서 '문제가 생기면 서버를 더 투입하면 된다' 같은 예전 방식은

이제 통하지 않게 되고있다. 개발자들은 기존 자원을 더 효율적이고 일관성 있게 사용하는 방법을 찾고있으며, 그 해법이 바로 `리액티브 스트림` 이다.

http://www.reactive-streams.org/ 에 간단히 정리되어 있는 리액티브 스트림은 `발행자(publisher)` 와 `구독자(subscriber)` 사이의 계약을

정의하는 명세다. 트래픽을 가능한 한 빨리 발행하는 대신에 구독자가 '난 10개만 더 받을 수 있어' 라고 발행자에게 알리는 방식으로 트래픽을 제어할

수 있다. 그러면 발행자는 10개만 더 보낸다. `수요조절(demand control)` 을 떠올리면 된다. 

기업 간 시스템을 발행자와 구독자 관계로 연결하면 시스템 범위를 `배압(backpressure)` 을 적용할 수 있다. 성능이 획기적으로 개선되는 것은

아니지만 트래픽을 잘 조절해서 관리할 수 있는 장점을 누릴 수 있다. 리액티브 스트림은 아주 단순해서 사실 애플리케이션 개발자가 직접 다루는 것

을 추천하지는 않는다. 대신에 프레임워크의 기초를 이루며 상호운영성을 높이는데 사용된다.



`프로젝트 리액터(Project Reactor)` 는 VM웨어 에서 만든 리액티브 스트림 구현체다. 리액터를 사용하면 다음특성을 따르는 리액티브 프로그래밍

을 활용할 수 있게 된다.

- 논블로킹, 비동기 프로그래밍 모델
- 함수형 프로그래밍 스타일
- 스레드를 신경 쓸 필요 없는 동시성



### 리액터 타입

프로젝트 리액터는 핵심 타입인 `Flux<T>` 를 사용해서 수요 조절을 구현한다. `Flux<T>` 는 일련의 `T` 객체를 담고있는 컨테이너다.

`Flux<T>` 는 실제 물건을 전달해주는 역할을 하는 플레이스홀더로 , 쉽게 말해 레스토랑에서 일하는 서빙 점원과 비슷하다.

주방에서 요리가 완성되면, 점원이 주방에서 요리를 받아 손님에게 가져다주고, 다시 제자리로와 다음 요리를 기다린다. 

서빙점원은 다음 요리가 주방에서 언제 완성될지 알 수 없다. 언제가 됐든 요리가 완성되고, 서빙점원이 그 요리를 받아 전달할 수 있는

상태라면 서빙점원은 다음 요리를 손님에게 가져다준다. 이제 주방역할을 담당하는 서비스를 모델링해보자

```java
class KitchenService{
  Flux<Dish> getDishes(){
    // 요리를 담당하는 ChefService를 모델링해서 요리를 위임할 수도 있지만,
    // 단순하게 설명하기 위해 그냥 하드코딩으로 대체한다
    return Flux.just(
    	new Dish("Seasame chicken"),
      new Dish("Lo mein noodles, plain"),
      new Dish("Sweet & sour beef"));
  }
}
```

서빙 점원은 손님에게 가져다줄 `Dish(요리)` 객체를 달라고 KitchenService에 요청할 수 있다. 코드에 나온 세가지 요리가 모두 완성된 후에 받을 수도

있지만, `Flux<Dish>` 객체로 바로 받을 수 도있다. `Flux<Dish>` 안에 포함된 요리는 아직은 완성되지 않았지만 머지않아 완성될 것이다.

하지만 정확히 언제 완성될지는 알 수 없다. 하지만 요리가 완성되면 서빙점원은 행동에 나설 수 있다. 즉 요리완성에 대한 반응 행동, `리액트(react)`

라고 할 수 있다. 리액터는 `논블로킹(non-blocking)` 방식으로 동작하기 때문에, 주방에서 요리가 완성될 때까지 서빙 점원(서버 스레드) 이 

다른일을 못한 채 계속 기다리게 하지 않는다. 결과가 아직 정해지지 않았고 미래 어느 시점이 되어야 알수 있다는 점에서 `Flux` 는 `Future` 와

비슷하다. 결과를 미래에 알 수 있다는 관점에서는 비슷하지만, `Future` 는 이미 시작되었음을 나타내는 반면에, `Flux` 는 시작할 수 있음을 나타냄.

그렇다면 `Future` 는 제공해주지 않지만 `Flux` 는 제공해주는 것은 무엇일까? `Flux` 에는 다음과 같은 특징이 있다.

- 하나 이상의 Dish(요리) 포함 가능
- 각 Dish(요리) 가 제공될 때 어떤 일이 발생하는지 지정 가능
- 성공과 실패의 두 가지 경로 모두에 대한 처리 방향 정의 가능
- 결과 폴링(polling) 불필요
- 함수형 프로그래밍 지원

이제 좀더 많은 코드를 작성해보며 서빙 점원이 요리를 손님에게 비동기적으로 가져다주는 개념을 머릿속에 각인해보자

```java
class SimpleServer{
  private final KitchenService kitchen;
  
  SimpleServer(KitchenService kitchen){
    this.kitchen=kitchen;
  }
  
  Flux<Dish> doingMyJob(){
    return this.kitchen.getDishes()
      .map(dish -> Dish.deliver(dish));
  }
}
```

평범한 서빙 점원은 다음과 같은 특징이 있다.

- SimpleServer 인스턴스를 생성하는쪽에서 KitchenService를 제공해야한다. 이러한방식을 `생성자 주입` 이라 한다.
- doingMyJob() 함수는 레스토랑 매니저가 서빙짐원을 툭 치면, kitchen에 가서 요리를 받아오는(getDishes) 임무를 수행하는것으로 생각할수있다.
- 주방에 요리를 요청후에는 요리 완성후 해야할 일을 map()함수를 호출해서 지정한다. Deliver(dish)를 호출해서 요리를 손님에게 가져다주는일을 지정했다.
- deliver(dish) 는 요리의 delivered 상태를 true로 설정한다. 

예제 코드는 단순한 형태의 리액티브 컨슈머다. 리액티브 컨슈머는 다른 리액티브 서비슬르 호출하고 결괄르 반환한다.

프로젝트 리액터는 풍부한 프로그래밍 모델을 제공한다. 함수형 프로그래밍에서는 수행하는 변환 뿐만 아니라, `onNext()`, `onError()`,`onComplete()` 시그널 처럼

`Future` ㅇㅔ는 없는 리액티브 스트림 수명주기에 연결 지을 수도 있다. 이제 이 리액티브 스트림 수명주기를 이해하기 위한 친절한 서빙점원을 만들자.

```java
class PoliteServer{
  private final KitchenService kitchen;
  
  PoliteServer(KitchenService kitchen){
    this.kitchen=kitchen;
  }
  
  Flux<Dish> doingMyJob(){
    return this.kitchen.getDishes()
      .doOnNext(dish -> System.out.println("Thank you for " + dish + "!"))
      .doOnError(error -> System.out.println("So sorry about " + error.getMessage()))
      .doOnComplete(() -> System.out.println("Thanks for all your hard work!"))
      .map(Dish::deliver);
  }
}
```

친절한 서빙 점원이 kitchen을 주입받아 초기화하고 요리를 받은 후 어떤 처리를 한다는 점은 평범한 서빙 점원과 동일하지만 doingMyJob()내용은 다르다

- `doOnNext()` 를 사용해서 리액티브 스트림의 `onNext()` 시그널을 받으면 kitchen에게 '감사합니다' 라는 말을 하는 기능이 추가됐다
- `doOnError()` 를 사용해서 `onError()` 시그널을 받으면 처리해야 할 일을 지정해준다.
- `doOnComplete()` 를 사용해서 주방에서 모든 요리가 완성됐음을 의미하는 `onComplete()` 시그널을 받으면 처리해야 할 일을 지정해준다.

지금까지 kitchen을 정의하고 여러 유형의 서빙점원을 만들어봤다. 리액티브 스트림 시그널에 반응하면서 어떻게 데이터를 변환하는지도 살펴보았다.

하짐나 이런 흐름의 시작을 어떻게 유발하는지는 아직 알아보지 않았다. 흐름을 시작한다는 것은 어떤 의미일까? 프로젝트 리액터에서는 필요한 모든 흐름과

모든 핸들러를 정의할 수 있지만 `구독(subscription)` 하기 전까지는 실제 아무런 연산도 일어나지 않는다. 구독이 핵심이다. 구독은 리액터의 일부일 뿐만

아니라 앞서 소개했던 리액티브 스트림 스펙의 일부이기도 하다. 누군가 실제 데이터를 요청하기 전까지는 아무런 데이터도 구체화되지 않으며 어떤행위도

실행되지 않는다. 다음은 레스토랑에서 서빙 점원에게 요리를 손님에게 가져다주라고 요청하는 코드이다.

```java
class PoliteRestaurant{
  public static void main(String... args){
    PoliteServer server = 
      new PoliteServer(new KitchenService());
    
    server.doingMyJob().subscribe(
    	dish -> System.out.println("Consuming "+ dish),
    	throwable -> System.err.println(throwable));
  }
}
```



Server.doingMyJob() 을 호출한 후에 `subscribe()` 를 호출한다. doingMyJob()은 앞에서 살펴본 것처럼 `Flux<Dish>` 를 반환하지만, 아직까지

아무런 일도 일어나지 않는다. `Flux<Dish>` 는 머지않아 전달될 결과를 담는 플레이스홀더일 뿐이라는 사실을 기억하자. 코드를 좀더 자세히 살펴보면,

`subscriber()` 는 자바8의 `Consumer` 를 첫번째 인자로 받아들이는 것을 볼 수 있다. 이 콜백은 리액티브 스트림의 `onNext()` 시그널과 함께 

완성된 모든 요리 각각에 대해 호출된다. 예제에서는 `Consumer`  타입의 인자로 람다식이 사용됐다. 그런데 `subscribe()` 메소드는 두번째 인자로

throwable -> System.err.println(throwable) 이라는 람다식을 받고있다. 이 람다식은 리액티브 스트림이 `onError(throwable)` 시그널을

보낼 때 어떻게 반응해야하는지를 표현하고 있다.



---

지금까지 줄곧 주방에서 만들어진 요리를 손님에게 전달하는 서빙 점원에 대해 얘기하고 있다. 이제 레스토랑의 손님들이 모두 실제 웹사이트를 방문하는

사람들이고 주방은 다양한 데이터 저장소와 서버 쪽 서비스의 혼합물이라고 생각해보자. 손님에게서 주문을 받아서 주방에 전달하고, 완성된 요리를

손님에게 가져다주는 서빙 점원의 역할은 무엇일까? 바로 `웹 컨트롤러` 다. 주문을 비동기적으로, 논블로킹 방식으로 처리하는 서빙 점원이 하는 일은

리액티브 웹 컨트롤러가 하는 일과 정확하게 동일하다. 리액티브 웹 컨트롤러 방식의 이 서빙점원은 주문을 받아 주방에 전달한 후 요리가 완성될 때까지

아무 일도 하지 않고 그저 기다리기만 하는 기존의 점원과는 다르다. 가만히 빈둥거리는 대신 홀에 나가 다른손님들의 주문을 받거나 다른 요청사항을 

들어준다. 요리가 완성됐다는 신호를 주방에서 받았을때만 완성된 요리를 손님에게 가져다준다. 리액티브 웹 컨트롤러도 이와 똑같은 방식으로 동작한다

### 스프링 웹플럭스의 등장

스프링 MVC는 많은 개발자에게 익숙하며 널리 사용되어온 자바 서블릿 API를 기반으로 만들어졌다. 서블릿 API는 그 규약 내부적으로 많은 가정에 의존

하고 있다. 그중 가장 큰부분을 차지하는 것은 서블릿 API는 블로킹방식으로 동작한다는 것이다. 물론 서블릿 3.1 이래로 비동기 방식을 일부 지원하기

시작했지만 완전히 리액티브하다고 볼 수 없다. 서블릿 3.1에 도입된 비동기방식은 리액티브 이벤트루프와 배압 시그널을 지원하지 않는다.

이쯔음 `네티(Netty)` 가 등장한다. 네티는 100% 논블로킹, 비동기 웹 컨테이너로, 서블릿 스펙에 구속되지 않는다. 스프링 웹플럭스와 네티는 궁합이

잘맞는다. 스프링 웹플럭스를 사용하면, 스프링 MVC가 큰 인기를 끌 수 있게 만들었던 프로그래밍 모델 그대로 작성한 코드를 서블릿 컨테이너가 아닌

네티 위에서 실행할 수 있다.







----

### 이제 프로젝트를 만들어보자. 책에 작성된 버젼보다 최신버젼을 사용하였으며 코드의 내용은 그대로이되 라이브러리와 빌드툴은 스터디를 위해 더 편한 것을 채택하였다.

#### 첫코드

스프링 이니셜라이저는 다음과 같은 애플리케이션 파일을 자동으로 만들어준다.

```java
@SpringBootApplication
public class Chap01ReactiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(Chap01ReactiveApplication.class, args);
    }
}
```

- `@SpringBootApplication` 은 자동설정과 컴포넌트 탐색기능을 포함하는 복합 애노테이션이다
- `SpringApplication.run(Chap01ReactiveApplication.class, args)` 는 이클래스를 애플리케이션 시작점으로 등록하는 스프링부트 훅이다.

이 클래스는 '나는 웹 컨테이너에 설치할 필요가 없는 애플리케이션이야' 라고 말하고있다. 이 요술은 바로 자동설정과 컴포넌트 탐색 덕분이다.

## 자동설정

스프링부트에는 자동설정 기능이 포함돼있다. 자동설정이란 스프링 부트 애플리케이션의 설정 내용을 분석해서 발견되는 정보에 맞게 다양한 빈을

자동으로 활성화하는 조건 분기 로직이다. 다음과 같이 스프링 부트는 자동설정에 필요한 다양한 정책을 갖추고 있다.

- 클래스패스
- 다양한 설정 파일
- 특정 빈의 존재 여부
- 기타 등등

자동설정은 이와같이 애플리케이션의 여러 측면을 살펴보고 유추한 다음, 다양한 컴포넌트를 자동으로 활성화 한다. 

예를들어 `WebFluxAutoConfiguration ` 빈은 다음 조건이 충족될 때만 활성화된다.

1. 리액티브 컨테이너 존재
2. 클래스패스에 스프링 웹플럭스 존재
3. `WebFluxConfigurationSupport` 타입 빈의 부존재

이 시나리오에서 활성화되는 리액티브 웹 컨테이너는 프로젝트 리액터에 맞도록 네티를 감싸서 만든 `리액터 네티 (Reactor Netty)` 다.

스프링 웹플럭스는 프로젝트 리액터 기반으로 만들어진 스프링의 새로운 웹스택이다.

`WebFluxConfigurationSupport` 는 자동설정에서 빠져나올 수 있는 탈출구라고 할 수 있다. `WebFluxConfigurationSupport` 타입의

빈이 없다면 스프링 부트는 스프링 웹플럭스와 네티를 사용하는데 필요한 빈을 자동으로 생성한다. 하지만 자동 생성되어 설정된 빈이 요구사항에

맞지 않는 경우에는 `WebFluxConfigurationSupport` 타입 빈에 원하는 설정 내용을 작성하면 된다. 그러면 개발자가 직접 만든 

`WebFluxConfigurationSupport` 타입 빈이 활성화되고 자동설정에 의해 생성되던 빈은 더이상 생성되지 않는다. 바로 이점이 스프링 부트에서

가장 빛나는 기능 중의 하나이다. 어떤 설정 세트에 의해 활성화되던 빈이 다른 설정세트에 의해 비활성화되고 아무런 효력을 발휘하지 않게된다.

쉽게 말해 개발자가 어떤부분에 대해 직접 지정한 설정이 없으면 스프링 부트가 알아서 필요한 빈을 적절히 생성해서 사용하고, 해당 설정이

있으면 직접 지정한 대로 동작하고 그 부분에 대한 자동설정은 무효화된다.

## 컴포넌트 탐색

스프링 애플리케이션에서 빈을 등록하는 방식은 두가지로 나눌 수 있다. 빈으로 등록될 클래스의 물리적 위치와 무관하게 환경설정 클래스에서

직접 하나하나 빈으로 등록할 수 있고, 또는 그냥 빈의 존재를 플래그로 표시하기만 하고 나머지는 스프링이 `컴포넌트 탐색(component scan)` 을

통해 자동으로 빈을 찾아내고 등록하게 할 수도 있다.



### 스프링 웹플럭스 컨트롤러 생성

```java
@RestController
@RequiredArgsConstructor
public class ServerController {
    private final KitchenService kitchen;
    
    @GetMapping(value = "/server", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Dish> serveDishes(){
        return this.kitchen.getDishes();
    }
}
```

이 간단한 컨트롤러가 하는 일은 다음과 같다.

```markdown
### 1. @RestController는 화면 구성을 위한 템플릿을 사용하는 대신에 결과 데이터를 직렬화하고 HTTP응답 본문에 직접써서
### 반환하는 REST 컨트롤러임을 나타낸다.
### 2. 애플리케이션이 실행되면 스프링은 KitchenService의 인스턴스를 찾아서 자동으로 생성자에 주입해준다
### 3. @GetMapping(...)은 /server로 향하는 HTTP GET요청을 serverDishes 메소드로 라우팅해주는 스프링 웹MVC
### 애노테이션이다. 반환되는 미디어 타입은 text/event-stream이고, 클라이언트는 서버가 반환하는 스트림을 쉽게
### 소비(consume) 할 수 이다.
```

`Flux<Dish>` 는 앞서 살펴본 것과 마찬가지로 준비된 다수의 요리를 반환해주는 타입이다. 전통적인 자바 `Collection` 과의 가장 큰 차이점은

요리가 비동기적으로 전달된다는 점이다. 이런 비동기 방식 전달은 리액티브 실행 환경인 리액터와 네티에서 담당한다.

이번에는 KitchenService를 리액티브하게 동작하게 코드를 작성해보자.

```java
@Service
public class KitchenService {

    /**
     * 요리 스트림 생성
     */
    Flux<Dish> getDishes(){
        return Flux.<Dish> generate(sink -> sink.next(randomDish()))
                .delayElements(Duration.ofMillis(250));
    }

    /**
     * 요리 무작위 선택
     */
    private Dish randomDish(){
        return menu.get(picker.nextInt(menu.size()));
    }

    private List<Dish> menu= Arrays.asList(
            new Dish("Sesame chicken"),
            new Dish("Lo mein noodles, plain"),
            new Dish("Sweet & sour beef"));

    private Random picker=new Random();
}
```



- getDishes() 는 하드코딩을 사용했던 기존 코드와 동일한 메소드 시그니처(signature) 를 가지고있지만 3개 요리만 제공했던 기존 코드와 다르게 

  세 가지 요리 중에서 선택된 1개의 요리를 250밀리초 간격으로 계속 제공한다

기존 코드에서는 Flux.just(...) 를 사용해서 고정적인 목록의 요리만 만들어주고 끝났으므로 실제 주방상황을 시뮬레이션한다고 보기 어려웠지만,

이번 코드는 `Flux.generate()` 를 사용해서 요리를 연속적으로 계속 만들어 제공해주므로 실제 주방에서 벌어지는 일과 훨씬 더 비슷하다.

이제 Dish를 작성해보자. 

```java
@Data
public class Dish {

    private String description;
    private boolean delivered=false;

    public static Dish deliver(Dish dish){
        Dish deliveredDish = new Dish(dish.description);
        deliveredDish.delivered=true;
        return deliveredDish;
    }

    public Dish(String description) {
        this.description = description;
    }
}
```

이제 작성한 코드를 실행해보자. IDE상에서 실행버튼을 누르거나 책과 달리 gradle을 사용했으므로 다음 명령어를 수행하면된다.

```bash
$ ./gradlew bootRun
```

이제 원하는 웹 CLI 도구를 사용해 서비스를 요청하면 된다. -N 옵션을 사용하면 버퍼링을 사용하지 않고 데이터가 들어오는 대로 처리한다

```bash
$ curl -N -v localhost:8080/server
```

![image](https://user-images.githubusercontent.com/40031858/127990065-2a176b2b-886e-48a2-8ab3-4fb5976dedfc.png)

이제 ServerController에 또 하나의 웹 메소드를 추가해보자.

```java
 @GetMapping(value = "/served-dishes",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Dish> deliverDishes(){
        return this.kitchen.getDishes()
                .map(Dish::deliver);
    }
```

```bash
$ curl -N -v localhost:8080/served-dishes
```

![image](https://user-images.githubusercontent.com/40031858/127990536-45bc61dc-bebd-4e40-8a7e-542040b83dc2.png)

앞에서 /server를 호출했을때와 유일한 차이점은 delivered 값이 true라는 것이다. 지금 실행된 것을 보면 delivered 상태를 바꾼것처럼

다른변환도 가능하다는 점을 어렵지 않게 떠올릴 수 있다. kitchenServer가 제공하는 데이터셋을 받아서 서비스의 컨슈머가 원하는 대로 

변환하고 조작할 수 있다.



### 템플릿 적용

웹페이지 화면을 만들때 템플릿 라이브러리를 사용해보자. 리액티브 프로그래밍을 도입했다면 리액티브 스트림을 완벽하게 지원하는 [티임리프](https://www.thymeleaf.org/) 를

사용하는 것이좋다. 타임리프는 HTML과 100%호환된다. 타임리프 템플릿을 사용하는 간단한 예제를 살펴보자.

```java
@Controller
public class HomeController {

    @GetMapping
    Mono<String> home(){
        return Mono.just("home");
    }
}
```

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Hacking with Spring Boot - Getting Started</title>
</head>
<body>
    <h1>Welcome to Hacking with Spring Boot!</h1>

<p>
    Over the span of this book, you'll build different parts of an e-commerce system
    which will include fleshing out this web template with dynamic content, using the
    power of Spring Boot
</p>
</body>
</html>
```

템플릿 이름은 리액티브 기구인 `Mono` ㅇㅔ 담긴 채로 반환되는데, 앞선 예제처럼 아무일도 하지않고 단순히 템플릿 이름만 반환할 때는 굳이 

`Mono` 에 담아서 반환할 필요가없다. 스프링 부트는 자동설정을 통해 포함된 타임리프 뷰 리졸버를 사용해서 home이라는 반환값을 

Classpath:/templates/home.html 로변환한다. 

![image](https://user-images.githubusercontent.com/40031858/127991858-b8d8eea2-96dd-4ba0-98ed-d2e4f802a403.png)

# 정리

지금까지 1장에서 배운 내용은 다음과 같다

- 레스토랑 서빙 점원이 손님 및 주방과 어떻게 의사소통하는지 살펴보면서 리액티브 프로그래밍의 기초를 살펴봤다.
- 리액티브 프로그래밍 개념을 스프링 웹플럭스 컨트롤러와 서비스에 적용해봤다
- 첫번째 스프링 부트 애플리케이션을 실행하고 CURL 을 사용해서 비동기 스트림으로 제공되는 요리가 사용되는 모습을 살펴봤다.
- 첫번째 타임리프 템플릿을 만들고 정적인 웹페이지로 렌더링해서 화면에 표시했다.

2장 `스프링 부트를 활용한 데이터 엑세스` 에서는 스프링 데이터와 리액티브 데이터베이스를 사용해서 리액티브 방식으로 데이터에 접근하는

방법을 알아보고, 획득한 데이터를 리액티브 방식으로 제공하고 사용하는 모습을 살펴본다.
