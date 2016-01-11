/**
 * 
 */
package teaselib.core.util;

/**
 * @author someone
 *
 */
public enum Environment {
    Windows, OSX, Linux, Android, Other;

    public enum Arch {
        x86, x64, Unknown
    }

    public final static Environment SYSTEM = getSystem();
    public final static Arch ARCH = getArch();

    private static Environment getSystem() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            return Windows;
        } else if (os.indexOf("mac") >= 0) {
            return OSX;
        } else if (os.indexOf("nux") > 0) {
            return Linux;
        } else if (os.indexOf("android") >= 0) {
            return Android;
        } else {
            return Other;
        }
    }

    private static Arch getArch() {
        final String architecture = System.getProperty("os.arch");
        if (architecture.equalsIgnoreCase("x86")) {
            return Arch.x86;
        } else if (architecture.equalsIgnoreCase("amd64")) {
            return Arch.x64;
        } else {
            throw new UnsupportedOperationException(
                    "LoadLibrary() processor architecture " + architecture
                            + " not supported yet");
        }
    }
}
