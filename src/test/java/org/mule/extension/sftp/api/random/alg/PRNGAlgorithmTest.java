package org.mule.extension.sftp.api.random.alg;

import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.common.util.security.eddsa.EdDSASecurityProviderRegistrar;
import org.junit.Test;

import java.security.Provider;

public class PRNGAlgorithmTest {



    @Test
    public void testRandomSecurityProvider() {
        SingletonRandomFactory singletonRandomFactory = new SingletonRandomFactory(SecurityUtils.getRandomFactory());
//        Provider securityProvider = new EdDSASecurityProviderRegistrar().getSecurityProvider();
    }
}