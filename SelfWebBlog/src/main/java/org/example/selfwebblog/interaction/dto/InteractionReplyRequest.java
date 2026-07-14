package org.example.selfwebblog.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InteractionReplyRequest {
    @NotNull
    private Long replyToId;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
