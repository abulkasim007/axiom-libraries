package libs.axiom.data.abstractions;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import libs.axiom.data.abstractions.configuration.AggregateTypeReference;
import libs.axiom.data.abstractions.models.AggregateRoot;
import libs.axiom.data.abstractions.proxy.DefaultAggregateRootRepository;
import libs.axiom.data.abstractions.reliability.OutboxConfig;

import java.lang.reflect.Type;

public class AggregateRootRepositories {

    private final Binder binder;

    private AggregateRootRepositories(Binder binder) {
        this.binder = binder;
    }

    public static AggregateRootRepositories from(Binder binder) {
        return new AggregateRootRepositories(binder);
    }

    @SuppressWarnings("unchecked")
    public <A extends AggregateRoot> AggregateRootRepositories add(Class<A> aggregateType) {
        // Create the type literal for AggregateRootRepository<A>
        Type repositoryType = Types.newParameterizedType(AggregateRootRepository.class, aggregateType);

        TypeLiteral<AggregateRootRepository<A>> repositoryTypeLiteral =
                (TypeLiteral<AggregateRootRepository<A>>) TypeLiteral.get(repositoryType);

        // Create the type literal for MongoAggregateRootRepository<A>
        Type implType = Types.newParameterizedType(DefaultAggregateRootRepository.class, aggregateType);

        TypeLiteral<DefaultAggregateRootRepository<A>> implTypeLiteral =
                (TypeLiteral<DefaultAggregateRootRepository<A>>) TypeLiteral.get(implType);

        binder.bind(repositoryTypeLiteral).to(implTypeLiteral).in(Singleton.class);

        // Aggregate type reference
        var aggregateTypeReferenceType = Types.newParameterizedType(AggregateTypeReference.class, aggregateType);
        binder.bind((TypeLiteral<AggregateTypeReference<A>>) TypeLiteral.get(aggregateTypeReferenceType))
                .toInstance(new AggregateTypeReference<>(aggregateType));

        return this;
    }

    public void withOutbox()
    {
        binder.bind(OutboxConfig.class).toInstance(new OutboxConfig(true));
    }
}

