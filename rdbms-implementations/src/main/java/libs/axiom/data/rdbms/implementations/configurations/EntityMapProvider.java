package libs.axiom.data.rdbms.implementations.configurations;

import libs.axiom.data.abstractions.DatabaseType;

import java.util.List;
import java.util.Map;

public class EntityMapProvider {
    private final Map<DatabaseType, List<Class<?>>> entityTypes;

    public EntityMapProvider(Map<DatabaseType, List<Class<?>>> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public Map<DatabaseType, List<Class<?>>> get() {
        return this.entityTypes;
    }
}