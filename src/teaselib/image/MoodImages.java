package teaselib.image;

import java.awt.Image;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import teaselib.Mood;
import teaselib.ResourceLoader;

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
    public Image next() throws IOException {
        if (hints == null) {
            return super.next();
        } else {
            for (String hint : hints) {
                // process each hint type
                if (Mood.isMood(hint)) {
                    List<String> matches = getMatchingImages(hint);
                    if (!matches.isEmpty()) {
                        return getRandomImage(matches);
                    }
                }
            }
            return super.next();
        }
    }

    private List<String> getMatchingImages(String mood) {
        String name = Mood.extractName(mood);
        List<String> matches = getMatches(name);
        if (matches.isEmpty() && mood != Mood.Neutral) {
            return getMatchingImages(Mood.lessIntense(mood));
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
