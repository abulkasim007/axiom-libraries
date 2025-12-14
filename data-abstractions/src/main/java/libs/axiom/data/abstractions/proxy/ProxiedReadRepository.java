package libs.axiom.data.abstractions.proxy;

import jakarta.inject.Inject;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.ReadRepository;
import libs.axiom.data.abstractions.RepositoryProxy;

public class ProxiedReadRepository extends ProxiedRepository implements ReadRepository {
    @Inject
    public ProxiedReadRepository(RepositoryProxy repositoryProxy) {
        super(DatabaseType.READ, repositoryProxy);
    }
}

