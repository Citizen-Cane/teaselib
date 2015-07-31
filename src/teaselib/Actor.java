package teaselib;

import teaselib.core.Images;

public class Actor {

    public final static String Dominant = "Dominant";

    public final String name;
    public final String locale;

    public Images images = null;

    public Actor(Actor actor) {
        this.name = actor.name;
        this.locale = actor.locale;
        this.images = actor.images;
    }

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
