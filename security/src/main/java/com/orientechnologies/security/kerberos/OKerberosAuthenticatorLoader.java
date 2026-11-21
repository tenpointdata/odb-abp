package com.orientechnologies.security.kerberos;

import com.orientechnologies.orient.core.security.OSecurityAuthenticator;
import com.orientechnologies.orient.core.security.OSecurityAuthenticatorLoader;

public class OKerberosAuthenticatorLoader implements OSecurityAuthenticatorLoader {

  @Override
  public String getName() {
    return "kerberos";
  }

  @Override
  public String getClassName() {
    return OKerberosAuthenticator.class.getName();
  }

  @Override
  public OSecurityAuthenticator newInstance() {
    return new OKerberosAuthenticator();
  }
}
