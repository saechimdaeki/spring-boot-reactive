# 5장 스프링 부트 운영

앞서 4장에서는 리액티브 애플리케이션을 테스트하는 방법을 알아보았다. 이제 5장에서는 개발 과정을 뒤로 하고 운영의 세계로 뛰어들어 Day2 운영에

필요한 사항을 알아본다. 5장에서 다룰 내용은 다음과 같다

- 우버 JAR 파일을 만들고 운영환경에 배포
- 컨테이너 생성을 위한 계층형 Dockerfile 생성
- Dockerfile을 사용하지 않는 컨테이너 생성
- 애플리케이션 운영을 도와주는 스프링 부트 액추에이터
- 운영을 위해 사용할 기능과 사용하지 않을 기능 분별
- 애플리케이션 버전 세부 내용 추가
- 관리 서비스 라우트 지정

## 애플리케이션 배포

스프링 부트를 사용하면 많은 노력을 기울여 만든 애플리케이션을 쉽게 배포할 수 있다. 앞으로 여러절에 걸쳐 애플리케이션을 운영환경에 배포하는 다양한방법을 알아보자.

### 우버 JAR배포

명령행에서 스프링 부트 애플리케이션을 실행하려면 `./gradlew bootRun` 명령을 실행하면된다. IDE에서 실행하려면 그냥 main()메소드를 실행하면 된다.

실행 가능한 JAR파일을 만들려면 다음 명령을 실행하면 된다

```bash
$ ./gradlew build
```

gradle의 build를 호출하면 컴파일과 테스트를 포함한 몇가지 단계를 거쳐 실행 가능한 JAR파일을 만들어준다. 스프링 부트는 이 기능을 활용해서 다음과 같은 우버 JAR파일을

만들어 낸다. 컴파일된 코드를 JAR파일로만들어 libs 디렉토리 아래에 둔다. 그런데 이 JAR 파일은 아직 실행 가능한 JAR파일이 아니고 그저 컴파일된 코드일 뿐이다. 

그 다음 spring-boot-gradle-plugin 명령이 컴파일된 파일 모음인 JAR파일과 애플리케이션이 사용하는 의존 라이브러리와 특별한 스프링 부트 코드 일부를 함께 묶어서 새 JAR

파일 생성 후 기존 JAR파일을 대체한다. 바로 이때 만들어진 JAR파일이 실행 가능한 JAR 파일이다. 이 최종 JAR파일은 다음과같이 실행할 수 있다.

```bash
$ java -jar build/libs/chap05-0.0.1-SNAPSHOT.jar
```

`jar tvf build/libs/chap05-0.0.1-SNAPSHOT.jar` 명령을 실행해서 JAR로 파일 내부를 들여다보면 다음과 같은 항목이 포함돼 있음을 확인할 수 있다.

- JAR 파일을 읽고 JAR 안에 포함돼 있는 JAR파일에 있는 클래스를 로딩하기 위한 스프링부트 커스텀 코드
- 애플리케이션 코드
- 사용하는 서드파티 라이브러리 전부

실행하기 위해 필요한 모든것이 JAR파일에 담겨있으므로 JDK가 설치된 장비라면 어디에서든 JAR파일로 패키징된 자바 애플리케이션을 실행할 수 있다.

하지만 자바가 설치돼 있지 않은 장비에는 어떻게 배포할 수 있을까?

### 도커 배포

도커를 사용하면 컨테이너에 자바와 애플리케이션을 함께 담아서 배포할 수 있다. 

```dockerfile
FROM adoptopenjdk/openjdk8:latest
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

이 코드는 아주 간단하지만, 상당히 복잡한 구성이 필요한 애플리케이션도 도커 컨테이너로 만들 수 있다. 위의 파일내용은 다음과 같은 작업을 수행한다

- 믿을 만한 OpenJDK 제공자가 올려둔 JDK를 기반으로 컨테이너를 생성한다
- 패턴 매칭을 사용해서 애플리케이션 JAR파일을 이미지 빌드에 사용할 인자로 지정한다
- JAR파일을 복사해서 컨테이너 내부에 app.jar로 이름을 바꿔 붙여넣는다
- 컨테이너 안에서 java -jar /app.jar 명령을 수행하게 해서 JAR파일을 실행한다

스프링 부트의 손쉬운 우버 JAR지원 기능 덕분에 애플리케이션을 도커화하고 실행하는 것도 아주 간단하다. 간단해서 좋지만 이것으로 충분할까?

도커는 캐시시스템을 가지고 잇다. 캐시 시스템은 컨테이너 빌드에 소요되는 시간을 줄이기 위해 `계층화(layering)`를 이용한다. Dockerfile의 여러 부분을 각각 

하나의 계층으로 만들어서, 해당 계층에 변경이 발생하면 그 계층만 새로 빌드한다. 스프링 부트의 우버 JAR는 개발자가 작성한 코드와 개발자가 선택한 버전의

스프링 부트 및 의존 라이브러리를 함께 묶어서 만들어진다. 이 모든 내용을 하나의 계층에 담으면, 개발자 코드만 변경됐을 때 개발자 코드뿐 아니라 스프링 부트와

의존 라이브러리까지 모두 다시 빌드돼야 한다. 

애플리케이션을 여러 부분으로 분할해서 여러 계층에 나눠 담고 도커의 캐시 시스템을 활용하는 것이 더 효율적이다. 이럴때 스프링 부트 그레이들 플러그인에 

내장된 도커 지원 기능을 활용하는 편이 더 낫다. 이를 위해 build.gradle에 계층화 방식을 사용한다고 지정한다.

```groovy
bootJar{
    layered()
}
```

이렇게하면 되지만 스프링 공식사이트에서 다음과 같이 나와있다.

```markdown
org.springframework.boot.gradle.tasks.bundling.BootJar.layered()
since 2.4.0 for removal in 2.6.0 as layering as now enabled by default.
```

따라서 2.4.0 이후 버젼부터는 default로 사용가능하다.

![image](https://user-images.githubusercontent.com/40031858/128620619-88dbf95a-ea41-408d-a222-c83d6ade11db.png)

스프링 부트가 jarmode=layertools 파라미터를 인식해서 list 명령을 만나면 JAR 파일에 내장 된 모든 계층을 보여준다. 이제 계층 관련 지식을 바탕으로 더 복잡한 

Dockerfile을 작성해보자

```dockerfile
FROM adoptopenjdk/openjdk11:latest as builder
WORKDIR application
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM adoptopenjdk/openjdk11:latest
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
#COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]

```

앞서 살펴본 단순 Dockerfile과는 다르게 여러 단계로 구성된다

1. 빌더(builder)로 사용할 컨테이너를 만든다

2. extract 명령으로 레이어를 추출한다

3. 두번째 컨테이너를 만든다

4. 빌더 컨테이너에서 추출한 여러 레이어를 두번째 컨테이너에 복사한다 COPY명령에는 도커의 계층 캐시 알고리즘이 적용된다. 그래서 서드파티 라이브러리는 캐시될 수 있다.

5. java -jar가 아니라 스프링 부트의 커스텀 런처(custom laucnher) 로 애플리케이션을 실행한다. 이 런처는 애플리케이션 시작시 불필요한 JAR파일 압축해제를 하지않으므로

   효율적이다

애플리케이션에서 사용하는 의존 라이브러리 버전에 따라 특정 계층이 만들어지지 않을 수 도있다. 예를들어, 스냅샷 의존관계가 없어 해당 계층이 만들어지지 않으면

위의 파일처럼 application/snapshot-dependencies를 주석처리하면 된다. 이제 다음 명령으로 컨테이너 이미지를 만들 수 있다.

```bash
$ docker build. --tag chap05
```

![image](https://user-images.githubusercontent.com/40031858/128620801-f1eaf47a-c8cc-41c6-b293-7f3becfd8100.png)

컨테이너 이미지를 처음 빌드할 때는 모든 계층이 새로 빌드된다. 하지만 소스 코드를 변경한 후 다시 이미지를 빌드하면 변경이 발생한 계층만 새로 빌드되는 것을 확인 할 수 있다.

초기 데이터를 로딩하는 TemplateDatabaseLoader 클래스에 mongo.save(new Item("TEST", "TEST", 99.99)); 를 추가한 후 ./gradlew clean build 명령을 실행해서 변경된

내용이 반영된 JAR파일을 새로 생성하고 다시 이미지를 빌드하면 변경된 application 계층 외의 나머지 계층은 캐시를 사용하는 것을 확인할 수 있다.

`Using cache` 가 출력되는 것을 확인할 수 있다. JDK 이미지에는 Using cache 표시가 없지만 해시값을 보면 앞단계에서 다운로드한 JDK 이미지를 재사용한다는 것을 알 수 있다.

나중에 스프링 부트의 버전을 변경하면 그에 따라 달라지는 계층도 새로 빌드될 것이다. 이제 이미지가 만들어졌으니 실행해보자.

```bash
$ docker run -it -p 8080:8080 chap05:latest
```

또한 스프링 부트 그레이들 플러그인에서 제공하는 bootBuildImage 명령을 이용해서 도커 이미지를 만들 수도 있다.

```bash
$ ./gradlew bootBuildImage
```

`bootBuildImage` 명령을 실행하면 스프링 부트가 `페이키토 빌드팩(Paketo buildpack)` 프로젝트에서 빌드팩을 가져와서 도커 컨테이너 이미지를 빌드한다. 이과정에서

Dockerfile은 전혀 필요하지 않다. 지금 한것처럼 수동으로 컨테이너 이미지를 빌드할 수 있고, 프로젝트 빌드할 때마다 컨테이너 이미지도 자동으로 빌드할 수 있다. 

이제 페이키토로 만든 이미지로 애플리케이션을 실행해보자.

```bash
$ docker run -it -p 8080:8080 docker.io/library/cahp05:0.0.1-SNAPSHOT
```

Dockerfile에서 만든 이미지를 실행할 때와 거의 똑같다. 두가지 방식의 장단점을 보자

| 방식                        | 장점                                                         | 단점                                                         |
| --------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 계층 기반 Docker 이미지     | - Dockerfile을 직접 작성하므로 이미지<br />빌드 전체 과정 제어 가능<br />- 스프링 부트에서 계층화를 제공하므로<br />빌드 과정 최적화가능 | - 컨테이너 직접 관리<br />- 컨테이너를 빌드 과정이 완전하지<br />않으면 보안에 취약한 계층 존재 위험 |
| 페이키토 빌드팩 기반 이미지 | - Dockerfile을 직접 다룰 필요 없음<br />- 최신 패치와 SSL을 포함한 업계<br />표준 컨테이너 기술이 빌드 과정에 포함<br />- 개발에 더 집중 가능 | - Dockerfile에 비해 제어할 수 있는 것이 적음                 |

둘다 장단점이 있고 스프링 부트는 개발자가 선택할 수 있는 최고의 옵션을 제공한다. 이제 컨테이너화된 애플리케이션을 도커허브같은 컨테이너 저장소에 업로드 할 수 있다.

우버 JAR파일과 도커 컨테이너 중 어떤것으로 배포할지 선택할 수 있다. 젠킨스, 컨코스 등의 지속적 통합 도구로 모든과정을 자동화할 수도 있다. 어느쪽을 택하든 자동화를

반드시 도입하기를 추천한다. 변경사항을 운영환경에 배포하는 과정을 자동화하면 비용과 위험을 모두 현격하게 줄일 수 있다.

## 운영 애플리케이션 관리

애플리케이션 배포 과정을 마치면서 Day 1임무 수행을 완료했다. 이제 Day2 임무인 애플리케이션 관리 단계로 들어가보자. 이어서, 애플리케이션 모니터링을 돕기 위해 

스프링 부트 액추에이터 모듈에서 제공하는 여러 도구도 살펴보자.

### 애플리케이션 정상 상태 점검 : /actuator/health

지금까지 웹플럭스를 사용해 웹페이지를 화면에 보여주는 스프링 부트 애플리케이션을 만들었다. 스프링 데이터 모듈을 활용해서 데이터를 조회하고 저장하는 기능도 추가했다

그럼 이제 어떤일이 필요할까? 애플리케이션을 운영하는 운영팀에서 가장 처음 던지는 질문은 아마 "애플리케이션 서버에 ping" 날릴 수 있나요? 일것이다.

그리고 "모니터링 지표는 어떻게 보나요?" 등 질문을 연이어 던질 것이다. 이런 질문을 답해줄 수있는 컨트롤러를 추가로 만들어서 대응할 수도 있겠지만, 스프링 부트액추에이터

를 추가하면 더 일찍 퇴근할 수 있다.

```groovy
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

스프링 부트 액추에이터를 추가하고 애플리케이션을 재실행하면 여러가지 컴포넌트가 추가된 것을 실행로그를 통해 확인할 수 있다. 먼저 다음과 같은 로그를 볼 수 있을것이다.

```bash
2021-08-08 14:22:45.640  INFO 23543 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 1 endpoint(s) beneath base path '/actuator'
```

스프링 부트 액추에이터가 추가됐고 1개의 엔드포인트가 활성화됐다는 메시지이다. 먼저 운영팀의 첫 질문인 ping을 처리하는 방법을 알아보자. 

http:localhost:8080/actuator/health 에 접속해보라고 답하면 된다 그럼 운영팀에서는 다음과 같은 ping 결과를 보게 될 것이다

```json
{
  status:"UP"
}
```

이 기초 상태 점검을 위해 개발자가 작성한 코드는 단 한줄도없다. 스프링 부트 액추에이터만 추가하면 스프링 부트 개발팀이 만들어둔 ping 관련 기능이 동작하므로, 나머지 시간은

비즈니스 요구사항을 충족시키는데 사용할 수 있다. 서버 상태의 세부정보를 표시하는 설정을 `application.properties` 파일에 추가하면 더 자세한 정보를 보여줄수있다.

```properties
management.endpoint.health.show-details=always
```



```js
{
"status": "UP",
"components": {
"diskSpace": {
"status": "UP",
"details": {
"total": 499963174912,
"free": 383443099648,
"threshold": 10485760,
"exists": true
}
},
"mongo": {
"status": "UP",
"details": {
"version": "4.2.9"
}
},
"ping": {
"status": "UP"
}
}
}
```

스프링 부트 액추에이터는 자동설정 정보를 사용해 다음 정보를 반환한다

- 몽고디비 상태 및 버전 정보
- 디스크 상태 및 용량 정보

레디스, 카산드라, 레빗엠큐, 관계형DB, 이메일 등 다른 모듈을 스프링 부트와 함께 사용하면 스프링 부트 액추에이터가 해당 모듈의 HealthIndicator 구현체를 찾아 등록한다.

각 구현체는 UP,DOWN, OUT_OF_SERVICE, UNKNOWN 중 하나를 status 값으로 반환한다. 모든 개별 컴포넌트의 status에 따라 JSON 결과 최상위 status 결괏값이정해진다

### 애플리케이션 상세정보: /actuator/info

애플리케이션 운영에는 정상상태 점검 외에 다른 정보도 필요하다. 즉 배포된 애플리케이션에 사용된 컴포넌트의 버전 정보도 필요하다. 배포된 애플리케이션 버전을 쉽게 확인할

수있으면 일찍 원인을 파악하는 경우가많다. `application.properties` 파일 에 몇가지 애플리케이션 버전 정보를 입력해두면 스프링 부트 액추에이터를 통해 쉽게 확인할 

수 있다

```properties
info.project.version=${project.version}
info.java.version=${java.version}
info.spring.framework.version=${spring-framework.version}
info.spring.data.version=${spring-data-bom.version}
```

${}로 감싸진 데이터의 실젯값이 자동으로 입력된다. 이제 애플리케이션을 재실행후 http://localhost:8080/actuator/info 에 접속하면 다음과 같이 애플리케이션 세부정보를

확인할 수 있다.

```json
{
project: {
version: "@project.version@"
},
java: {
version: "@java.version@"
},
spring: {
framework: {
version: "@spring-framework.version@"
},
data: {
version: "@spring-data-bom.version@"
}
}
}
```

이런 정보를 쉽게 확인할 수 있으면 고객이 최신버전을 사용중인지 쉽게 판별할 수 있다. 빌드파일에서 애플리케이션 버전 정보를 읽고, 플러그인을 통해 소스코드버전정보를

자동으로 읽어서 통합된 정보를 보여주므로, 빌드되는 애플리케이션과 배포되는 애플리케이션의 일관성을 보장할 수 있다. 수작업을 줄이면 고객에게 업데이트되지 않은 잘못된

정보를 제공하는 위험을 대폭 줄일 수 있다.

### 다양한 액추에이터 엔드포인트

지금까지는 웹으로 접근할 수 있는 기본 액추에이터 엔드포인트인 health와 info에 대해 알아봤다. 스프링 부트는 훨씬 더 다양한 액추에이터 엔드포인트를 제공하지만, 웹으로

접근하는 것은 기본적으로 허용하지 않는다. 하지만 모든 액추에이터 엔드포인트를 웹으로 공개해야 한다면 할수는 있다. 다음과 같이 `application.properties` 파일에

단 한줄만 추가하면 된다.

```properties
management.endpoints.web.exposure.include=*
```

모든 액추에이터 엔드포인트를 웹으로 공개하는 것은 이렇게나 간단하다. 하지만 강조하자면 절대로 이렇게 웹으로 모두공개해서는 안된다. 현재 접근 가능한 모든 엔드포인트를

공개하기로 결정했다고 하더라도 *를 사용해서공개해서는 안된다. 향후에 추가될 엔드포인트도 모두 공개되기 때문이다. 따라서 다음과 같이 공개할 엔트포인트를

하나하나 명시해야 그나마 최소한의 안정성을 확보할 수 있다.

```properties
management.endpoints.web.exposure.include=auditevents,beans,caches,conditions,configprops,env,flyway,health,heapdump,httptrace,info,logfile,loggers,metrics,mappings,shutdown,threaddump
```

### 로깅 정보 엔드포인트: /actuator/loggers

`loggers` 엔드포인트를 공개하면 사용중인 모든 로거와 로그레벨을 알 수 있다. 

![image](https://user-images.githubusercontent.com/40031858/128622259-f19ad3e1-d8f1-427f-8e86-b9b496dd22ce.png)

또한 스프링 부트 액추에이터는 데이터를 보여주기만 하는 것이 아니라 저장하는 것도 가능하다.

- 로그 레벨을 변경할 수 있는 액추에이터 엔드포인트는 /actuator/loggers/{package}다. {package} 는 위에 나온 JSON의 loggers 항목 아래에 있는 키를 나타내며 실젯

  값은 패키지 이름이다.

- 컨텐트 타입은 반드시 application/json 이어야 한다

- 저장에 사용되는 데이터는 반드시 {"configuredLevel" : "로그레벨값"} 으로 전송돼야 한다. 

  ​					

## 다양한 운영 데이터 확인

원활한 운영을 위해 확인해야 할 운영 데이터는 매우 많다. 이제부터 여러가지 운영데이터를 액추에이터를 통해 확인하는 방법을 알아보자

### 스레드 정보 확인:/actuator/threaddump

http://localhost:8080/actuator/threaddump에 접속하면 현재 애플리케이션에서 사용되고 있는 모든 스레드의 정보를 확인할 수 있다. 유의해야 할 점은 기본적으로

리액터 기반으로 처리되는 로직은 리액터 스레드에서 실행된다는 점이다. 그리고 리액터에 사용되는 스케줄러는 기본적으로 CPU코어 하나당 한개의 스레드만 생성된다. 그래서

4코어 장비에서는 4개의 리액터 스레드만 생성된다. 하지만 리액터 스레드만 있는 것은 아니고 그외에도 많은 스레드가 사용된다. 예를 들면 스프링 부트 개발자 도구가 제공하는

라이브 리로드 서버는 리액터 스레드가 아닌 별도의 스레드에서 사용된다. 이런 스레드는 애플리케이션 코드에 의해 직접적으로 사용되지 않는다는 점을 감안하면, 애플리케이션

코드에서 리액터 플로우를 제대로 동작하기 위해 만들기 위해 블로킹 코드를 사용하지 못하게 한다고 해도 이런 스레드는 영향받지 않는다.

`threaddump` 엔드포인트로 확인할 수 있는 리액터 스레드 정보는 다음과 같다.

![image](https://user-images.githubusercontent.com/40031858/128622397-569d3b05-1d3a-4ddd-89a9-cef9980820d3.png)

스레드 정보를 확인하면 애플리케이션의 여러 단계에서 어떻게 동작하고 있는지 상세하게 조사할 수 있는 스냅샷을 얻을 수 있다. 부하가 많이 걸릴때나 부하가 별로 없을 때 각각 스레드

정보를 확인하면 애플리케이션의 상태를 스레드 수준에서 세부적으로 확인할 수 있다.

### 힙 정보 확인:/actuator/heapdump

http://localhost:8080/actuator/heapdump 에 접속해보면 브라우저가 JSON 데이터를 화면에 보여주는 대신 gzip으로 압축된 hprof파일 다운로드를 물어볼 것이다.

후에 다음명령어를 실행해보자. 

```bash
$ jhat heapdump
```

jhat 명령이 성공적으로 실행된후 localhost:7000에 접속하면 스프링 부트 액추에이터의 `ThreadDumpEndpoint` 에 의해 만들어진 heapdump파일을 확인할 수 있다.

마지막까지 훝어보면 다음과 같은 리포트 데이터를 볼 수 있다. 

- 힙 히스토그램
- 플랫폼 포함 모든 클래스의 인스턴스 개수
- 플랫폼 제외 모든 클래스의 인스턴스 개수

이정도 리포트는 기본이지만 jhat 명령이 JDK에 포함돼 있어서 JDK가 설치된 어디에서나 사용가능하다는 장점이 있다. 자바 버전 관리 도구인 sdkman을 사용하고 있다면

비쥬얼 VM은 다음과 같은 명령으로 간단하게 설치할 수 있다

```bash
$ sdk list visualvm
$ sdk install visualvm 2.0.6
$ visualvm --jdkhome $JAVA_HOME		
```

비쥬얼 VM을 실행후 다음과 같은 방법으로 힙 덤프 파일을 읽을 수 있다

1. File -> Load 클릭
2. 다운로드한 힙 덤프 파일이 있는 폴더탐색
3. 힙 덤프 파일 선택 및 열기

### HTTP 호출 트레이싱:/actuator/httptrace

스프링 부트 액추에이터는 애플리케이션을 누가 호출하는지 쉽게 볼 수 있는 기능을 제공한다. 이를 통해 다음과 같은 궁금증에 대한 답을 얻을 수 있다

- 가장 많이 사용되는 클라이언트 유형은? 모바일? 또는 특정브라우저?
- 어떤 언어로 된 요청이 가장 많은가? 
- 가장 많이 요청되는 엔드포인트는?
- 요청이 가장 많이 발생하는 지리적 위치는?

스프링 부트 액추에이터 덕분에 이런 데이터 분석을 손쉽게 할 수 있으며, 고객의 요구에 더 부합하도록 애플리케이션을 개선하는 방향을 찾아낼 수 있다.

스프링 부트는 `HTTPTraceRepository` 인터페이스를 제공하고, 이 인터페이스를 구현한 빈을 자동으로 찾아서 /actuator/httptrace 요청처리에 사용한다.

다만 어떤 구현체를 사용해서 빈으로 등록할지는 개발자가 결정해야한다. 메모리 기반으로 동작하는 InMemoryHttpTraceRepository는 간편하게 사용할 수 있는

구현체이며 다음과 같이 간단하게 빈으로 등록할 수 있다.

```java
 		@Bean
    HttpTraceRepository traceRepository(){
        return new InMemoryHttpTraceRepository();
    }
```

`HttpTraceRepository` 타입의 빈이 애플리케이션 컨텍스트에 등록되면 스프링 부트 액추에이터가 이를 감지하고 /actuator/httptrace 엔드포인트를 자동으로 활성화한다.

그리고 이를 스프링 웹플럭스와 연동해서 모든 웹 요청을 추적하고 로그를 남긴다. 이제 애플리케이션을 재시작하고 여러 요청을 발생시킨후 http://localhost:8080/actuator/httptrace

에 접속하면 지금까지 발생한 웹요청에 대한 트레이스 정보가 표시된다.

![image](https://user-images.githubusercontent.com/40031858/128622652-354ea5ba-47c6-4482-b937-70d8e58565b5.png)

이 긴 내용에는 다음과 같은 정보가 포함돼 있다.

- 타임스탬프
- 보안 상세정보
- 세션 ID
- 요청 상세정보(HTTP 메소드, URI, 헤더)
- 응답 상세정보(HTTP 상태코드, 헤더)
- 처리시간(밀리초)

이 많은 정보는 모두 InMemoryTraceRepository가 만들어낸다. 메모리 기반 레포지토리이므로 다음과 같은 특징이 있다

- 트레이스 정보는 현재 인스턴스에만 존재한다. 로드밸런서 뒤에 여러 대의 인스턴스가 존재한다면 인스턴스마다 자기 자신에게 들어온 요청에 대한 트레이스 정보가 생성된다
- 현재 인스턴스를 재시작하면 그동안의 트레이스 정보는 모두 소멸된다

개념증명 차원에서는 인메모리 트레이스 레포지토리 정도 기능임녀 충분하지만, 클라우드에서 천 대의 노드에서 수십만 사용자의 웹 요청을 추적하는데는 적합하지 않다

이럴때는 어떻게 해야할까? 다음과 같은 요구사항을 충족하는 HttpTraceRepository 구현체를 직접 만들면된다

- 애플리케이션이 재시작되더라도 트레이스 정보는 유지돼야 한다
- 모든 인스턴스에서 발생하는 트레이스 정보가 중앙화된 하나의 데이터 스토어에 저장돼야 한다

이를 만족하려면 인스턴스 외부에 있는 중앙화된 데이터베이스가 필요하다. 바로 몽고디비를 여기에 사용하자. 스프링 부트 액추에이터의 HttpTraceRepositorty는 트레이스 정보를 

HttpTrace 인스턴스에 담아서 저장한다. 하지만 불행히도 HttpTrace에는 키로 사용할 속성이 없어 몽고디비에 바로 저장할 수는없다. 게다가 HttpTrace는 final로 선언돼 있어서

상속받아 새로운 클래스를 만들어 사용할 수도없다. 따라서 HttpTrace를 감싸는 새로운 래퍼클래스를 몽고디비에 저장하자

```java
@Getter
public class HttpTraceWrapper {
    private @Id String id;
    private HttpTrace httpTrace;

    public HttpTraceWrapper(HttpTrace httpTrace) {
        this.httpTrace = httpTrace;
    }
}
```

```java
public interface HttpTraceWrapperRepository extends Repository<HttpTraceWrapper,String> {
    
    Stream<HttpTraceWrapper> findAll();
    
    void save(HttpTraceWrapper trace);
}
```

MongoRepository나 CrudRepository를 보면 Repository 마커 인터페이스를 상속해서 상당히 다양한 메소드를 정의하고 있음을 알 수 있다. 그래서 MongoRepository나

ReactiveMongoRepository를 상속받으면 미리 만들어져 있는 여러 메소드를 따로 구현할 필요없이 그대로 신속하게 데이터 접근 기능을 구현할 수 있다. 

하지만 이번처럼 특별한 시나리오에서는 꼭 필요한만큼의 간소한 API를 직접 만들수도 있다. HttpTraceWrapperRepository는 사용자의 요구사항이 아니라 애플리케이션의

요구사항을 충족하기 위해 필요하다. 이제 구현클래스를 작성하자.

```java
@RequiredArgsConstructor
public class SpringDataHttpTraceRepository implements HttpTraceRepository {
    
    private final HttpTraceWrapperRepository repository;
    
    @Override
    public List<HttpTrace> findAll() {
        return repository.findAll()
                .map(HttpTraceWrapper::getHttpTrace)
                .collect(Collectors.toList());
    }

    @Override
    public void add(HttpTrace trace) {
        repository.save(new HttpTraceWrapper(trace));
    }
}

```

이제 스프링 데이터 몽고디비 레포지토리 빈을 등록할 수 있다.

```java
		@Bean
    HttpTraceRepository springDataTraceRepository(HttpTraceWrapperRepository repository){
        return new SpringDataHttpTraceRepository(repository);
    }
```

이제 스프링 부트 애플리케이션에서 만들어지는 HttpTrace 객체를 저장할 수 있게 됐다. 하지만 저장된 HttpTrace 객체를 읽어와서 사용하려면 넘어야 할 장애물이 있다.

스프링 데이터 몽고디비는 저장된 객체 데이터를 읽어와서 HttpTrace 객체로 만들어내는 방법을 모른다. 스프링 데이터는 몽고디비 뿐 아니라 다른 데이터 저장소에 대해서도

대체로 가변 객체 패러다임을 지원한다. 가변객체 패러다임은 세터메소드로 객체의 속성값을 지정할 수 있는 방식을 말한다. 

하지만 스프링 부트 액추에이터의 HttpTrace는 불변 타입이다. 생성자를 사용해서 인스턴스를 만든 후 세터메소드로 속성값을 지정할 수 없으므로 역직렬화하는 방법이 생성자에

마련돼야한다. 잭슨은 생성자 호출을 표현할 수 있도록 애노테이션뿐 아니라 믹스인 기능도 지원한다. 하지만 HttpTrace의 생성자에 접근할 수 없으므로 다른 방법을 동원해야한다

여기서 스프링 데이터 컨버터가 사용된다.

```java
static Converter<Document, HttpTraceWrapper> CONVERTER = 
            new Converter<Document, HttpTraceWrapper>() { 
                @Override
                public HttpTraceWrapper convert(Document document) {
                    Document httpTrace = document.get("httpTrace", Document.class);
                    Document request = httpTrace.get("request", Document.class);
                    Document response = httpTrace.get("response", Document.class);

                    return new HttpTraceWrapper(new HttpTrace( 
                            new HttpTrace.Request( 
                                    request.getString("method"), 
                                    URI.create(request.getString("uri")), 
                                    request.get("headers", Map.class), 
                                    null),
                            new HttpTrace.Response( 
                                    response.getInteger("status"), 
                                    response.get("headers", Map.class)),
                            httpTrace.getDate("timestamp").toInstant(), 
                            null, 
                            null, 
                            httpTrace.getLong("timeTaken")));
                }
            };
```

이 정적 컨버터는 몽고디비  Document에서 HttpTrace 레코드를 추출하고 정보를 읽어서  HttpTraceWrapper 객체를 생성하고 반환한다. 

새 스프링 컨버터를 스프링 데이터 몽고디비에 등록하려면 `MappingMongoConverter` 빈을 생성해야한다.

```java
 @Bean
    public MappingMongoConverter mappingMongoConverter(MongoMappingContext context){
        MappingMongoConverter mappingMongoConverter=
                new MappingMongoConverter(NoOpDbRefResolver.INSTANCE,context);
        
        mappingMongoConverter.setCustomConversions(
                new MongoCustomConversions(Collections.singletonList(CONVERTER)));
        return mappingMongoConverter;

    }
```



이제 오래 구동된 인스턴스와 짧게 구동된 인스턴스 모두에서 발생한 HTTP 트레이스 데이터를 몽고디비 쿼리를 통해 분석할  수 있다.

### 그밖의 엔드포인트

지금 까지 다룬 것 외에도 액추에이터에는 다음과 같은 다양한 엔드포인트가 준비돼있다

| 액추에이터 엔드포인트 | 설명                                                         |
| --------------------- | ------------------------------------------------------------ |
| /actuator/auditevents | 감사(audit) 이벤트 표시                                      |
| /actuator/beans       | 직접 작성한 빈과 자동설정에 의해 애플리케이션 컨텍스트에 등록된 모든 빈 표시 |
| /actuator/caches      | 모든 캐시 정보 표시                                          |
| /actuator/conditions  | 스프링 부트 자동설정 기준 조건 표시                          |
| /actuator/configprops | 모든 환경설정 정보 표시                                      |
| /actuator/env         | 현재 시스템 환경 정보 표시                                   |
| /actuator/flyway      | 등록된 플라이웨이(Flyway) 데이터베이스 마이그레이션 도구 표시 |
| /actuator/mappings    | 모든 스프링 웹플럭스 경로 표시                               |
| /actuator/metrics     | 마이크로미터(micrometer) 를 사용해서 수집하는 지표(metrics) 표시 |

## 관리 서비스 경로 수정

기본으로 제공되는 경로를 사용해도 좋지만, 시스템 모니터링 에이전트에 맞도록 애플리케이션 설정을 변경해야 할 때도 있다. 이를 위해 스프링 부트 액추에이터는 엔드포인트 변경

기능을 제공한다. `application.propertries` 파일에 다음 내용을 추가하면 /actuator 대신 /manage에 접속해서 액추에이터 서비스를 활용할 수 있다.

```properties
management.endpoints.web.base-path=/manage
```

다음과 같이 지정하면 /actuator/loggers 대신 /logs에 접속해서 로거 정보를 확인할 수 있다. 

```properties
management.endpoints.web.base-path=/
management.endpoints.web.path-mapping.loggers=logs
```

## 정리

지금까지 5장에서 배운 내용은 다음과 같다

- 실행 가능한 JAR파일 생성
- 계층 기반 Dockerfile 작성 및 컨테이너 생성
- 페이키토 빌드팩을 사용해서 Dockerfile없이 컨테이너 생성
- 스프링 부트 액추에이터 추가
- 필요한 관리 서비스만 노출
- 애플리케이션 정보 및 빌드 정보 변경
- HTTP 트레이스 데이터를 몽고디비에 저장하고 조회하는 코드 작성
- 관리 서비스 경로 변경

6장 스프링 부트 API 서버 구축에서는 스프링 부트를 사용해서  API서버를 만들고 하이퍼 미디어 기능을 추가하고 애플리케이션을 문서화하는 효율적이고 효과적인 방법을 알아본다
