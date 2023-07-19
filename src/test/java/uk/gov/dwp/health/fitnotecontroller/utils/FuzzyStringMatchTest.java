package uk.gov.dwp.health.fitnotecontroller.utils;

import uk.gov.dwp.health.fitnotecontroller.exception.FuzzyStringMatchException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FuzzyStringMatchTest {
    private static final String NINTY_PERCENT_MATCH_BIG_STRING = "KlMnOpQrAbcDe1234X67890fGhIj";
    private static final String NINTY_PERCENT_MATCH_SAME_LENGTH = "XbcDe12345";
    private static final String SEVENTY_PERCENT_MATCH_SAME_LENGTH = "XXXDe12345";

    private static final String STRING_TO_FIND = "AbcDe12345";
    private static final int PERCENTAGE_HAPPY_WITH = 90;

    @Test
    public void validateFuzzyStringContainsReturns100WithSingleMatchChar() throws FuzzyStringMatchException {
        assertThat("Expecting 100% match", FuzzyStringMatch.fuzzyStringContains("a", "a"), is(equalTo(100)));
    }

    @Test(expected = FuzzyStringMatchException.class)
    public void validateFuzzyStringContainsThrowsExceptionWhenGivenNull() throws FuzzyStringMatchException {
        FuzzyStringMatch.fuzzyStringContains(null, "lookingFor");
    }

    @Test(expected = FuzzyStringMatchException.class)
    public void validateFuzzyStringContainsThrowsExceptionWhenGivenSecondNull() throws FuzzyStringMatchException {
        FuzzyStringMatch.fuzzyStringContains("inspected", null);
    }

    @Test
    public void validateFuzzyStringValidReturnsTrueGiven70PctString() throws FuzzyStringMatchException {
        assertTrue(FuzzyStringMatch.fuzzyStringValid(SEVENTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND, 70));
    }

    @Test
    public void validateFuzzyStringValidReturnsFalseGiven71Char70PctString() throws FuzzyStringMatchException {
        assertFalse("70% string should fail 71% threshold", FuzzyStringMatch.fuzzyStringValid(SEVENTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND, 71));
    }

    @Test
    public void validateFuzzyStringValidReturnsTRUEGiven90PctSameLengthString() throws FuzzyStringMatchException {
        assertTrue(FuzzyStringMatch.fuzzyStringValid(NINTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND, PERCENTAGE_HAPPY_WITH));
    }

    @Test
    public void validateFuzzyStringValidReturnsTRUEGiven90PctBigString() throws FuzzyStringMatchException {
        assertTrue(FuzzyStringMatch.fuzzyStringValid(NINTY_PERCENT_MATCH_BIG_STRING, STRING_TO_FIND, PERCENTAGE_HAPPY_WITH));
    }

    @Test
    public void validateFuzzyStringContainsGivenKlMnOpQrAbcDe1234S67890FGhIj() throws FuzzyStringMatchException {
        assertEquals(90, FuzzyStringMatch.fuzzyStringContains(NINTY_PERCENT_MATCH_BIG_STRING, STRING_TO_FIND));
    }//this contains a string that matches 90%

    @Test
    public void validateFuzzyStringContainsGivenStuff() throws FuzzyStringMatchException {
        assertEquals(75, FuzzyStringMatch.fuzzyStringContains("aaccccaabbccaaaabbccaabcdaaaabbbbccbbbbbc", "ebcd"));
    }//this contains a string that matches 90%

    @Test
    public void validateFuzzyStringsMatch90GivenAbcDe12340() throws FuzzyStringMatchException {
        assertEquals(90, FuzzyStringMatch.fuzzyStringsMatch(NINTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND));
    }

    @Test
    public void validateFuzzyStringsMatch70GivenAbcDe12340() throws FuzzyStringMatchException {
        assertThat(FuzzyStringMatch.fuzzyStringsMatch(SEVENTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND), is(equalTo(70)));
    }

    @Test
    public void verifyIsValidStringReturnsFalseGivenNull() {
        assertFalse(FuzzyStringMatch.isValidString(null));
    }

    @Test
    public void verifyIsValidStringReturnsFalseGivenEmptyString() {
        assertFalse(FuzzyStringMatch.isValidString(""));
    }

    @Test
    public void verifyIsValidStringReturnsFalseGivenDoubleQuotes() {
        assertFalse(FuzzyStringMatch.isValidString(""));
    }

    @Test
    public void verifyIsValidStringReturnsTrueGivenABC() {
        assertTrue(FuzzyStringMatch.isValidString("ABC"));
    }

    @Test
    public void verifyReplaceInvalidCharactersReturnsFLOSSWhenGivenFlosCharacter() throws FuzzyStringMatchException {
        assertEquals("Floss", FuzzyStringMatch.replaceInvalidCharacters("Floß "));
    }

    @Test
    public void verifyReplaceInvalidCharactersReturnsFitnoteWhenGivenFitnoteAccent() throws FuzzyStringMatchException {
        assertEquals("Fitnote", FuzzyStringMatch.replaceInvalidCharacters("Fitnoté"));
    }

    @Test
    public void verifyReplaceInvalidCharacters() throws FuzzyStringMatchException {
        assertEquals("Fitn te", FuzzyStringMatch.replaceInvalidCharacters("Fitn%té"));
    }

    @Test
    public void testPhraseMatchingInScanFromExample() throws FuzzyStringMatchException {
        int scanMatchNew = FuzzyStringMatch.fuzzyStringContains("  \ndsgfhdhkjesdfghj    \tmary hid _ little limb _?!##wsdfghytrdefg".toUpperCase(), "MARY HAD A LITTLE LAMB");
        assertThat("should be 92 percent", scanMatchNew, is(equalTo(83)));
    }
}
