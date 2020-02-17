package propra.imageconverter.util;

import java.util.function.Supplier;

public class Validator {
    public static <E extends Exception> void ensure(boolean condition, Supplier<E> exceptionSupplier) throws E {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }
}
