package emu.grasscutter.utils.algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.Set;

public class MersenneTwister64Test {

    @Test
    public void test_nextLong() {
        MersenneTwister64 mt = new MersenneTwister64();

        mt.setSeed(12345L);

        int numValues = 1000;
        Set<Long> generatedValues = new HashSet<>();

        for (int i = 0; i < numValues; i++) {
            long value = mt.nextLong();
            assertFalse(generatedValues.contains(value), "Duplicate value found at index " + i);
            generatedValues.add(value);
        }
    }
}
