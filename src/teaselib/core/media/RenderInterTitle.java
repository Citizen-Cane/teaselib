package teaselib.core.media;

import java.io.IOException;

import teaselib.core.TeaseLib;

public class RenderInterTitle extends MediaRendererThread implements ReplayableMediaRenderer {
    private final RenderedMessage message;

    public RenderInterTitle(RenderedMessage message, TeaseLib teaseLib) {
        super(teaseLib);
        this.message = message;
    }

    @Override
    protected void renderMedia() throws InterruptedException, IOException {
        teaseLib.transcript.info("Intertitle:" + message);
        teaseLib.host.showInterTitle(message.toString());
    }

}
