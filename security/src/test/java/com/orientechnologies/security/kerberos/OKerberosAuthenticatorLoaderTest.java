package com.orientechnologies.security.kerberos;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.orient.core.security.OSecurityAuthenticatorLoader;
import org.junit.Test;

public class OKerberosAuthenticatorLoaderTest {

  /**
   * Authenticators configured in security.json by "class" are only instantiated through the
   * {@link OSecurityAuthenticatorLoader} service registrations.
   */
  @Test
  public void kerberosAuthenticatorCanBeConfigured() {
    final OSecurityAuthenticatorLoader loader =
        OSecurityAuthenticatorLoader.loadersByClassName()
            .get(OKerberosAuthenticator.class.getName());

    assertNotNull("Kerberos authenticator loader is not registered", loader);
    assertTrue(loader.newInstance() instanceof OKerberosAuthenticator);
  }
}
