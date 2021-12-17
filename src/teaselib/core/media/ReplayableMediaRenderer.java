/**
 * 
 */
package teaselib.core.media;

import teaselib.Replay;

public interface ReplayableMediaRenderer extends MediaRenderer {

    void set(Replay.Position replayPosition);

}
