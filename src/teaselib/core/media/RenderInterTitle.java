package teaselib.core.media;

import java.io.IOException;

import teaselib.Message;
import teaselib.core.TeaseLib;

public class RenderInterTitle extends MediaRendererThread implements ReplayableMediaRenderer {
    private final Message message;

    public RenderInterTitle(Message message, TeaseLib teaseLib) {
        super(teaseLib);
        this.message = message;
    }

    @Override
    protected void renderMedia() throws InterruptedException, IOException {
        teaseLib.host.showInterTitle(message.toString());
    }

}
