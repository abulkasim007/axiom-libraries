package libs.axiom.messaging.abstractions;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.util.List;
import java.util.UUID;

@Entity
public  class UserContext {

    public static final String USER_ROLE_NAME = "user";
    public static final String ANONYMOUS_ROLE_NAME = "anonymous";
    public static final UUID ANONYMOUS_USER_ID = new UUID(0L, 0L);
    @Id
    private  UUID id;
    private  UUID sessionId;
    private  UUID userId;
    private  UUID applicationId;
    private  UUID tenantId;
    private  UUID verticalId;
    private  String serviceId;
    private  String email;
    private  String phoneNumber;
    private  String userName;
    private  String displayName;
    private  String language;
    @Transient
    private  List<String> roles;

    public UserContext() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getVerticalId() {
        return verticalId;
    }

    public void setVerticalId(UUID verticalId) {
        this.verticalId = verticalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
