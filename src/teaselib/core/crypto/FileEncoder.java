/**
 * 
 */
package teaselib.core.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

/**
 * @author someone
 *
 */
public class FileEncoder extends Encoder {

    public FileEncoder() throws GeneralSecurityException {
        super();
    }

    /**
     * Encrypts the AES key to a file using an RSA public key
     */
    public void saveAESKey(File publicKeyFile, File encryptedKeyFile)
            throws IOException, GeneralSecurityException {
        FileInputStream publicKeyInputStream = new FileInputStream(
                publicKeyFile);
        FileOutputStream fileOutputStream = new FileOutputStream(
                encryptedKeyFile);
        saveAESKey(fileOutputStream, publicKeyInputStream);
        publicKeyInputStream.close();
        fileOutputStream.close();
    }

    /**
     * Encrypts and then copies the contents of a given file.
     */
    public void encrypt(File in, File out) throws IOException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        FileInputStream is = new FileInputStream(in);
        FileOutputStream os = new FileOutputStream(out);
        encrypt(is, os, is.getChannel().size());
        is.close();
        os.close();
    }

}
