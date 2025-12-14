package libs.axiom.messaging.abstractions;

@FunctionalInterface
public interface PipelineFunction<T extends Message> {
    void apply(Context<T> context);
}