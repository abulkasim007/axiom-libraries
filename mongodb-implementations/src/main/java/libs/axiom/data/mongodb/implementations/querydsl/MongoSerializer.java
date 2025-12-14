package libs.axiom.data.mongodb.implementations.querydsl;

import com.mongodb.DBRef;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.mongodb.document.MongodbDocumentSerializer;

import java.util.UUID;

public class MongoSerializer extends MongodbDocumentSerializer {

  private final static String ENTITY_ID_PROPERTY_NAME = "id";
  private final static String MONGO_ID_PROPERTY_NAME = "_id";

  @Override
  protected boolean isReference(Path<?> arg) {
    return false;
  }

  @Override
  protected DBRef asReference(Object constant) {
    return new DBRef("", UUID.randomUUID().toString());
  }

  @Override
  protected DBRef asReferenceKey(Class<?> entity, Object id) {
    return new DBRef("", UUID.randomUUID().toString());
  }

  @Override
  protected String getKeyForPath(Path<?> expr, PathMetadata metadata) {

    if (expr.getMetadata().getElement().equals(ENTITY_ID_PROPERTY_NAME)) {
      return MONGO_ID_PROPERTY_NAME;
    }
    return super.getKeyForPath(expr, metadata);
  }
}