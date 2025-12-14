package libs.axiom.host.implementations.extensions;

import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.serialization.abstractions.SerializationFormat;
import libs.axiom.serialization.abstractions.SerializerProvider;

import java.util.Map;

public class DefaultServiceConfigurationProvider implements ServiceConfigurationProvider {

    private final Service currentService;

    public DefaultServiceConfigurationProvider(SerializerProvider serializerProvider, Map<String, Object> serviceConfigurationMap) {
        this.currentService = serializerProvider.getSerializer(SerializationFormat.JSON).deserialize(serviceConfigurationMap, Service.class);
    }

    public Service getCurrentService() {
        return this.currentService;
    }
}