/**
 * 
 */
package teaselib.core.crypto;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import teaselib.core.util.FileUtilities;

/**
 * @author someone
 *
 */
public class CryptoSyncTests {
    File currentDir = new File(getClass().getResource(
            getClass().getSimpleName() + ".class").getPath()).getParentFile();
    File resources = new File(currentDir, getClass().getSimpleName()
            + " resources");
    File decryptedTestData = new File(resources, "assets");
    File encryptedTestData = new File(resources, "encrypted assets");

    final static List<String> testFiles = Arrays.asList("test (1).png",
            "test (2).png", "more/test (3).png", "more/test (4).png",
            "marquis1.jpg", "more/marquis2.jpg");

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testFileNaming() throws Exception {
        CryptoSync cryptoSync = new CryptoSync(decryptedTestData,
                encryptedTestData);
        assertEquals(testFiles.get(0),
                cryptoSync.getDecryptedFile(testFiles.get(0)).getName());
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Creates a fresh set of encrypted test data, which can be used to ensure
     * existing encrypted files can still be decrypted.
     * <p>
     * 
     * @throws IOException
     * @throws GeneralSecurityException
     */
    // @Test
    public void createEncryptedTestData() throws IOException,
            GeneralSecurityException {
        CryptoSync cryptoSync = new CryptoSync(decryptedTestData,
                encryptedTestData);
        cryptoSync.sync();
    }

    @Test
    public void testFileSync() throws IOException, GeneralSecurityException {
        final File encryptedFiles = temporaryFolder.newFolder();
        CryptoSync cryptoSync = new CryptoSync(decryptedTestData,
                encryptedFiles);
        cryptoSync.sync();
        final File decryptedCopies = temporaryFolder.newFolder();
        CryptoSync cryptoSyncCopy = new CryptoSync(decryptedCopies,
                encryptedFiles);
        cryptoSyncCopy.sync();
        for (String name : testFiles) {
            assertTrue(cryptoSync.getDecryptedFile(name).exists());
            assertTrue(cryptoSync.getEncryptedFile(name).exists());
            assertTrue(cryptoSyncCopy.getDecryptedFile(name).exists());
            assertTrue(cryptoSyncCopy.getEncryptedFile(name).exists());
            assertTrue(FileUtilities.sameContent(
                    cryptoSync.getDecryptedFile(name),
                    cryptoSyncCopy.getDecryptedFile(name)));
        }
    }

    @Test
    public void ensurePersistedEncryptionWorks() throws IOException,
            GeneralSecurityException {
        final File decryptedFiles = temporaryFolder.newFolder();
        CryptoSync cryptoSync = new CryptoSync(decryptedFiles,
                encryptedTestData);
        CryptoSync cryptoSyncOriginal = new CryptoSync(decryptedTestData,
                encryptedTestData);
        cryptoSync.sync();
        for (String name : testFiles) {
            assertTrue(cryptoSync.getDecryptedFile(name).exists());
            assertTrue(cryptoSync.getEncryptedFile(name).exists());
            assertTrue(cryptoSyncOriginal.getDecryptedFile(name).exists());
            assertTrue(cryptoSyncOriginal.getEncryptedFile(name).exists());
            assertTrue(FileUtilities.sameContent(
                    cryptoSync.getDecryptedFile(name),
                    cryptoSyncOriginal.getDecryptedFile(name)));
        }
    }

    @Test
    public void testEncryptedTestDataIsComplete()
            throws GeneralSecurityException, IOException {
        final File noFiles = temporaryFolder.newFolder();
        assertEquals(6, new CryptoSync(noFiles, encryptedTestData).size());
    }

    @Test
    public void testFileFilter() throws GeneralSecurityException, IOException {
        assertEquals(2, new CryptoSync(decryptedTestData, encryptedTestData,
                FileUtilities.getFileFilter("jpg")).size());
        assertEquals(4, new CryptoSync(decryptedTestData, encryptedTestData,
                FileUtilities.getFileFilter("png")).size());
        assertEquals(6, new CryptoSync(decryptedTestData, encryptedTestData,
                FileUtilities.getFileFilter("jpg", "png")).size());
        final File noFiles = temporaryFolder.newFolder();
        assertEquals(
                2,
                new CryptoSync(noFiles, encryptedTestData, FileUtilities
                        .getFileFilter("jpg")).size());
        assertEquals(
                4,
                new CryptoSync(noFiles, encryptedTestData, FileUtilities
                        .getFileFilter("png")).size());
        assertEquals(
                6,
                new CryptoSync(noFiles, encryptedTestData, FileUtilities
                        .getFileFilter("jpg", "png")).size());
    }
}
