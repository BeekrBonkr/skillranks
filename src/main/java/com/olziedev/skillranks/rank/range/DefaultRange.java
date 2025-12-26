package com.olziedev.skillranks.rank.range;

import java.util.Arrays;

public class DefaultRange extends RangeParser {

    @Override
    public boolean isInRange(int number, String range) {
        String[] split = range.split(this.getCharacter());
        if (split.length == 1) {
            return getAllNumbers(range) == number;
        }
        return number >= Integer.parseInt(split[0]) && number <= Integer.parseInt(split[1]);
    }

    @Override
    public String getCharacter() {
        return "-";
    }
}
