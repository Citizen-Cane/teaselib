package teaselib;

import teaselib.image.Images;

public class Actor {

    public final static String Dominant = "Dominant";

    public final String name;
    public final String locale;

    public Images images = null;

    public Actor(String name, String locale) {
        super();
        this.name = name;
        this.locale = locale;
        this.images = null;
    }

    public Actor(String name, String locale, Images images) {
        super();
        this.name = name;
        this.locale = locale;
        this.images = images;
    }
}
