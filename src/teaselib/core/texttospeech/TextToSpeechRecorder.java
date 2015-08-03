package teaselib.core.texttospeech;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Part;
import teaselib.Mood;
import teaselib.TeaseLib;
import teaselib.core.ResourceLoader;

public class TextToSpeechRecorder {
    public final static String SpeechDirName = "speech";
    public final static String MessageFilename = "message.txt";
    public final static String ResourcesFilename = "inventory.txt";

    private final ResourceLoader resources;
    private File speechDir = null;
    private final Map<String, Voice> voices;
    private final Set<String> actors = new HashSet<String>();
    private final ActorVoices actorVoices;

    private final TextToSpeechPlayer speechSynthesizer;

    private final long buildStart = System.currentTimeMillis();
    int newEntries = 0;
    int changedEntries = 0;
    int upToDateEntries = 0;
    int reusedDuplicates = 0;

    public TextToSpeechRecorder(TeaseLib teaseLib, ResourceLoader resources)
            throws IOException {
        this.resources = resources;
        // TODO TextToSpeechPlayer is already global in TeaseLib
        this.speechSynthesizer = teaseLib.speechSynthesizer;
        this.voices = teaseLib.speechSynthesizer.textToSpeech.getVoices();
        File assetsDir = resources.getAssetPath("");
        speechDir = createSubDir(assetsDir, SpeechDirName);
        InstalledVoices available = new InstalledVoices(voices);
        available.store(assetsDir);
        actorVoices = new ActorVoices(resources);
        TeaseLib.log("Build start: " + new Date(buildStart).toString());
    }

    private static File createSubDir(File dir, String name) {
        File subDir = new File(dir, name);
        if (subDir.exists() == false) {
            TeaseLib.log("Creating directory " + subDir.getAbsolutePath());
            subDir.mkdirs();
        } else {
            TeaseLib.log("Using directory " + subDir.getAbsolutePath());
        }
        return subDir;
    }

    public void create(ScriptScanner scanner) throws IOException {
        TeaseLib.log("Scanning script '" + scanner.getScriptName() + "'");
        Set<String> created = new HashSet<String>();
        for (Message message : scanner) {
            Actor actor = message.actor;
            File characterDir = createSubDir(speechDir, actor.name);
            // Process voices for each character
            final Voice voice;
            voice = getVoice(actor);
            TeaseLib.log("Voice: " + voice.name);
            speechSynthesizer.textToSpeech.setVoice(voice);
            File voiceDir = createSubDir(characterDir, voice.guid);
            createActorFile(actor, voice);
            String hash = recordMessage(message, voice, voiceDir);
            // Unused directories will remain because an update changes the
            // checksum of the message
            // TODO clean up by checking unused
            created.add(hash);
        }
    }

    private String recordMessage(Message message, final Voice voice,
            File voiceDir) throws IOException {
        String hash = getHash(message);
        File messageDir = new File(voiceDir, hash);
        if (messageDir.exists()) {
            long lastModified = messageDir.lastModified();
            final File messageFile = new File(messageDir, MessageFilename);
            String oldMessageHash = readMessage(messageFile);
            String messageHash = message.toHashString();
            if (messageHash.equals(oldMessageHash)) {
                if (lastModified > buildStart) {
                    TeaseLib.log(hash + " is reused");
                    reusedDuplicates++;
                } else {
                    TeaseLib.log(hash + " is up to date");
                    upToDateEntries++;
                }
            } else if (lastModified > buildStart) {
                // Collision
                TeaseLib.log(hash + " collision!");
                TeaseLib.log("Old:");
                TeaseLib.log(oldMessageHash);
                TeaseLib.log("New:");
                TeaseLib.log(messageHash);
                throw new IllegalStateException("Collision");
            } else {
                TeaseLib.log(hash + " has changed");
                // Change - delete old first
                for (String file : messageDir.list()) {
                    new File(messageDir, file).delete();
                }
                create(message, messageDir, voice);
                changedEntries++;
            }
            // - check whether we have created this during the current
            // scan
            // -> collision
            // - if created before the current build, then change
        } else {
            TeaseLib.log(hash + " is new");
            // new
            messageDir.mkdir();
            create(message, messageDir, voice);
            newEntries++;
        }
        return hash;
    }

    private void createActorFile(Actor actor, final Voice voice)
            throws IOException {
        if (!actors.contains(actor.name)) {
            // Create a tag file containing the actor voice properties, for
            // information and because
            // the resource loader can just load files, but not check for
            // directories in the resource paths
            actors.add(actor.name);
            PreRecordedVoice actorVoice = new PreRecordedVoice(actor.name,
                    voice.guid, resources);
            actorVoice.clear();
            actorVoice.put(actor.name, voice);
            actorVoice.store(new File(new File(speechDir, actor.name),
                    voice.guid));
            // update actor voices property file
            actorVoices.putGuid(actor.name, voice);
            actorVoices.store(resources.getAssetPath(""));
            // reserve voice for this actor
            // speechSynthesizer.selectVoice(resources, new Message(new Actor(
            // Actor.Dominant, neutralVoice.locale)));
        }
    }

    private Voice getVoice(Actor actor) {
        final Voice neutralVoice;
        String voiceGuid = speechSynthesizer.getAssignedVoiceFor(actor,
                resources);
        if (voiceGuid == null) {
            neutralVoice = speechSynthesizer.getVoiceFor(actor, resources);
        } else {
            neutralVoice = voices.get(voiceGuid);
        }
        if (neutralVoice == null) {
            throw new IllegalArgumentException("Voice for actor '" + actor
                    + "' not found in " + ActorVoices.VoicesFilename);
        }
        return neutralVoice;
    }

    public void finish() {
        TeaseLib.log("Finished: " + upToDateEntries + " up to date, "
                + reusedDuplicates + " reused duplicates, " + changedEntries
                + " changed, " + newEntries + " new");
    }

    private static String readMessage(File file) throws FileNotFoundException,
            IOException {
        StringBuilder message = new StringBuilder();
        BufferedReader fileReader = new BufferedReader(new FileReader(file));
        String line = null;
        try {
            while ((line = fileReader.readLine()) != null) {
                if (message.length() > 0) {
                    message.append("\n");
                }
                message.append(line);
            }
        } finally {
            fileReader.close();
        }
        return message.toString();
    }

    public void create(Message message, File messageDir, Voice neutralVoice)
            throws IOException {
        final String messageHash = message.toHashString();
        TeaseLib.log("Recording message:\n" + messageHash);
        int index = 0;
        mp3.Main lame = new mp3.Main();
        List<String> soundFiles = new Vector<String>();
        String mood = Mood.Neutral;
        for (Part part : message.getParts()) {
            // TODO Process or ignore special instructions
            // TODO refactor message handling into separate class,
            // because otherwise it would be handled here and in MessageRenderer
            if (part.type == Message.Type.Mood) {
                mood = part.value;
            } else if (part.type == Message.Type.Text) {
                TeaseLib.log("Recording part " + index);
                File soundFile = new File(messageDir, Integer.toString(index));
                speechSynthesizer.textToSpeech.setVoice(neutralVoice);
                speechSynthesizer.textToSpeech.setHint(mood);
                String recordedSoundFile = speechSynthesizer.textToSpeech
                        .speak(part.value, soundFile.getAbsolutePath());
                if (!recordedSoundFile.endsWith(".mp3")) {
                    String encodedSoundFile = recordedSoundFile.replace(".wav",
                            ".mp3");
                    // sampling frequency is read from the wav audio file
                    String[] argv = { recordedSoundFile, encodedSoundFile,
                            "--preset", "standard" };
                    lame.run(argv);
                    new File(recordedSoundFile).delete();
                    recordedSoundFile = encodedSoundFile;
                }
                soundFiles.add(new File(recordedSoundFile).getName());
                index++;
            }
        }
        {
            // Write sound inventory file
            File sounds = new File(messageDir, ResourcesFilename);
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(sounds));
                for (String soundFile : soundFiles) {
                    writer.write(soundFile);
                    writer.newLine();
                }
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
        {
            // Write message file to indicate the folder is complete
            File text = new File(messageDir, MessageFilename);
            FileWriter writer = null;
            try {
                writer = new FileWriter(text);
                writer.write(messageHash);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    public static String getHash(Message message) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] string = null;
        try {
            string = message.toHashString().getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(string);
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
