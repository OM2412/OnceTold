package com.oncetold.oncetold.dto;

import com.oncetold.oncetold.entity.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private Long customerId;
    private String subject;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private List<MessageResponse> messages;
}
