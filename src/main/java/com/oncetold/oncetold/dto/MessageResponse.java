package com.oncetold.oncetold.dto;

import com.oncetold.oncetold.entity.SenderType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long ticketId;
    private SenderType sender;
    private String content;
    private String imageData;
    private LocalDateTime createdAt;
}
