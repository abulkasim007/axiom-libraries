package libs.axiom.messaging.abstractions;

import libs.axiom.serialization.abstractions.SerializationFormat;

public class Topic {

    public static final String FANOUT = "fanout";
    public static final String DIRECT = "direct";

    private String name;
    private String type;
    private SerializationFormat serializationFormat;

    public Topic(String name, String type, SerializationFormat serializationFormat) {
        this.name = name;
        this.type = type;
        this.serializationFormat = serializationFormat;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Topic{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public SerializationFormat getSerializationFormat() {
        return serializationFormat;
    }

    public void setSerializationFormat(SerializationFormat serializationFormat) {
        this.serializationFormat = serializationFormat;
    }
}
