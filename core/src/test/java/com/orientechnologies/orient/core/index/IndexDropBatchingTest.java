package com.orientechnologies.orient.core.index;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

/** Dropping an index removes its entries in batches of 1000, not one transaction per entry. */
public class IndexDropBatchingTest extends BaseMemoryDatabase {

  @Test
  public void testDropIndexCommitsInBatches() {
    checkDropIndexCommitsInBatches(2500, 0);
  }

  @Test
  public void testDropIndexCommitsNullKeysInBatches() {
    checkDropIndexCommitsInBatches(0, 2500);
  }

  @Test
  public void testDropIndexCommitsMixedKeysInBatches() {
    checkDropIndexCommitsInBatches(1000, 1500);
  }

  private void checkDropIndexCommitsInBatches(int keyedEntries, int nullEntries) {
    OClass cls = db.getMetadata().getSchema().createClass("Item");
    OIndex index =
        cls.createProperty("code", OType.INTEGER)
            .createIndex(
                OClass.INDEX_TYPE.NOTUNIQUE, new ODocument().field("ignoreNullValues", false));

    db.begin();
    for (int i = 0; i < keyedEntries; i++) {
      db.save(new ODocument("Item").field("code", i));
    }
    for (int i = 0; i < nullEntries; i++) {
      db.save(new ODocument("Item"));
    }
    db.commit();
    try (Stream<ORID> rids = index.getInternal().getRids(null)) {
      Assert.assertEquals(nullEntries, rids.count());
    }

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
    int expectedBatches = (keyedEntries + 999) / 1000 + (nullEntries + 999) / 1000;
    // The DROP INDEX statement can add its own commit.
    Assert.assertTrue("commits: " + commits.get(), commits.get() >= expectedBatches);
    Assert.assertTrue("commits: " + commits.get(), commits.get() <= expectedBatches + 1);
  }
}
