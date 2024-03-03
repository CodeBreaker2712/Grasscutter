package emu.grasscutter.utils.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KahnsSortTest {

    @Test
    void test_doSort() {
        List<KahnsSort.Node> nodes = Arrays.asList(
            new KahnsSort.Node(1, 2),
            new KahnsSort.Node(1, 3),
            new KahnsSort.Node(2, 4),
            new KahnsSort.Node(3, 4)
        );

        List<Integer> nodeList = Arrays.asList(1, 2, 3, 4);

        KahnsSort.Graph graph = new KahnsSort.Graph(nodes, nodeList);
        List<Integer> result = KahnsSort.doSort(graph);

        Set<Integer> expectedSet = new HashSet<>(Arrays.asList(1, 2, 3));
        assert result != null;
        Set<Integer> resultSet = new HashSet<>(result);

        assertEquals(expectedSet, resultSet);
    }

    @Test
    void test_doSort_withLoop() {
        List<KahnsSort.Node> nodesWithLoop = Arrays.asList(
            new KahnsSort.Node(1, 2),
            new KahnsSort.Node(2, 3),
            new KahnsSort.Node(3, 1)
        );

        List<Integer> nodeListWithLoop = Arrays.asList(1, 2, 3);

        KahnsSort.Graph graphWithLoop = new KahnsSort.Graph(nodesWithLoop, nodeListWithLoop);
        List<Integer> result = KahnsSort.doSort(graphWithLoop);

        assertNull(result, "The result should be null due to the presence of a loop.");
    }
}
