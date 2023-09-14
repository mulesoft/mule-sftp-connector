package org.mule.extension.sftp.internal.command;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SftpWriteCommandTest {

    @Mock
    public SftpClient sftpClient;

    @Mock
    public SftpFileSystem sftpFileSystem;


    public SftpWriteCommand sftpWriteCommand;

    @Before
    public void setUp(){
        sftpWriteCommand = new SftpWriteCommand(sftpFileSystem, sftpClient);
    }

    @Test
    public void write() throws Exception {
        final Constructor<SftpATTRS> c = SftpATTRS.class.getDeclaredConstructor();
        c.setAccessible(true);
        final SftpATTRS sftpATTRS = c.newInstance();

        final String filePath = "test.txt";
        final FileWriteMode fileWriteMode = FileWriteMode.OVERWRITE;
        final InputStream stubInputStream = IOUtils.toInputStream("some test data for my input stream", "UTF-8");
        final URI uri = new URI("/upload/test.txt");

        when(sftpClient.getAttributes(any())).thenReturn(new SftpFileAttributes(uri, sftpATTRS));
        when(sftpFileSystem.getBasePath()).thenReturn("/upload");

        doThrow(new TimeoutException()).when(sftpClient).write(filePath, stubInputStream, fileWriteMode);

        try{
            sftpWriteCommand.write(filePath, stubInputStream, fileWriteMode, false, true);
        }catch (final Exception exception){
            verify(sftpClient).exitChannel();
        }
    }


}