package libs.axiom.data.abstractions.proxy;

import jakarta.inject.Inject;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.RepositoryProxy;
import libs.axiom.data.abstractions.StateRepository;

public class ProxiedStateRepository extends ProxiedRepository implements StateRepository {
    @Inject
    public ProxiedStateRepository(RepositoryProxy repositoryProxy) {
        super(DatabaseType.STATE, repositoryProxy);
    }
}