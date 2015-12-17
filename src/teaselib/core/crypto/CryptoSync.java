/**
 * 
 */
package teaselib.core.crypto;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

    public static void main(String[] argv) {
    }

    public static String EncryptedFileExtension = "encrypted";

    private final File decryptedDir;
    private final File encryptedDir;
    private final Collection<String> files;
    private final byte[] publicKey = CipherUtility
            .readKey(getKey(KeyStore.TeaseLibGeneralPublicKey));
    private final byte[] privateKey = CipherUtility
            .readKey(getKey(KeyStore.TeaseLibGeneralPrivateKey));

    public CryptoSync(File decryptedDir, File encryptedDir) throws IOException,
            GeneralSecurityException {
        this.decryptedDir = decryptedDir;
        this.encryptedDir = encryptedDir;
        // Build files set
        files = new HashSet<String>();
        for (File file : decryptedDir.listFiles()) {
            files.add(file.getName());
        }
        for (File file : encryptedDir.listFiles()) {
            files.add(getDecryptedName(file));
        }
    }

    public CryptoSync(File decryptedDir, File encryptedDir,
            Collection<String> files) throws IOException,
            GeneralSecurityException {
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
        this.decryptedDir = decryptedDir;
        this.encryptedDir = encryptedDir;
        // Build files set
        files = new HashSet<String>();
        for (File file : decryptedDir.listFiles(filter)) {
            files.add(file.getName());
        }
        for (File file : encryptedDir.listFiles()) {
            final String name = getDecryptedName(file);
            if (filter.accept(new File(name))) {
                files.add(name);
            }
        }
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
        for (String name : filesToDecrypt) {
            decrypt(name);
        }
    }

    private void encrypt(Collection<String> filesToEncrypt)
            throws GeneralSecurityException, IOException {
        for (String name : filesToEncrypt) {
            encrypt(name);
        }
    }

    private void decrypt(String name) throws GeneralSecurityException,
            IOException {
        Decoder decoder = new Decoder();
        File zipFile = getEncryptedFile(name);
        File decryptedFile = getDecryptedFile(name);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            System.out.println("Extracting: " + entry);
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
