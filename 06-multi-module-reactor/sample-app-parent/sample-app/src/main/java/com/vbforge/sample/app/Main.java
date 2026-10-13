package com.vbforge.sample.app;

import com.vbforge.sample.service.GreetingService;

/**
 * A tiny entry point that exercises the whole chain
 * {@code sample-core → sample-service → sample-app}.
 */
public class Main {

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "reactor";
        System.out.println(new GreetingService().greet(name));
    }
}