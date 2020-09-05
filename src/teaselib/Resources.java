package teaselib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import teaselib.core.Script;

public class Resources extends ArrayList<String> {

    private static final long serialVersionUID = 1L;

    public final Script script;

    public Resources(Script script, List<String> paths) {
        super(paths);
        this.script = script;
    }

    public byte[] getBytes(String resource) throws IOException {
        return script.resources.get(resource).readAllBytes();
    }

}
