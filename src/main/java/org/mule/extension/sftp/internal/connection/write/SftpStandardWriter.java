package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.internal.connection.SftpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public class SftpStandardWriter implements SftpWriter {

    private SftpClient sftpClient;

    public SftpStandardWriter(SftpClient sftpClient) {
        this.sftpClient = sftpClient;
    }

    @Override
    public void write(String path, InputStream stream, FileWriteMode mode, URI uri) throws IOException {
        try (OutputStream out = sftpClient.getOutputStream(path, mode)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }

    }
}
