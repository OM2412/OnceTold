package com.oncetold.oncetold.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client for the Python FastAPI Cognee memory microservice running on port 8000.
 * Calls are best-effort — failures are logged but never propagate to the caller so that
 * a memory service outage does not break the ticket workflow.
 */
@Service
@Slf4j
public class MemoryClient {

    private final RestClient restClient;

    public MemoryClient(@Value("${memory.service.base-url}") String baseUrl) {
        // Cognee Cloud's real graph completion queries can legitimately take up to
        // ~70s. Without an explicit timeout here, RestClient's underlying HTTP
        // client defaults to a 10s read timeout, silently killing every recall/
        // remember call before Cognee finishes — this was the actual bug.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(90_000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public void remember(String customerId, String ticketId, String content) {
        try {
            restClient.post()
                    .uri("/remember")
                    .body(Map.of("customer_id", customerId, "ticket_id", ticketId, "content", content))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Memory /remember called for customer {} ticket {}", customerId, ticketId);
        } catch (Exception e) {
            log.warn("Memory /remember failed for customer {} ticket {}: {}", customerId, ticketId, e.getMessage());
        }
    }

    public String recall(String customerId) {
        try {
            String result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/recall")
                            .queryParam("customer_id", customerId)
                            .build())
                    .retrieve()
                    .body(String.class);
            log.debug("Memory /recall returned for customer {}", customerId);
            return result;
        } catch (Exception e) {
            log.warn("Memory /recall failed for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    public void improve(String customerId, String ticketId, String resolution) {
        try {
            restClient.post()
                    .uri("/improve")
                    .body(Map.of("customer_id", customerId, "ticket_id", ticketId, "resolution", resolution))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Memory /improve called for customer {} ticket {}", customerId, ticketId);
        } catch (Exception e) {
            log.warn("Memory /improve failed for customer {} ticket {}: {}", customerId, ticketId, e.getMessage());
        }
    }

    public void forget(String customerId) {
        try {
            restClient.post()
                    .uri("/forget")
                    .body(Map.of("customer_id", customerId))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Memory /forget called for customer {}", customerId);
        } catch (Exception e) {
            log.warn("Memory /forget failed for customer {}: {}", customerId, e.getMessage());
        }
    }
}