package teaselib.core.crypto;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encryption tests.
 */
public class CipherUtilityTests {
    private static final Logger logger = LoggerFactory.getLogger(CipherUtilityTests.class);

    File currentDir = new File(getClass().getResource(getClass().getSimpleName() + ".class").getPath()).getParentFile();
    File resources = new File(currentDir, getClass().getSimpleName() + " resources");
    File encryptedKeyFile = new File(resources, "encryptedKey.key");
    File[] testFiles;

    @BeforeClass
    public static void setUpBeforeClass() {
        // Installed ciphers
        for (Provider provider : Security.getProviders()) {
            logger.info("{}", provider.getName());
            SortedSet<String> sorted = new TreeSet<>(provider.stringPropertyNames());
            for (String key : sorted) {
                logger.info("\t{}\t{}", key, provider.getProperty(key));
            }
        }
    }

    @Before
    public void setUp() {
        testFiles = new File[] { new File(resources, "test1.png"), new File(resources, "test2.jpg"),
                new File(resources, "test3.jpg") };
    }

    private static boolean equalSize(File file1, File file2) throws IOException {
        long length1 = file1.length();
        long length2 = file2.length();
        if (length1 != length2) {
            throw new IOException("File size differs : " + file1.getName() + "=" + length1 + " , " + file2.getName()
                    + "=" + length2 + " , diff=" + (length2 - length1));
        }
        return true;
    }

    @Test
    public void testAESKey() throws IOException, GeneralSecurityException {
        AESKey aes = new AESKey();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            aes.save(bos);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            AESKey aesRestored = new AESKey(bis);
            assertEquals(aesRestored, aes);
        }
    }

    @Test
    public void testEncryptDecrypt() throws IOException, GeneralSecurityException {
        for (File fileToEncrypt : testFiles) {
            testFileEncryption(fileToEncrypt);
        }
    }

    public void testFileEncryption(File fileToEncrypt) throws GeneralSecurityException, IOException {
        File encryptedFile = new File(resources, fileToEncrypt.getName() + ".ncrptd");
        File unencryptedFile = new File(
                fileToEncrypt.getParent() + File.separator + "Copy of " + fileToEncrypt.getName());
        encryptedFile.delete();
        unencryptedFile.delete();

        encryptFile(fileToEncrypt, encryptedFile);
        decrpytFile(encryptedFile, unencryptedFile);

        assertTrue(equalSize(fileToEncrypt, unencryptedFile));
    }

    private void encryptFile(File fileToEncrypt, File encryptedFile)
            throws GeneralSecurityException, IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, FileNotFoundException, InvalidAlgorithmParameterException {
        FileEncoder encoder = new FileEncoder();
        // to encrypt a file
        try (FileOutputStream encryptedKey = new FileOutputStream(encryptedKeyFile);) {
            encoder.saveAESKey(encryptedKey, CipherUtility.getKey(CipherUtility.KeyStore.TeaseLibGeneralPublicKey));
        }
        encoder.encrypt(fileToEncrypt, encryptedFile);
    }

    private void decrpytFile(File encryptedFile, File unencryptedFile) throws GeneralSecurityException, IOException {
        // to decrypt it again
        FileDecoder decoder = new FileDecoder();
        try (FileInputStream encryptedKey = new FileInputStream(encryptedKeyFile);) {
            decoder.loadAESKey(encryptedKey, CipherUtility.getKey(CipherUtility.KeyStore.TeaseLibGeneralPrivateKey));
        }
        decoder.decrypt(encryptedFile, unencryptedFile);
    }

    @Test
    public void testZipEncryption() throws IOException, GeneralSecurityException {
        for (File fileToEncrypt : testFiles) {
            testZipEncryption(fileToEncrypt);
        }
    }

    private void testZipEncryption(File fileToEncrypt) throws GeneralSecurityException, IOException {
        File zipFile = new File(resources, fileToEncrypt.getName() + ".zip");
        File unencryptedFile = new File(resources, "Copy of " + fileToEncrypt.getName());
        zipFile.delete();
        unencryptedFile.delete();

        // Encrypt the RSA-encoded key into the zip file
        encryptZipFile(fileToEncrypt, zipFile);
        // Decrypt the data in the zip-file by decrypting the AES key
        decrypt(zipFile, unencryptedFile);

        assertTrue(equalSize(fileToEncrypt, unencryptedFile));
    }

    private static void encryptZipFile(File fileToEncrypt, File zipFile) throws GeneralSecurityException, IOException {
        Encoder encoder = new Encoder();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));) {
            ZipEntry encodedKey = new ZipEntry("encodedKey");
            zos.putNextEntry(encodedKey);
            encoder.saveAESKey(zos, CipherUtility.getKey(CipherUtility.KeyStore.TeaseLibGeneralPublicKey));
            ZipEntry encodedData = new ZipEntry("encodedData");
            zos.putNextEntry(encodedData);
            try (FileInputStream is = new FileInputStream(fileToEncrypt);) {
                encoder.encrypt(is, zos, fileToEncrypt.length());
            }
        }
    }

    private static void decrypt(File zipFile, File unencryptedFile) throws GeneralSecurityException, IOException {
        Decoder decoder = new Decoder();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                logger.info("Extracting: {}", entry);
                // The sequence is important
                if (entry.getName().equals("encodedKey")) {
                    try (InputStream privateKey = CipherUtility
                            .getKey(CipherUtility.KeyStore.TeaseLibGeneralPrivateKey);) {
                        decoder.loadAESKey(zis, privateKey);
                    }
                } else if (entry.getName().equals("encodedData")) {
                    try (FileOutputStream os = new FileOutputStream(unencryptedFile);) {
                        decoder.decrypt(zis, os);
                    }
                }
            }
        }
    }
}
