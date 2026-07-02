package com.oncetold.oncetold.controller;

import com.oncetold.oncetold.dto.CreateTicketRequest;
import com.oncetold.oncetold.dto.MessageResponse;
import com.oncetold.oncetold.dto.PostMessageRequest;
import com.oncetold.oncetold.dto.TicketResponse;
import com.oncetold.oncetold.dto.ResolveTicketRequest;
import com.oncetold.oncetold.entity.User;
import com.oncetold.oncetold.repository.UserRepository;
import com.oncetold.oncetold.service.TicketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    // ── Resolve the UserDetails principal back to our User entity ─────────────

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    // ── POST /api/tickets ─────────────────────────────────────────────────────
    // Create a new ticket (CUSTOMER role recommended; AGENTs can too)

    @PostMapping
    public ResponseEntity<?> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            User user = resolveUser(principal);
            TicketResponse response = ticketService.createTicket(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/tickets ──────────────────────────────────────────────────────
    // CUSTOMER sees own tickets; AGENT sees all

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(ticketService.getTickets(user));
    }

    // ── GET /api/tickets/{id} ─────────────────────────────────────────────────
    // Returns ticket + all messages

    @GetMapping("/{id}")
    public ResponseEntity<?> getTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            User user = resolveUser(principal);
            return ResponseEntity.ok(ticketService.getTicketById(id, user));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ── POST /api/tickets/{id}/messages ──────────────────────────────────────
    // Post a message to an open ticket; also calls /remember on memory service

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> postMessage(
            @PathVariable Long id,
            @Valid @RequestBody PostMessageRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            User user = resolveUser(principal);
            MessageResponse response = ticketService.postMessage(id, request, user);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── PUT /api/tickets/{id}/resolve ─────────────────────────────────────────
    // AGENT only (enforced in SecurityConfig); also calls /improve on memory service

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody ResolveTicketRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            TicketResponse response = ticketService.resolveTicket(id, request.getResolution());
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/tickets/all ──────────────────────────────────────────────────
    // AGENT only (enforced in SecurityConfig) - returns all tickets

    @GetMapping("/all")
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(ticketService.getAllTickets(user));
    }
}
