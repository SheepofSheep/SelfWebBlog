package org.example.selfwebblog.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InteractionThreadResponse {
    private List<InteractionItemResponse> items;
    private long total;
    private int page;
    private int size;
}
