/**
 * 
 */
package teaselib.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import teaselib.core.util.Stream;

/**
 * @author someone
 *
 */
public class Encoder extends CipherUtility {

    public Encoder() throws GeneralSecurityException {
        super();
        aesKey = new AESKey();
    }

    public void saveAESKey(OutputStream encryptedKeyOutputStream, InputStream publicKeyInputStream)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        // read public key to be used to encrypt the AES key
        byte[] encodedKey = readKey(publicKeyInputStream);
        saveAESKey(encryptedKeyOutputStream, encodedKey);
    }

    public void saveAESKey(OutputStream encryptedKeyOutputStream, byte[] publicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IOException {
        // create public key
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
        KeyFactory kf = KeyFactory.getInstance(PublicKeyEncryptionAlgorithm);
        PublicKey pk = kf.generatePublic(publicKeySpec);
        // write AES key
        pkCipher.init(Cipher.ENCRYPT_MODE, pk);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            try (CipherOutputStream cos = new CipherOutputStream(bos, pkCipher);) {
                aesKey.save(cos);
            }
            // write encrypted AES key to output stream
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());) {
                Stream.copy(bis, encryptedKeyOutputStream);
            }
        }
    }

    public void encrypt(InputStream is, OutputStream os, long size)
            throws InvalidKeyException, IOException, InvalidAlgorithmParameterException {
        try (CipherOutputStream cos = aesKey.getEncryptionStream(os);) {
            try (DataOutputStream dos = new DataOutputStream(cos);) {
                dos.writeLong(size);
                Stream.copy(is, dos);
            }
        }
    }
}
