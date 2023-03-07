package org.jboss.modules;

import org.junit.Test;
import org.openjdk.jmh.annotations.*;
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

    @Test
    public void launch() throws RunnerException {
        final Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .warmupTime(TimeValue.seconds(15))
            .timeout(TimeValue.seconds(30))
            .timeUnit(TimeUnit.MICROSECONDS)
            .forks(1)
            .threads(2)
            .addProfiler(GCProfiler.class)
            .warmupIterations(2)
            .measurementIterations(5)
            .shouldDoGC(true)
            .shouldFailOnError(true)
            .build();

        new Runner(opt).run();
    }

    @State(Scope.Thread)
    public static class Iteration {
        public long idx;
    }

    @State(Scope.Benchmark)
    public static class Plan {
        // this serves as a list of all the inputs for the canonicalize function
        public String[] inputs = new String[] {
            // empty string
            "",
            // results in empty string
            "../",
            ".",
            // this short string will not be changed by the canonicalization process
            "/this/path/has/no/need/to/be/canonicalized",
            // this string will be changed by the canonicalization process
            "/this/./path/has/a/../need/to/be/canonicalized",
            // these strings will be changed a lot by the canonicalization process
            "/../../../../../.././.thing/../../././../",
            "./../../../../..//////.course/../../././../",
            // these strings are more representative of the types of strings found in applications and should be represented heavily in the benchmark
            "META-INF/application.properties",
            "com/thing/solution/model/SomeModel.class",
            "/com/thing/impl/GoodImpl.class",
            "../com/thing/impl/BadImpl.class",
            "WEB-INF/web.xml",
            "WEB-INF/templates/fragments/../main.tmpl",
            // here are some worst case strings that have to be scanned entirely
            "META-INF/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/.",
            "META-INF/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/.hidden"
        };


    }

    @Benchmark
    public void current(Iteration it, Plan plan, Blackhole bh) {
        bh.consume(PathUtils.canonicalize(plan.inputs[(int) it.idx++ % plan.inputs.length]));
    }

    @Benchmark
    public void original(Iteration it, Plan plan, Blackhole bh) {
        bh.consume(PathUtilsTest.originalCanonicalize(plan.inputs[(int) it.idx++ % plan.inputs.length]));
    }

}
