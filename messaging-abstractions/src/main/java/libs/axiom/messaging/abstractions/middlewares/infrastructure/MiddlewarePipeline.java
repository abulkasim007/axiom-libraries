package libs.axiom.messaging.abstractions.middlewares.infrastructure;

import libs.axiom.messaging.abstractions.Context;
import libs.axiom.messaging.abstractions.Message;
import libs.axiom.messaging.abstractions.Middleware;
import libs.axiom.messaging.abstractions.PipelineFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MiddlewarePipeline<T extends Message> {

    private final PipelineFunction<T> pipeline;

    private MiddlewarePipeline(PipelineFunction<T> pipeline) {
        this.pipeline = pipeline;
    }

    public void execute(Context<T> context) {
        pipeline.apply(context);
    }

    public static class Builder<T extends Message> {
        private final List<Function<PipelineFunction<T>, PipelineFunction<T>>> components = new ArrayList<>();

        public Builder<T> use(Middleware<T> middleware) {
            components.add(next -> context -> middleware.invoke(context, next));
            return this;
        }

        public MiddlewarePipeline<T> build() {
            PipelineFunction<T> pipeline = _ -> {};

            for (int i = components.size() - 1; i >= 0; i--) {
                pipeline = components.get(i).apply(pipeline);
            }

            return new MiddlewarePipeline<>(pipeline);
        }
    }
}