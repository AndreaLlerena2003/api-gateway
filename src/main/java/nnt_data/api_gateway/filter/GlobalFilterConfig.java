package nnt_data.api_gateway.filter;

import nnt_data.api_gateway.filter.custom.AuthenticationFilter;
import nnt_data.api_gateway.filter.custom.LoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalFilterConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private LoggingFilter loggingFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("customer-service-main", r -> r.path("/customers/**")
                        .filters(f -> f.stripPrefix(0)
                                .filter(authenticationFilter))
                        .uri("lb://CUSTOMER-MICROSERVICE"))
                .route("bank-account-service", r -> r.path("/accounts/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://BANKACCOUNT-MICROSERVICE"))
                .route("debit-card-service", r -> r.path("/debitCard/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://BANKACCOUNT-MICROSERVICE"))
                .route("credit-service", r -> r.path("/credits/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://CREDIT-SERVICE"))
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://AUTH-MICROSERVICE"))
                .route("yanki-service", r -> r.path("/yanki/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://YANKI-MICROSERVICE"))
                .build();
    }
}