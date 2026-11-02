package com.orientechnologies.orient.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The console is how scripts such as backup.sh reach a running server. */
public class OConsoleRemoteConnectTest {
  private OServer server;

  @Before
  public void before() throws Exception {
    server = OServer.startFromClasspathConfig("abstract-orientdb-server-config.xml");
    try (OrientDB orientDB =
        new OrientDB("remote:localhost", "root", "root", OrientDBConfig.defaultConfig())) {
      orientDB.execute(
          "create database consoleRemoteConnect memory users (admin identified by 'admin' role"
              + " admin)");
      try (ODatabaseSession session = orientDB.open("consoleRemoteConnect", "admin", "admin")) {
        session.createClass("Item");
        session.save(new ODocument("Item").field("name", "first"));
      }
    }
  }

  @After
  public void after() {
    server.shutdown();
  }

  @Test
  public void connectsToARemoteDatabaseAndQueriesIt() {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final OConsoleDatabaseApp console =
        new OConsoleDatabaseApp(
            new String[] {
              "connect remote:localhost/consoleRemoteConnect admin admin;"
                  + "select count(*) as items from Item;"
            });
    console.setOutput(new PrintStream(out, true, StandardCharsets.UTF_8));
    try {
      assertEquals(0, console.run());
    } finally {
      console.close();
    }

    final String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output, output.contains("1 item(s) found"));
  }
}
