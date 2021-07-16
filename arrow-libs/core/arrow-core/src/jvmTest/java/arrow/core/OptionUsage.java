package arrow.core;

import kotlin.jvm.functions.Function1;

public class OptionUsage {

    public void testUsage() {
        Option<Integer> fromNullable = Option.fromNullable(null);
        Option.tryCatch((throwable) -> {
            throwable.printStackTrace();
            return None.INSTANCE;
        }, () -> 1);
        
        Option<Integer> invoke = Option.invoke(1);
        Function1<Option<? extends String>, Option<String>> lift = Option.lift((a) -> a.toUpperCase());
    }
}
