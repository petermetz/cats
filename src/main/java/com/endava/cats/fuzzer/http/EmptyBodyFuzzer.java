package com.endava.cats.fuzzer.http;

import com.endava.cats.annotations.HttpFuzzer;
import com.endava.cats.fuzzer.executor.SimpleExecutor;
import com.endava.cats.model.FuzzingData;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@HttpFuzzer
public class EmptyBodyFuzzer extends BaseHttpWithPayloadSimpleFuzzer {

    @Inject
    public EmptyBodyFuzzer(SimpleExecutor executor) {
        super(executor);
    }

    @Override
    protected String getScenario() {
        return "Send a request with an empty string body";
    }

    @Override
    protected String getPayload(FuzzingData data) {
        return "";
    }

    @Override
    public String description() {
        return "send a request with a empty string body";
    }
}