package teaselib.texttospeech;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import teaselib.ResourceLoader;
import teaselib.TeaseLib;
import teaselib.text.Message;
import teaselib.text.Message.Part;

public class TextToSpeechRecorder {
    public final static String SpeechDirName = "speech";
    public final static String MessageFilename = "message.txt";
    public final static String ResourcesFilename = "inventory.txt";

    private final ResourceLoader resources;
    private final TextToSpeech textToSpeech;
    private File speechDir = null;
    private final Map<String, Voice> voices;
    private final Set<String> actors = new HashSet<String>();

    private final TextToSpeechPlayer ttsPlayer;

    private final long buildStart = System.currentTimeMillis();
    int newEntries = 0;
    int changedEntries = 0;
    int upToDateEntries = 0;
    int reusedDuplicates = 0;

    public TextToSpeechRecorder(ResourceLoader resources) throws IOException {
        this.resources = resources;
        this.textToSpeech = new TextToSpeech();
        this.voices = textToSpeech.getVoices();
        TextToSpeechPlayer ttsPlayer = new TextToSpeechPlayer(resources,
                textToSpeech);
        File assetsDir = resources.getAssetPath("");
        speechDir = createSubDir(assetsDir, SpeechDirName);
        AvailableVoicesProperties available = new AvailableVoicesProperties(
                voices);
        available.store(assetsDir);

        VoicesProperties voicesProperties = new VoicesProperties(resources);
        if (voicesProperties.empty()) {
            // Write default file
            // TODO Language and gender selection for voices
            String first = voices.keySet().iterator().next();
            voicesProperties.put(Actor.Dominant, voices.get(first));
            voicesProperties.store(assetsDir);
            ttsPlayer = new TextToSpeechPlayer(resources, textToSpeech);
        }
        this.ttsPlayer = ttsPlayer;
        TeaseLib.log("Build start: " + new Date(buildStart).toString());
    }

    private File createSubDir(File dir, String name) {
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
            String hash = getHash(message);
            Actor actor = message.actor;
            File characterDir = createSubDir(speechDir, actor.name);
            // Process voices for each character
            final Voice voice;
            String voiceGuid = ttsPlayer.getAssignedVoiceFor(actor);
            if (voiceGuid == null) {
                voice = ttsPlayer.getVoiceFor(actor);
            } else {
                voice = voices.get(voiceGuid);
            }
            if (voice == null) {
                throw new IllegalArgumentException("Voice for actor '" + actor
                        + "' not found in " + VoicesProperties.VoicesFilename);
            }
            TeaseLib.log("Voice: " + voice.name);
            textToSpeech.setVoice(voice);
            File voiceDir = createSubDir(characterDir, voice.guid);
            if (!actors.contains(actor.name)) {
                // Create a tag file containing the actor voice properties, for
                // information and because
                // the resource loader can just load files, but not check for
                // directories in the resource paths
                actors.add(actor.name);
                ActorVoice actorVoice = new ActorVoice(actor.name, voice.guid,
                        resources);
                actorVoice.clear();
                actorVoice.put(actor.name, voice);
                actorVoice.store(new File(new File(speechDir, actor.name),
                        voice.guid));
            }
            File messageDir = new File(voiceDir, hash);
            if (messageDir.exists()) {
                long lastModified = messageDir.lastModified();
                if (isUpToDate(message, messageDir)) {
                    if (lastModified > buildStart) {
                        TeaseLib.log(hash + " is reused");
                        reusedDuplicates++;
                    } else {
                        TeaseLib.log(hash + " is up to date");
                        upToDateEntries++;
                    }
                } else if (lastModified > buildStart) {
                    // Collision
                    TeaseLib.log(hash + " collision");
                    throw new IllegalStateException("Collision");
                } else {
                    TeaseLib.log(hash + " has changed");
                    // Change - delete old first
                    for (String file : messageDir.list()) {
                        new File(messageDir, file).delete();
                    }
                    create(message, messageDir);
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
                create(message, messageDir);
                newEntries++;
            }
            // Unused directories will remain because an update changes the
            // checksum of the message
            // TODO clean up by checking unused
            created.add(hash);
        }
    }

    public void finish() {
        TeaseLib.log("Finished: " + upToDateEntries + " up to date, "
                + reusedDuplicates + " reused duplicates, " + changedEntries
                + " changed, " + newEntries + " new");
    }

    private boolean isUpToDate(Message message, File messageDir)
            throws IOException {
        Message readIn = new Message(message.actor);
        File file = new File(messageDir, MessageFilename);
        if (file.exists()) {
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            String line = null;
            try {

                while ((line = fileReader.readLine()) != null)
                    readIn.add(line);
            } finally {
                if (fileReader != null) {
                    fileReader.close();
                }
            }
            String messageHash = message.toHashString();
            String readInHash = readIn.toHashString();
            return messageHash.equals(readInHash);
        } else {
            return false;
        }
    }

    public void create(Message message, File messageDir) throws IOException {
        TeaseLib.log("Recording message:\n" + message.toHashString());
        int index = 0;
        mp3.Main lame = new mp3.Main();
        List<String> soundFiles = new Vector<String>();
        for (Part paragraph : message.getParagraphs()) {
            // TODO Process or ignore special instructions
            // TODO refactor message handling into separate class,
            // because otherwise it would be handled here and in MessageRenderer
            TeaseLib.log("Recording part " + index);
            File soundFile = new File(messageDir, Integer.toString(index));
            String recordedSoundFile = textToSpeech.speak(paragraph.value,
                    soundFile.getAbsolutePath());
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
                writer.write(message.toHashString());
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
