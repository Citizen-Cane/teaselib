/**
 * 
 */
package teaselib.core.media;

import teaselib.Replay;

public interface ReplayableMediaRenderer extends MediaRenderer.Threaded {

    void set(Replay.Position replayPosition);

}
