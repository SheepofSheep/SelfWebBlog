package org.example.selfwebblog.interaction;

import org.example.selfwebblog.interaction.model.Interaction;
import org.example.selfwebblog.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionServiceTest {

    @Test
    void repliesAlwaysResolveToOneRootLevel() {
        Interaction root = interaction(10L, null, "POST", 7L);
        Interaction reply = interaction(11L, 10L, "POST", 7L);

        assertEquals(10L, InteractionService.resolveRootId(root));
        assertEquals(10L, InteractionService.resolveRootId(reply));
    }

    @Test
    void replyCannotCrossInteractionTargets() {
        Interaction root = interaction(10L, null, "POST", 7L);

        assertThrows(IllegalArgumentException.class,
                () -> InteractionService.requireSameTarget(root, "POST", 8L));
    }

    @Test
    void moderationUsesAnExplicitStateMachine() {
        assertTrue(InteractionPolicy.canTransition("PENDING", "PUBLISHED"));
        assertTrue(InteractionPolicy.canTransition("PUBLISHED", "HIDDEN"));
        assertFalse(InteractionPolicy.canTransition("PUBLISHED", "PENDING"));
        assertFalse(InteractionPolicy.canTransition("SPAM", "PUBLISHED"));
    }

    @Test
    void anonymousVisitorsCannotReply() {
        assertThrows(IllegalArgumentException.class,
                () -> InteractionService.requireAuthenticatedReply(null, 10L));
        InteractionService.requireAuthenticatedReply(new User(), 10L);
    }

    private Interaction interaction(Long id, Long rootId, String targetType, Long targetId) {
        Interaction interaction = new Interaction();
        interaction.setId(id);
        interaction.setRootId(rootId);
        interaction.setTargetType(targetType);
        interaction.setTargetId(targetId);
        return interaction;
    }
}
