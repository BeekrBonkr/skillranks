package com.olziedev.skillranks.rank.range;

import java.util.Arrays;
import java.util.List;

public abstract class RangeParser {

    public static final List<RangeParser> RANGE_PARSERS = Arrays.asList(new DefaultRange(), new BiggerRange(), new LessRange());

    public abstract boolean isInRange(int number, String range);

    public abstract String getCharacter();

    protected int getAllNumbers(String range) {
        return Integer.parseInt(range.replaceAll("[^0-9]", ""));
    }

    protected String getSymbols(String range) {
        return range.replaceAll("[0-9]", "");
    }

    public static RangeParser getRangeParser(String range) {
        for (RangeParser rangeParser : RANGE_PARSERS) {
            String character = rangeParser.getCharacter();
            if (range.contains(character)) {
                return rangeParser;
            }
        }
        return new DefaultRange();
    }
}
