package com.olziedev.skillranks.rank.range;

public final class CompiledRange {

    private enum Type { EXACT, BETWEEN, GT, GTE, LT, LTE }

    private final Type type;
    private final int a;
    private final int b;

    private CompiledRange(Type type, int a, int b) {
        this.type = type;
        this.a = a;
        this.b = b;
    }

    /**
     * Parses a range expression (">N", ">=N", "<N", "<=N", "N", "N-M").
     * Returns null if the expression is malformed or the bounds are
     * reversed (min > max) - callers are expected to warn.
     */
    public static CompiledRange parse(String raw) {
        if (raw == null) return null;

        String r = raw.trim().replace(" ", "");
        if (r.isEmpty()) return null;

        try {
            if (r.startsWith(">=")) return new CompiledRange(Type.GTE, Integer.parseInt(r.substring(2)), 0);
            if (r.startsWith(">"))  return new CompiledRange(Type.GT,  Integer.parseInt(r.substring(1)), 0);
            if (r.startsWith("<=")) return new CompiledRange(Type.LTE, Integer.parseInt(r.substring(2)), 0);
            if (r.startsWith("<"))  return new CompiledRange(Type.LT,  Integer.parseInt(r.substring(1)), 0);

            // "N-M" - search from index 1 so a leading '-' is read as a
            // negative sign, not a separator (e.g. "-10--5").
            int dash = r.indexOf('-', 1);
            if (dash > 0) {
                int min = Integer.parseInt(r.substring(0, dash));
                int max = Integer.parseInt(r.substring(dash + 1));
                if (min > max) return null;
                return new CompiledRange(Type.BETWEEN, min, max);
            }

            // exact number
            return new CompiledRange(Type.EXACT, Integer.parseInt(r), 0);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean contains(int n) {
        return switch (type) {
            case EXACT   -> n == a;
            case BETWEEN -> n >= a && n <= b;
            case GT      -> n > a;
            case GTE     -> n >= a;
            case LT      -> n < a;
            case LTE     -> n <= a;
        };
    }

    @Override
    public String toString() {
        return "CompiledRange{type=" + type + ", a=" + a + ", b=" + b + "}";
    }
}
