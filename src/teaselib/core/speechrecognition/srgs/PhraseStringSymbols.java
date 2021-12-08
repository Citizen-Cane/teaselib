package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PhraseStringSymbols extends StringSequences {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> symbols = new TreeMap<>(String::compareToIgnoreCase);

    public PhraseStringSymbols(List<String> phrases) {
        super(phrases.size());
        addAll(phrases);
    }

    private void addAll(List<String> phrases) {
        for (int i = 0; i < phrases.size(); i++) {
            add(phrase(phrases.get(i), i));
        }
    }

    public Sequence<String> phrase(String string, int index) {
        String[] words = PhraseString.words(string);
        var sequence = new Sequence<>(StringSequence.Traits, words.length);
        for (String word : words) {
            String symbol = symbols.computeIfAbsent(word, s -> s);
            sequence.add(symbol);
        }
        return sequence;
    }

    public PhraseStringSequences toPhraseStringSequences() {
        PhraseStringSequences sequences = new PhraseStringSequences(size());
        int i = 0;
        for (var symbols : this) {
            var indices = Collections.singleton(i);
            Sequence<PhraseString> sequence = new Sequence<>(PhraseString.Traits, symbols.size());
            for (var symbol : symbols) {
                sequence.add(new PhraseString(symbol, indices));
            }
            sequences.add(sequence);
            i++;
        }
        return sequences;
    }

    public PhraseStringSymbols joinDuplicates() {

        while (true) {
            Set<String> candidates = (stream().flatMap(Sequence::stream).map(Objects::toString)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(String::compareToIgnoreCase))));
            // Last seen successor
            Map<String, String> matches = new TreeMap<>(String::compareToIgnoreCase);
            // last seen predecessor
            Map<String, String> used = new TreeMap<>(String::compareToIgnoreCase);

            // first symbols don't have predecessor
            stream().map(sequence -> sequence.get(0)).toList().forEach(element -> used.put(element, null));

            for (var sequence : this) {
                int size = sequence.size();
                for (int i = 0; i < size; i++) {
                    String symbol = sequence.get(i);
                    if (candidates.contains(symbol)) {
                        if (i < size - 1) {
                            String successor = sequence.get(i + 1);
                            String match = matches.get(symbol);
                            if (match == null) {
                                if (used.containsKey(successor)) {
                                    // successor already used - remove both
                                    candidates.remove(symbol);
                                    removeOtherCandidate(candidates, used, successor);
                                } else {
                                    matches.put(symbol, successor);
                                    used.put(successor, symbol);
                                }
                            } else if (!match.equalsIgnoreCase(successor)) {
                                // matched above, used - but now found different successor
                                candidates.remove(symbol);
                                removeOtherCandidate(candidates, used, successor);
                            }
                        } else {
                            // last symbol
                            candidates.remove(symbol);
                            if (i > 0) {
                                // symbols might not be used when the preceding symbol
                                // has been removed from the candidates set
                                String predecessor = sequence.get(i - 1);
                                if (!used.containsKey(symbol)) {
                                    used.put(symbol, predecessor);
                                }
                            }
                        }
                    } else if (i < size - 1) {
                        // cleanup removed candidates whose successor has already another predecessors
                        String successor = sequence.get(i + 1);
                        removeOtherCandidate(candidates, used, successor);
                    } else {
                        // last symbol
                        continue;
                    }
                }
            }

            if (candidates.isEmpty()) {
                break;
            }

            join(candidates);
        }

        return this;
    }

    // why is it necessary to remove the preceding candidate symbol?
    // A C
    // B C
    // B like to join to C but because C has also A as it predecessor, C must remove A & B, since they're not the same
    // symbol.
    private void removeOtherCandidate(Set<String> candidates, Map<String, String> used, String symbol) {
        String u = used.get(symbol);
        // first symbols are preceded by null -> any symbol might be preceded by null
        if (u != null) {
            candidates.remove(u);
        }
    }

    private void join(Set<String> candidates) {
        for (var sequence : this) {
            for (int i = 0; i < sequence.size() - 1; i++) {
                var symbol = sequence.get(i);
                if (candidates.contains(symbol)) {
                    String sibling = sequence.get(i + 1);
                    symbols.remove(symbol);
                    symbols.remove(sibling);

                    List<String> elements = sequence.subList(i, i + 2);
                    String joined = traits.joinSequenceOperator.apply(elements);
                    joined = symbols.computeIfAbsent(joined, key -> key);
                    sequence.set(i, joined);
                    sequence.remove(i + 1);
                    i--;
                }
            }
        }
    }

    @Override
    public String toString() {
        return stream().map(sequence -> sequence.stream().collect(joining(", "))) //
                .map(phrase -> "\t" + phrase + " ").collect(joining("\n"));
    }

}
