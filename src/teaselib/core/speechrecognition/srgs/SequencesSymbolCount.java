package teaselib.core.speechrecognition.srgs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SequencesSymbolCount<T> {
    private final Map<String, AtomicInteger> symbols;

    public SequencesSymbolCount(Sequences<T> sequences) {
        this.symbols = new HashMap<>();

        sequences.stream().filter(Sequence::nonEmpty).flatMap(Sequence::stream)
                .forEach(element -> symbols.computeIfAbsent(key(element), t -> new AtomicInteger(0)).incrementAndGet());
    }

    private String key(T element) {
        return element.toString().toLowerCase();
    }

    @Override
    public String toString() {
        return symbols.toString();
    }

    public int get(T key) {
        return get(key(key));
    }

    public int get(String key) {
        return symbols.get(key).get();
    }

}
