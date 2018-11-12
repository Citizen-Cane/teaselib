package teaselib.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Answer;
import teaselib.Config.SpeechRecognition.Intention;
import teaselib.Message;
import teaselib.Replay;
import teaselib.ScriptFunction;
import teaselib.util.SpeechRecognitionRejectedScript;

final class SpeechRecognitionRejectedScriptAdapter extends SpeechRecognitionRejectedScript {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionRejectedScriptAdapter.class);

    private Script script;
    private final ScriptFunction scriptFunction;
    private boolean finishedWithPrompt = false;

    SpeechRecognitionRejectedScriptAdapter(Script script, ScriptFunction scriptFunction) {
        super(script);
        this.script = script;
        this.scriptFunction = scriptFunction;
    }

    @Override
    public void run() {
        script.endAll();
        Replay beforeSpeechRecognitionRejected = script.getReplay();
        script.actor.speechRecognitionRejectedScript.run();
        if (finishedWithPrompt) {
            beforeSpeechRecognitionRejected.replay(Replay.Position.FromMandatory);
        } else {
            beforeSpeechRecognitionRejected.replay(Replay.Position.End);
        }
    }

    @Override
    protected void renderIntertitle(String... text) {
        super.renderIntertitle(text);
        finishedWithPrompt = false;
    }

    @Override
    protected void renderMessage(Message message, boolean useTTS) {
        super.renderMessage(message, useTTS);
        finishedWithPrompt = false;
    }

    @Override
    protected String showChoices(List<Answer> answers, ScriptFunction scriptFunction, Intention intention) {
        finishedWithPrompt = true;
        return super.showChoices(answers, scriptFunction, intention);
    }

    // Handling speech recognition rejected events:
    // RecognitionRejectedEvent-scripts doesn't work in reply-calls that
    // invoke script functions but they work inside script functions.
    //
    // Reason are:
    // - The event handler would have to wait until messages rendered by
    // the script function are completed -> delay in response
    // - script functions may include timing which would be messed up by
    // pausing them
    // - Script functions may invoke other script functions, but the
    // handler management is neither multi-threading-aware nor
    // synchronized
    // - The current code is unable to recover to the choice on top of
    // the choices stack after a recognition-rejected pause event
    //
    // The recognitionRejected handler won't trigger immediately when
    // a script function renders messages, because it will wait until
    // the render queue is empty, and this includes message delays.
    // Therefore script functions are not supported, because the script
    // function would still render messages while the choices are shown.
    // However rendering messages while showing choices should be fine.
    @Override
    public boolean canRun() {
        SpeechRecognitionRejectedScript speechRecognitionRejectedScript = actor.speechRecognitionRejectedScript;
        if (scriptFunction != null) {
            // This would work for the built-in confirmative timeout script functions:
            // - TimeoutBehavior.InDubioMitius and maybe also for
            // - TimeoutBehavior.TimeoutBehavior.InDubioMitius
            log(speechRecognitionRejectedScript, scriptFunction.relation.toString() + " script functions running");
            return false;
        } else if (scriptRenderer.hasCompletedMandatory()) {
            // must complete all to avoid parallel rendering, see {@link Message#ShowChoices}
            log(speechRecognitionRejectedScript, "message rendering still in progress");
            return false;
        } else if (!speechRecognitionRejectedScript.canRun()) {
            log(speechRecognitionRejectedScript, "RecognitionRejectedScript.canRun() returned false");
            return false;
        } else {
            return true;
        }
    }

    private static void log(Script speechRecognitionRejectedScript, String message) {
        if (logger.isInfoEnabled()) {
            logger.info("{} - skipping RecognitionRejectedScript {}", message, speechRecognitionRejectedScript);
        }
    }
}