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
