/**
 * 
 */
package teaselib.core.crypto;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

import teaselib.core.util.Stream;

/**
 * @author someone
 *
 */
public class Decoder extends CipherUtility {

    public Decoder() throws GeneralSecurityException {
        super();
    }

    public void makeAESKey() throws GeneralSecurityException {
        aesKey = new AESKey();
    }

    /**
     * Decrypts an AES key from a stream using an RSA private key
     */
    public void loadAESKey(InputStream is, InputStream privateKeyInputStream)
            throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException,
            NoSuchPaddingException {
        // read private key to be used to decrypt the AES key
        byte[] encodedKey = readKey(privateKeyInputStream);
        // create private key
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory kf = KeyFactory.getInstance(PublicKeyEncryptionAlgorithm);
        PrivateKey pk = kf.generatePrivate(privateKeySpec);
        pkCipher.init(Cipher.DECRYPT_MODE, pk);
        // read AES key, cannot be closed because closing the cipher stream
        // would close the input stream
        @SuppressWarnings("resource")
        CipherInputStream cis = new CipherInputStream(is, pkCipher);
        aesKey = new AESKey(cis);
    }

    public void decrypt(InputStream is, OutputStream os)
            throws InvalidKeyException, IOException,
            InvalidAlgorithmParameterException {
        CipherInputStream cis = aesKey.getDecryptionStream(is);
        // Cannot close the data input stream, as this would also close the
        // cipher input stream as well as the original input stream,
        // which must not be closed
        @SuppressWarnings("resource")
        DataInputStream dis = new DataInputStream(cis);
        long size = dis.readLong();
        Stream.copy(cis, os, size);
    }

}
