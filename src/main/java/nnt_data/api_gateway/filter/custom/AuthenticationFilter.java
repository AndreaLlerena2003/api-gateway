package nnt_data.api_gateway.filter.custom;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import io.jsonwebtoken.security.Keys;

@Component
public class AuthenticationFilter implements GatewayFilter {

    @Value("${jwt.secret:secretKeyDefaultValueNeedsToBeAtLeast32CharactersLong}")
    private String secret;

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isOpenApiEndpoint(request)) {
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid Authorization header format");
        }

        String token = authHeader.substring(7);

        try {
            if (isTokenExpired(token)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token expired");
            }

            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            String userId = claims.get("userId", String.class);

            // Crear nueva request sin el token pero con la información necesaria
            ServerHttpRequest modifiedRequest = request.mutate()
                    .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION)) // Remueve el token
                    .header("X-Auth-User-Id", userId)
                    .header("X-Auth-Username", username)
                    .header("X-Auth-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid token: " + e.getMessage());
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    private boolean isOpenApiEndpoint(ServerHttpRequest request) {
        Predicate<ServerHttpRequest> isSecured = r -> OPEN_API_ENDPOINTS.stream()
                .noneMatch(uri -> r.getURI().getPath().contains(uri));
        return !isSecured.test(request);
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = extractAllClaims(token).getExpiration();
        return expirationDate.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}