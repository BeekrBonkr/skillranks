package com.olziedev.skillranks.rank.range;

public class BiggerRange extends RangeParser {

    @Override
    public boolean isInRange(int number, String range) {
        int allNumbers = getAllNumbers(range);
        String symbols = getSymbols(range);
        if (symbols.equals(">=")) {
            return number >= allNumbers;
        }
        return number > allNumbers;
    }

    @Override
    public String getCharacter() {
        return ">";
    }
}
