package com.oncetold.oncetold.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client for the Python FastAPI Cognee memory microservice.
 *
 * remember/improve/forget run @Async (fire-and-forget) since the ticket
 * workflow already saved its own data to the database and does not need to wait for
 * Cognee to finish writing — blocking on these was making ticket creation
 * and chat replies take as long as Cognee's slowest write.
 *
 * Recall stays synchronous because the bot needs prior context before replying.
 * Its bounded timeout is configurable: cloud graph queries need a larger budget
 * than the local default. On a timeout the bot can still answer without history.
 */
@Service
@Slf4j
public class MemoryClient {

    private final RestClient patientClient; // for remember/improve/forget (async, can afford to wait)
    private final RestClient recallClient;  // bounded synchronous recall for chat

    public MemoryClient(@Value("${memory.service.base-url}") String baseUrl,
                        @Value("${memory.service.recall-timeout-ms:8000}") int recallTimeoutMs) {
        SimpleClientHttpRequestFactory patientFactory = new SimpleClientHttpRequestFactory();
        patientFactory.setConnectTimeout(10_000);
        patientFactory.setReadTimeout(90_000);
        this.patientClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(patientFactory)
                .build();

        SimpleClientHttpRequestFactory fastFactory = new SimpleClientHttpRequestFactory();
        fastFactory.setConnectTimeout(5_000);
        fastFactory.setReadTimeout(Math.max(1_000, Math.min(recallTimeoutMs, 120_000)));
        this.recallClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fastFactory)
                .build();
    }

    /**
     * POST /remember — fire-and-forget, does not block the caller.
     */
    @Async
    public void remember(String customerId, String ticketId, String content) {
        try {
            patientClient.post()
                    .uri("/remember")
                    .body(Map.of("customer_id", customerId, "ticket_id", ticketId, "content", content))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Memory /remember completed for customer {} ticket {}", customerId, ticketId);
        } catch (Exception e) {
            log.warn("Memory /remember failed for customer {} ticket {}: {}", customerId, ticketId, e.getMessage());
        }
    }

    /**
     * GET /recall — synchronous with a bounded, configurable read timeout.
     * Returns null on failure so the conversation can continue.
     */
    public String recall(String customerId) {
        try {
            String result = recallClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/recall")
                            .queryParam("customer_id", customerId)
                            .build())
                    .retrieve()
                    .body(String.class);
            log.debug("Memory /recall returned for customer {}", customerId);
            return result;
        } catch (Exception e) {
            log.warn("Memory /recall failed or was too slow for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    /** Long recall for the authenticated UI; the bridge stays loopback-only. */
    public String recallDetails(String customerId) {
        String result = patientClient.get()
                .uri(uriBuilder -> uriBuilder.path("/recall")
                        .queryParam("customer_id", customerId)
                        .queryParam("query", "history").build())
                .retrieve().body(String.class);
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("Empty memory response");
        }
        return result;
    }

    /**
     * POST /improve — fire-and-forget, does not block ticket resolution.
     */
    @Async
    public void improve(String customerId, String ticketId, String resolution) {
        try {
            patientClient.post()
                    .uri("/improve")
                    .body(Map.of("customer_id", customerId, "ticket_id", ticketId, "resolution", resolution))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Memory /improve completed for customer {} ticket {}", customerId, ticketId);
        } catch (Exception e) {
            log.warn("Memory /improve failed for customer {} ticket {}: {}", customerId, ticketId, e.getMessage());
        }
    }

    /**
     * POST /forget — fire-and-forget.
     */
    @Async
    public void forget(String customerId) {
        try {
            patientClient.post()
                    .uri("/forget")
                    .body(Map.of("customer_id", customerId))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Memory /forget completed for customer {}", customerId);
        } catch (Exception e) {
            log.warn("Memory /forget failed for customer {}: {}", customerId, e.getMessage());
        }
    }
}