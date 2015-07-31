package teaselib.core.jni;

import java.io.File;

import teaselib.core.util.Environment;
import teaselib.core.util.Environment.Arch;

public class LibraryLoader {

    public static void load(String name) {
        String extension;
        Environment system = Environment.SYSTEM;
        if (system == Environment.Windows) {
            extension = "dll";
        } else {
            throw new UnsupportedOperationException("LoadLibrary() " + system
                    + " not supported yet");
        }
        final String architecture;
        Arch arch = Environment.ARCH;
        if (arch == Environment.Arch.x86) {
            architecture = "x86";
        } else if (arch == Environment.Arch.x64) {
            architecture = "x64";
        } else {
            throw new UnsupportedOperationException(
                    "LoadLibrary() processor architecture " + arch
                            + " not supported yet");
        }
        // Release
        try {
            System.load(getAbsolutePath(new File("lib/TeaseLib"), name,
                    architecture, extension));
        } catch (UnsatisfiedLinkError e) {
            // try dev folder
            try {
                System.load(getAbsolutePath(new File("lib"), name,
                        architecture, extension));
            } catch (UnsatisfiedLinkError e1) {

                // throw the original exception
                throw e;
            }
        }
    }

    private static String getAbsolutePath(File folder, String name,
            final String architecture, String extension) {
        return new File(folder, name + "_" + architecture + "." + extension)
                .getAbsolutePath();
    }
}
