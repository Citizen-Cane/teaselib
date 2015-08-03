package teaselib.core.speechrecognition.implementation;

import java.util.List;

import teaselib.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;

public class TeaseLibSR extends SpeechRecognitionImplementation {

    private long nativeObject;

    private Thread eventThread = null;
    private Throwable eventThreadException = null;

    public TeaseLibSR() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibSR");
    }

    @Override
    public void init(
            SpeechRecognitionEvents<SpeechRecognitionImplementation> events,
            String languageCode) throws Throwable {
        initSR(events, languageCode);
        eventThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (eventThread) {
                        try {
                            // Never ends
                            initSREventThread(eventThread);
                        } catch (Throwable t) {
                            TeaseLib.log(this, t);
                            eventThreadException = t;
                        }
                        eventThread.notifyAll();
                    }
                } catch (IllegalMonitorStateException e) {
                    TeaseLib.log(this, e);
                }
            }
        });
        synchronized (eventThread) {
            eventThread.setName("Speech Recognition event thread");
            eventThread.start();
            try {
                eventThread.wait();
            } catch (InterruptedException e) {
                TeaseLib.logDetail(this, e);
            }
            if (eventThreadException != null) {
                throw eventThreadException;
            }
        }
    }

    /**
     * // Init the recognizer
     * 
     * @param languageCode
     */
    public native void initSR(
            SpeechRecognitionEvents<SpeechRecognitionImplementation> events,
            String languageCode);

    /**
     * Init the speech recognizer event handling thread. Creating the thread in
     * native code and then attaching the VM would cause the thread to get a
     * default class loader, without the class path to teaselib, resulting in
     * not being able to create event objects
     */
    private native void initSREventThread(Thread jthread);

    @Override
    public native void setChoices(List<String> choices);

    @Override
    public native void setMaxAlternates(int n);

    @Override
    public native void startRecognition();

    @Override
    public native void emulateRecognition(String emulatedRecognitionResult);

    @Override
    public native void stopRecognition();

    @Override
    public native void dispose();

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
        super.finalize();
    }
}