package teaselib.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * @author Citizen-Cane
 *
 */
// TODO Rename to IOStream to resolve name collision with java.util.stream.Stream
public class Stream {

    private Stream() {}

    public static void copy(byte[] buf, OutputStream os) throws IOException {
        os.write(buf, 0, buf.length);
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        int i;
        int size = 1024 * 1024;
        byte[] b = new byte[size];
        while ((i = is.read(b, 0, size)) != -1) {
            os.write(b, 0, i);
        }
    }

    public static void copy(InputStream is, OutputStream os, long s) throws IOException {
        int i;
        int bufferSize = 1024 * 1024;
        byte[] b = new byte[bufferSize];
        long reamaining = s;
        while ((i = is.read(b, 0, Math.min(bufferSize, (int) reamaining))) != -1) {
            reamaining -= i;
            os.write(b, 0, i);
            if (reamaining == 0) {
                break;
            }
        }
    }

    public static boolean sameContent(InputStream is1, InputStream is2) throws IOException {
        try (ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
                ByteArrayOutputStream bos2 = new ByteArrayOutputStream();) {
            copy(is1, bos1);
            copy(is2, bos2);
            byte[] ba1 = bos1.toByteArray();
            byte[] ba2 = bos2.toByteArray();

            if (ba1.length != ba2.length) {
                return false;
            }

            for (int i = 0; i < ba1.length; i++) {
                if (ba1[i] != ba2[i]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String toString(InputStream inputStream) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);) {
            java.util.stream.Stream<String> lines = bufferedReader.lines();
            return lines.collect(Collectors.joining(System.lineSeparator()));
        } finally {
            inputStream.close();
        }
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copy(inputStream, baos);
            return baos.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    public static InputStream temporaryFileInputStream(Path file) throws IOException {
        var stream = Files.newInputStream(file);
        return new InputStream() {
            @Override
            public void close() throws IOException {
                Files.delete(file);
                stream.close();
            }

            @Override
            public int read() throws IOException {
                return stream.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return stream.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return stream.read(b, off, len);
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                return stream.readAllBytes();
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                return stream.readNBytes(len);
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                return stream.readNBytes(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return stream.skip(n);
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                stream.skipNBytes(n);
            }

            @Override
            public int available() throws IOException {
                return stream.available();
            }

            @Override
            public synchronized void mark(int readlimit) {
                stream.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                stream.reset();
            }

            @Override
            public boolean markSupported() {
                return stream.markSupported();
            }

            @Override
            public long transferTo(OutputStream out) throws IOException {
                return stream.transferTo(out);
            }

        };
    }
}
