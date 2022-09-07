package teaselib.core.sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

/**
 * @author Citizen-Cane
 *
 */
public class AudioDevices {

    public static List<String> inputs() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        List<Line.Info> availablePorts = new ArrayList<>();
        List<Line.Info> availableLines = new ArrayList<>();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            System.out.println("\nFound " + mixer.getClass().getSimpleName() + ": " + "\"" + mixerInfo.getName() + "\""
                    + " (\"" + mixerInfo.getDescription() + "\", " + mixerInfo.getVendor() + ", " + mixerInfo.getVersion() + ")");
            try {
                mixer.open();
                for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                    Line line = mixer.getLine(lineInfo);
                    System.out.println(
                            "    Found source " + line.getClass().getSimpleName() + ": " + lineInfo + " with " + mixer.getMaxLines(lineInfo)
                                    + " max-instances.");
                    try {
                        line.open();
                        for (Control thisControl : line.getControls()) {
                            System.out.println(AnalyzeControl(thisControl));
                        }
                        if (lineInfo.matches(Port.Info.MICROPHONE) || lineInfo.matches(Port.Info.LINE_IN) || lineInfo.matches(Port.Info.COMPACT_DISC)) {
                            availablePorts.add(lineInfo);
                        } else {
                            availableLines.add(lineInfo);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("\t\tLine unavailable: " + e.getMessage());
                    }
                }

                for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                    Line line = mixer.getLine(lineInfo);
                    System.out.println(
                            "    Found target " + line.getClass().getSimpleName() + ": " + lineInfo + " with " + mixer.getMaxLines(lineInfo)
                                    + " max-instances.");
                    try {
                        line.open();
                        for (Control thisControl : line.getControls()) {
                            System.out.println(AnalyzeControl(thisControl));
                        }
                        if (lineInfo.matches(Port.Info.HEADPHONE) || lineInfo.matches(Port.Info.SPEAKER) || lineInfo.matches(Port.Info.LINE_OUT)) {
                            availablePorts.add(lineInfo);
                        } else {
                            availableLines.add(lineInfo);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("\t\tLine unavailable: " + e.getMessage());
                    }
                }
            } catch (LineUnavailableException e) {
                System.out.println("\t\tLine unavailable." + e.getMessage());
            }
        }

        System.out.println("\nAvailable ports:\n" + availablePorts.stream().map(Line.Info::toString).collect(Collectors.joining("\n")));
        System.out.println("\nAvailable lines:\n" + availableLines.stream().map(Line.Info::toString).collect(Collectors.joining("\n")));
        return availableLines.stream().map(Line.Info::toString).toList();

    }

    public static String AnalyzeControl(Control thisControl) {
        String type = thisControl.getType().toString();
        if (thisControl instanceof BooleanControl) {
            return "\t\t    Control: " + type + " (boolean)";
        }
        if (thisControl instanceof CompoundControl) {
            System.out.println("\t\t    Control: " + type +
                    " (compound - values below)");
            String toReturn = "";
            for (Control children : ((CompoundControl) thisControl).getMemberControls()) {
                toReturn += "  " + AnalyzeControl(children) + "\n";
            }
            return toReturn.substring(0, toReturn.length() - 1);
        }
        if (thisControl instanceof EnumControl) {
            return "\t\t    Control:" + type + " (enum: " + thisControl.toString() + ")";
        }
        if (thisControl instanceof FloatControl) {
            return "\t\t    Control: " + type + " (float: from " +
                    ((FloatControl) thisControl).getMinimum() + " to " +
                    ((FloatControl) thisControl).getMaximum() + ")";
        }
        return "    Control: unknown type";
    }

    public static List<String> outputs() {
        return Collections.emptyList();
    }

}
