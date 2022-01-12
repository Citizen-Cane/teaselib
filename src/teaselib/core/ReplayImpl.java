package teaselib.core;

import java.util.ArrayList;
import java.util.List;

import teaselib.Replay;
import teaselib.core.media.MediaRenderer;

class ReplayImpl implements Replay {
    private final ScriptRenderer scriptRenderer;
    private final List<MediaRenderer> renderers;

    public ReplayImpl(ScriptRenderer scriptRenderer, List<MediaRenderer> renderers) {
        this.scriptRenderer = scriptRenderer;
        this.renderers = new ArrayList<>(renderers);
        ScriptRenderer.logger.info("Remembering renderers in replay instance {}", this);
    }

    @Override
    public void replay(Replay.Position position) {
        try {
            scriptRenderer.renderQueue.awaitAllCompleted();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
        // TODO log message comes too early - before the interjection (but looks good in player)
        ScriptRenderer.logger.info("Replaying {} from {}", this, position);
        scriptRenderer.replay(renderers, position);
    }

}
