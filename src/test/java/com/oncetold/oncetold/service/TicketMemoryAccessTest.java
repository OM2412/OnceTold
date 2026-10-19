package com.oncetold.oncetold.service;
import com.oncetold.oncetold.entity.*;
import com.oncetold.oncetold.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketMemoryAccessTest {
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final MemoryClient memory = mock(MemoryClient.class);
    private final TicketService service = new TicketService(
            tickets, mock(MessageRepository.class), memory, mock(ChatClient.class));
    @Test void ownerRecallUsesThePersistedTicketCustomer() {
        when(tickets.findById(12L)).thenReturn(Optional.of(
                Ticket.builder().id(12L).customerId(42L).build()));
        when(memory.recallDetails("42")).thenReturn("{\"memories\":[]}");
        assertEquals("{\"memories\":[]}", service.recallTicketMemory(
                12L, User.builder().id(42L).role(Role.CUSTOMER).build()));
        verify(memory).recallDetails("42");
    }
    @Test void anotherCustomerCannotReachTheMemoryBridge() {
        when(tickets.findById(12L)).thenReturn(Optional.of(
                Ticket.builder().id(12L).customerId(42L).build()));
        assertThrows(AccessDeniedException.class, () -> service.recallTicketMemory(
                12L, User.builder().id(99L).role(Role.CUSTOMER).build()));
        verifyNoInteractions(memory);
    }
    @Test void agentRecallStillUsesTheTicketOwnerRatherThanTheAgent() {
        when(tickets.findById(12L)).thenReturn(Optional.of(
                Ticket.builder().id(12L).customerId(42L).build()));
        when(memory.recallDetails("42")).thenReturn("{\"memories\":[]}");
        service.recallTicketMemory(12L, User.builder().id(7L).role(Role.AGENT).build());
        verify(memory).recallDetails("42");
    }
}
