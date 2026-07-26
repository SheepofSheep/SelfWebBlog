package org.example.selfwebblog.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InteractionReplyRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;
}
