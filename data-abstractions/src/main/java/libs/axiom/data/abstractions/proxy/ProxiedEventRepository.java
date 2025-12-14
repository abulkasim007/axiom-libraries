package libs.axiom.data.abstractions.proxy;

import jakarta.inject.Inject;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.EventRepository;
import libs.axiom.data.abstractions.RepositoryProxy;

public class ProxiedEventRepository extends ProxiedRepository implements EventRepository {
    @Inject
    public ProxiedEventRepository(RepositoryProxy repositoryProxy) {
        super(DatabaseType.EVENT, repositoryProxy);
    }
}

