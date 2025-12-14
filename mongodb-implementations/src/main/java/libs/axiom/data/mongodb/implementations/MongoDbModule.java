package libs.axiom.data.mongodb.implementations;

import com.google.inject.AbstractModule;
import jakarta.inject.Singleton;
import libs.axiom.data.abstractions.RepositoryCache;
import libs.axiom.data.abstractions.RepositoryModule;
import libs.axiom.data.abstractions.reliability.OutboxProcessor;
import libs.axiom.data.mongodb.implementations.dbcontext.MongoRepositoryCache;

public class MongoDbModule extends AbstractModule {
    @Override
    protected void configure() {

        install(new RepositoryModule());

        bind(RepositoryCache.class).to(MongoRepositoryCache.class).in(Singleton.class);

        bind(OutboxProcessor.class).to(DefaultOutboxProcessor.class).in(Singleton.class);
    }
}
