package libs.axiom.data.abstractions.models;

import java.util.List;

public class ViewModel extends Entity implements RowLevelSecurity {

    private List<String> rolesAndIdsAllowedToRead;


    @Override
    public List<String> getRolesAndIdsAllowedToRead() {
        return rolesAndIdsAllowedToRead;
    }


    @Override
    public void setRolesAndIdsAllowedToRead(List<String> rolesAndIdsAllowedToRead) {
        this.rolesAndIdsAllowedToRead = rolesAndIdsAllowedToRead;
    }
}
