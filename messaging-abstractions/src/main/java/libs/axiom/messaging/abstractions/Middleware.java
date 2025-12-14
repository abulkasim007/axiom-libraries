package libs.axiom.messaging.abstractions;

@FunctionalInterface
public interface Middleware<T extends Message> {
    void invoke(Context<T> context, PipelineFunction<T> next);
}