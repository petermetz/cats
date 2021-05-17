package com.endava.cats.fuzzer.headers.only;

import com.endava.cats.fuzzer.headers.Expect4XXBaseHeadersFuzzer;
import com.endava.cats.io.ServiceCaller;
import com.endava.cats.model.FuzzingStrategy;
import com.endava.cats.report.TestCaseListener;

import java.util.List;
import java.util.stream.Collectors;

public abstract class InvisibleCharsOnlyTrimValidateFuzzer extends Expect4XXBaseHeadersFuzzer {

    public InvisibleCharsOnlyTrimValidateFuzzer(ServiceCaller sc, TestCaseListener lr) {
        super(sc, lr);
    }

    @Override
    public List<FuzzingStrategy> fuzzStrategy() {
        return this.getInvisibleChars()
                .stream().map(value -> FuzzingStrategy.replace().withData(value))
                .collect(Collectors.toList());
    }

    public String description() {
        return "iterate through each header and " + typeOfDataSentToTheService();
    }

    abstract List<String> getInvisibleChars();
}