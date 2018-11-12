package teaselib.stimulation.pattern;

import teaselib.core.media.MediaRenderer;
import teaselib.core.media.MediaRendererQueue;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class SoundStimulation implements Stimulation {
    private final Stimulation stimulation;
    private final MediaRenderer.Threaded mediaRenderer;
    private final MediaRendererQueue renderQueue;

    public static SoundStimulation of(Stimulation stimulation, MediaRenderer.Threaded mediaRenderer) {
        return new SoundStimulation(stimulation, mediaRenderer);
    }

    private SoundStimulation(Stimulation stimulation, MediaRenderer.Threaded mediaRenderer) {
        this.stimulation = stimulation;
        this.mediaRenderer = mediaRenderer;
        this.renderQueue = new MediaRendererQueue();
    }

    public void play() {
        renderQueue.submit(mediaRenderer);
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        return stimulation.waveform(stimulator, intensity);
    }
}
