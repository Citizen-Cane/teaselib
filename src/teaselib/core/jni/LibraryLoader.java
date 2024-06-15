package teaselib.core.jni;

import java.nio.file.Files;
import java.nio.file.Path;

import teaselib.core.ResourceLoader;
import teaselib.core.util.Environment;
import teaselib.core.util.Environment.Arch;

public class LibraryLoader {

    private LibraryLoader() { //
    }

    public static void load(String name) {
        String library = library(name);
        Path projectPath = ResourceLoader.getProjectPath(LibraryLoader.class).toPath();
        Path releaseLocation = projectPath.getParent().resolve("lib");
        Path path = releaseLocation.resolve(library);
        if (!Files.exists(path)) {
            Path gradleLocation = projectPath.getParent().getParent().getParent().getParent().resolve("lib");
             path = gradleLocation.resolve(library);
        }
        System.load(path.toString());
    }

    private static String library(String name) {
        String extension;
        Environment system = Environment.SYSTEM;
        if (name.toLowerCase().endsWith(".dll") || name.toLowerCase().endsWith(".so")) {
            return name;
        } else {
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
                throw new UnsupportedOperationException(
                        "LoadLibrary() processor architecture " + arch + " unsupported");
            }
            return name + "_" + architecture + "." + extension;
        }
    }

}
