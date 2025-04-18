package graphql.execution.instrumentation;

import graphql.Internal;
import graphql.PublicSpi;
import graphql.execution.FieldValueInfo;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

@PublicSpi
public interface ExecuteObjectInstrumentationContext extends InstrumentationContext<Map<String, Object>> {

    @Internal
    ExecuteObjectInstrumentationContext NOOP = new ExecuteObjectInstrumentationContext() {
        @Override
        public void onDispatched() {
        }

        @Override
        public void onCompleted(Map<String, Object> result, Throwable t) {
        }
    };

    /**
     * This creates a no-op {@link InstrumentationContext} if the one pass in is null
     *
     * @param nullableContext a {@link InstrumentationContext} that can be null
     *
     * @return a non null {@link InstrumentationContext} that maybe a no-op
     */
    @NonNull
    @Internal
    static ExecuteObjectInstrumentationContext nonNullCtx(ExecuteObjectInstrumentationContext nullableContext) {
        return nullableContext == null ? NOOP : nullableContext;
    }

    default void onFieldValuesInfo(List<FieldValueInfo> fieldValueInfoList) {
    }

    default void onFieldValuesException() {
    }

}
