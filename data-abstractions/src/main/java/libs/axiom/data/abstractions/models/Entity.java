package libs.axiom.data.abstractions.models;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import libs.axiom.messaging.abstractions.UserContext;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public class Entity implements Identity {

    @Id
    private UUID id;
    private UUID createdBy;
    private Instant createdDate;
    private String language;
    private Instant lastUpdatedDate;
    private UUID lastUpdatedBy;
    private UUID tenantId;
    private UUID verticalId;
    private String serviceId;
    private boolean isMarkedToDelete;

    @Version
    private int version;




    @Override
    public UUID getId() {
        return id;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLanguage() {
        return language;
    }

    public Instant getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public UUID getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getVerticalId() {
        return verticalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public int getVersion() {
        return version;
    }



    public void setId(UUID id) {
        this.id = id;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLastUpdatedDate(Instant lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public void setLastUpdatedBy(UUID lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void setVerticalId(UUID verticalId) {
        this.verticalId = verticalId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setVersion(int version) {
        this.version = version;
    }



    public void assignEntityDefaults(UserContext userContext) {
        Instant currentTime = Instant.now();
        userContext.setId(UUID.randomUUID());

        if (this.getTenantId() == null) {
            setCreatedDate(currentTime);
            setCreatedBy(userContext.getUserId());
            setLanguage(userContext.getLanguage());
            setTenantId(userContext.getTenantId());
            setServiceId(userContext.getServiceId());
            setVerticalId(userContext.getVerticalId());
        }

        setLastUpdatedDate(currentTime);
        setLastUpdatedBy(userContext.getUserId());

        this.setVersion(this.getVersion() + 1);
    }

    public boolean isMarkedToDelete() {
        return isMarkedToDelete;
    }

    public void setMarkedToDelete(boolean markedToDelete) {
        isMarkedToDelete = markedToDelete;
    }
}
