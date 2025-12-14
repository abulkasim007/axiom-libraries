package libs.axiom.data.abstractions;

public interface RepositoryProxy {
    Repository get(DatabaseType databaseType);
}