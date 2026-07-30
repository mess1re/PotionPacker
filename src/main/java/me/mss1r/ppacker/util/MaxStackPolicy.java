package me.mss1r.ppacker.util;

final class MaxStackPolicy {
    private MaxStackPolicy() {}

    static Decision configuredSize(int desired, int vanilla, Integer current) {
        if (desired <= 0 || desired == vanilla) {
            return current == null ? Decision.none() : Decision.reset();
        }
        return current == null || current != desired
                ? Decision.set(desired)
                : Decision.none();
    }

    static Decision preserveAmount(int amount, Integer current) {
        if (amount <= 0 || current != null && current >= amount) {
            return Decision.none();
        }
        return Decision.set(amount);
    }

    enum Operation {
        NONE,
        SET,
        RESET
    }

    record Decision(Operation operation, int value) {
        static Decision none() {
            return new Decision(Operation.NONE, 0);
        }

        static Decision set(int value) {
            return new Decision(Operation.SET, value);
        }

        static Decision reset() {
            return new Decision(Operation.RESET, 0);
        }
    }
}
