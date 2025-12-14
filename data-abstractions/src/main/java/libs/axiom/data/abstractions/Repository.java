package libs.axiom.data.abstractions;

import com.querydsl.core.types.Predicate;
import libs.axiom.data.abstractions.utils.AutoCloseableIterator;

import java.util.List;
import java.util.UUID;

public interface Repository  {

    <T> T get(UUID id, Class<T> type);

    <T> T get(Predicate filter, Class<T> type);

    <T> List<T> getMany(Predicate filter, Class<T> type);

    <T> List<T> getMany(List<UUID> entityIds, Class<T> type);

    <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type);

    <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type, String collectionName);

    <T> boolean exists(Predicate filter, Class<T> type);

    <T> void save(T entity, Class<T> type);

    <T> void save(T entity, Class<T> type, String collectionName);

    <T> void save(List<T> entities, Class<T> type);

    <T> void delete(Predicate filter, Class<T> type);

    <T > long update(Predicate filter, T entity, Class<T> type);

    <T > long update( T entity, UUID entityId, int version, Class<T> type);
}
