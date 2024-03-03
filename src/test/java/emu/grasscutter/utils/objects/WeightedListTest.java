package emu.grasscutter.utils.objects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeightedListTest {

    @Test
    void test_weightedList() {
        WeightedList<Integer> weightedList = new WeightedList<>();

        weightedList.add(1.0, 1)
            .add(2.0, 2)
            .add(0.5, 3);

        assertEquals(3, weightedList.size());

        Integer result = weightedList.next();
        assertNotNull(result);
    }
}
