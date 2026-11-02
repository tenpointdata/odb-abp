package com.orientechnologies.orient.etl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.orient.server.plugin.OServerPluginLoader;
import org.junit.Test;

public class OETLPluginLoaderTest {

  /**
   * The server resolves the plugin declared in the ETL plugin.json ("javaClass") through the
   * {@link OServerPluginLoader} service registrations, so the ETL loader must be registered.
   */
  @Test
  public void etlPluginIsDiscoverableByTheServer() {
    final OServerPluginLoader loader =
        OServerPluginLoader.loadersByClassName().get(OETLPlugin.class.getName());

    assertNotNull("ETL plugin loader is not registered in META-INF/services", loader);
    assertTrue(loader.newInstance() instanceof OETLPlugin);
  }
}
