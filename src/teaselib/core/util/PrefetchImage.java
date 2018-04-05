package teaselib.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.ResourceLoader;

public final class PrefetchImage implements Callable<byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(PrefetchImage.class);

    private final String resourcePath;
    private final ResourceLoader resources;
    private final Configuration config;

    public PrefetchImage(String resourcePath, ResourceLoader resources, Configuration config) {
        this.resourcePath = resourcePath;
        this.resources = resources;
        this.config = config;
    }

    @Override
    public byte[] call() throws Exception {
        return getImageBytes(resourcePath);
    }

    private byte[] getImageBytes(String path) throws IOException {
        byte[] imageBytes = null;
        try (InputStream resource = resources.getResource(path);) {
            imageBytes = convertInputStreamToByte(resource);
        } catch (IOException e) {
            ExceptionUtil.handleIOException(e, config, logger);
        }
        return imageBytes;
    }

    private static byte[] convertInputStreamToByte(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = is.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }
}