package org.example.selfwebblog.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InteractionCreateRequest {
    @NotBlank
    @Pattern(regexp = "POST|GUESTBOOK")
    private String targetType;

    @NotNull
    private Long targetId;

    private Long replyToId;

    @NotBlank
    @Size(max = 2000)
    private String content;

    @Size(max = 30)
    private String guestName;

    @Size(max = 80)
    private String captchaId;

    @Size(max = 12)
    private String captchaAnswer;

    @Size(max = 0)
    private String website;
}
