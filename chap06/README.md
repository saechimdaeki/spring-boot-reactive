# 6장 스프링 부트 API 서버 구축

앞서 5장에서는 운영에 필요한 스프링 부트 액추에이터 사용법을 알아봤다. 이제 외부시스템을 위한 인터페이스인 API서버를 만들 차례다

6장에서 배울 내용은 다음과 같다.

- JSON기반 웹 서비스 구축
- 스프링 레스트 독(REST Docs) 을 활용한 API 문서화
- 스프링 부트로 만든 API포털에서 다양한 API 제공
- 스프링 헤이티오스(HATEOAS)를 사용한 하이퍼 미디어 활용
- API포털에 하이퍼미디어 링크 추가

## HTTP 웹 서비스 구축

가장 단순한 API 서버는 쿼리를 실행하고 결과를 반환한다. 예전에는 XML이나 기술 스택에 따른 바이너리 데이터를 반환해주는 서버가 일반적이였다.

오늘날 이커머스와 매시업 분야에서 가장 중요한 키워드는 JSON이다. HTML대신 JSON을 반환하는 스프링 웹플럭스 엔드포인트는 만들기 아주쉽다.

지금까지 하나 이상의 상품을 담는 장바구니를 만들었다. 그리고 상품과 장바구니 객체를 몽고디비에 저장하고, 템플릿을 사용해서 사용자에게 보여줬다.

한번 상상해보자. 레포지토리에서 데이터를 가져와 모델을 사용하고, 모델을 템플릿에 바인딩해서 화면을 반환하는 대신 데이터를 그대로 직접반환하면?

```java
@RestController
@RequiredArgsConstructor
public class ApiItemController {
    private final ItemRepository itemRepository;   
}
```

`@RestController` 가 붙은 클래스는 스프링이 컴포넌트 스캐닝을 통해 자동으로 감지하고 빈으로 등록한다는 관점에서 기존의 `@Controller` 와같다.

하지만 웹 요청이 들어오면 `@RestController` 가 붙은 클래스의 메소드는 화면 HTML을 렌더링하는데 사용되는 값을 반환하지 않는다. 대신에 데이터

객체 자체를 반환하는데, 반환된 데이터 객체는 직렬화되고 응답 본문에 직접 기록된다. 모든 Item객체를 반환하는 웹메소드를 만들어보자.

```java
 		@GetMapping("/api/items")
    Flux<Item> findAll(){
        return this.itemRepository.findAll();
    }
```

모든 Item 객체를 받아서 아무런 필터링이나 가공없이 그대로 반환하는 간단한 예제다. 사실상 데이터가 저장된 몽고디비로의 연결 통로 역할만 한다.

가장 먼저 눈여겨볼 것은 메소드가 반환하는 겂이 리액터 타입인 `Flux` 라는 점이다. 한 개의 Item 객체를 조회하는 것도 어렵지 않다.

```java
		@GetMapping("/api/items/{id}")
    Mono<Item> findOne(@PathVariable String id){
        return this.itemRepository.findById(id);
    }
```

조회가 아니라 새로운 Item 객체를 저장해야 한다면 다른 접근 방식이 필요하다. 멱등 하지 않고 시스템의 상태 변화를 유발하는 HTTP POST 요청이 

새로운 Item 객체를 만드는 요청에 사용하기에 적합하다.

```java
 		@PostMapping("/api/items")
    Mono<ResponseEntity<?>> addNewItem(@RequestBody Mono<Item> item){
        return item.flatMap(this.itemRepository::save)
                .map(savedItem -> ResponseEntity.created(URI.create("/api/items/" + savedItem.getId()))
                .body(savedItem));
    }
```

기존 Item 객체를 교환하는 기능도 한번 개발해보자.

```java
 @PutMapping("/api/items/{id}")
    public Mono<ResponseEntity<?>> updateItem(@RequestBody Mono<Item> item,
                                              @PathVariable String id){
        return item.map(content -> new Item(id, content.getName(),content.getDescription(),content.getPrice()))
                .flatMap(this.itemRepository::save)
                .map(ResponseEntity::ok);
    }
```

​	PUT 을 사용한 데이터 교체는 새 데이터를 추가하는 POST와 조금 다르지만, 교체나 추가 모두 데이터 저장소의 변경을 유발하므로 작동 방식과

코드도 크게 다르지않다. 이제부터는 지금까지 만든 API의 문서화 작업을 시작해보자

## API 포털 생성

웹 서비스를 출시한 후에는 사용자에게 사용법을 알려줘야한다. 가장 좋은 방법은 API포털을만들고 사용자에게 필요한 정보를 제공하는 것이다.

스프링 레스트 독(Rest Docs)이 API 문서화 작업을 도와준다. 스프렝 레스트 독을 프로젝트에 추가하면 사용자가 직접 사용해볼 수 있는 API예제를

포함해서 API 문서를 쉽게 만들어낼 수 있다. 여러 분야에서 사용성이 입증된 아스키닥터(Asciidoctor) 문서화 도구를 사용해 세부 내용도 쉽게 문서로

만들 수 있다. 아스키닥터를 사용하려면 다음 내용을 build.gradle에 추가하자.  이 책을 공부하는 [`saechimdaeki`](https://github.com/saechimdaeki) 는 다음과 같이 커스텀설정하였다

```groovy
plugins {
    id 'org.springframework.boot' version '2.5.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id 'org.asciidoctor.jvm.convert' version '3.3.0'

}
bootJar{
    layered()
}
group = 'com.saechimdaeki'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

asciidoctor {
    sourceDir 'src/main/asciidoc'
    attributes \
		'snippets': file('build/generated-snippets/')
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}


dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.mongodb:mongodb-driver-sync'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'de.flapdoodle.embed:de.flapdoodle.embed.mongo'
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
    testImplementation 'org.springframework.restdocs:spring-restdocs-webtestclient'

}

test {
    useJUnitPlatform()
}

```

자 이제 테스트 케이스를 작성하고 API 문서를 자동생성해볼 준비가 끝났다. 이제 직접 코드를 작성해보자.

```java
@WebFluxTest(controllers = ApiItemController.class)
@AutoConfigureRestDocs
public class ApiItemControllerDocumentationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    InventoryService service;

    @MockBean
    ItemRepository repository;

    @Test
    void findingAllItems() {
        when(repository.findAll()).thenReturn(
                Flux.just(new Item("item-1", "Alf alarm clock",
                        "nothing I really need", 19.99)));

        this.webTestClient.get().uri("/api/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findAll", preprocessResponse(prettyPrint())));
    }
}
```

`document()` 메소드는 2개의 인자를 받는다. 첫 번째 인자로 "findAll" 이 전달되는데, 이렇게 하면 findAll 디렉토리가 생성되고 그 안에 여러 

.adoc 파일이 생성된다. 두 번째 인자인 preprocessResponse(prettyPrint()) 는 요청 결과로 반환되는 JSON 문자열을 보기 편한 형태로 출력해준다.

새 Item 객체를 생성하는 API에 대한 테스트와 문서화도 추가해보자.

```java
		@Test
    void postNewItem() {
        when(repository.save(any())).thenReturn(
                Mono.just(new Item("1", "Alf alarm clock", "nothing important", 19.99)));

        this.webTestClient.post().uri("/api/items")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(document("post-new-item", preprocessResponse(prettyPrint())));
    }
```

바로 위에서 다뤘던 조회 테스트와는 조금 다른 부분이 있지만 ,모키토(Mockto) 를 통해 반환값을 지정하고, 처리 결과를 단언하고, 자동으로 문서를 

생성해서 특정 위치에 저장한다는 큰틀에서는 비슷하다. 테스트 케이스도 작성됐으니 이제 API문서를 생성하자. 

`./gradlew build` or `gradlew assemble` 실행이 성공적으로 완료되면 다음 내용을 포함하는 문서 조각(snippet) 이 생성된다

- CURL, HTTPPie 형식에 맞는 요청 명령
- HTTP 형식에 맞는 요청 및 응답 메시지
- JSON 형식으로 된 요청 본문
- JSON 형식으로 된 응답 본문

문서 조각들은  [`saechimdaeki`](https://github.com/saechimdaeki) 기준 build/generated-snippets 디렉토리를 기준으로 테스트코드에서 `document()` 메소드의 첫 번째

문자열로 지정해준 서브 디렉토리 아래에 생성된다. 

![image](https://user-images.githubusercontent.com/40031858/128798762-c3c0247f-88d5-4104-a155-7e076d16f4ad.png)

findAll 디렉토리 아래에 findAll 테스트와 관련된 문서 조각들이 자동으로 생성됐다. curl-request.adoc 파일을 열어보면

다음과 같은 curl 명령ㅇ ㅣ들어 있다.

```bash
$ curl 'http://localhost:8080/api/items' -i -X GET
```

http-request.adoc 파일을 열어보면 HTTP 요청 메시지가 들어있다.

```bash
GET /api/items HTTP/1.1
Host: localhost:8080
```

response-body.adoc파일에는 응답 본문이 들어 있다.

```json
[ {
  "id" : "item-1",
  "name" : "Alf alarm clock",
  "description" : "nothing I really need",
  "price" : 19.99
} ]
```

이제 이 문서 조각들을 사용자가 보기 좋은 형태로 API 포털에 모아야한다. index.adoc 파일에 다음과 같이 문서 조각들을 포함하는 내용을 추가한다.

```markdown
= 스프링 부트 실전 활용 마스터

웹 서비스를 출시하면 개발자들에게 사용법을 알려줘야 합니다.

스프링 레스트 독 덕분에 테스트 케이스에서 서비스의 모든 상호 작용을 추출하고 읽기 좋은 문서를 자동으로 만들 수 있으며, +
IDE를 통해 아주 쉽게 작업을 수행할 수 있습니다.


다음 요청을 실행하면:

include::{snippets}/findAll/curl-request.adoc[]

`ApiItemController`는 다음과 같은 응답을 반환합니다.

include::{snippets}/findAll/response-body.adoc[]

HTTPie를 사용하시나요? 다음 명령을 실행해보세요.

include::{snippets}/findAll/httpie-request.adoc[]

동일한 응답 본문이 반환됩니다. curl과 HTTPie 중 좋아하는 것을 사용하시면 됩니다.


```

![image](https://user-images.githubusercontent.com/40031858/128799894-44252122-ef4e-4191-85bc-cac68f3f660e.png)

이제 만들어진 API 문서를 볼 수 있다. 스프렝 레스트 독에 의해 자동 생성된 문서 조각들이 API문서에 보기좋게 포함되어 있다.

스프링 레스트 독은 API문서를 다듬을 수 있는 요청 전처리기인 `preprocessRequest` 와 응답 전처리기인 `preprocessResponse` 를 제공한다

| 전처리기                                                  | 설명                                                         |
| --------------------------------------------------------- | ------------------------------------------------------------ |
| prettyPrint()                                             | 요청이나 응답 메시지에 줄바꿈, 들여쓰기 등 적용              |
| removeHeaders(String... headerNames)                      | 표시하지 않을 헤더 이름 지정.<br />스프링의 HttpHeaders 유틸 클래스에 표준 헤더 이름이 상수로<br />등록돼 있으므로 함께 사용하면 편리하다 |
| removeMatchingHeaders(String...<br /> headerNamePatterns) | 표시하지 않을 헤더를 정규 표현식으로 지정                    |
| maskLinks()                                               | href 링크 항목 내용을 '...' 로 대체.<br />HAL(Hypertext Application Language) 을 적용할때 API 문서에 하드코딩된<br />URI 대신 링크를 통해 API 사용을 독려하기 위해 URI 링크를 '...' 로 대체한다 |
| maskLinks(String mask)                                    | href 항목을 대체할 문자열 명시                               |
| replacePattern(Pattern pattern, <br />String replacement) | 정규 표현식에 매칭되는 문자열을 주어진 문자열로 대체         |
| modifyParameters()                                        | 평문형 API(fluent API) 를 사용해서 요청 파라미터 추가, 변경, 제거 |
| modifyUris()                                              | 평문형 API를 사용해서 로컬 환경에서 테스트할 때 API 문서에 표시되는 URI 지정 |

이 표에 제시된 기능이 부족하다면 직접 `OperationPreprocessor` 인터페이스를 구현해서 전처리기를 만들어서 사용할 수도 있는데, 

`OperationPreprocessAdapter` 추상 클래슬르 상속해서 필요한 부분만 오버라이드 하는 것이 편리하다.

## API 진화 반영

지금까지 알아본 JSON 반환 API 작성은 아주 어렵지는 않았다. 그보다는 리액터 플로우에 적응하는 것이 훨씬 어려웠다. 그리고 API 사용법을 알려주는 

API 포털을 만드는 일도 스프링 레스트 독을 사용하면 쉽게 완수할 수 있었다. 하지만 앞으로 마주하게 될 가장 큰 난관은 API 구현이나 문서화가 

아니다. 바로 API가 진화한다는 것이다. 개발자에게 변경이란 피해갈 수 없는 숙명이다. 이미 배포된 API를 변경하면 API 사용자는 예전처럼 API를 

사용할 수 있을까? 장자끄 뒤브레는 버저닝 비용 이해 라는 연구를 진행했었다. 이 논문에서 뒤브레는 세가지 API 변경 유형을 설명한다

- 매듭(knot): 모든 API 사용자가 단 하나의 버전에 묶여있다. API가 변경되면 모든 사용자도 함께 변경을 반영해야 하므로 엄청난 여파를몰고온다
- 점대점(point-to-point): 사용자마다 별도의 API 서버를 통해 API를 제공한다. 사용자별로 적절한 시점에 API를 변경할 수 있다
- 호환성 버저닝(compatible versioning): 모든 사용자가 호환 가능한 하나의 API 서비스 버전을 사용한다

점대점 방식은 매듭방식과 비교하면 상당히 많은 비용을 줄일 수 있지만 여전히 적지않은 비용이 소요되고 증가 속도도 가파르다. 이방식에서는

API 사용자에게 미치는 영향은 줄어들지만, 여러 버전을 유지 관리해야 하므로 서버 개발팀의 비용은 많이 늘어난다

호환성 방식은 비용 규모도 적고 증가속도도 완만하다. 동일한 API에 대해 기존 사용자도 그대로 사용할 수 있고 새로 추가된 기능도 사용할 수 

있게 해주므로, API 사용자는 자기 상황에 맞춰 가장 적합할 때 업그레이드 하면된다. 서버 개발팀도 여러 버전을 관리할 필요가 없으므로

부담이 적다. 그렇다면 하위 호환성을 유지하는 호환성 방식 서비스는 어떻게 만들까? 바로 하이퍼미디어를 적용해서 만들 수 있다

## 하이퍼미디어 기반 웹서비스 구축

`하이퍼미디어` 는 어떤 형태로든 우리에게 친숙한 개념이다. `하이퍼미디어`가 웹을 지금처럼 강력하게 만들어준 것과 마찬가지로 `하이퍼미디어`를

API에 추가하면 더 유연하게 API를 진화시킬 수 있다. `하이퍼미디어` 를 직접 작성하려면 비용이 많이든다. 그래서 이런 비용을 줄이기 위해

스프링 헤이티오스(Spring HATEOAS)가 만들어졌다. 스프링 헤이티오스는 스프링 웹플럭스도 지원하며 서비스를 아주 쉽고 신속하게 하이퍼

미디어 형식으로 표현할 수 있도록 도와준다. 스프링 헤이티오스를 자세히 알아보기전 먼저 한개의 Item을 반환하는 웹 메소드를 다시 되돌아보자

```java
@GetMapping("/api/items/{id}")
Mono<Item> findOne(@PathVariable String id){
  return this.repository.findById(id);
}
```

조회할 Item의 id를 지정하고 위 메소드를 호출하면 스프링 데이터 레포지토리와 스프링 웹플럭스를 통해 데이터를 조회하고 다음 JSON 데이터를 반환.

```json
{
  "id": "item-1",
  "name": "Alf alarm clock",
  "description": "nothing I really need",
  "price": 19.99
}
```

그런데 Item 관련 정보 중에는 JSON에 포함되지 않았지만 가치 있는 정보가 더 있다. 조회한 Item 정보 전체를 교체(PUT) 하거나, 일부를 변경(PATCH)

하거나, 삭제(DELETE)할 수 있는 리으를 함께 제공한다면 사용자가 쉽게 해당 작업을 수행할 수 있다. 이런기능을 제공하기 위해 먼저 스프링헤이티오스

를 애플리케이션에 추가하자.

```groovy
implementation('org.springframework.boot:spring-boot-starter-hateoas'){
        exclude group:"org.springframework.boot", module:"spring-boot-starter-web"
    }
```

spring-boot-starter-hateoasf를 추가하면 스프링 헤이티오스를 사용할 수 있다. 스프링 헤이티오스는 원래 스프링 MVC를 지원하는 용도로 만들어져서,

스프링 MVC와 아파치 톰캣을 사용할 수 있게 해주는 spring-boot-starter-web이 포함돼있다.  여기서는 스프링 MVC와 아파치 톰캣 대신에

스프링 웹플럭스와 리액터 네티를 사용하는 웹서비스를 만들고 있으므로, spring-boot-starter-webflux를 사용하도록 spring-boot-starter-web을

제되해야한다. 이제 스프링 헤이티오스를 웹 컨트롤러에 적용하는 방법을 알아보자.

```java
@RestController
@RequiredArgsConstructor
public class HypermediaItemController {
    
    private final ItemRepository repository;

    @GetMapping("/hypermedia/items")
    Mono<CollectionModel<EntityModel<Item>>> findAll() {

        return this.repository.findAll() 
                .flatMap(item -> findOne(item.getId())) 
                .collectList() 
                .flatMap(entityModels -> linkTo(methodOn(HypermediaItemController.class) 
                        .findAll()).withSelfRel() 
                        .toMono() 
                        .map(selfLink -> CollectionModel.of(entityModels, selfLink)));
    }
    
    @GetMapping("/hypermedia/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id){
        HypermediaItemController controller= methodOn(HypermediaItemController.class);
        
        Mono<Link> selfLink=linkTo(controller.findOne(id)).withSelfRel().toMono();
        
        Mono<Link> aggregateLink=linkTo(controller.findAll())
                .withRel(IanaLinkRelations.ITEM).toMono();
        
        return Mono.zip(repository.findById(id),selfLink,aggregateLink)
                .map(o -> EntityModel.of(o.getT1(), Links.of(o.getT2(), o.getT3())));
    }
}
```

```markdown
`` findOne 메소드 설명 ``
1. 스프링 헤이티오스의 정적 메소드인 WebFluxLinkBuilder.methodOn() 연산자를. 사용해 컨트롤러에 대한 프록시를 생성
2. webFluxLinkBuilder.linkTo() 연산자를 사용해서 컨트롤러의 findOne() 메소드에 대한 링크를 생성한다. 현재 메소드가
	findOne() 메소드이므로 self라는 이름의 링크를 추가하고 리액터 Mono에 담아 반환한다
3. 모든 상품을 반환하는 findAll()메소드를 찾아서 aggregate root에 대한 링크를 생성한다. IANA표준에 따라 링크 이름을 item으로 명명
4. 여러 개의 비동기 요청을 실행하고 각 결과를 하나로 합치기 위해 Mono.zip() 메소드를 사용한다. 예제에서는 findById() 메소드 호출과
	selfLink, aggregateLink 생성 요청 결과를 타입 안정성이 보장되는 리액터 Tuple 타입에 넣고 Mono로 감싸서 반환한다.
5. 마지막으로 map()을통해 Tuple에 담겨 있던 여러 비동기 요청 결과를 꺼내서 EntityModel을 만들고 Mono로 감싸서 반환한다	
```

하이퍼미디어 일읔를 만들 때는 가장 먼저 도메인 객체와 링크를 조합해야 한다. 이 작업을 쉽게 수행할 수 있도록 스프링 헤이티오스는 다음과 같이 

벤더 중립적인 모델을 제공한다

- `RepresentationModel` : 링크 정보를 포함하는 도메인 객체를 정의하는 기본 타입
- `EntityModel`: 도메인 객체를 감싸고 링크를 추가할 수 있는 모델. `RepresentationModel`을 상속받는다
- `CollectionModel`: 도메인 객체 컬렉션을 감싸고 링크를 추가할 수 있는 모델. `RepresentationModel`을 상속받는다
- `PagedModel`: 페이징 관련 메타데이터를 포함하는 모델. `CollectionModel`을 상속받는다

스프링 헤이티오스는 이 네가지 모델과 Link, Links 객체를 기반으로 하이퍼미디어 기능을 제공한다. 웹 메소드가 이 네가지 모델 중 하나를 그대로

반환하거나 리액터 타입에 담아서 반환하면 스프링 헤이티오스의 직렬화 기능이 동작하고 하이퍼미디어를 만들어낸다.

REST에서는 상호작용하는 대상을 `리소스(resource)` 라고 부른다. 웹플럭스 컨트롤러에 작성한 웹 메소드가 반환하는 것이 바로 리소스이다.

스프링 헤이티오스는 리소스와 관련한 리으를 추가해서 하이퍼미디어로 만들어준다. 이제 하이퍼미디어를 만들어내는 테스트를 만들어보자.

```java
@WebFluxTest(controllers = HypermediaItemController.class)
@AutoConfigureRestDocs
public class HypermediaItemControllerDocumentationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    InventoryService service;

    @MockBean
    ItemRepository repository;
  
   @Test
    void findOneItem() {
        when(repository.findById("item-1")).thenReturn(Mono.just(
                new Item("item-1", "Alf alarm clock", "nothing I really need", 19.99)));

        this.webTestClient.get().uri("/hypermedia/items/item-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findOne-hypermedia", preprocessResponse(prettyPrint()),
                        links(
                                linkWithRel("self").description("이 `Item`에 대한 공식 링크"),
                                linkWithRel("item").description("`Item` 목록 링크"))));
    }
}
```

문서화에 사용할 테스트 케이스 준비는 앞에서 했던 것과 동일하다. 하이퍼미디어를 만들어내는 HypermediaItemController 클래스를 대상으로 한다는 

것이 유일한 차이점이다. 하이퍼미디어든 아니든, API가 반환하는 HTTP응답을 각로채서 테스트하는 것이 핵심이다. 먼저 ITEM 객체 하나를

조회하는 것으로 시작한다. 이 ITEM 객체 그대로 반환하는 것과 하이퍼 미디어로 반환하는 것의 차이를 볼 수 있다. 스프링 레스트 독은 링크가 존재하는지

검증한 후 링크를 담고있는 links.adoc 파일을 생성한다.  테스트 케이스 결과로 생성되는 HTTP 응답은 다음과 같으며 

build/generated-snippets/findOne-hypermedia/response-body.adoc에 저장된다

```json
{
  "id" : "item-1",
  "name" : "Alf alarm clock",
  "description" : "nothing I really need",
  "price" : 19.99,
  "links" : [ {
    "rel" : "self",
    "href" : "/hypermedia/items/item-1"
  }, {
    "rel" : "item",
    "href" : "/hypermedia/items"
  } ]
}
```

Item 객체 자체에 대한 정보뿐 아니라 하이퍼미디어 링크도 JSON에 추가돼 있다. 그리고 다음과 같이 링크 정보를 보여주는 links.adoc파일도 따로 생성

된다.

```markdown
|===
|Relation|Description

|`+self+`
|이 `Item`에 대한 공식 링크

|`+item+`
|`Item` 목록 링크

|===
```

이제 links.adoc파일도 API 포털 문서에 포함시켜서 함께 렌더링되게 만들 수 있다.

```markdown
= 스프링 부트 실전 활용 마스터

웹 서비스를 출시하면 개발자들에게 사용법을 알려줘야 합니다.

스프링 레스트 독 덕분에 테스트 케이스에서 서비스의 모든 상호 작용을 추출하고 읽기 좋은 문서를 자동으로 만들 수 있으며, +
IDE를 통해 아주 쉽게 작업을 수행할 수 있습니다.


다음 요청을 실행하면:

include::{snippets}/findAll/curl-request.adoc[]

`ApiItemController`는 다음과 같은 응답을 반환합니다.

include::{snippets}/findAll/response-body.adoc[]

HTTPie를 사용하시나요? 다음 명령을 실행해보세요.

include::{snippets}/findAll/httpie-request.adoc[]

동일한 응답 본문이 반환됩니다. curl과 HTTPie 중 좋아하는 것을 사용하시면 됩니다.

'''
== 상품

=== 한 건 조회

==== 요청

===== http

include::{snippets}/findOne-hypermedia/http-request.adoc[]

===== curl

include::{snippets}/findOne-hypermedia/curl-request.adoc[]

===== HTTPie

include::{snippets}/findOne-hypermedia/httpie-request.adoc[]

==== 응답

include::{snippets}/findOne-hypermedia/http-response.adoc[]

===== 응답 본문

include::{snippets}/findOne-hypermedia/response-body.adoc[]

===== 링크

include::{snippets}/findOne-hypermedia/links.adoc[]

```



![image](https://user-images.githubusercontent.com/40031858/128960055-21f5b530-f46a-4141-9258-4c3d6c7cad86.png)

## 하이퍼미디어의 가치

지금까지 하이퍼미디어를 만들고, 테스트하고 API 문서로 만드는 방법을알아봤다. 하지만 '이런작업을 왜하는거지?' 라는 근본적인 질문이

떠오를 수 있다. 단순히 데이터만을 제공하기 위해 하이퍼미디어를 사용하는 것이 아니다. 데이터 사용 방법에 대한 정보도 함께 제공하기 위해

하이퍼미디어를 사용한다. 그래서 하이퍼미디어 문서에 데이터에 대한 설명을 여러가지 JSON 형식으로 제공하는 `프로파일링크` 가 종종

포함되기도 한다. 프로파일 링크에 포함된 링크는 자바스크립트 라이브러리가 자동으로 생성/수정 입력폼을 만드는데 사용될 수 있다.

API가 JSON 스키마 형식으로 반환하면 클라이언트 쪽에서 JSON에디터가 읽어서 다음과 같이 HTML폼을 자동으로 만들어낸다. 

스프링 헤이티오스는 ALPS(Application-Level Profile Semantics) 도 지원한다. 다음과 같이 ALPS를 사용하는 웹 메소드를 작성하면

자신만의 프로파일을 만들어서 사용할 수도 있다.

```java
@GetMapping(value = "/hypermedia/items/profile", produces = MediaTypes.ALPS_JSON_VALUE)
    public Alps profile(){
        return alps()
                .descriptor(Collections.singletonList(descriptor()
                .id(Item.class.getSimpleName()+"-repr")
                .descriptor(Arrays.stream(
                        Item.class.getDeclaredFields())
                .map(field -> descriptor()
                .name(field.getName())
                .type(Type.SEMANTIC)
                .build())
                .collect(Collectors.toList()))
                .build()))
                .build();
    }
```



```json
{
"version": "1.0",
"descriptor": [
{
"id": "Item-repr",
"descriptor": [
{
"name": "id",
"type": "SEMANTIC"
},
{
"name": "name",
"type": "SEMANTIC"
},
{
"name": "description",
"type": "SEMANTIC"
},
{
"name": "price",
"type": "SEMANTIC"
}
]
}
]
}
```

하이퍼미디어를 사용하는 목적이 오직 HTML 폼 자동 생성만은 아니라는 점을 이해하는 것이 중요하다. 동변관계 라는 더 깊고 근본적인 개념이 작동한다.

"소프트웨어 엔지니어링에서 2개의 컴포넌트 중 하나에서 변경이 발생할 때 나머지 하나도 수정을 해야 두컴포넌트를 포함하는 시스템의 전체적인 정합성

이 유지된다면, 이 두컴포넌트는 동변관계에 있다" 고 한다. 하나의 팀에서 프론트엔드와 백엔드를 포함하는 전체 애플리케이션을 담당한다면, 사실상 이미

강결합돼있을 가능성이 높다. 이경우 하이퍼미디어를 사용할 때의 장점을 느끼지 못할것이다. 하지만 내/외부의 여러 팀에서 사용하는 API를 만들어 공개

했다면 얘기가 다르다. 주문 상태 정보를 포함하는 주문 서비스 API를 만든다고 상상해보자. 주문 상태가 '준비 중' 일때는 주문을 취소할 수 있다. 

하지만 주문 상태가 '발송 완료' 로 바뀌면 취소할 수 없다. 클라이언트가 '주문 취소' 버튼 표시 여부를 주문 상태에 따라 결정하도록 로직을 작성했다면

백엔드와 강하게 결합돼 있는 것이다. 클라이언트가 직접적으로 도메인 지식에 의존하는 대신 프로토콜에만 의존하게 만들면, 예를 들어 클라이언트가

주문에 대한 지식을 직접 사용하지 말고 단순히 링크를 읽고 따라가게만 만든다면, 클라이언트는 백엔드의 변경에서 유발되는 잠재적인 문제를 

피해갈 수 있다. 바로 이 점이 REST가 지향하는 바다. 사람들이 뭐라 말하든 REST는 /orders/23처럼 URI를 깔끔하게 작성하는 방법이 아니고,

스프링 웹플럭스+Jackson 처럼 데이터를 JSON 으로 나타내는 방법도 아니며, JSON 데이터를 POST 요청에 실어 보내서 데이터베이스에

새로운 레코드 하나를 추가하는 방법도 아니다. `REST는 상거래, 정부 등 우리 삶을 연결하는 수많은 분야의 중심에 웹이 자리 잡을 수 있게`

`해 줬던 것과 똑같은 전순을 사용한다는 것을 말한다` 쉽게 말해 웹사이트의 변경이 웹브라우저의 업데이트를 유발하지 않는다는 순수한 사실은,

서버 쪽에서 변경이 발생해도 클라이언트에 영향을 미치지 않게만든다는 증거가 된다. 로이필딩 박사가 논문에서 제안한 개념이 적용된 API는

하위 호환성을 갖게된다. 이런 API를 사용하면 시간이 지남에 따라 유지 관리에 드는 총비용을 절감할 수 있다.

## API에 행동 유도성 추가

이제 하이퍼미디어를 만들었고 적절한 링크도 추가했다. 하지만 뭔가 빠진 것 같은 느낌이 든다. 지금까지 봐온 JSON 형식은 기존의 보통 JSON이

거나 HAL형식으로 기술된 하이퍼미디어였다. 이런 방식들은 단순하다는 장점 덕분에 하이퍼미디어용으로 가장 널리 사용되는 형식이다. 

그래서 스프링 헤이티오스도 기본적으로 이 형식으로 데이터를 생성한다. 하지만 여기엔 문제가 하나 있다. 동일한 URI를 가리키는 GET과 PUT

을 함께 담으면 HAL문서는 한개의 링크만 생성한다. 그 결과 사용자는 원래는 GET, PUT 두 가지의 서로 다른 선택지가 존재했었다는 사실을 알수없게

된다. GET과 PUT을 다른 링크로 표현하도록 강제하더라도 클라이언트가 PUT요청을 보내려면 어떤 속성 정보를 제공해야 하는지 클라이언트에

알려주지 않는다. 이를 클라이언트가 알아내게 하는것은 결코 좋은 방법이 아니다. 바로 이지점에서 스프링 헤이티오스가 하이퍼미디어에 

`행동 유도성(affordance)` 를 추가한 API를 제공해준다. 하나의 Item을 보여줄 때, 그 Item을 수정할 수 있는 행동 유도성을 추가해주는 것이 

전형적인 사례라고 할 수 있다. 스프링 헤이티오스는 관련 있는 메소드를 연결할 수 있는 수단을 제공한다. Item 사례에서는 GET연산에 대한

링크가 PUT연산으로 이어질 수 있다. HAL로 표현되면 여전히 하나의 링크만 표시된다. 하지만 HAL-FORMS 같은 하이퍼미디어 형식은

추가정보를 렌더링할 수 있는 연결 정보도 보여줄 수 있다. 행동 유도성을 추가할 수 있는 어떤 미디어 타입이라도 이런 메타데이터를 

제공할 수 있다는 장점이 있다. GET과 PUT을 연결하기 전에 먼저 PUT을 처리해보자.

```java
@PutMapping("/affordances/items/{id}") 
    public Mono<ResponseEntity<?>> updateItem(@RequestBody Mono<EntityModel<Item>> item, 
                                              @PathVariable String id) {
        return item 
                .map(EntityModel::getContent) 
                .map(content -> new Item(id, content.getName(), 
                        content.getDescription(), content.getPrice())) 
                .flatMap(this.repository::save) 
                .then(findOne(id)) 
                .map(model -> ResponseEntity.noContent() 
                        .location(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).build());
    }
```

단순하게 보면 Item데이터를 받아서 해당 id를 식별자로 해서 저장하고, 저장된 정보를 findOne(id)로 다시 조회해서 필요한 URI를 추가해서 사용자에게

반환한다. findOne()메소드는 행동 유도성이 적용된 이클래스에는 아직 정의되지 않았다. HypermediaItemController에서 작성한 코드를 응용해

findOne()메소드를 작성해보자

```java
@GetMapping("/affordances/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id) {
        AffordancesItemController controller = methodOn(AffordancesItemController.class);

        Mono<Link> selfLink = linkTo(controller.findOne(id))
                .withSelfRel()
                .andAffordance(controller.updateItem(null, id))
                .toMono();

        Mono<Link> aggregateLink = linkTo(controller.findAll())
                .withRel(IanaLinkRelations.ITEM)
                .toMono();

        return Mono.zip(repository.findById(id), selfLink, aggregateLink)
                .map(o -> EntityModel.of(o.getT1(), Links.of(o.getT2(), o.getT3())));
    }
```

2개 이상의 메소드를 연결하는 것도 가능하다. 예를 들어, /affordances/items/{id} 링크를 통해 Item을 삭제할 수 있다면 이 링크도 포함할 수 있다.

지금까지 수행한 수작업 내용을 검증할 수 있는 방법은 여러가지다. 서비스를 실행해서 검증할 수도있고 단위테스트를 작성할수도 있다. 하지만 아스키독

코드 조각으로 만들어 문서에 포함시키는 것이 좋다. 행동유도성이 추가된 웹플럭스 컨트롤러를 만들었으므로 이를 테스트할 수 있는 테스트를만들자.

```java
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@WebFluxTest(controllers = AffordancesItemController.class)
@AutoConfigureRestDocs
class AffordancesItemControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockBean InventoryService service;

    @MockBean ItemRepository repository;

    @Test
    void findSingleItemAffordances() {
        when(repository.findById("item-1")).thenReturn(Mono.just( 
                new Item("item-1", "Alf alarm clock", "nothing I really need", 19.99)));

        this.webTestClient.get().uri("/affordances/items/item-1") 
                .accept(MediaTypes.HAL_FORMS_JSON) 
                .exchange() 
                .expectStatus().isOk() 
                .expectBody() 
                .consumeWith(document("single-item-affordances", 
                        preprocessResponse(prettyPrint()))); 
    }
}
```

이를 수행하면 다음과 같은 응답을 확인할 수 있다.

```json
{
  "id" : "item-1",
  "name" : "Alf alarm clock",
  "description" : "nothing I really need",
  "price" : 19.99,
  "links" : [ {
    "rel" : "self",
    "href" : "/affordances/items/item-1"
  }, {
    "rel" : "item",
    "href" : "/affordances/items"
  } ]
}
```

이제 행동유도성을 포함한 애그리것 루트에 대한 링크만 추가하면 API를 마무리 지을 수 있다.

```java
 @GetMapping("/affordances/items")
    Mono<CollectionModel<EntityModel<Item>>> findAll() {
        AffordancesItemController controller = methodOn(AffordancesItemController.class);

        Mono<Link> aggregateRoot = linkTo(controller.findAll())
                .withSelfRel()
                .andAffordance(controller.addNewItem(null))
                .toMono();

        return this.repository.findAll()
                .flatMap(item -> findOne(item.getId()))
                .collectList()
                .flatMap(models -> aggregateRoot
                        .map(selfLink -> CollectionModel.of(
                                models, selfLink)));
    }
```

또, 새로운 Item을 추가하는 것도 어렵지 않다.

```java
@PostMapping("/affordances/items")
    Mono<ResponseEntity<?>> addNewItem(@RequestBody Mono<EntityModel<Item>> item) {
        return item
                .map(EntityModel::getContent)
                .flatMap(this.repository::save)
                .map(Item::getId)
                .flatMap(this::findOne)
                .map(newModel -> ResponseEntity.created(newModel
                        .getRequiredLink(IanaLinkRelations.SELF)
                        .toUri()).body(newModel.getContent()));
    }
```

## 정리

6장에서 배운 내용은 다음과 같다.

- 원격 접근을 통해 시스템을 변경하는 API 생성
- 스프링 레스트 독을 사용해서 API 문서화 포털을 만드는 테스트 작성
- HAL 기반 링크 정보를 포함하는 하이퍼미디어 제공 컨트롤러 작성
- 링크 정보 및 관련 세부정보를 추가해서 문서화 테스트 보완
- 행동 유도성 소개 및 HAL-FORMS 형식 데이터와 데이터 템플릿 제공
- 아스키독 문서 조각을 합쳐서 API문서화 포털 구축

다음 7장 '스프링 부트 메시징' 에서는 비동기 메시징 활용 방법을 알아본다
