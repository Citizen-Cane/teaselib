package teaselib.stimulation.pattern;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.media.RenderSound;
import teaselib.core.util.ExceptionUtil;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class SoundStimulation implements Stimulation {
    private static final Logger logger = LoggerFactory.getLogger(SoundStimulation.class);

    private final Stimulation stimulation;
    private final RenderSound sound;
    private final MediaRendererQueue renderQueue;

    public static SoundStimulation of(Stimulation stimulation, ResourceLoader resources, String soundResource,
            TeaseLib teaseLib) {
        return new SoundStimulation(stimulation, resources, soundResource, teaseLib);
    }

    public SoundStimulation(Stimulation stimulation, ResourceLoader resources, String soundResource,
            TeaseLib teaseLib) {
        this.stimulation = stimulation;
        RenderSound sound = loadSoundResource(resources, soundResource, teaseLib);
        this.sound = sound;
        this.renderQueue = new MediaRendererQueue(teaseLib.globals.get(MediaRendererQueue.class));
    }

    private static RenderSound loadSoundResource(ResourceLoader resources, String soundResource, TeaseLib teaseLib) {
        RenderSound sound;
        try {
            sound = new RenderSound(resources, soundResource, teaseLib);
        } catch (IOException e) {
            ExceptionUtil.handleException(e, teaseLib.config, logger);
            sound = null;
        }
        return sound;
    }

    public void play() {
        renderQueue.submit(sound);
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        return stimulation.waveform(stimulator, intensity);
    }
}
