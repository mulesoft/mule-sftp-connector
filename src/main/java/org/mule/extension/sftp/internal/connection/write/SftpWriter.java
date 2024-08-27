package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.FileWriteMode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface SftpWriter {

    void write(String path, InputStream stream, FileWriteMode mode, URI uri) throws IOException;

}
