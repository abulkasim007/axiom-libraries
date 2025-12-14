package libs.axiom.data.rdbms.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.RepositoryCache;
import libs.axiom.data.abstractions.RepositoryModule;
import libs.axiom.data.abstractions.reliability.OutboxProcessor;
import libs.axiom.data.rdbms.implementations.configurations.EntityMapProvider;
import libs.axiom.data.rdbms.implementations.dbcontext.RdbmsRepositoryCache;
import libs.axiom.data.rdbms.implementations.repositories.OutboxRepositoryFactory;

import java.util.List;
import java.util.Map;

public class RdbmsModule extends AbstractModule {
    private final Map<DatabaseType, List<Class<?>>> entityTypes;

    public RdbmsModule(Map<DatabaseType, List<Class<?>>> entityTypes) {

        if (entityTypes == null || entityTypes.isEmpty()) {
            throw new IllegalArgumentException("You must specify at least one entity when using JPA module");
        }

        this.entityTypes = entityTypes;
    }

    @Override
    protected void configure() {

        install(new RepositoryModule());

        bind(OutboxRepositoryFactory.class).in(Singleton.class);
        bind(RepositoryCache.class).to(RdbmsRepositoryCache.class).in(Singleton.class);
        bind(EntityMapProvider.class).toInstance(new EntityMapProvider(this.entityTypes));
        bind(OutboxProcessor.class).to(DefaultOutboxProcessor.class).in(jakarta.inject.Singleton.class);
    }
}
// Working configurations
// "jakarta.persistence.jdbc.driver=org.postgresql.Driver;jakarta.persistence.jdbc.url=jdbc:postgresql://localhost/test_state_database;jakarta.persistence.jdbc.user=postgres;jakarta.persistence.jdbc.password=12345;hibernate.hbm2ddl.auto=create;hibernate.show_sql=true;hibernate.format_sql=true;hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;hibernate.connection.autocommit=false;hibernate.hikari.minimumIdle=5;hibernate.hikari.maximumPoolSize=10;hibernate.hikari.idleTimeout=30000;hibernate.hikari.maxLifetime=1800000;hibernate.hikari.connectionTimeout=1800000;hibernate.connection.provider_class=org.hibernate.hikaricp.internal.HikariCPConnectionProvider;hibernate.connection.provider_disables_autocommit=true;hibernate.allow_update_outside_transaction=false;hibernate.current_session_context_class=thread",
