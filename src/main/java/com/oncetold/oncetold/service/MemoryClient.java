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
 * workflow already saved its own data to H2 and does not need to wait for
 * Cognee to finish writing — blocking on these was making ticket creation
 * and chat replies take as long as Cognee's slowest write.
 *
 * recall stays synchronous (the bot needs *something* back before it can
 * reply) but uses a short, fast-fail timeout — Cognee's graph queries can
 * take up to ~90s, which is far too long to make a customer wait mid-chat.
 * If it doesn't return in time, the bot just replies without deep history
 * for that turn rather than freezing the conversation.
 */
@Service
@Slf4j
public class MemoryClient {

    private final RestClient patientClient; // for remember/improve/forget (async, can afford to wait)
    private final RestClient fastClient;    // for recall (blocks chat reply, must fail fast)

    public MemoryClient(@Value("${memory.service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory patientFactory = new SimpleClientHttpRequestFactory();
        patientFactory.setConnectTimeout(10_000);
        patientFactory.setReadTimeout(90_000);
        this.patientClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(patientFactory)
                .build();

        SimpleClientHttpRequestFactory fastFactory = new SimpleClientHttpRequestFactory();
        fastFactory.setConnectTimeout(5_000);
        fastFactory.setReadTimeout(8_000);
        this.fastClient = RestClient.builder()
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
     * GET /recall — synchronous, fast-fail. Returns null quickly rather than
     * blocking a chat reply if Cognee is slow.
     */
    public String recall(String customerId) {
        try {
            String result = fastClient.get()
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