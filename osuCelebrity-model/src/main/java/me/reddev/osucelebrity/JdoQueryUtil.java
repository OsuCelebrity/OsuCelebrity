package me.reddev.osucelebrity;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.jdo.JDOQuery;

import java.util.Optional;
import javax.jdo.PersistenceManager;

/**
 * Utility class for common JDO queries.
 */
public class JdoQueryUtil {
  /**
   * Get a unique object.
   * 
   * @param pm the persistence manager.
   * @param table the table to select from.
   * @param where where clause.
   * @return an the unique queried value.
   */
  public static <T> Optional<T> getUnique(PersistenceManager pm, EntityPath<T> table,
      Predicate... where) {
    try (JDOQuery<T> query = new JDOQuery<T>(pm).select(table).from(table).where(where)) {
      return Optional.ofNullable(query.fetchOne());
    }
  }
}
