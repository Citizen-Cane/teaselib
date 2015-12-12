/**
 * 
 */
package teaselib.core.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import teaselib.core.util.Stream;

/**
 * @author someone
 *
 */
public class CipherUtility {

    /**
     * The symetric algorithm used to encrypt the data.
     */
    public static final String PublicKeyEncryptionAlgorithm = "RSA";

    static class TeaseLib {
        final static String PublicKey = "RSA_TeaseLibPublic2048.der";
        final static String PrivateKey = "RSA_TeaseLibPrivate2048.der";
    }

    public static InputStream getKey(String key) {
        return CipherUtility.class.getResourceAsStream(key);
    }

    Cipher pkCipher;
    AESKey aesKey;

    public CipherUtility() throws GeneralSecurityException {
        // create RSA public key cipher
        pkCipher = Cipher.getInstance(PublicKeyEncryptionAlgorithm);
    }

    protected static byte[] readKey(InputStream keyInputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Stream.copy(keyInputStream, baos);
        byte[] encodedKey = baos.toByteArray();
        keyInputStream.read(encodedKey);
        return encodedKey;
    }

}
