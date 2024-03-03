package emu.grasscutter.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    @Test
    public void test_randomRange() {
        int min = 5;
        int max = 10;
        int result = Utils.randomRange(min, max);
        assertTrue(result >= min && result <= max, "Result should be within the specified range");
    }

    @Test
    public void test_randomFloatRange() {
        float min = 2.5f;
        float max = 5.5f;
        float result = Utils.randomFloatRange(min, max);
        assertTrue(result >= min && result <= max, "Result should be within the specified range");
    }

    @Test
    public void test_lerp() {
        int x = 7;
        int[][] xyArray = {
            {5, 10},
            {8, 20},
            {10, 30}
        };

        int result = Utils.lerp(x, xyArray);

        assertEquals(16, result, "Linear interpolation should be accurate");

        int x1 = 5;
        int x2 = 10;

        int result1 = Utils.lerp(x1, xyArray);
        int result2 = Utils.lerp(x2, xyArray);

        assertEquals(10, result1, "Result should be equal to the first point's y value");
        assertEquals(30, result2, "Result should be equal to the last point's y value");
    }

    @Test
    public void test_lerp_clampToFirstPoint() {

        int x = 3;
        int[][] xyArray = {
            {5, 10},
            {8, 20},
            {10, 30}
        };

        int result = Utils.lerp(x, xyArray);

        assertEquals(10, result, "Result should be equal to the first point's y value");
    }

    @Test
    public void test_lerp_clampToLastPoint() {
        int x = 12;
        int[][] xyArray = {
            {5, 10},
            {8, 20},
            {10, 30}
        };
        int result = Utils.lerp(x, xyArray);
        assertEquals(30, result, "Result should be equal to the last point's y value");
    }
}
