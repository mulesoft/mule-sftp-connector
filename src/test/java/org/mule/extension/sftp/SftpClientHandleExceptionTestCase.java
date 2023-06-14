package org.mule.extension.sftp;

import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.extension.sftp.api.SftpConnectionException;
import org.mule.extension.sftp.internal.connection.SftpClient;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class SftpClientHandleExceptionTestCase {

    private final SftpClient sftpMuleClient;
    private final String errorMessage;
    private final int sftpErrorCode;
    private final Class<? extends Exception> expectedException;

    public SftpClientHandleExceptionTestCase(SftpClient sftpMuleClient, String errorMessage, int sftpErrorCode,
                                             Class<? extends Exception> expectedException) {
        this.sftpMuleClient = sftpMuleClient;
        this.errorMessage = errorMessage;
        this.sftpErrorCode = sftpErrorCode;
        this.expectedException = expectedException;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {mock(SftpClient.class), "", SftpConstants.SSH_FX_NO_SUCH_FILE,
                SftpException.class},
            {mock(SftpClient.class), "", SftpConstants.SSH_FX_PERMISSION_DENIED,
                SftpException.class},
            {mock(SftpClient.class), "", SftpConstants.SSH_FX_CONNECTION_LOST,
                SftpConnectionException.class}
        });
    }

    @Test
    public void expectConnectionExceptionWhenHandleException() {
        when(sftpMuleClient.handleException(anyString(), any(Exception.class))).thenCallRealMethod();
        RuntimeException exception = sftpMuleClient.handleException(errorMessage, new SftpException(sftpErrorCode, ""));

        Assert.assertEquals(expectedException, exception.getCause().getClass());
        Assert.assertEquals(expectedException, exception.getCause().getClass());
    }
}
