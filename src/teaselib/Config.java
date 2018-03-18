/**
 * 
 */
package teaselib;

/**
 * This class lists up all configuration properties.
 * 
 * The default value is always {@code false}, and chosen to maximize the user experience.
 * <P>
 * Values are written via the TeaseLib {@code getXXX(...)/set(...)} methods, as a result the TeaseLib settings are
 * maintained by the host.
 * <P>
 * Examples:
 * <P>
 * {@code boolean ignoreMissing = teaseLib.getBoolean(Config.Namespace, Config.Debug.IgnoreMissingResources);}
 * <P>
 * {@code teaseLib.set(Config.Namespace, Config.Debug.IgnoreMissingResources, ignoreMissing);}
 */
public enum Config {
    /**
     * Path to a directory containing assets
     */
    Assets;

    public enum Debug {
        StopOnAssetNotFound,
        StopOnRenderError,
        LogDetails,
    }

    public enum Render {
        Speech,
        Sound,
        ActorImages,
        InstructionalImages
    }

    public enum InputMethod {
        SpeechRecognition,
        GameController,
        HeadGestures,
    }

    public enum SpeechRecognition {
        ;
        public enum Intention {
            Chat,
            Confirm,
            Decide
        }
    }
}
