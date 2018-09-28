package com.sebastian_daschner.porcupine_metrics;

import com.airhacks.porcupine.execution.entity.Statistics;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@ApplicationScoped
public class PorcupineMetrics {

    @Inject
    Instance<List<Statistics>> statistics;

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry metricRegistry;

    private final Map<String, SimpleGauge> gauges = new ConcurrentHashMap<>();

    public void updateMetrics() {
        statistics.get().forEach(this::updateMetrics);
    }

    private void updateMetrics(Statistics statistics) {
        update(statistics, "tasks.total", Statistics::getTotalNumberOfTasks);
        update(statistics, "tasks.completed", Statistics::getCompletedTaskCount);
        update(statistics, "tasks.rejected", Statistics::getRejectedTasks);

        update(statistics, "active.thread.count", Statistics::getActiveThreadCount);
        update(statistics, "current.thread.pool.size", Statistics::getCurrentThreadPoolSize);
        update(statistics, "largest.thread.pool.size", Statistics::getLargestThreadPoolSize);
        update(statistics, "core.pool.size", Statistics::getCorePoolSize);
        update(statistics, "maximum.pool.size", Statistics::getMaximumPoolSize);

        update(statistics, "min.queue.capacity", Statistics::getMinQueueCapacity);
        update(statistics, "remaining.queue.capacity", Statistics::getRemainingQueueCapacity);
    }

    private void update(Statistics statistics, String metric, Function<Statistics, ? extends Number> supplier) {
        final String name = MetricRegistry.name("porcupine.pipelines", statistics.getPipelineName(), metric);
        final SimpleGauge gauge = gauges.computeIfAbsent(name, this::registerGauge);
        gauge.set(supplier.apply(statistics).longValue());
    }

    private SimpleGauge registerGauge(String name) {
        return metricRegistry.register(name, new SimpleGauge());
    }

    private static class SimpleGauge extends AtomicLong implements Gauge<Long> {

        @Override
        public Long getValue() {
            return get();
        }

    }
}
