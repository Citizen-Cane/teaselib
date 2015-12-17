package teaselib.core.crypto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class AESKey {
    /**
     * The asymetric public key algorithm used to encrypt the data symetric
     * encryption key.
     */
    private static final String DataEncryptionAlgorithm = "AES";

    /**
     * Require padding, actual data size is stored in RSA encoded key together
     * with the IV
     */
    private static final String DataEncryptionAlgorithmTransformation = "AES/CBC/PKCS5Padding"; // AES/CBC/PKCS5Padding

    /**
     * All java distributions support at least 128bit
     */
    private static final int AES_Default_Key_Size = 128;

    private final byte[] key;
    private final transient SecretKeySpec spec;
    private final transient Cipher cipher;
    private static final SecureRandom secureRandom = new SecureRandom();

    public AESKey() throws NoSuchAlgorithmException, GeneralSecurityException {
        KeyGenerator kgen = KeyGenerator.getInstance(DataEncryptionAlgorithm);
        kgen.init(AES_Default_Key_Size);
        SecretKey secretKey = kgen.generateKey();
        key = secretKey.getEncoded();
        spec = new SecretKeySpec(key, AESKey.DataEncryptionAlgorithm);
        cipher = Cipher.getInstance(DataEncryptionAlgorithmTransformation);
    }

    public AESKey(InputStream inputStream) throws IOException,
            NoSuchPaddingException, NoSuchAlgorithmException {
        DataInputStream dis = new DataInputStream(inputStream);
        final int aesKeyLength = dis.readInt();
        key = new byte[aesKeyLength];
        dis.readFully(this.key);
        spec = new SecretKeySpec(key, AESKey.DataEncryptionAlgorithm);
        cipher = Cipher.getInstance(DataEncryptionAlgorithmTransformation);
    }

    void save(OutputStream outputStream) throws IOException {
        DataOutputStream dos = new DataOutputStream(outputStream);
        int keyLength = key.length;
        dos.writeInt(keyLength);
        dos.write(key);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(key);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AESKey other = (AESKey) obj;
        if (!Arrays.equals(key, other.key))
            return false;
        return true;
    }

    CipherOutputStream getEncryptionStream(OutputStream os) throws IOException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        int ivLength = iv.length;
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(ivLength);
        dos.write(iv);
        cipher.init(Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(iv));
        CipherOutputStream cos = new CipherOutputStream(os, cipher);
        return cos;
    }

    CipherInputStream getDecryptionStream(InputStream is) throws IOException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        DataInputStream dis = new DataInputStream(is);
        final int ivLength = dis.readInt();
        byte[] iv = new byte[ivLength];
        dis.readFully(iv);
        cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(iv));
        CipherInputStream cis = new CipherInputStream(is, cipher);
        return cis;
    }
}
