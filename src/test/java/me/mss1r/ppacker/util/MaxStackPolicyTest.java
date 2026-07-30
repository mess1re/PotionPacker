package me.mss1r.ppacker.util;

import org.junit.jupiter.api.Test;

import static me.mss1r.ppacker.util.MaxStackPolicy.Operation.NONE;
import static me.mss1r.ppacker.util.MaxStackPolicy.Operation.RESET;
import static me.mss1r.ppacker.util.MaxStackPolicy.Operation.SET;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MaxStackPolicyTest {

    @Test
    void setsConfiguredSizeWhenComponentIsMissing() {
        assertDecision(MaxStackPolicy.configuredSize(16, 1, null), SET, 16);
    }

    @Test
    void replacesDifferentConfiguredSize() {
        assertDecision(MaxStackPolicy.configuredSize(32, 1, 16), SET, 32);
    }

    @Test
    void keepsMatchingConfiguredSize() {
        assertDecision(MaxStackPolicy.configuredSize(16, 1, 16), NONE, 0);
    }

    @Test
    void resetsCustomComponentForVanillaSize() {
        assertDecision(MaxStackPolicy.configuredSize(1, 1, 16), RESET, 0);
    }

    @Test
    void leavesVanillaItemWithoutComponentUntouched() {
        assertDecision(MaxStackPolicy.configuredSize(1, 1, null), NONE, 0);
    }

    @Test
    void treatsNonPositiveConfiguredSizeAsVanilla() {
        assertDecision(MaxStackPolicy.configuredSize(0, 1, 16), RESET, 0);
    }

    @Test
    void raisesComponentToPreserveOverstackedAmount() {
        assertDecision(MaxStackPolicy.preserveAmount(32, 16), SET, 32);
        assertDecision(MaxStackPolicy.preserveAmount(32, null), SET, 32);
    }

    @Test
    void neverLowersComponentWhenPreservingAmount() {
        assertDecision(MaxStackPolicy.preserveAmount(16, 32), NONE, 0);
        assertDecision(MaxStackPolicy.preserveAmount(32, 32), NONE, 0);
        assertDecision(MaxStackPolicy.preserveAmount(0, null), NONE, 0);
    }

    private void assertDecision(
            MaxStackPolicy.Decision decision,
            MaxStackPolicy.Operation operation,
            int value
    ) {
        assertEquals(operation, decision.operation());
        assertEquals(value, decision.value());
    }
}
