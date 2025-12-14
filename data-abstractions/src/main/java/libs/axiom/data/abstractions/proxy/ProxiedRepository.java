package libs.axiom.data.abstractions.proxy;


import com.querydsl.core.types.Predicate;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.Repository;
import libs.axiom.data.abstractions.RepositoryProxy;
import libs.axiom.data.abstractions.utils.AutoCloseableIterator;

import java.util.List;
import java.util.UUID;

public class ProxiedRepository implements Repository {
    private final DatabaseType databaseType;
    private final RepositoryProxy repositoryProxy;

    public ProxiedRepository(DatabaseType databaseType, RepositoryProxy repositoryProxy) {
        this.databaseType = databaseType;
        this.repositoryProxy = repositoryProxy;
    }


    @Override
    public <T> T get(UUID id, Class<T> type) {
        return this.repositoryProxy.get(databaseType).get(id, type);
    }

    @Override
    public <T> T get(Predicate filter, Class<T> type) {
        return this.repositoryProxy.get(databaseType).get(filter, type);
    }

    @Override
    public <T> List<T> getMany(Predicate filter, Class<T> type) {
        return this.repositoryProxy.get(databaseType).getMany(filter, type);
    }

    @Override
    public <T> List<T> getMany(List<UUID> entityIds, Class<T> type) {
        return this.repositoryProxy.get(databaseType).getMany(entityIds, type);
    }

    @Override
    public <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type) {
        return this.repositoryProxy.get(databaseType).getIterator(filter, type);
    }

    @Override
    public <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type, String collectionName) {
        return this.repositoryProxy.get(databaseType).getIterator(filter, type, collectionName);
    }

    @Override
    public <T> boolean exists(Predicate filter, Class<T> type) {
        return this.repositoryProxy.get(databaseType).exists(filter, type);
    }

    @Override
    public <T> void save(T entity, Class<T> type) {
        this.repositoryProxy.get(databaseType).save(entity, type);
    }

    @Override
    public <T> void save(T entity, Class<T> type, String collectionName) {
        this.repositoryProxy.get(databaseType).save(entity, type, collectionName);
    }

    @Override
    public <T> void save(List<T> entities, Class<T> type) {
        this.repositoryProxy.get(databaseType).save(entities, type);
    }

    @Override
    public <T> void delete(Predicate filter, Class<T> type) {
        this.repositoryProxy.get(databaseType).delete(filter, type);
    }

    @Override
    public <T> long update(Predicate filter, T entity, Class<T> type) {
        return this.repositoryProxy.get(databaseType).update(filter, entity, type);
    }

    @Override
    public <T> long update(T entity, UUID entityId, int version, Class<T> type) {
        return this.repositoryProxy.get(databaseType).update(entity, entityId, version, type);
    }
}