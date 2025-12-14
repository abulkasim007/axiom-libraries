package libs.axiom.data.mongodb.implementations.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.querydsl.core.types.Predicate;
import libs.axiom.data.abstractions.Repository;
import libs.axiom.data.abstractions.utils.AutoCloseableIterator;
import libs.axiom.data.mongodb.implementations.querydsl.MongodbQuery;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MongoCqrsRepository implements Repository {

    private static final String DB_ID_FIELD_NAME = "_id";
    private static final String VERSION_FIELD_NAME = "version";
    private static final String COLLECTION_NAME_PLURAL_CHARACTER = "s";


    private final MongoDatabase mongoDatabase;

    private static final InsertManyOptions InsertManyOptions =
            new InsertManyOptions().ordered(false).bypassDocumentValidation(true);

    private static final ReplaceOptions REPLACE_OPTIONS =
            new ReplaceOptions().upsert(false).bypassDocumentValidation(true);

    public MongoCqrsRepository(String databaseName, MongoClient mongoClient) {
        this.mongoDatabase = mongoClient.getDatabase(databaseName);
    }

    @Override
    public <T> T get(UUID id, Class<T> type) {
        return getCollection(type).find(new Document(DB_ID_FIELD_NAME, id)).first();
    }

    @Override
    public <T> T get(Predicate filter, Class<T> type) {
        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();

        var query = mongodbQuery.where(filter);

        return getCollection(type).find(query.asDocument()).first();
    }

    @Override
    public <T> List<T> getMany(Predicate filter, Class<T> type) {
        List<T> results = new ArrayList<>();

        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();

        return getCollection(type).find(mongodbQuery.where(filter).asDocument()).into(results);
    }

    @Override
    public <T> List<T> getMany(List<UUID> entityIds, Class<T> type) {
        List<T> results = new ArrayList<>();

        Bson filter = Filters.in(DB_ID_FIELD_NAME, entityIds);

        return getCollection(type).find(filter).into(results);
    }

    @Override
    public <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type) {

        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();

        MongoCursor<T> cursor = getCollection(type).find(mongodbQuery.where(filter).asDocument()).iterator();

        return new AutoCloseableIterator<>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public T next() {
                return cursor.next();
            }

            @Override
            public void close() {
                cursor.close();
            }
        };
    }

    @Override
    public <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type, String collectionName) {
        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();

        MongoCursor<T> cursor = getCollection(collectionName, type).find(mongodbQuery.where(filter).asDocument()).iterator();

        return new AutoCloseableIterator<>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public T next() {
                return cursor.next();
            }

            @Override
            public void close() {
                cursor.close();
            }
        };
    }

    @Override
    public <T> boolean exists(Predicate filter, Class<T> type) {
        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();

        try (MongoCursor<T> cursor = getCollection(type).find(mongodbQuery.where(filter).asDocument()).cursor()) {
            return cursor.hasNext();
        }
    }

    @Override
    public <T> void save(T entity, Class<T> type) {
        getCollection(type).insertOne(entity);
    }

    @Override
    public <T> void save(T entity, Class<T> type, String collectionName) {
        getCollection(collectionName, type).insertOne(entity);
    }

    @Override
    public <T> void save(List<T> entities, Class<T> type) {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("List have no items");
        }

        if (entities.size() == 1) {
            save(entities.getFirst(), type);
            return;
        }

        MongoCollection<T> collection = getCollection(type);

        collection.insertMany(entities, InsertManyOptions);
    }

    @Override
    public <T> void delete(Predicate filter, Class<T> type) {
        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();

        getCollection(type).deleteMany(mongodbQuery.where(filter).asDocument());
    }

    @Override
    public <T> long update(Predicate filter, T entity, Class<T> type) {
        MongodbQuery<T> mongodbQuery = new MongodbQuery<>();
        return getCollection(type).replaceOne(mongodbQuery.where(filter).asDocument(), entity).getModifiedCount();
    }

    @Override
    public <T> long update(T entity, UUID entityId, int version, Class<T> type) {
        var filter = and(eq(DB_ID_FIELD_NAME, entityId), eq(VERSION_FIELD_NAME, version));
        return getCollection(type).replaceOne(filter, entity, REPLACE_OPTIONS).getModifiedCount();
    }

    private <T> MongoCollection<T> getCollection(Class<T> clazz) {
        String collectionName = clazz.getSimpleName() + COLLECTION_NAME_PLURAL_CHARACTER;
        return getCollection(collectionName, clazz);
    }

    private <T> MongoCollection<T> getCollection(String collectionName, Class<T> clazz) {
        return this.mongoDatabase.getCollection(collectionName, clazz);
    }
}
