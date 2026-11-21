package com.orientechnologies.orient.core.index;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

/** Dropping an index removes its entries in batches of 1000, not one transaction per entry. */
public class IndexDropBatchingTest extends BaseMemoryDatabase {

  @Test
  public void testDropIndexCommitsInBatches() {
    OClass cls = db.getMetadata().getSchema().createClass("Item");
    cls.createProperty("code", OType.INTEGER).createIndex(OClass.INDEX_TYPE.NOTUNIQUE);

    final int entries = 2500;
    db.begin();
    for (int i = 0; i < entries; i++) db.save(new ODocument("Item").field("code", i));
    db.commit();

    AtomicInteger commits = new AtomicInteger();
    ODatabaseListener counter =
        new ODatabaseListener() {
          @Override
          public void onBeforeTxCommit(ODatabase iDatabase) {
            commits.incrementAndGet();
          }
        };
    db.registerListener(counter);
    try {
      db.command("DROP INDEX Item.code").close();
    } finally {
      db.unregisterListener(counter);
    }

    ODatabaseDocumentInternal database = (ODatabaseDocumentInternal) db;
    Assert.assertNull(
        database.getMetadata().getIndexManagerInternal().getIndex(database, "Item.code"));
    // 2500 entries in batches of 1000 is 3 commits plus the final one
    Assert.assertTrue("commits: " + commits.get(), commits.get() <= entries / 1000 + 2);
  }
}
