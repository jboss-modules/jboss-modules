package org.jboss.modules;

import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

//@Ignore
public class PathUtilsBenchmarkTest {

    /**
     *  This string will not be changed by the canonicalization process
     */
    private static final String NO_CHANGE_STRING = "/this/path/has/no/need/to/be/canonicalized";

    /**
     * This string will be changed by the canonicalization process
     */
    private static final String CHANGE_STRING = "/this/./path/has/a/../need/to/be/canonicalized";

    /**
     * This string will be changed a lot by the canonicalization process
     */
    private static final String MANY_CHANGE_STRING = "/../../../../../.././.thing/../../././../";

    /**
     * Represents a path with just a dot but no need to canonicalize
     */
    private static final String WITH_DOT_BUT_FINE = "META-INF/application.properties";

    @Test
    public void launch() throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .timeout(TimeValue.seconds(5))
            .timeUnit(TimeUnit.MICROSECONDS)
            .forks(1)
            .warmupIterations(1)
            .measurementIterations(4)
            .shouldDoGC(true)
            .shouldFailOnError(true)
            .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void noChangeString(Blackhole bh) {
        bh.consume(PathUtils.canonicalize(NO_CHANGE_STRING));
    }

    @Benchmark
    public void changeString(Blackhole bh) {
        bh.consume(PathUtils.canonicalize(CHANGE_STRING));
    }

    @Benchmark
    public void manyChangeString(Blackhole bh) {
        bh.consume(PathUtils.canonicalize(MANY_CHANGE_STRING));
    }

    @Benchmark
    public void withDotButFine(Blackhole bh) {
        bh.consume(PathUtils.canonicalize(WITH_DOT_BUT_FINE));
    }

    @Benchmark
    public void directNoChangeString(Blackhole bh) {
        bh.consume(PathUtils.directCanonicalize(NO_CHANGE_STRING));
    }

    @Benchmark
    public void directChangeString(Blackhole bh) {
        bh.consume(PathUtils.directCanonicalize(CHANGE_STRING));
    }

    @Benchmark
    public void directManyChangeString(Blackhole bh) {
        bh.consume(PathUtils.directCanonicalize(MANY_CHANGE_STRING));
    }

    @Benchmark
    public void directWithDotButFine(Blackhole bh) {
        bh.consume(PathUtils.directCanonicalize(WITH_DOT_BUT_FINE));
    }
}
