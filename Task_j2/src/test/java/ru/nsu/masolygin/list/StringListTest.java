package ru.nsu.masolygin.list;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nsu.masolygin.sort.SortDelay;
import ru.nsu.masolygin.sort.SortWorkers;

@Timeout(15)
class StringListTest {
    @ParameterizedTest
    @ValueSource(strings = {"linked", "array"})
    void testAddFirstAndSnapshot(String implementation) {
        StringList list = create(implementation);
        list.addFirst("a");
        list.addFirst("b");
        var snapshot = list.iterator();
        list.addFirst("c");
        List<String> previous = new ArrayList<>();
        snapshot.forEachRemaining(previous::add);
        assertEquals(List.of("b", "a"), previous);
        assertEquals(List.of("c", "b", "a"), values(list));
    }

    @ParameterizedTest
    @ValueSource(strings = {"linked", "array"})
    void testPartialPassAndSortedListStillTakeFullSteps(String implementation) throws InterruptedException {
        StringList list = create(implementation);
        list.addFirst("a");
        list.addFirst("b");
        list.addFirst("c");
        SortDelay delay = new SortDelay(0, 0);
        assertEquals(2, list.sortPass(delay));
        assertEquals(List.of("b", "a", "c"), values(list));
        assertEquals(2, list.sortPass(delay));
        assertEquals(List.of("a", "b", "c"), values(list));
        assertEquals(2, list.sortPass(delay));
        assertEquals(6, list.steps());
    }

    @ParameterizedTest
    @ValueSource(strings = {"linked", "array"})
    void testConcurrentPassesKeepAllRecordsAndExactStepCount(String implementation) throws InterruptedException {
        StringList list = create(implementation);
        for (int index = 0; index < 12; index++) {
            list.addFirst(String.format("%02d", index));
        }
        try (SortWorkers workers = new SortWorkers(list, new SortDelay(0, 0), 4, 12)) {
            workers.start();
            workers.await();
        }
        assertEquals(4L * 12 * 11, list.steps());
        List<String> expected = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            expected.add(String.format("%02d", index));
        }
        assertEquals(expected, values(list));
    }

    @ParameterizedTest
    @ValueSource(strings = {"linked", "array"})
    void testEmptyAndSingleRecordHaveNoSteps(String implementation) throws InterruptedException {
        StringList list = create(implementation);
        assertEquals(0, list.sortPass(new SortDelay(0, 0)));
        list.addFirst("only");
        assertEquals(0, list.sortPass(new SortDelay(0, 0)));
        assertEquals(List.of("only"), values(list));
    }

    private StringList create(String implementation) {
        return implementation.equals("linked") ? new LinkedStringList() : new ArrayStringList();
    }

    private List<String> values(StringList list) {
        List<String> values = new ArrayList<>();
        for (String value : list) {
            values.add(value);
        }
        return values;
    }
}
