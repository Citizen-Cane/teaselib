package teaselib.texttospeech;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import teaselib.ResourceLoader;
import teaselib.TeaseLib;
import teaselib.text.Message;

public class TextToSpeechRecorder {

	/*
	 * Prerender voice for PCM: Compressed audio format needed, can Windows
	 * provide one? -> research -> No ... Write a SpeechRenderer that uses the
	 * sound files - Android doesn't support speech bookmarks - bookmarks are
	 * really complex to implement -> no bookmarks, no commata, render full
	 * sentences only
	 * 
	 * Final conclusions: + remove comma feature from message rendering in order
	 * to render only complete sentences + collect speech items from scripts +
	 * render each paragraph into a single sound file + store all sounds files
	 * of the message in one folder + store the message text with the sound
	 * files + use action number as key -> easy - use SHA-256 of complete
	 * message to create folder names otherwise - update items whose text has
	 * actually changed - update items older than script, a collision occurred
	 * when the texfile date is newer than the build start date
	 * 
	 * How does it work: + recorder will work from project and produces output
	 * in target folder - next to the zips + recorder will enum TTS- renderers
	 * to find available voices and write them to available.cfg + recorder will
	 * create voice recordings for voices in create. cfg + message renderer will
	 * read the project config file and use the pre-rendered voice set there +
	 * message renderer will calc the SHA-256 checksum of each message, load the
	 * sound files and play them instead of calling TTS
	 */

	public final static String ResourcesFilename = "inventory.txt";
	public final static String MessageFilename = "message.txt";
	public final static String SpeechDirName = "speech";
	public final static String AvailableVoicesFilename = "installed.properties";
	public final static String RecordedVoicesFilename = "recorded.properties";

	private final TextToSpeech textToSpeech;
	private File speechDir = null;
	private final Map<String, Voice> voices;
	private final TextToSpeechPlayer ttsPlayer;

	private final long buildStart = System.currentTimeMillis();
	int newEntries = 0;
	int changedEntries = 0;
	int upToDateEntries = 0;
	int reusedDuplicates = 0;
	
	public TextToSpeechRecorder(ResourceLoader resources) throws IOException {
		this.textToSpeech = new TextToSpeech();
		this.voices = textToSpeech.getVoices();
		TextToSpeechPlayer ttsPlayer = new TextToSpeechPlayer(resources,
				textToSpeech);
		speechDir = createSubDir(resources.getAssetsPath(""), SpeechDirName);
		Properties available = new Properties();
		// List available voices by guid and language
		for (String key : voices.keySet()) {
			available.put(key, voices.get(key).language);
		}
		File availableVoicesFile = new File(speechDir, AvailableVoicesFilename);
		TeaseLib.log("Creating " + availableVoicesFile.toString());
		available.store(new FileOutputStream(availableVoicesFile), "");
		if (!ttsPlayer.hasPrerecordedVoices()) {
			// Write default file
			TeaseLib.log("Creating defaults for " + RecordedVoicesFilename);
			// TODO Language and gender selection for voices
			String first = voices.keySet().iterator().next();
			Properties record = new Properties();
			record.put(Message.Dominant, voices.get(first).guid);
			record.store(new FileOutputStream(new File(speechDir,
					RecordedVoicesFilename)), "");
			ttsPlayer = new TextToSpeechPlayer(resources, textToSpeech);
		}
		this.ttsPlayer = ttsPlayer;
		TeaseLib.log("Build start: "
				+ new Date(buildStart).toString());
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
		Set<String> created = new HashSet<>();
		for (Message message : scanner) {
			String hash = getHash(message);
			File characterDir = createSubDir(speechDir, message.getCharacter());
			// Process voices for each character
			String voiceForCharacter = ttsPlayer.getVoiceFor(message
					.getCharacter());
			Voice voice = voices.get(voiceForCharacter);
			TeaseLib.log("Voice: " + voice.name);
			textToSpeech.setVoice(voice);
			File voiceDir = createSubDir(characterDir, voice.guid);
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

	public void finish()
	{
		TeaseLib.log("Finished: " + upToDateEntries + " up to date, "
				+ reusedDuplicates + " reused duplicates, " + changedEntries
				+ " changed, " + newEntries + " new");
	}

	private boolean isUpToDate(Message message, File messageDir)
			throws IOException {
		Message readIn = new Message();
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
		List<String> soundFiles = new Vector<>();
		for (String paragraph : message.getParagraphs()) {
			// TODO Process or ignore special instructions
			// TODO refactor message handling into separate class,
			// because otherwise it would be handled here and in MessageRenderer
			TeaseLib.log("Recording part " + index);
			File soundFile = new File(messageDir, Integer.toString(index));
			String recordedSoundFile = textToSpeech.speak(paragraph,
					soundFile.getAbsolutePath());
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
