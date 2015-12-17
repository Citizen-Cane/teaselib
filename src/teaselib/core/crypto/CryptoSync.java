/**
 * 
 */
package teaselib.core.crypto;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import teaselib.core.util.FileUtilites;

/**
 * Synchronizes a set of files to encrypted versions and back.
 * <p>
 * 
 * Only the encrypted files have to be checked in, they're restored on checkout
 * when the project is updated.
 * <p>
 * As a result, the images cannot be inspected by nosy snoops, and can safely be
 * stored on public servers. The encryption also prevents the images from being
 * indexed by robots.
 * <p>
 * The private TeaseLib RSA key is used to restore the images.
 * 
 * @author someone
 *
 */
public class CryptoSync extends CipherUtility {

    /**
     * 
     */
    private static final String ENCODED_DATA = "encodedData";

    /**
     * 
     */
    private static final String ENCODED_KEY = "encodedKey";

    public static void main(String[] argv) throws GeneralSecurityException,
            IOException {
        if (argv.length < 2) {
            throw new IllegalArgumentException(CryptoSync.class.getSimpleName()
                    + ": decryptedDir encryptedDir extensions...");
        }
        File decryptedFiles = new File(argv[0]);
        File encryptedFiles = new File(argv[1]);
        System.out.println("Decrypted files: " + decryptedFiles);
        System.out.println("Encrypted files: " + encryptedFiles);
        final CryptoSync sync;
        if (argv.length > 2) {
            String[] extensions = Arrays.copyOfRange(argv, 2, argv.length);
            sync = new CryptoSync(decryptedFiles, encryptedFiles,
                    FileUtilites.getFileFilter(extensions));
            System.out.print(extensions.length + " extensions:");
            for (String extension : extensions) {
                System.out.print(" " + extension);
            }
            System.out.print(", resulting in ");
        } else {
            sync = new CryptoSync(decryptedFiles, encryptedFiles);
        }
        System.out.println(sync.size() + " items");
        sync.sync();
    }

    public static String EncryptedFileExtension = "encrypted";

    private final File decryptedDir;
    private final File encryptedDir;
    private final Collection<String> files;
    private final byte[] publicKey = CipherUtility
            .readKey(getKey(KeyStore.TeaseLibGeneralPublicKey));
    private final byte[] privateKey = CipherUtility
            .readKey(getKey(KeyStore.TeaseLibGeneralPrivateKey));

    public CryptoSync(File decryptedDir, File encryptedDir,
            Collection<String> files) throws IOException,
            GeneralSecurityException {
        checkValidOrThrow(decryptedDir);
        checkValidOrThrow(encryptedDir);
        this.decryptedDir = decryptedDir;
        this.encryptedDir = encryptedDir;
        this.files = files;
    }

    /**
     * Syncronizes files accepted by the {@code FileFilter} argument.
     * 
     * @param decryptedDir
     * @param encryptedDir
     * @param fileFilter
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public CryptoSync(File decryptedDir, File encryptedDir, FileFilter filter)
            throws IOException, GeneralSecurityException {
        this(checkValidOrThrow(decryptedDir), checkValidOrThrow(encryptedDir),
                fromFilter(decryptedDir, encryptedDir, filter));
    }

    public CryptoSync(File decryptedDir, File encryptedDir) throws IOException,
            GeneralSecurityException {
        this(checkValidOrThrow(decryptedDir), checkValidOrThrow(encryptedDir),
                allFiles(decryptedDir, encryptedDir));
    }

    private static File checkValidOrThrow(File path)
            throws FileNotFoundException, IOException {
        if (!path.exists())
            throw new FileNotFoundException(path.getPath());
        if (!path.isDirectory())
            throw new IOException(path.getPath() + ": Not a directory");
        return path;
    }

    private static Set<String> allFiles(File decryptedDir, File encryptedDir) {
        Set<String> files = new HashSet<String>();
        for (File file : decryptedDir.listFiles()) {
            if (!file.isDirectory()) {
                files.add(file.getName());
            }
        }
        for (File file : encryptedDir.listFiles()) {
            if (!file.isDirectory()) {
                files.add(getDecryptedName(file));
            }
        }
        return files;
    }

    private static Set<String> fromFilter(File decryptedDir, File encryptedDir,
            FileFilter filter) {
        Set<String> files = new HashSet<String>();
        for (File file : decryptedDir.listFiles(filter)) {
            files.add(file.getName());
        }
        for (File file : encryptedDir.listFiles()) {
            final String name = getDecryptedName(file);
            if (filter.accept(new File(name))) {
                files.add(name);
            }
        }
        return files;
    }

    public int size() {
        return files.size();
    }

    public void sync() throws IOException, GeneralSecurityException {
        Collection<String> filesToDecrypt = filesToDecrypt();
        Collection<String> filesToEncrypt = filesToEncrypt();
        decrypt(filesToDecrypt);
        encrypt(filesToEncrypt);
    }

    public void updateDecrypted() throws IOException, GeneralSecurityException {
        Collection<String> filesToDecrypt = filesToDecrypt();
        decrypt(filesToDecrypt);
    }

    public void updateEcrypted() throws IOException, GeneralSecurityException {
        Collection<String> filesToEncrypt = filesToEncrypt();
        encrypt(filesToEncrypt);
    }

    private Collection<String> filesToDecrypt() {
        Collection<String> filesToDecrypt = new Vector<String>();
        for (String name : files) {
            File encryptedFile = getEncryptedFile(name);
            if (getDecryptedFile(name).lastModified() < encryptedFile
                    .lastModified()) {
                filesToDecrypt.add(name);
            }
        }
        return filesToDecrypt;
    }

    private Collection<String> filesToEncrypt() {
        Collection<String> filesToEncrypt = new Vector<String>();
        for (String name : files) {
            final File decryptedFile = getDecryptedFile(name);
            if (decryptedFile.lastModified() > getEncryptedFile(name)
                    .lastModified()) {
                filesToEncrypt.add(name);
            }
        }
        return filesToEncrypt;
    }

    private void decrypt(Collection<String> filesToDecrypt)
            throws GeneralSecurityException, IOException {
        if (filesToDecrypt.size() > 0) {
            System.out
                    .println("Decrypting " + filesToDecrypt.size() + " items");
            for (String name : filesToDecrypt) {
                decrypt(name);
            }
        } else {
            System.out.println("Decrypted files are up to date");
        }
    }

    private void encrypt(Collection<String> filesToEncrypt)
            throws GeneralSecurityException, IOException {
        if (filesToEncrypt.size() > 0) {
            System.out
                    .println("Encrypting " + filesToEncrypt.size() + " items");
            for (String name : filesToEncrypt) {
                encrypt(name);
            }
        } else {
            System.out.println("Encrypted files are up to date");
        }
    }

    private void decrypt(String name) throws GeneralSecurityException,
            IOException {
        System.out.println("<< " + name);
        Decoder decoder = new Decoder();
        File zipFile = getEncryptedFile(name);
        File decryptedFile = getDecryptedFile(name);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(ENCODED_KEY)) {
                decoder.loadAESKey(zis, privateKey);
            } else if (entry.getName().equals(ENCODED_DATA)) {
                FileOutputStream os = new FileOutputStream(decryptedFile);
                decoder.decrypt(zis, os);
                os.close();
            }
        }
        zis.close();
        decryptedFile.setLastModified(zipFile.lastModified());
    }

    private void encrypt(String name) throws GeneralSecurityException,
            IOException {
        System.out.println(">> " + name);
        Encoder encoder = new Encoder();
        File zipFile = getEncryptedFile(name);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry encodedKey = new ZipEntry(ENCODED_KEY);
        zos.putNextEntry(encodedKey);
        encoder.saveAESKey(zos, publicKey);
        ZipEntry encodedData = new ZipEntry(ENCODED_DATA);
        zos.putNextEntry(encodedData);
        File decryptedFile = getDecryptedFile(name);
        FileInputStream is = new FileInputStream(decryptedFile);
        encoder.encrypt(is, zos, decryptedFile.length());
        is.close();
        zos.close();
        zipFile.setLastModified(decryptedFile.lastModified());
    }

    File getEncryptedFile(String name) {
        if (name.endsWith(EncryptedFileExtension)) {
            throw new IllegalArgumentException(name);
        }
        return new File(encryptedDir, name + "." + EncryptedFileExtension);
    }

    File getDecryptedFile(String name) {
        if (name.endsWith(EncryptedFileExtension)) {
            throw new IllegalArgumentException(name);
        }
        return new File(decryptedDir, name);
    }

    private static String getDecryptedName(File file) {
        String name = file.getName();
        if (name.endsWith(EncryptedFileExtension)) {
            return name.substring(0,
                    name.length() - EncryptedFileExtension.length() - 1);
        } else {
            return name;
        }
    }
}
