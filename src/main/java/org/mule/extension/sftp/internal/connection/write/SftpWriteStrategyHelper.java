package org.mule.extension.sftp.internal.connection.write;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.WriteStrategy;

public class SftpWriteStrategyHelper {

    public static SftpWriter getStrategy(org.mule.extension.sftp.internal.connection.SftpClient muleSftpClient,
                                         org.apache.sshd.sftp.client.SftpClient apacheSftpClient,
                                         WriteStrategy writeStrategy, CustomWriteBufferSize bufferSizeForWriteStrategy) {
        switch (writeStrategy) {
            case CUSTOM:
                return new SftpCustomWriter(muleSftpClient, apacheSftpClient, bufferSizeForWriteStrategy);
            default: // STANDARD
                return new SftpStandardWriter(muleSftpClient);
        }
    }
}
