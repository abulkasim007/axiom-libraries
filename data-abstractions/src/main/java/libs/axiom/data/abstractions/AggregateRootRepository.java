package libs.axiom.data.abstractions;

import com.querydsl.core.types.Predicate;
import libs.axiom.data.abstractions.models.AggregateRoot;

import java.util.List;
import java.util.UUID;

public interface AggregateRootRepository<T extends AggregateRoot> {

    T get(UUID id);

    void save(T aggregate);

    void update(T aggregate);

    T get(Predicate filter);

    List<T> getMany(Predicate filter);

    boolean exists(Predicate filter);

    void delete(Predicate filter);
}