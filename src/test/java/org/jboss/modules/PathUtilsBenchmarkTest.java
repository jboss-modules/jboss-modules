package org.jboss.modules;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

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

    /**
     * This is a long worst case that has a need for canonicalization but pattern matches at the end.
     */
    private static final String LONG_STRING_WORST_CASE_CONTAINS = "META-INF/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/.";

    /**
     * This is a long worst case that does not need canonicalization but _does_ get through the pre-check.
     */
    private static final String LONG_STRING_NO_CHANGE = "META-INF/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/.hidden";

    @Test
    public void launch() throws RunnerException {
        final Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .timeout(TimeValue.seconds(30))
            .timeUnit(TimeUnit.MICROSECONDS)
            .forks(1)
            .threads(4)
            .addProfiler(GCProfiler.class)
            .warmupIterations(2)
            .measurementIterations(5)
            .shouldDoGC(true)
            .shouldFailOnError(true)
            .build();

        new Runner(opt).run();
    }

//    @Benchmark
//    public void noChangeString(Blackhole bh) {
//        bh.consume(PathUtils.canonicalize(NO_CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void directNoChangeString(Blackhole bh) {
//        bh.consume(PathUtils.directCanonicalize(NO_CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void originalNoChangeString(Blackhole bh) {
//        bh.consume(PathUtilsTest.originalCanonicalize(NO_CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void changeString(Blackhole bh) {
//        bh.consume(PathUtils.canonicalize(CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void directChangeString(Blackhole bh) {
//        bh.consume(PathUtils.directCanonicalize(CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void originalChangeString(Blackhole bh) {
//        bh.consume(PathUtilsTest.originalCanonicalize(CHANGE_STRING));
//    }
//
//
//    @Benchmark
//    public void manyChangeString(Blackhole bh) {
//        bh.consume(PathUtils.canonicalize(MANY_CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void directManyChangeString(Blackhole bh) {
//        bh.consume(PathUtils.directCanonicalize(MANY_CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void originalManyChangeString(Blackhole bh) {
//        bh.consume(PathUtilsTest.originalCanonicalize(MANY_CHANGE_STRING));
//    }
//
//    @Benchmark
//    public void withDotButFine(Blackhole bh) {
//        bh.consume(PathUtils.canonicalize(WITH_DOT_BUT_FINE));
//    }
//
//
//    @Benchmark
//    public void directWithDotButFine(Blackhole bh) {
//        bh.consume(PathUtils.directCanonicalize(WITH_DOT_BUT_FINE));
//    }
//
//    @Benchmark
//    public void originalWithDotButFine(Blackhole bh) {
//        bh.consume(PathUtilsTest.originalCanonicalize(WITH_DOT_BUT_FINE));
//    }
//
//    @Benchmark
//    public void longWorstCase(Blackhole bh) {
//        bh.consume(PathUtils.canonicalize(LONG_STRING_WORST_CASE_CONTAINS));
//    }
//
//    @Benchmark
//    public void directLongWorstCase(Blackhole bh) {
//        bh.consume(PathUtils.directCanonicalize(LONG_STRING_WORST_CASE_CONTAINS));
//    }
//
//    @Benchmark
//    public void originalLongWorstCase(Blackhole bh) {
//        bh.consume(PathUtilsTest.originalCanonicalize(LONG_STRING_WORST_CASE_CONTAINS));
//    }

    @Benchmark
    public void longNoChangeCase(Blackhole bh) {
        bh.consume(PathUtils.canonicalize(LONG_STRING_NO_CHANGE));
    }

    @Benchmark
    public void directLongNoChangeCase(Blackhole bh) {
        bh.consume(PathUtils.directCanonicalize(LONG_STRING_NO_CHANGE));
    }

    @Benchmark
    public void originalLongNoChangeCase(Blackhole bh) {
        bh.consume(PathUtilsTest.originalCanonicalize(LONG_STRING_NO_CHANGE));
    }

}
