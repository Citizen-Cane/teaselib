package teaselib.core.media;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import teaselib.core.TeaseLib;

public class RenderInterTitle extends MessageRenderer {

    public RenderInterTitle(RenderedMessage message, TeaseLib teaseLib) {
        super(teaseLib, Collections.singletonList(message));
        accumulatedText.addAll(messages);
    }

    @Override
    protected void renderMedia() throws InterruptedException, IOException {
        teaseLib.transcript.info("Intertitle:\n" + accumulatedText.toString());
        startCompleted();
        teaseLib.host.showInterTitle(accumulatedText.paragraphs);
        teaseLib.host.show();
        mandatoryCompleted();
    }

    @Override
    public boolean append(List<RenderedMessage> newMessages) {
        synchronized (messages) {
            messages.addAll(newMessages);
            accumulatedText.addAll(newMessages);
            lastParagraph = lastParagraph();
            return false;
        }
    }

    @Override
    public void forwardToEnd() {
        // no-op
    }

}
