package com.instagram.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Liveness probe so we can confirm the gateway boots. Not under /api, so it is
 *  served locally and never proxied downstream. */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}
