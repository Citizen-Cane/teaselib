package teaselib.core.jni;

import java.nio.file.Path;

import teaselib.core.ResourceLoader;
import teaselib.core.util.Environment;
import teaselib.core.util.Environment.Arch;

public class LibraryLoader {

    private LibraryLoader() { //
    }

    public static void load(String name) {
        String library = library(name);
        Path releaseLocation = ResourceLoader.getProjectPath(LibraryLoader.class).toPath().getParent().resolve("lib");
        System.load(releaseLocation.resolve(library).toString());
    }

    private static String library(String name) {
        String extension;
        Environment system = Environment.SYSTEM;
        if (system == Environment.Windows) {
            extension = "dll";
        } else {
            throw new UnsupportedOperationException("LoadLibrary() " + system + " unsupported");
        }
        final String architecture;
        Arch arch = Environment.ARCH;
        if (arch == Environment.Arch.x64) {
            architecture = "x64";
        } else {
            throw new UnsupportedOperationException("LoadLibrary() processor architecture " + arch + " unsupported");
        }
        return name + "_" + architecture + "." + extension;
    }

}
