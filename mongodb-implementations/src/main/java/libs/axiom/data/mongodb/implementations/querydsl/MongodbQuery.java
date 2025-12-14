package libs.axiom.data.mongodb.implementations.querydsl;

import com.querydsl.core.types.Predicate;
import com.querydsl.mongodb.document.AbstractMongodbQuery;

import java.util.List;

public class MongodbQuery<Q> extends AbstractMongodbQuery<MongodbQuery<Q>> {

  public MongodbQuery() {
    super(new MongoSerializer());
  }

  @Override
  protected List<Object> getIds(Class<?> targetType, Predicate condition) {
    return List.of();
  }
}