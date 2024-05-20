package org.mule.extension.sftp.internal.connection;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class FileBasedConfigProviderTestCase {

  @Test
  public void testFetchConfigFileProperties() {
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider("src/test/resources/mule_sshd_config");
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(1, properties.size());
    assertEquals("diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha512@ssh.com,diffie-hellman-group1-sha1",
                 properties.getProperty("KexAlgorithms"));
  }

  @Test
  public void testFileNotFound() {
    FileBasedConfigProvider fileBasedConfigProvider = new FileBasedConfigProvider("mule_sshd");
    Properties properties = fileBasedConfigProvider.getConfigProperties();
    assertEquals(0, properties.size());
  }

}
