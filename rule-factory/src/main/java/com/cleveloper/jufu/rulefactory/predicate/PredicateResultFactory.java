package com.cleveloper.jufu.rulefactory.predicate;

import java.util.function.Supplier;

public final class PredicateResultFactory {

    private PredicateResultFactory() {
    }

    public static <T, R> R select(PredicateCondition<? super T> predicate, T input, R onTrue, R onFalse) {
        if (predicate == null) {
            throw new IllegalArgumentException("predicate must not be null");
        }
        return predicate.test(input) ? onTrue : onFalse;
    }

    public static <T, R> R selectLazy(
            PredicateCondition<? super T> predicate,
            T input,
            Supplier<? extends R> onTrueSupplier,
            Supplier<? extends R> onFalseSupplier
    ) {
        if (predicate == null) {
            throw new IllegalArgumentException("predicate must not be null");
        }

        boolean result = predicate.test(input);
        Supplier<? extends R> selectedSupplier = result ? onTrueSupplier : onFalseSupplier;
        if (selectedSupplier == null) {
            throw new IllegalArgumentException(result
                    ? "true branch supplier must not be null when selected"
                    : "false branch supplier must not be null when selected");
        }

        return selectedSupplier.get();
    }
}

