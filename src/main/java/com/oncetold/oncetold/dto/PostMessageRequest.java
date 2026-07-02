package com.oncetold.oncetold.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostMessageRequest {

    @NotBlank(message = "Message content is required")
    private String content;
}
