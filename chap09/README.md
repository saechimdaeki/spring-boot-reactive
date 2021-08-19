# 9장 스프링 부트 애플리케이션 보안

앞서 8장에서는 완전한 리액티브 프로토콜인 R소켓을을 사용해서 애플리케이션을 연결하는 방법을 알아봤다. 이번 9장에서는 애플리케이션을 실제

상용환경에 배포하기 전에 반드시 갖춰야 할 항목, 바로 `보안` 에 대해 설명하고자 한다. 

애플리케이션은 이용자 접근을 제어할 수 있는 인증과 엄격한 권한 제어가 적용되기 전까지는 그저 장난감에 지나지 않는다. 이번 장에서 보안을 다루는

이유이기도 하다. 9장에서 다룰 내용은 다음과 같다.

- 다양한 사용자 정보 저장소를 사용하는 스프링 시큐리티 설정
- HTTP엔드포인트에 라우트 기반 보안 설정 적용
- 리액티브 엔드포인트에 메소드 수준 보안 적용
- 권한 검사를 위한 스프링 시큐리티 컨텍스트 연동

## 스프링 시큐리티 시작하기

스프링 시큐리티를 사용해보는 가장 간단한 방법은 스프링 부트 애플리케이션에 적용해보는 것이다. 스프링 시큐리티 의존관계를 추가하고 스프링부트가

어떤 부분을 자동 구성해주는지 살펴보자

```groovy
    implementation 'org.springframework.boot:spring-boot-starter-security'
    testImplementation 'org.springframework.security:spring-security-test'
```

스프링 부트 스타터 시큐리티와 함께 스프링 시큐리티 테스트도 추가했다. 스프링 시큐리티 테스트를 사용하면 9장에서 주로 다룰 보안 기능을 주요 관심사로

하는 테스트 케이스를 아주 쉽게 작성할 수 있다. 이제 스프링 애플리케이션을 실행해보자.

![image](https://user-images.githubusercontent.com/40031858/129648569-e6420e32-731f-4f3d-a899-9946c31c1c5a.png)

`Using generated security password` 라는, 지금까지 볼 수 없던 로그하나가 눈에 들어온다. 이제 http://localhost:8080에 접속하면 어떻게될까

![image](https://user-images.githubusercontent.com/40031858/129648631-5d6a6521-36b9-435e-a1b5-13c67aa6cac9.png)

브라우저 주소창에 입력했던 URL이 http://localhost:8080/login 으로 저절로 바뀌었고, 사용자 이름과 비밀번호를 입력하는 페이지가 표시된다.

스프링 부트가 자동설정으로 스프링 시큐리티를 애플리케이션에 적용해서 웹 사이트에 아무나 접근할 수 없게 만들었다. 그리고 실제 운영환경에 배포된 

애플리케이션이 'password' 같은 유추하기 쉬운 비밀번호로 뚫리는 일이 없도록 애플리케이션이 실행될 때마다 무작위로 만들어진 문자열이 비밀번호로

설정되고 콘솔에 표시된다. 이제 정확하게 어떤 일이 벌어지고 있는지 알아보자. 스프링 시큐리티는 다음과 같은 다중 계층 방식으로 광범위한 보안을 적용한다

- 여러가지 필터가 생성되고 적절한 순서로 등록된다
- 웹 페이지에 다양한 지시어가 추가된다
  - 바람직하지 않은 정보가 브라우저 캐시에 유입되는 것 방지
  - 클락재킹, 세션 고정 공격, 크로스 사이트 스크립트 공격 등 보안 위험 방어
  - 서버 응답에 적절한  보안 헤더 추가
  - 크로스 사이트 요청 위조 방지 활성화

요컨대 스프링 시큐리티는 보안 위협에 대한 최신 방어책을 포함하고 있으며, 애플리케이션 사용자를 쉽게 보호할 수 있도록 도와준다. 결국 스프링 부트 

애플리케이션을 사용하면 스프링 시큐리티도 쉽게 적용할 수 있고, 결과적으로 애플리케이션 보안 적용도 쉽게 처리할 수 있다. 

## 실무 적용

스프링 시큐리티는 사용자 정보를 하드코딩해서 저장할 수 있는 여러 가지 방법을 제공하지만, 데이터 저장소를 연결해서 사용하는 편이 더 쉽다.

아이디, 비밀번호, 역할을 저장한다는 기본 개념을 구체적으로 구현하는 것도 전혀 어렵지 않다.

이제 사용자 정보 관리 기능을 만들어 보자. 먼저 User타입을 정의해야 한다. 

```java
public class User {
    private @Id String id;
    private String name;
    private String password;
    private List<String> roles;
    private User(){}

    public User(String id, String name, String password, List<String> roles) { 
        this.id = id;
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    public User(String name, String password, List<String> roles) {
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(name, user.name) &&
                Objects.equals(password, user.password) &&
                Objects.equals(roles, user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, password, roles);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", password='" + "*******" + '\'' +
                ", roles=" + roles +
                '}';
    }
}
```

​	또한 User 데이터에 접근하려면 스프링 데이터 레포지토리를 정의해야한다

```java
public interface UserRepository extends CrudRepository<User,String> {   
    Mono<User> findByName(String name);
}
```

레포지토리는 이름으로 사용자를 조회하는 findByName() 메소드만 가지고 있다. 곧 알게되겠지만 이 점이 아주 중요하다. 스프링 시큐리티는 username

기준으로 하나의 사용자를 찾을 수 있어야 한다. 스프링 시큐리티를 설정하기 위해 SecurityConfig 클래스를 만든다

```java
@Configuration
public class SecurityConfig {

    @Bean
    public ReactiveUserDetailsService userDetailsService(UserRepository repository) { 
        return username -> repository.findByName(username) 
                .map(user -> User.withDefaultPasswordEncoder() 
                        .username(user.getName()) 
                        .password(user.getPassword()) 
                        .authorities(user.getRoles().toArray(new String[0])) 
                        .build()); 
    }
}
```

UserRepository를 사용해서 User객체를 조회하고 스프링 시큐리티의 User객체로 변환할 수 있는 빈을 설정클래스 안에 추가해야한다.

사용자 조회를 위해 개발자가 만든 빈을 스프링 시큐리티가 리액티브 애플리케이션 안에서 찾아서 사용하게 하려면 

ReactiveUserDetailsService 인터페이스를 구현해야한다.  ReactiveUserDetailsService는 username을 인자로 받아서 `Mono<UserDetails>` 를

반환하는 단 하나의 메소드만 가지고 있다. 그래서 자바 8의 람다를 사용해서 간단하게 정의할 수 있으며, UserRepository의 findByName() 메소드를

사용하는 것으로시작한다. 이 얼마 되지 않는 코드 덕분에 개발자가 작성한 몽고디비 레포지토리와 스프링 시큐리티를 연결해서 사용자 세부정보를 

스프링 시큐리티를 통해 관리할 수 있게 됐다. 이제 스프링 시큐리티가 적용된 애플리케이션을 만들기 위해 마지막으로해야할 일은 테스트용 사용자 정보를

미리 로딩해두는 것이다. 이를 위해 SecurityConfig에 빈을하나 더 추가하자.

```java
@Bean
    CommandLineRunner userLoader(MongoOperations operations){
        return args -> {
            operations.save(new saechimdaeki.springsecurity.User(
                    "saechim","1234", Arrays.asList("ROLE_USER")
            ));
        };
    }
```

스프링 시큐리티 레퍼런스 문서를 보면 테스트용 사용자 정보를 추가할 수 있는 다른 API도 볼 수 있을 것이다. 이제 애플리케이션을 실행하고 

http://localhost:8080 에 접속하면 로그인 페이지로 바로 이동될 것이다. 이제 설정한 사용자 정보로 로그인을 해보자.

정확히 어떤 기능이 활성화된 것일까? 스프링 부트는 스프링 시큐리티가 제공하는 `@EnableWebFluxSecurity` 애노테이션을 적용할지 말지 결정한다.

`@EnableWebFluxSecurity` 가 적용되면 스프링 시큐리티는 기본적으로 다음 기능을 활성화한다.

- HTTP BASIC을 활성화해서 CURL 같은 도구로도 계정명/비밀번호 값을 전송할 수 있다.

- HTTP FORM을 활성화해서 로그인되지 않은 사용자는 브라우저의 기본 로그인 팝업 창 대신에 스프링 시큐리티가 제공하는 로그인 페이지로 리다이렉트한다

- 사용자가 로그인에 성공해서 인증이 완료되면 애플리케이션의 모든 자원에 접근 가능하다. 이는 인증만 받으면 애플리케이션 자원에 접근하기 위해

  추가적인 허가가 필요하지 않음을 의미한다

이정도로 충분한 보안 조치가 적용되었을까? 전혀 그렇지 않다. 인증된 모든 사용자에게 모든 자원에 대한 접근을 허용하는 것은 바람직하지 않다. 인증된

사용자가 접근할 수 있는 자원에 제약을 두는 것이 안전하며, 사용자가 볼 수 있도록 허가받은 화면만 보여줘야 한다. 이는 사용자가 볼 수 있는 링크를 

사용자마다 다르게 해서, 볼 수 없는 페이지에 대한 링크는 제공되지도 말아야 함을 의미한다. 이제부터 접근 가능한 페이지를 지정해서 해당 페이지만

보이도록 설정을 추가해보자.

## 스프링 시큐리티 커스텀 정책

스프링 시큐리티는 개발자가 만든 커스텀 필터를 끼워 넣을 수 있도록 다양한 주입점을 제공한다

```markdown
### 스프링 웹플럭스에는 서블릿이 사용되지 않는다. 그래서 javax.servlet.Filter 훅(hook)을 사용할 수 없다.

### 하지만 필터링은 웹 애플리케이션에서는 매우 쓸모가 많은 패러다임이다. 그래서 스프링 웹플럭스는 서블릿과는 다른 버전의 필터

### API인 WebFilter를 제공하며, 스프링 시큐리티에서도 WebFilter를 만들어 제공함으로써 웹플럭스를 지원한다.
```

스프링 시큐리티는 애플리케이션을 적절하게 보호할 수 있는 중요한 필터를 되도록 모두 등록하려고 노력하며, 개발자가 만든 필터도 등록할 수 있게 해준다.

스프링 시큐리티에서 제공하는 필터를 개발자가 만든 커스텀 필터로 대체할 수도 있지만 이경우 상당한 주의가 필요하다. 이는 애플리케이션 커스터마이징의 

일반적인 경로를 한참 벗어나는 방식이며 이 책에서도 다루지는 않는다. 그렇다면 어떻게 커스터마이징할 수 있을까? 이제부터 알아보자.

```java
                                                                                                
  @Bean                                                                                         
  SecurityWebFilterChain myCustomSecurityPolicy(ServerHttpSecurity http) {                      
      return http                                                                               
              .authorizeExchange(exchanges -> exchanges                                         
                      .pathMatchers(HttpMethod.POST, "/").hasRole(INVENTORY)                     
                      .pathMatchers(HttpMethod.DELETE, "/**").hasRole(INVENTORY)                
                      .anyExchange().authenticated()                                            
                      .and()                                                                     
                      .httpBasic()                                                               
                      .and()                                                                     
                      .formLogin())                                                             
              .csrf().disable()                                                                 
              .build();                                                                         
  }
```

ROLE_INVENTORY 역할을 가진 테스트용 사용자를 추가하자.

```java
  static String role(String auth) {                                                            
      return "ROLE_" + auth;                                                                   
  }                                                                                            
                                                                                               
  @Bean                                                                                        
  CommandLineRunner userLoader(MongoOperations operations) {                                   
      return args -> {                                                                         
          operations.save(new saechimdaeki.springsecurity.User(                                
                  "saechim", "1234", Arrays.asList(role(USER))));                              
                                                                                               
          operations.save(new saechimdaeki.springsecurity.User(                                
                  "manager", "1234", Arrays.asList(role(USER), role(INVENTORY))));             
      };                                                                                       
  }                                                                                            
                                                                                               
```

이런 규칙을 정의한 후에 가장 먼저 해야할 일은 테스트를 통해 확인해보는 것이다. 먼저 적절한 역할이 없는 사용자가 Item 추가를 시도하는테스트를

작성해보자

```java
 @Test
    @WithMockUser(username = "alice", roles = {"SOME_OTHER_ROLE"})
    void addingInventoryWithoutProperRoleFails(){
        this.webTestClient.post().uri("/")
                .exchange()
                .expectStatus().isForbidden();
    }
```

- 스프링 시큐리티의 @WithMockUser를 사용해서 SOME_OTHER_RULE이라는 역할을 가진 테스트용 가짜 사용자 alice를 테스트에 사용한다
- HTTP 403 Forbidden 상태 코드가 반환되는지 확인한다.

`403 Forbidden` 은 사용자가 인증은 됐지만, 특정 웹 호출을 할 수 있도록 인가받지는 못했음을 의미한다. 

올바른 역할을 갖지못한 사용자의 접근이 거부되는 것을 확인했으므로 이제 올바른 역할을 가진 사용자의 접근이 허용되는지 테스트해보자.

```java
 @Test
    @WithMockUser(username = "bob",roles = {"INVENTORY"})
    void addingInventoryWithProperRoleSucceeds(){
        this.webTestClient 
                .post().uri("/") 
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{" + 
                        "\"name\": \"iPhone 11\", " +
                        "\"description\": \"upgrade\", " + 
                        "\"price\": 999.99" + 
                        "}") 
                .exchange()
                .expectStatus().isOk(); 

        this.repository.findByName("iPhone 11") 
                .as(StepVerifier::create) 
                .expectNextMatches(item -> { 
                    assertThat(item.getDescription()).isEqualTo("upgrade");
                    assertThat(item.getPrice()).isEqualTo(999.99);
                    return true; 
                }) 
                .verifyComplete();
```

- INVENTORY 역할을 가진 가짜 사용자 bob을 테스트에 사용한다

- 주입받은 ItemRepository를 사용해서 몽고디비에 쿼리를 날려서 새 ITEM이 추가됐는지 확인한다.

- 리액터 응답을 검증하기 위해 StepVerifier로 감싼다

- 새로 생성된 Item의 설명항목과 가격항목의 값을 단언문을 통해 확인한다

- 단언문이 모두 성공할 때만 true를 반환한다. expectNextMatches()는 인자로 받은 조건식이 true를 반환하면 테스트를 통과시키고 false를 반환하

  면 테스트를 실패시킨다.

쉽게말하면 JSON데이터를 HTTP POST방식으로 서버에서 보내서 데이터베이스에 저장된 값을 조회해서 예상대로 동작했는지 확인한 것이다.

처음 써볼 때는 조금 복잡하게 느껴질 수도 있지만 리액터 테스트를계속 작성하다 보면 꽤 유용하다는 사실을 알게 될 것이다.

## 사용자 컨텍스트 접근

보안 관리 기능을 추가한다는 것은 현재 사용자의 세부정보에 접근할 수 있다는 점에서 또 다른 중요한 의미를 갖는다. 이제부터 사용자의 장바구니 정보에

대한 얘기를 다룰 것이다. 로그인한 사용자의 세부정보에 접근할 수 있으므로 사용자별로 서로 다른 장바구니를 보여줄 수 있어야 한다.

```java
@GetMapping
    Mono<Rendering> home(Authentication auth) {
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items", this.inventoryService.getInventory())
                .modelAttribute("cart", this.inventoryService.getCart(cartName(auth))
                        .defaultIfEmpty(new Cart(cartName(auth))))
                .modelAttribute("auth", auth)
                .build());
    }
```

- Authentication 객체를 템플릿에 모델로 제공해주면, 템플릿이 웹 페이지의 컨텍스트에 모델 데이터를 담아서 사용할 수 있게 된다.

`Authentication` 정보를 활용하도록 웹 페이지를 수정하기 전에 cartName()메소드가 정확히 무슨일을 하는지 알아보자.

```java
private static String cartName(Authentication auth) {
        return auth.getName() + "'s Cart";
    }
```

이 간단한 이름 생성 로직을 정적 메소드로 옮기면 장바구니 생성 로직을 일원화할 수 있다. Authentication 객체를 템플릿의 모델로 추가하면 사용자

컨텍스트 정보를 보여줄 수 있게된다는 확실한 장점이 추가된다. 다음 HTML코드를 home.html 템플릿의 `<body>`  태그 안의 맨위에 추가하자

```html
<table>
    <tr>
        <td>Name:</td>
        <td th:text="${auth.name}"></td>
    </tr>
    <tr>
        <td>Authorities:</td>
        <td th:text="${auth.authorities}"></td>
    </tr>
</table>
<form action="/logout" method="post">
    <input type="submit" value="Logout">
</form>
<hr/>
```

이 코드가 추가되면 사용자의 이름과 권한 목록이 표시된다. 로그아웃 버튼도 추가해서 다양한 시나리오를 쉽게 테스트할 수 있게 했고, 마지막으로 `<hr/>` 을

사용해서 화면에 수평선을 표시해서 화려한 CSS없이도 쉽게 구별할 수 있게 했다. 이제 애플리케이션을 재시작하고 http://localhost:8080에 접속해

username을 manager, password를 1234 를 입력하면 다음과 같이 manager의 컨텍스트 정보가 화면 상단에 표시된다.

![image](https://user-images.githubusercontent.com/40031858/129815442-80e05751-59ab-4ab3-bcc3-22a58438a7a3.png)

새로 추가한 HTML 코드에 의해 사용자 이름과 권한, 로그아웃 버튼이 화면에 표시된다. 이제 사용자의 장바구니에서 Item을 추가/삭제하는 기능도

개선해보자. 

```java
@PostMapping("/add/{id}")
    Mono<String> addToCart(Authentication auth, @PathVariable String id) {
        return this.inventoryService.addItemToCart(cartName(auth), id)
                .thenReturn("redirect:/");
    }

    @DeleteMapping("/remove/{id}")
    Mono<String> removeFromCart(Authentication auth, @PathVariable String id) {
        return this.inventoryService.removeOneFromCart(cartName(auth), id)
                .thenReturn("redirect:/");
    }
```

`Authentication` 이 두 메소드의 인자로 추가됐다. 사용자 장바구니에 Item을 추가하거나 삭제하기 위해 먼저 장바구니를 찾아야 하며, 이때 cartName() 

메소드를 사용한다. 사용자 세부정보를 기준으로 장바구니를 구별할 수 있도록 약간 변경했을 뿐이지만 전체 애플리케이션에서 장바구니가 하나밖에 없던

시스템에서 사용자별 장바구니를 사용할 수 있는 시스템으로 전환됐다. 이제 말그대로 수백만 개의 장바구니도 감당할 수 있게 됐다. 

## 메소드 수준 보안

지금까지 기본적인 보안을 적용했는데 그에 따른 이슈도 있다. pathMatchers(POST,"/").hasRole(...) 같은 HTTP동사와 URL규칙을 사용해서 세부적인

제어를 할 수 있게 됐지만 여전히 다음과 같은 한계가 있다.

- 컨트롤러 클래스를 변경하면 시큐리티 정책도 함께 변경해야 한다.
- 컨트롤러가 추가될수록 SecurityWebFilterChain 빈에 추가해야 할 규칙도 금세 늘어난다.
- 웹 엔드포인트와 직접적으로 연결되지는 않지만 역할 기반의 보안규칙을 적용할 수 있다면 좋지않을까?

이런 이슈를 해결하기 위해 메소드 수준 보안방식이 등장했다.

스프링 시큐리티 애노테이션을 메소드에 직접 명시해서 비즈니스 로직이 있는 곳에 필요한 보안 조치를 직접 적용할 수 있다. 수십개의 컨트롤러의 수많은

URL에 대한 보안 규칙을 SecurityConfig에 정의하는 대신에, 비즈니스 로직에 따라 적절한 보안 규칙을 비즈니스 로직 바로 곁에 둘 수 있다.

메소드 수준 보안을 더 자세히 알아보기 전에 몇가지 웹 컨트롤러 메소드보다 더 실질적인 예제가 필요하다. 스프링 헤이티오스(Spring HATEOAS)를

사용하는 REST API를 추가한다.

```groovy
implementation('org.springframework.boot:spring-boot-starter-hateoas'){
        exclude group:"org.springframework.boot", module:"spring-boot-starter-web"
    }
```

메소드 수준 보안은 기본으로 활성화되지는 않으며 다음과 같이 `@EnableReactiveMethodSecurity` 를 추가해야 활성화된다. 물론 아무 클래스에나

추가하는 것보다 보안 설정 클래스를 추가하는 것이 가장 좋다.

```java
@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {
  ...
}
```

리액티브 버전인 `@EnableReactiveMethodSecurity` 를 붙이는 것이 중요하다. 그렇지 않으면 제대로 동작하지 않는다. 메소드 수준 보안으로 변경

하는 작업의 첫 단계는 pathMatcher()를 제거하는 것이다.

```java
@Bean
    SecurityWebFilterChain myCustomSecurityPolicy(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                        .and()
                        .httpBasic()
                        .and()
                        .formLogin())
                .csrf().disable()
                .build();
    }
```

pathMatcher()로 저장했던 규칙을 제거하니까 기본 보안 정책과 거의 비슷할 정도로 단순해졌다. 유일한 차이는 CSRF를 비활성화했다는 것이다.

인가 규칙을 제거하고 나면 ApiItemController를 작성할 차례다.  새 Item 객체를 생성하는 메소드를 집중해서 살펴보자. 인가된 사용자에

의해서만 실행되게 한다.

```java
@PreAuthorize("hasRole('" + INVENTORY + "')")
    @PostMapping("/api/items/add")
    Mono<ResponseEntity<?>> addNewItem(@RequestBody Item item, Authentication auth) {
        return this.repository.save(item)
                .map(Item::getId)
                .flatMap(id -> findOne(id, auth))
                .map(newModel -> ResponseEntity.created(newModel
                        .getRequiredLink(IanaLinkRelations.SELF)
                        .toUri()).build());
    }
```

- @PreAuthorize는 메소드 수준에서 보안을 적용할 수 있게 해주는 스프링 시큐리티의 핵심 애노테이션이다 스프링 시큐리티 SpEL 표현식을

  사용해서 이 메소드를 호출하는 사용자가 ROLE_INVENTORY 역할을 가지고 있는지 단언한다. INVENTORY는 앞에서 "INVENTORY" 라는

  문자열을 가진 단순한 상수다.

- 이 메소드도 Authentication 객체를 인자로 받는다. 어떤 이유에서든 메소드가 현재 사용자의 보안 컨텍스트를 사용할 필요가 있다면 이방식으로 주입

  받을 수 있다. 주입받은 Authentication 객체를 사용하는 방법은 다음절에서 다룬다.

장바구니에서 Item을 삭제하는 메소드에도 동일한 메소드 수준 보안을 적용해야한다.

```java
@PreAuthorize("hasRole('" + INVENTORY + "')")
    @DeleteMapping("/api/items/delete/{id}")
    Mono<ResponseEntity<?>> deleteItem(@PathVariable String id) {
        return this.repository.deleteById(id) 
                .thenReturn(ResponseEntity.noContent().build());
    }
```

`@PreAuthorize` 는 메소드 수준 보안에 관해서는 스프링 시큐리티의 중심 타자라고 할 수 있다. 더 복잡한 표현식을 사용할 수도 있으며 심지어 메소드

인자를 사용할 수도 있다. 또한 `@PostAuthorize` 를 사용하면 메소드 호출 후에 보안 규칙을 적용할 수도 있다. 중요 결정 사항이 포함된 핵심 내용이 

반환되는 경우 `@PostAuthorize` 를 사용하면 좋다. 스프링 시큐리티 SpEL 표현식에 단순히 returnObject를 사용해서 반환값을 참조하면 된다.

하지만 데이터베이스를 수정하고 반환값으로 제어하는 것은 비용이 든다. 결과 목록을 반환받은 후에 필터링을 하고싶다면 `@PostFilter` 를 

사용할 수 있다. 이렇게 하면 현재 사용자가 볼 수 있도록 인가되지 않은 데이터를 반환 목록에서 필터링해서 제외할 수 있다. 하지만 결국 필터링될 

데이터를 포함해서 많은 양의 데이터를 조회하는 것 자체가 비효율적이다. 그래서 스프링 데이터는 Authentication 객체를 사용해서 현재 사용자가

볼 수 있는 데이터만 조회하는 기능을 지원한다. 메소드 수준 보안을 적용했으므로 잊지 말고 테스트를 통해 확인하자. 먼저 적절한 권한이 없는

사용자가 새 Item 추가를 시도했을 때 새 Item이 추가되지 않는지 테스트해보자.

```java
@Test
    @WithMockUser(username = "alice",roles = {"SOME_OTHER_ROLE"})
    void addingInventoryWithoutProperRoleFails(){
        this.webTestClient
                .post().uri("/api/items/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{" +
                        "\"name\": \"iPhone X\", " +
                        "\"description\": \"upgrade\", " +
                        "\"price\": 999.99" +
                        "}")
                .exchange()
                .expectStatus().isForbidden();
    }
```

- 스프링 시큐리티의 @WithMockUser를 써서 SOME_OTHER_RULE이라는 역할이 부여된 인가되지 않은 가짜 사용자를 테스트에 요청한다

인가되지 않은 사용자 테스트는 예상대로 통과되었다. 이제 인가된 사용자 테스트를 작성하자.

```java
@Test
    @WithMockUser(username = "bob", roles = {"INVENTORY"})
    void addingInventoryWithProperRoleSucceeds() {
        this.webTestClient 
                .post().uri("/api/items/add") 
                .contentType(MediaType.APPLICATION_JSON) 
                .bodyValue("{" +
                        "\"name\": \"iPhone X\", " + 
                        "\"description\": \"upgrade\", " + 
                        "\"price\": 999.99" + 
                        "}") 
                .exchange() 
                .expectStatus().isCreated(); 

        this.repository.findByName("iPhone X") 
                .as(StepVerifier::create) 
                .expectNextMatches(item -> { 
                    assertThat(item.getDescription()).isEqualTo("upgrade");
                    assertThat(item.getPrice()).isEqualTo(999.99);
                    return true; 
                }) 
                .verifyComplete(); 
    }
```

이런 테스트 케이스를 작성하면 보안 프로파일이 적절하게 설정됐는지 쉽게 확인할 수 있다. 그리고 인가된 사용자와 인가되지 않은 사용자에 대해

각각 최소 1개씩의 테스트를 작성해서 보안 정책의 정상동작에 높은 신뢰성을 확보할 수 있다. 보안 정책을 변경한다면 테스트 코드에도 많은

변경사항이 발생하지 않을까? 그렇다. 하지만 그게 나쁜 것은 아니다. 보안 정책이 변경되면 많은 부분이 변경될 수 있는데, 테스트가 있다면 테스트가 실패

하면서 변경돼야 하는 부분을 더 쉽게 찾아 변경할 수 있게 된다. 보안 관점에서 가장 중요한 첫 번째 원칙은 권한이 부족한 사용자가 인가받지 않은 기능을

사용하지 못하게 하는 것이고, 앞의 예제에서 바로 그부분을 다뤘다. ROLE_INVENTORY 권한을 가진 사용자만 시스템의 재고를 변경하는 기능을

수행할 수 있다. 보안 관점에서 두 번째로 중요한 원칙은 첫 번째 원칙을 위배할 수 있는 어떤 단서도 사용자에게 보여주지 않는 것이다. 하이퍼미디어 관점

에서는 인가받지 못한 사용자가 접근할 수 없는 링크는 제공하지 말아야 함을 의미한다. 두번째 원칙을 지킬 수 있도록 하이퍼미디어 레코드를

보여주는 findOne() 메소드를 점검해서 불필요한 정보를 인가되지 않은 사용자에게 전달하지 않는 방법을 알아보자.

```java
private static final SimpleGrantedAuthority ROLE_INVENTORY =
            new SimpleGrantedAuthority("ROLE_" + INVENTORY);

@GetMapping("/api/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id, Authentication auth) {
        ApiItemController controller = methodOn(ApiItemController.class);

        Mono<Link> selfLink = linkTo(controller.findOne(id, auth)).withSelfRel()
                .toMono();

        Mono<Link> aggregateLink = linkTo(controller.findAll(auth))
                .withRel(IanaLinkRelations.ITEM).toMono();

        Mono<Links> allLinks;

        if (auth.getAuthorities().contains(ROLE_INVENTORY)) {
            Mono<Link> deleteLink = linkTo(controller.deleteItem(id)).withRel("delete")
                    .toMono();
            allLinks = Mono.zip(selfLink, aggregateLink, deleteLink)
                    .map(links -> Links.of(links.getT1(), links.getT2(), links.getT3()));
        } else {
            allLinks = Mono.zip(selfLink, aggregateLink)
                    .map(links -> Links.of(links.getT1(), links.getT2()));
        }

        return this.repository.findById(id)
                .zipWith(allLinks)
                .map(o -> EntityModel.of(o.getT1(), o.getT2()));
    }
```

- 사용자에게 반환할 링크 정보를 `Mono<Links>` 타입의 allLinks에 담을것이다. `Mono<Links>` 는 스프링 헤이티오스의 링크 데이터 모음인

  Links 타입을 원소로 하는 리액터 버전 컬렉션이다. 

- 사용자가 ROLE_INVENTORY 권한을 가지고 있는지 검사해서 가지고 있으면 DELETE 기능에 대한 링크를 self와 애그리것 루트 링크와 함께

  allLinks에 포함한다. 리액터의 Mono.zip() 연산은 주어진 3개의 링크를 병한한 후 튜플(tuple) 로 만든다. 그리고 map()을 통해 Links객체로 변환

- 사용자가 ROLE_INVENTORY 권한을 가지고 있지 않다면 self링크만 애그리것 루트 링크에 포함한다

- 데이터 스토어에서 Item 객체를 조회하고 Links 정보를 추가해서 스프링 헤이티오스의 EntityModel 컨테이너로 변환해서 반환한다.

주의깊게 봐야할 것은 Mono.zip()을 사용한다는 점이다. zip은 함수형 프로그래밍에서는 매우 친숙한 개념이다. 리액터에서는 여러개의 결과가 

필요하지만 결과가 언제 종료될지 알 수 없을때 `zip` 을 사용한다. 예를들어 3개의 원격 호출이 필요하고 3개의 결과를 하나로 묶어서 받고 싶다고

가정하자. Mono.zip()은 3개의 응답을 모두 받았을때 콜백을 호출해서 결과를 묶음 처리한다.

```java
@Test
    @WithMockUser(username = "alice")
    void navigateToItemWithoutInventoryAuthority() {
        RepresentationModel<?> root = this.webTestClient.get().uri("/api")
                .exchange()
                .expectBody(RepresentationModel.class)
                .returnResult().getResponseBody();

        CollectionModel<EntityModel<Item>> items = this.webTestClient.get()
                .uri(root.getRequiredLink(IanaLinkRelations.ITEM).toUri())
                .exchange()
                .expectBody(new CollectionModelType<EntityModel<Item>>() {
                })
                .returnResult().getResponseBody();

        assertThat(items.getLinks()).hasSize(1);
        assertThat(items.hasLink(IanaLinkRelations.SELF)).isTrue();

        EntityModel<Item> first = items.getContent().iterator().next();

        EntityModel<Item> item = this.webTestClient.get()
                .uri(first.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .exchange()
                .expectBody(new EntityModelType<Item>() {
                }) 
                .returnResult().getResponseBody();

        assertThat(item.getLinks()).hasSize(2);
        assertThat(item.hasLink(IanaLinkRelations.SELF)).isTrue();
        assertThat(item.hasLink(IanaLinkRelations.ITEM)).isTrue();
    }
```

첫번째 webTestClient는 /api에 GET요청을 전송해서 링크 목록을 반환받는다. 링크 목록만 포함돼 있으므로 RepresentationModel<?> 객체로

추출할 수 있다. 이제 Item 링크를 요청해서 URI를 추출하고 애그리것 루트의 응답을 CollectionModel<EntityModel< Item>> 으로 반환한다. 

인가받은 사용자에 대한 테스트이므로 self링크뿐만 아니라 add링크도 포함돼 있어야 하며, 단언문을 통해 확인한다.

CollectionModel에서 첫 번째 EntityModel< Item> 을 가져와서 self 링크를 알아내고 다시 self링크로 GET요청을 보내서 Item 세부정보를 가져와서

delete 링크가 포함돼 있는지 단언한다. 이제 메소드 수준에서 세밀하게 보안 제어를 적용하는 방법을 알게됐다. 그리고 사용자의 보안 프로파일 정보를 

이용해서 화면에 표시되는 정보도 바꿀 수 있게 됐다. 

 ## OAuth 보안

소셜 미디어 네트워크가 인기를 얻게 되자 새로운 보안 이슈가 떠오르게 됐다. 회사들은 널리 사용되는 웹 사이트를 만들었고 자사의 앱을 만들기 위한 API

도 함께 만들었다. 그리고 서드파티 애플리케이션이 나타나기 시작했다. 하지만 사용자들은 소셜 미디어 네트워크에 접속하기 위해 서드파티 앱에 인증 정보를

입력해야 했다. 그리고 소셜 미디어 네트워크 사이트에서 인증 정보를 변경하면 서드파티 앱에서도 인증 정보를 업데이트 해야했다. 이부분이 사용자들을

불편하게 만들었다. 인증 정보를 공유하는 것은 항상 문제를 일으킨다.  이문제를 해결하기 위해 `OAuth` 개념이 탄생했다. `OAuth` 는 안전한 위임 접속을

보장하는 공개 프로토콜이다. 구체적으로 얘기하면 서드파티 앱을 통해 소셜 미디어 네트워크에 접속할 때 인증 정보를 입력하지 않아도 된다는 말이다.

대신에 서드파티 앱에서 소셜 미디어 네트워크 사이트의 로그인 페이지를 띄워주며, 소셜 미디어 네트워크 사이트에서 로그인을 하면, 보안 토큰이 서드파티

앱에 전달되고, 서드파티 앱은 그 이후로는 사용자의 인증 정보가 아니라 보안 토큰을 통해 소셜 미디어 네트워크에 있는 사용자의 데이터에 접근할 수 있게 된다.

서드파티 앱은 인증 정보를 관리할 필요 없이 오로지 토큰만 사용하면 된다. 토큰에는 만기, 갱신 등의 핵심 기능이 포함돼 있다.

다시 현실로 돌아와 만드는 스프링 애플리케이션에서 구글이나 페이스북을 사용해서 로그인하려면 어떻게 할까? 이제 알아보자

먼저 spring-boot-starter-security 대신에 다음과 같은 의존관계를 추가해야 한다.

```groovy
implementation 'org.springframework.security:spring-security-config'
    implementation 'org.springframework.security:spring-security-oauth2-client'
    implementation 'org.springframework.security:spring-security-oauth2-jose'
```

- spring-security-config: 컨트롤러에서 스프링 시큐리티 설정 애노테이션과 타입을 사용하기 위해 필요
- spring-security-oauth-client: 애플리케이션이 OAuth 클라이언트로서 OAuth 프로바이더와 통신할 때 필요
- spring-security-oauth2-jose: JOSE(javascript signing and encryption)를 사용할때 필요 

의존관계를 추가한 후에는 OAuth 프로바이더인 구글에 새 애플리케이션을 등록해야한다. 다음 순서를 따라 하나씩 실행해보자.

1. https://developers.google.com/identity/protocols/OpenIDConnect 에 접속한다
2. Credentials page 링크를 클릭한다.
3. 아직 클라우드에서 프로젝트를 만든 적이 없다면 https://cloud.google.com/resource-manager/docs/creating-managing-projects?hl=ko
   에 접속하고 아래쪽으로 스크롤하면 보이는 리소스 관리 페이지로 이동을 클릭한다. 로그인을 하면 다음과 같이 리소스 관리 화면을 볼 수 있다.

![image](https://user-images.githubusercontent.com/40031858/130019441-0192e5b7-e3f5-4c14-93c4-80a5b4364bbf.png)

만들고나면 다시 

![image](https://user-images.githubusercontent.com/40031858/130019793-8c3205eb-238e-496a-b8e4-aa8ec774c380.png)

![image](https://user-images.githubusercontent.com/40031858/130019976-fc772edd-e741-4fd4-9b1c-40cda8aae4fb.png)

![image](https://user-images.githubusercontent.com/40031858/130020107-a2239d5d-f5df-4571-8bc5-a7a2d613ee79.png)

클라이언트 ID와 클라이언트 보안 비밀번호가 생성됐으면 다시 IDE로 돌아오자. 이제 application.yml 파일에 OAuth 클라이언트 정보를 입력한다

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: 674437713780-rshehbdnrbbfa3tkv61gmnlf6vkkq1ej.apps.googleusercontent.com
            client-secret: Y-oW_7o1AHcx2wc6Ovs5YEgL
```

google 이라는항목 아래에 있는 client-id와 secret 값은 앞에서 실제로 발급받은 정보를 입력해야 한다. 스프링 시큐리티에는 OAuth를 사용하는 데 필요

한 대부분의 정보가 이미 설정돼 있으며 client-id와 client-secret만 실젯값으로 입력하면 된다. 

스프링 시큐리티에는 구글, 깃허브, 페이스북, 옥타의 클라이언트로 사용할 수 있는 기능이 미리 만들어져 제공되고 있다. 이 외에 스프링 시큐리티에서 지원

하는 OAuth 프로바이더 정보는 CommonOAuth2Provider 클래스를 참고한다. 이제 사이트의 홈페이지를 방문했을 때 사용자의 장바구니를 읽어오도록

HomeController를 수정해보자.

```java
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
```

- 단순히 Authentication 객체를 가져오는 대신에 OAuth2AuthorizedClient와 OAuth2User를 주입받는다. OAuth2AuthorizedClient에는 OAuth

  클라이언트 정보가 담겨 있고, OAuth2User에는 로그인한 사용자 정보가 담겨 있다. @RegisteredOAuth2AuthorizedClient와 

  @AuthenticationPrincipal 애노테이션은 컨트롤러 메소드의 파라미터에 붙어서 스프링 시큐리티가 컨트롤러 메소드의 파라미터값을 결정하는데사용



이제 OAuth 인증을 통해 접근할 수 있는 내용을 화면에 보여주기 위해 HTML 템플릿 내용을 수정하자.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="https://www.thymeleaf.org/thymeleaf-extras-springsecurity5">
<head>
    <meta charset="UTF-8"/>
    <title>Hacking with Spring Boot - Getting Started</title>
</head>
<body>

<!-- tag::user-context[] -->
<div sec:authorize="isAuthenticated()">
    <table>
        <tr>
            <td>User:</td>
            <td><span sec:authentication="name"></span></td>
        </tr>
        <tr>
            <td>Authorities:</td>
            <td th:text="${authorities}"></td>
        </tr>
        <tr th:each="userAttribute : ${userAttributes}">
            <td th:text="${userAttribute.key}"/>
            <td th:text="${userAttribute.value}"/>
        </tr>
    </table>
    <form action="#" th:action="@{/logout}" method="post">
        <input type="submit" value="Logout"/>
    </form>
</div>
```

애플리케이션을 실행하고 http://localhost:8080 에 접속하면 아까와는 완전히 다른 흐름으로 전개되는 것을 확인할 수 있다.

![image](https://user-images.githubusercontent.com/40031858/130021537-1126ce45-8a4d-4d99-89a6-ea39c135e0ff.png)

구글 로그인 화면으로 리다이렉트되는 것을 확인할 수 있다. 앞서 생성한 테스트 계정 정보를 입력하고 구글 로그인에 성공하면 우리가 만든 애플리케이션

으로 리다이렉트 된다. 

OAuth2를 사용하는 주된 이유는 사용자 정보 관리를 위임할 수 있기 때문이다. 보안 문제 발생빈도를 생각해보면 사용자 정보 관리를 직접 하기보다, 구글,

페이스북, 옥타, 깃허브처럼 이미 안전하게 관리하고 있는곳에 위임하는 것도 꽤 현명한 생각이다. 주요 OAuth 프로바이더별 장단점은 다음과 같다

- 구글과 페이스북은 가장 널리 사용되는 서비스이며 대부분의 사용자는 이 두서비스의 계정을 가지고있다
- 깃허브 계정이 없는 개발자는 많지 않을 것이다. 애플리케이션의 주 사용자가 개발자라면 깃허브에 위임하는 것도 좋다
- 옥타는 세밀한 사용자 제어가 필요한 상황에서는 편리하지만, 필요 이상으로 복잡하게 느껴질 수도 있다

사용자 관리를 외부에 위임할때 고려해야할 사항은 여러가지 역할이나 권한을 선언하는 대신에 스코프를 다뤄야한다는 점이다. 스코프도 SCOPE_

접두어가 붙은 권한의 일종이라고 생각하면 쉽다. OAuth 프로바이더가 제공하는 스코프 외에 커스텀 스코프가 필요한지를 검토해보는것이 중요하다

필요하지 않다면 어떤 OAuth 프로바이더를 사용하더라도 괜찮다. 하지만 필요하다면 옥타를 사용하는 것이 좋다. 구글이나 페이스북은 자기들의 API

를 사용하는데 중점을 두고있어서 커스텀 스코프나 그룹을 만들 수 없다. 이제 상황과 필요에 따라 어떤 OAuth 프로바이더를 선택해야 하는지도 

알게 됐다. 커스텀 스코프 없이 일반적인 접근 제어만 필요하다면 구글이나 페이스북을 선택하면 된다. 사용자가 대부분 개발자라면 깃허브를 

사용할 수 있다. 세밀한 역할 관리가 필요하다면 옥타를 선택하는 편이 가장 좋을 것이다. 적절히 선택한다면 어느 쪽이든 스프링 부트 애플리케이션에

강력한 보안 기능이 장착될 것이다

## 정리

지금까지 9장에서 다룬 내용은 다음과 같다

- 스프링 부트 시큐리티 스타터를 추가해서 데모 애플리케이션 생성
- 데이터베이슬르 통해 사용자 정보를 관리할 수 있도록 스프링 데이터 레포지토리 사용
- URL 기준 보안 규칙 설정
- 메소드 수준 보안 설정을 통한 상세한 접근 제어
- 사용자 관리를 구글 같은 서드파티 OAuth 프로바이더에 위임
