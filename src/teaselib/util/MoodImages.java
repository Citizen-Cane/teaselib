package teaselib.util;

import java.util.ArrayList;
import java.util.List;

import teaselib.Mood;
import teaselib.Resources;

/**
 * Handles image iteration over a set of resources
 * 
 * @author Citizen-Cane
 */
public class MoodImages extends RandomImages {

    public MoodImages(Resources imageResources) {
        super(imageResources);
    }

    @Override
    public String next(String... hints) {
        if (hints == null || hints.length == 0) {
            return super.next();
        } else {
            for (String hint : hints) {
                // process each hint type
                if (Mood.isMood(hint)) {
                    List<String> matching = getMatchingResources(hint);
                    if (!matching.isEmpty()) {
                        return randomImage(matching);
                    }
                }
            }
            return super.next();
        }
    }

    private List<String> getMatchingResources(String mood) {
        String name = Mood.extractName(mood);
        List<String> matches = getMatches(name);
        if (matches.isEmpty() && !Mood.Neutral.equalsIgnoreCase(mood)) {
            return getMatchingResources(Mood.lessIntense(mood));
        }
        return matches;
    }

    private List<String> getMatches(String mood) {
        List<String> matches = new ArrayList<>();
        for (String resource : resources) {
            if (resource.toLowerCase().contains(mood)) {
                matches.add(resource);
            }
        }
        return matches;
    }

}
