package com.orientechnologies.orient.core.sql.executor;

import static org.junit.Assert.assertEquals;

import com.orientechnologies.BaseMemoryDatabase;
import org.junit.Test;

/** An indexed DATETIME property compared with a computed long, as accepted by 3.2. */
public class OIndexedDateComputedValueTest extends BaseMemoryDatabase {

  private long count(String query) {
    try (OResultSet rs = db.query(query)) {
      return ((Number) rs.next().getProperty("c")).longValue();
    }
  }

  private void prepare(String indexType) {
    db.command("create class T");
    db.command("create property T.d DATETIME");
    db.command("create index T.d on T (d) " + indexType);
    db.command("insert into T set d = date('2000-01-11 00:00:00')");
    db.command("insert into T set d = date('2000-01-12 00:00:00')");
  }

  @Test
  public void computedLongMatchesNotUniqueIndexedDate() {
    prepare("NOTUNIQUE");
    assertEquals(
        1, count("select count(*) as c from T where d = date('2000-01-11 00:00:00').asLong()"));
  }

  @Test
  public void computedLongMatchesHashIndexedDate() {
    prepare("NOTUNIQUE_HASH_INDEX");
    assertEquals(
        1, count("select count(*) as c from T where d = date('2000-01-11 00:00:00').asLong()"));
  }

  @Test
  public void literalAndDateStillMatch() {
    prepare("NOTUNIQUE");
    assertEquals(1, count("select count(*) as c from T where d = date('2000-01-11 00:00:00')"));
    assertEquals(
        1,
        count(
            "select count(*) as c from T where d = "
                + db.query("select date('2000-01-11 00:00:00').asLong() as l")
                    .next()
                    .getProperty("l")));
  }
}
