package teaselib.util;

import java.util.List;
import java.util.Vector;

import teaselib.Mood;
import teaselib.core.ResourceLoader;

/**
 * Handles image iteration over a set of resources
 * 
 * @author someone
 */
public class MoodImages extends RandomImages {

    String[] hints = null;

    public MoodImages(ResourceLoader resources, String path) {
        super(resources, path);
    }

    @Override
    public String next() {
        if (hints == null) {
            return super.next();
        } else {
            for (String hint : hints) {
                // process each hint type
                if (Mood.isMood(hint)) {
                    List<String> matches = getMatchingResources(hint);
                    if (!matches.isEmpty()) {
                        return getRandomResource(matches);
                    }
                }
            }
            return super.next();
        }
    }

    private List<String> getMatchingResources(String mood) {
        String name = Mood.extractName(mood);
        List<String> matches = getMatches(name);
        if (matches.isEmpty() && mood != Mood.Neutral) {
            return getMatchingResources(Mood.lessIntense(mood));
        }
        return matches;
    }

    private List<String> getMatches(String mood) {
        List<String> matches = new Vector<String>();
        for (String resource : images) {
            if (resource.toLowerCase().contains(mood)) {
                matches.add(resource);
            }
        }
        return matches;
    }

    @Override
    public void hint(String... hint) {
        hints = hint;
    }
}
