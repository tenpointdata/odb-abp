package com.orientechnologies.tinkerpop.server;

import static org.junit.Assert.assertNotNull;

import com.orientechnologies.orient.server.plugin.OServerPluginLoader;
import com.orientechnologies.tinkerpop.handler.OGraphServerHandler;
import org.junit.Test;

public class OGremlinServerPluginLoaderTest {

  /**
   * The loaders are registered by the module that contains their classes, so a server that only
   * has the Gremlin driver jar on its classpath can still discover its other plugins.
   */
  @Test
  public void gremlinServerPluginsAreDiscoverable() {
    final var loaders = OServerPluginLoader.loadersByClassName();

    assertNotNull(loaders.get(OGremlinServerPlugin.class.getName()));
    assertNotNull(loaders.get(OGraphServerHandler.class.getName()));
  }
}
