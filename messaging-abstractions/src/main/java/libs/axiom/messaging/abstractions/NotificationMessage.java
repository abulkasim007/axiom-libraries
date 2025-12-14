package libs.axiom.messaging.abstractions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationMessage {

    String getTopic();

    void setTopic(String topic);

    Map<String, Object> getMessage();

    void setMessage(Map<String, Object> message);

    List<UUID> getUserIds();

    void setUserIds(List<UUID> userIds);

    List<String> getRoles();

    void setRoles(List<String> roles);

    List<UUID> getSessionIds();

    void setSessionIds(List<UUID> sessionIds);

    List<Devices> getDevices();

    void setDevices(List<Devices> devices);

    Priorities getPriority();

    void setPriority(Priorities priority);

    boolean isOffline(); // Java convention for boolean getters is `is`

    void setOffline(boolean offline);

    int getType();

    void setType(int type);

    List<UUID> getApplicationIds();

    void setApplicationIds(List<UUID> applicationIds);
}
