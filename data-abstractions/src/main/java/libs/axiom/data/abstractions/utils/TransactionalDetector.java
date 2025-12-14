package libs.axiom.data.abstractions.utils;

import jakarta.transaction.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public final class TransactionalDetector {
    private static final StackWalker walker = StackWalker.getInstance(
            StackWalker.Option.RETAIN_CLASS_REFERENCE
    );

    public static boolean isTransactionalCall() {
        return walker.walk(frames ->
                frames
                        .skip(1)
                        .anyMatch(frame -> isMethodAnnotated(frame, Transactional.class))
        );
    }

    private static boolean isMethodAnnotated(StackWalker.StackFrame frame, Class<? extends Annotation> annotation) {
        try {
            for (Method method : frame.getDeclaringClass().getDeclaredMethods()) {
                if (method.getName().equals(frame.getMethodName())) {
                    if (method.isAnnotationPresent(annotation)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {

        }
        return false;
    }

}