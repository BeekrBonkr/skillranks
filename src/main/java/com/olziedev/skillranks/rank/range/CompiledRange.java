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

    public static CompiledRange parse(String raw) {
        if (raw == null) return new CompiledRange(Type.EXACT, 0, 0);

        String r = raw.trim().replace(" ", "");

        try {
            if (r.startsWith(">=")) return new CompiledRange(Type.GTE, Integer.parseInt(r.substring(2)), 0);
            if (r.startsWith(">"))  return new CompiledRange(Type.GT,  Integer.parseInt(r.substring(1)), 0);
            if (r.startsWith("<=")) return new CompiledRange(Type.LTE, Integer.parseInt(r.substring(2)), 0);
            if (r.startsWith("<"))  return new CompiledRange(Type.LT,  Integer.parseInt(r.substring(1)), 0);

            int dash = r.indexOf('-');
            if (dash > 0) {
                int min = Integer.parseInt(r.substring(0, dash));
                int max = Integer.parseInt(r.substring(dash + 1));
                return new CompiledRange(Type.BETWEEN, min, max);
            }

            // exact number
            return new CompiledRange(Type.EXACT, Integer.parseInt(r), 0);
        } catch (NumberFormatException ex) {
            // invalid config range; treat as never matching
            return new CompiledRange(Type.EXACT, Integer.MIN_VALUE, 0);
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
