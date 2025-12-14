package libs.axiom.data.abstractions.models;

import java.util.List;

public interface RowLevelSecurity {

    List<String> getRolesAndIdsAllowedToRead();

    void setRolesAndIdsAllowedToRead(List<String> rolesAndIdsAllowedToRead);
}