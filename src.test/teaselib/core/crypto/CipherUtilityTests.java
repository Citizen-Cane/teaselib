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

import javax.crypto.NoSuchPaddingException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Encryption tests.
 */
public class CipherUtilityTests {
    File currentDir = new File(getClass().getResource(
            getClass().getSimpleName() + ".class").getPath()).getParentFile();
    File resources = new File(currentDir, getClass().getSimpleName()
            + " resources");
    File encryptedKeyFile = new File(resources, "encryptedKey.key");
    File[] testFiles;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Installed ciphers
        for (Provider provider : Security.getProviders()) {
            System.out.println(provider.getName());
            SortedSet<String> sorted = new TreeSet<String>(
                    provider.stringPropertyNames());
            for (String key : sorted) {
                System.out.println("\t" + key + "\t"
                        + provider.getProperty(key));
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        testFiles = new File[] { new File(resources, "test1.png"),
                new File(resources, "test2.jpg"),
                new File(resources, "test3.jpg") };
    }

    private static boolean equalSize(File file1, File file2) throws IOException {
        long length1 = file1.length();
        long length2 = file2.length();
        if (length1 != length2) {
            throw new IOException("File size differs : " + file1.getName()
                    + "=" + length1 + " , " + file2.getName() + "=" + length2
                    + " , diff=" + (length2 - length1));
        }
        return true;
    }

    @Test
    public void testAESKey() throws IOException, GeneralSecurityException {
        AESKey aes = new AESKey();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        aes.save(bos);
        bos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        AESKey aesRestored = new AESKey(bis);
        assertEquals(aesRestored, aes);
    }

    @Test
    public void testEncryptDecrypt() throws IOException,
            GeneralSecurityException {
        for (File fileToEncrypt : testFiles) {
            testFileEncryption(fileToEncrypt);
        }
    }

    public void testFileEncryption(File fileToEncrypt)
            throws GeneralSecurityException, NoSuchAlgorithmException,
            IOException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        File encryptedFile = new File(resources, fileToEncrypt.getName()
                + ".ncrptd");
        File unencryptedFile = new File(fileToEncrypt.getParent()
                + File.separator + "Copy of " + fileToEncrypt.getName());
        encryptedFile.delete();
        unencryptedFile.delete();
        {
            FileEncoder encoder = new FileEncoder();
            // to encrypt a file
            FileOutputStream encryptedKey = new FileOutputStream(
                    encryptedKeyFile);
            encoder.saveAESKey(encryptedKey, CipherUtility
                    .getKey(CipherUtility.KeyStore.TeaseLibGeneralPublicKey));
            encryptedKey.close();
            encoder.encrypt(fileToEncrypt, encryptedFile);
        }
        {
            // to decrypt it again
            FileDecoder decoder = new FileDecoder();
            final FileInputStream encryptedKey = new FileInputStream(
                    encryptedKeyFile);
            decoder.loadAESKey(encryptedKey, CipherUtility
                    .getKey(CipherUtility.KeyStore.TeaseLibGeneralPrivateKey));
            encryptedKey.close();
            decoder.decrypt(encryptedFile, unencryptedFile);
        }
        assertTrue(equalSize(fileToEncrypt, unencryptedFile));
    }

    @Test
    public void testZipEncryption() throws IOException,
            GeneralSecurityException {
        for (File fileToEncrypt : testFiles) {
            testZipEncryption(fileToEncrypt);
        }
    }

    public void testZipEncryption(File fileToEncrypt)
            throws GeneralSecurityException, NoSuchAlgorithmException,
            FileNotFoundException, IOException, InvalidKeySpecException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        File zipFile = new File(resources, fileToEncrypt.getName() + ".zip");
        File unencryptedFile = new File(resources, "Copy of "
                + fileToEncrypt.getName());
        zipFile.delete();
        unencryptedFile.delete();
        // Encrypt the RSA-encoded key into the zip file
        {
            encrypt(fileToEncrypt, zipFile);
        }
        // Decrypt the data in the zip-file by decrypting the AES key
        {
            decrypt(zipFile, unencryptedFile);
        }
        assertTrue(equalSize(fileToEncrypt, unencryptedFile));
    }

    public void encrypt(File fileToEncrypt, File zipFile)
            throws GeneralSecurityException, FileNotFoundException,
            IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        Encoder encoder = new Encoder();
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry encodedKey = new ZipEntry("encodedKey");
        zos.putNextEntry(encodedKey);
        encoder.saveAESKey(zos, CipherUtility
                .getKey(CipherUtility.KeyStore.TeaseLibGeneralPublicKey));
        ZipEntry encodedData = new ZipEntry("encodedData");
        zos.putNextEntry(encodedData);
        FileInputStream is = new FileInputStream(fileToEncrypt);
        encoder.encrypt(is, zos, fileToEncrypt.length());
        is.close();
        zos.close();
    }

    public void decrypt(File zipFile, File unencryptedFile)
            throws GeneralSecurityException, FileNotFoundException,
            IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, NoSuchPaddingException,
            InvalidAlgorithmParameterException {
        Decoder decoder = new Decoder();
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            System.out.println("Extracting: " + entry);
            // The sequence is important
            if (entry.getName().equals("encodedKey")) {
                InputStream privateKey = CipherUtility
                        .getKey(CipherUtility.KeyStore.TeaseLibGeneralPrivateKey);
                decoder.loadAESKey(zis, privateKey);
                privateKey.close();
            } else if (entry.getName().equals("encodedData")) {
                FileOutputStream os = new FileOutputStream(unencryptedFile);
                decoder.decrypt(zis, os);
                os.close();
            }
        }
        zis.close();
    }
}
