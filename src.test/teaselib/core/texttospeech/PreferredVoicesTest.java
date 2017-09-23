package teaselib.core.texttospeech;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

public class PreferredVoicesTest {
    @Test
    public void testSortingReverse() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("a", "1");
        values.put("b", "2");
        values.put("c", "3");
        List<Entry<String, String>> list = new ArrayList<Entry<String, String>>(values.entrySet());

        Map<String, String> sorted = PreferredVoices.sortByValue(list);

        List<Entry<String, String>> result = new ArrayList<Entry<String, String>>(sorted.entrySet());

        assertEquals("3", result.get(0).getValue());
        assertEquals("2", result.get(1).getValue());
        assertEquals("1", result.get(2).getValue());
    }

    @Test
    public void testAlreadySorted() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("c", "3");
        values.put("b", "2");
        values.put("a", "1");
        List<Entry<String, String>> list = new ArrayList<Entry<String, String>>(values.entrySet());

        Map<String, String> sorted = PreferredVoices.sortByValue(list);

        List<Entry<String, String>> result = new ArrayList<Entry<String, String>>(sorted.entrySet());

        assertEquals("3", result.get(0).getValue());
        assertEquals("2", result.get(1).getValue());
        assertEquals("1", result.get(2).getValue());
    }

    @Test
    public void testSortingRandom() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("b", "2");
        values.put("c", "3");
        values.put("a", "1");
        List<Entry<String, String>> list = new ArrayList<Entry<String, String>>(values.entrySet());

        Map<String, String> sorted = PreferredVoices.sortByValue(list);

        List<Entry<String, String>> result = new ArrayList<Entry<String, String>>(sorted.entrySet());

        assertEquals("3", result.get(0).getValue());
        assertEquals("2", result.get(1).getValue());
        assertEquals("1", result.get(2).getValue());
    }
}
