package com.cleveloper.jufu;

import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

public class Main {
    public static void main(String[] args) {
        String eager = PredicateResultFactory.select(age -> age >= 18, 21, "ADULT", "MINOR");
        String lazy = PredicateResultFactory.selectLazy(
                age -> age >= 18,
                17,
                () -> "ADULT-LAZY",
                () -> "MINOR-LAZY"
        );

        System.out.println("eager result = " + eager);
        System.out.println("lazy result = " + lazy);
    }
}
