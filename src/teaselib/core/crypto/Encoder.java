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

    public void saveAESKey(OutputStream encryptedKeyOutputStream,
            InputStream publicKeyInputStream) throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException {
        // read public key to be used to encrypt the AES key
        byte[] encodedKey = readKey(publicKeyInputStream);
        // create public key
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory kf = KeyFactory.getInstance(PublicKeyEncryptionAlgorithm);
        PublicKey pk = kf.generatePublic(publicKeySpec);
        // write AES key
        pkCipher.init(Cipher.ENCRYPT_MODE, pk);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherOutputStream cos = new CipherOutputStream(bos, pkCipher);
        aesKey.save(cos);
        // cos.write(aesKey);
        cos.close();
        bos.close();
        // write encrypted AES key to output stream
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Stream.copy(bis, encryptedKeyOutputStream);
        bis.close();
    }

    public void encrypt(InputStream is, OutputStream os, long size)
            throws InvalidKeyException, IOException,
            InvalidAlgorithmParameterException {
        CipherOutputStream cos = aesKey.getEncryptionStream(os);
        DataOutputStream dos = new DataOutputStream(cos);
        dos.writeLong(size);
        Stream.copy(is, dos);
        dos.close();
        cos.close();
    }

}
