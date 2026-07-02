package com.oncetold.oncetold.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveTicketRequest {

    @NotBlank(message = "Resolution is required")
    private String resolution;
}
