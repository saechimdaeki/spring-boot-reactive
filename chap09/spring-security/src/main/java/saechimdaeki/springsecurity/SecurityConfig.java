package saechimdaeki.springsecurity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;


@Configuration
public class SecurityConfig {
    // end::code[]

    @Bean
    public ReactiveUserDetailsService userDetailsService(UserRepository repository) {
        return username -> repository.findByName(username)
                .map(user -> User.withDefaultPasswordEncoder()
                        .username(user.getName())
                        .password(user.getPassword())
                        .authorities(user.getRoles().toArray(new String[0]))
                        .build());
    }

    static final String USER = "USER";
    static final String INVENTORY = "INVENTORY";

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
}
