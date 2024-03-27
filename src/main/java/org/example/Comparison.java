package org.example;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.example.internal.FixedArgsCacheKey;
import org.example.internal.NArgsCacheKey;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;


@Fork(1)
@Measurement(iterations = 2, time = 5)
@Warmup(iterations = 2, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
// @Threads(4)
public class Comparison {

    private static final Tags COMMON_TAGS = Tags.of("application", "abcservice", "az", "xyz", "environment",
            "production", "random-meta", "random-meta");

    private static final Map<Integer, Counter> cacheWithHash = new ConcurrentHashMap<>();
    private static final Map<FixedArgsCacheKey, Counter> cacheWithFixedArgsCustomKey = new ConcurrentHashMap<>();
    private static final Map<NArgsCacheKey, Counter> cacheWithNargsCustomKey = new ConcurrentHashMap<>();
    private static final Map<Meter.Id, Counter> cacheWithId = new ConcurrentHashMap<>();
    private static final Map<Tags, Counter> cacheWithTags = new ConcurrentHashMap<>();
    private static final String METER_NAME = "counter";
    private static final String TAG_KEY_1 = "tag-key-1";
    private static final String TAG_KEY_2 = "tag-key-2";
    private static final String STATIC_TAG = "static-tag";
    private static final String STATIC_VALUE = "static-value";
    private static final Meter.MeterProvider<Counter> counterProvider = Counter.builder(METER_NAME)
            .tag(STATIC_TAG, STATIC_VALUE)
            .withRegistry(Metrics.globalRegistry);

    static {
        Metrics.globalRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(final Meter.Id id) {
                return id.withTags(COMMON_TAGS);
            }

            @Override public MeterFilterReply accept(final Meter.Id id) {
                if(id.getName().startsWith(METER_NAME)) {
                    return MeterFilterReply.ACCEPT;
                }
                return MeterFilter.super.accept(id);
            }
        });
    }

    @State(Scope.Thread)
    public static class DataProvider {
        private final Iterator<String> iterator = new CircularListIterator<>(IntStream.range(0, 100).mapToObj(i -> "tag"
                + "-value-"+i).collect(Collectors.toList()));

        @TearDown(Level.Trial)
        public void tearDown() {
            Metrics.globalRegistry.clear();
            cacheWithId.clear();
            cacheWithHash.clear();
            cacheWithTags.clear();
            cacheWithFixedArgsCustomKey.clear();
            cacheWithNargsCustomKey.clear();
        }
    }

    @Benchmark
    public void recordWithoutCache(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        Counter.builder(Comparison.METER_NAME)
                .tags(TAG_KEY_1, dynamicValue, TAG_KEY_2, dynamicValue, STATIC_TAG, STATIC_VALUE)
                .register(Metrics.globalRegistry).increment();
    }

    @Benchmark
    public void recordWithCacheUsingTags(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        final Tags key = Tags.of(TAG_KEY_1, dynamicValue, TAG_KEY_2, dynamicValue, STATIC_TAG, STATIC_VALUE);
        getOrCreateMeter(cacheWithTags, key,
                k -> Counter.builder(
                                Comparison.METER_NAME)
                        .tags(key)
                        .register(Metrics.globalRegistry)).increment();
        bh.consume(key);
    }

    @Benchmark
    public void recordWithCacheUsingId(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        final Meter.Id key = new Meter.Id(Comparison.METER_NAME, Tags.of(TAG_KEY_1, dynamicValue, TAG_KEY_2,
                dynamicValue, STATIC_TAG, STATIC_VALUE), null, null, Meter.Type.COUNTER);
        getOrCreateMeter(cacheWithId, key, k -> Counter.builder(Comparison.METER_NAME)
                .tags(key.getTags())
                .register(Metrics.globalRegistry)).increment();
        bh.consume(key);
    }

    @Benchmark
    public void recordWithCacheUsingHash(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        // If hash collides, then we might be incrementing wrong meter.
        final int key = Objects.hash(dynamicValue, dynamicValue);
        getOrCreateMeter(cacheWithHash, key, k -> Counter.builder(Comparison.METER_NAME)
                .tags(TAG_KEY_1, dynamicValue, TAG_KEY_2, dynamicValue, STATIC_TAG, STATIC_VALUE)
                .register(Metrics.globalRegistry)).increment();
        bh.consume(key);
    }

    @Benchmark
    public void recordWithCacheUsingFixedArgsCacheKey(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        final FixedArgsCacheKey key = new FixedArgsCacheKey(dynamicValue, dynamicValue);
        getOrCreateMeter(cacheWithFixedArgsCustomKey, key, k -> Counter.builder(
                        Comparison.METER_NAME)
                .tags(TAG_KEY_1, dynamicValue, TAG_KEY_2, dynamicValue, STATIC_TAG, STATIC_VALUE)
                .register(Metrics.globalRegistry)).increment();
        bh.consume(key);
    }

    @Benchmark
    public void recordWithCacheUsingNArgsCacheKey(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        final NArgsCacheKey key = new NArgsCacheKey(new String[] { dynamicValue, dynamicValue });
        getOrCreateMeter(cacheWithNargsCustomKey, key,
                k -> Counter.builder(
                                Comparison.METER_NAME)
                        .tags(TAG_KEY_1, dynamicValue, TAG_KEY_2, dynamicValue, STATIC_TAG, STATIC_VALUE)
                        .register(Metrics.globalRegistry)).increment();
        bh.consume(key);
    }

    @Benchmark
    public void recordUsingMeterProvider(Blackhole bh, DataProvider dataProvider) {
        String dynamicValue = dataProvider.iterator.next();
        counterProvider.withTags(TAG_KEY_1, dynamicValue, TAG_KEY_2, dynamicValue).increment();
    }

    private static <K, V> V getOrCreateMeter(Map<K, V> map, K key, Function<K, V> mappingFunction) {
        final V value = map.get(key);
        if(value != null) {
            return value;
        }

        return map.computeIfAbsent(key, mappingFunction);
    }

    private static class CircularListIterator<T> implements Iterator<T> {
        private final List<T> list;
        private final int size;
        private int index;

        public CircularListIterator(List<T> list) {
            if (list.isEmpty()) {
                throw new RuntimeException("List cannot be empty");
            }
            this.list = Collections.unmodifiableList(list);
            this.size = list.size();
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return !list.isEmpty();
        }

        @Override
        public T next() {
            T item = list.get(index);
            index = ++index % size;
            return item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(Comparison.class.getSimpleName())
//                .addProfiler(JavaFlightRecorderProfiler.class)
                .addProfiler(GCProfiler.class)
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark.json")
                .build();
        new Runner(opt).run();
    }
}
