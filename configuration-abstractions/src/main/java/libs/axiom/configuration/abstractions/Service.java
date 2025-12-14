package libs.axiom.configuration.abstractions;

public record Service(
        String id,
        String serviceName,
        String logLevel,
        String[] eventPackages,
        Vertical[] verticals
) {
    public static final String SERVICE_CONFIGURATION_KEY = "serviceConfiguration";
}
