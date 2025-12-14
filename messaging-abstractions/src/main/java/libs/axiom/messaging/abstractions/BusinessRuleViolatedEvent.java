package libs.axiom.messaging.abstractions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BusinessRuleViolatedEvent extends Event implements NotifiedByAggregateRoot {

    private String topic;
    private Map<String, Object> message;
    private List<UUID> userIds;
    private List<String> roles;
    private List<UUID> sessionIds;
    private List<Devices> devices;
    private Priorities priority;
    private boolean isOffline;
    private int type;
    private List<UUID> applicationIds;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public Map<String, Object> getMessage() {
        return message;
    }

    @Override
    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    @Override
    public List<UUID> getUserIds() {
        return userIds;
    }

    @Override
    public void setUserIds(List<UUID> userIds) {
        this.userIds = userIds;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public List<UUID> getSessionIds() {
        return sessionIds;
    }

    @Override
    public void setSessionIds(List<UUID> sessionIds) {
        this.sessionIds = sessionIds;
    }

    @Override
    public List<Devices> getDevices() {
        return devices;
    }

    @Override
    public void setDevices(List<Devices> devices) {
        this.devices = devices;
    }

    @Override
    public Priorities getPriority() {
        return priority;
    }

    @Override
    public void setPriority(Priorities priority) {
        this.priority = priority;
    }

    @Override
    public boolean isOffline() {
        return isOffline;
    }

    @Override
    public void setOffline(boolean offline) {
        isOffline = offline;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public List<UUID> getApplicationIds() {
        return applicationIds;
    }

    @Override
    public void setApplicationIds(List<UUID> applicationIds) {
        this.applicationIds = applicationIds;
    }
}

