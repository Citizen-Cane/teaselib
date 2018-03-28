package teaselib.core.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

/**
 * @author Citizen-Cane
 *
 */
public class FileDecoder extends Decoder {

    public FileDecoder() throws GeneralSecurityException {
        super();
    }

    /**
     * Decrypts an AES key from a file using an RSA private key
     */
    public void loadAESKey(File in, File privateKeyFile) throws GeneralSecurityException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(in);
                FileInputStream privateKeyInputStream = new FileInputStream(privateKeyFile);) {
            loadAESKey(fileInputStream, privateKeyInputStream);
        }
    }

    /**
     * Decrypts and then copies the contents of a given file.
     */
    public void decrypt(File in, File out) throws IOException, InvalidKeyException, InvalidAlgorithmParameterException {
        try (FileInputStream is = new FileInputStream(in); FileOutputStream os = new FileOutputStream(out);) {
            decrypt(is, os);
        }
    }
}
