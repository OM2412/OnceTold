package com.oncetold.oncetold.service;

import org.springframework.ai.chat.client.ChatClient;
import com.oncetold.oncetold.dto.CreateTicketRequest;
import com.oncetold.oncetold.dto.MessageResponse;
import com.oncetold.oncetold.dto.PostMessageRequest;
import com.oncetold.oncetold.dto.TicketResponse;
import com.oncetold.oncetold.entity.*;
import com.oncetold.oncetold.repository.MessageRepository;
import com.oncetold.oncetold.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final MemoryClient memoryClient;
    private final ChatClient chatClient;

    // ── Create Ticket ──────────────────────────────────────────────────────────

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, User currentUser) {
        Ticket ticket = Ticket.builder()
                .customerId(currentUser.getId())
                .subject(request.getSubject())
                .build();

        Ticket saved = ticketRepository.save(ticket);

        // Inform the memory service a new ticket was opened
        memoryClient.remember(
                String.valueOf(currentUser.getId()),
                String.valueOf(saved.getId()),
                "New ticket opened: " + request.getSubject()
        );

        return toTicketResponse(saved, List.of());
    }

    // ── List Tickets ───────────────────────────────────────────────────────────

    public List<TicketResponse> getTickets(User currentUser) {
        List<Ticket> tickets = currentUser.getRole() == Role.AGENT
                ? ticketRepository.findAll()
                : ticketRepository.findByCustomerId(currentUser.getId());

        return tickets.stream()
                .map(t -> toTicketResponse(t, List.of()))
                .toList();
    }

    // ── Get Ticket Detail ──────────────────────────────────────────────────────

    public TicketResponse getTicketById(Long ticketId, User currentUser) {
        Ticket ticket = findTicketAndCheckAccess(ticketId, currentUser);
        List<MessageResponse> messages = messageRepository
                .findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
        return toTicketResponse(ticket, messages);
    }

    // ── Post Message ───────────────────────────────────────────────────────────

    @Transactional
public MessageResponse postMessage(Long ticketId, PostMessageRequest request, User currentUser) {
    Ticket ticket = findTicketAndCheckAccess(ticketId, currentUser);

    if (ticket.getStatus() == TicketStatus.RESOLVED) {
        throw new IllegalStateException("Cannot post to a resolved ticket");
    }

    boolean hasText = request.getContent() != null && !request.getContent().isBlank();
    boolean hasImage = request.getImageData() != null && !request.getImageData().isBlank();

    if (!hasText && !hasImage) {
        throw new IllegalArgumentException("Message must contain text or an image");
    }

    SenderType senderType = currentUser.getRole() == Role.AGENT
            ? SenderType.AGENT
            : SenderType.CUSTOMER;

    Message message = Message.builder()
            .ticketId(ticketId)
            .sender(senderType)
            .content(hasText ? request.getContent() : "")
            .imageData(hasImage ? request.getImageData() : null)
            .build();
    messageRepository.save(message);

    // Never send raw base64 image data into Cognee or the LLM prompt — it's
    // meaningless to both, wastes tokens/cost, and can blow past context
    // limits. Substitute a short, human-readable placeholder instead.
    String memorySafeText = hasImage
            ? (hasText ? request.getContent() + " [image attached]" : "[Customer sent an image]")
            : request.getContent();

    // Store in Cognee session memory
    memoryClient.remember(
            String.valueOf(ticket.getCustomerId()),
            String.valueOf(ticketId),
            "[" + senderType + "] " + memorySafeText
    );

    // Generate bot reply only for CUSTOMER messages
    if (senderType == SenderType.CUSTOMER) {
        log.info("[Bot Reply] Reached postMessage for CUSTOMER. Ticket ID: {}, Customer ID: {}", ticketId, ticket.getCustomerId());
        try {
            log.info("[Bot Reply] Querying memory recall for Customer ID: {}", ticket.getCustomerId());
            String recalled = memoryClient.recall(String.valueOf(ticket.getCustomerId()));
            log.info("[Bot Reply] Memory recalled context: {}", recalled);
            
            String prompt = """
                You are OnceTold, a friendly, concise, and solution-focused support agent with memory.
                
                Customer history from previous interactions:
                %s
                
                Current ticket subject: %s
                Customer message: %s
                
                Instructions:
                - If memories/history are empty, unavailable, or says "No previous history", warmly greet the customer and ask how you can help. Do NOT mention that no history exists or that this is their first time reaching out.
                - If memories exist, naturally reference them in the conversation without saying "I recall our previous conversation" or "as we discussed before" — just use the context naturally.
                - Never tell the customer you don't have their history.
                - If the customer sent an image, acknowledge it naturally and ask for any details you need — you cannot see the image contents.
                - Keep responses friendly, concise (under 100 words), and solution-focused.
                """.formatted(
                    recalled != null && !recalled.trim().isEmpty() ? recalled : "No previous history",
                    ticket.getSubject(),
                    memorySafeText
            );

            log.info("[Bot Reply] Invoking ChatClient prompt call...");
            String botReply = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("[Bot Reply] ChatClient response success: {}", botReply);

            Message botMessage = Message.builder()
                    .ticketId(ticketId)
                    .sender(SenderType.BOT)
                    .content(botReply)
                    .build();
            messageRepository.save(botMessage);
            log.info("[Bot Reply] Bot message successfully saved to database.");

        } catch (Exception e) {
            log.error("[Bot Reply] Exception occurred while generating bot reply for ticket " + ticketId, e);
        }
    }

    return toMessageResponse(message);
}
    // ── Resolve Ticket ─────────────────────────────────────────────────────────

    @Transactional
    public TicketResponse resolveTicket(Long ticketId, String resolution) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new IllegalStateException("Ticket is already resolved");
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);

        // Promote to permanent graph memory in Cognee using the agent's resolution summary
        memoryClient.improve(
                String.valueOf(ticket.getCustomerId()),
                String.valueOf(ticketId),
                resolution
        );

        List<MessageResponse> messages = messageRepository
                .findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return toTicketResponse(saved, messages);
    }

    public List<TicketResponse> getAllTickets(User currentUser) {
        if (currentUser.getRole() != Role.AGENT) {
            throw new AccessDeniedException("Only agents can retrieve all tickets");
        }
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .map(t -> toTicketResponse(t, List.of()))
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Ticket findTicketAndCheckAccess(Long ticketId, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Ticket not found: " + ticketId));

        // Customers can only access their own tickets
        if (currentUser.getRole() == Role.CUSTOMER
                && !ticket.getCustomerId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this ticket");
        }

        return ticket;
    }

    private TicketResponse toTicketResponse(Ticket ticket, List<MessageResponse> messages) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .customerId(ticket.getCustomerId())
                .subject(ticket.getSubject())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .messages(messages)
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicketId())
                .sender(message.getSender())
                .content(message.getContent())
                .imageData(message.getImageData())
                .createdAt(message.getCreatedAt())
                .build();
    }
}