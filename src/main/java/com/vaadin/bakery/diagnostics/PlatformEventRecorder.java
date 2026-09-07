package com.vaadin.bakery.diagnostics;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionLockAcquiredEvent;
import com.vaadin.flow.server.SessionLockReleasedEvent;
import com.vaadin.flow.server.SessionLockRequestedEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.RpcInvocationEndedEvent;
import com.vaadin.flow.server.communication.RpcInvocationEvent;
import com.vaadin.flow.server.data.DataCountEndedEvent;
import com.vaadin.flow.server.data.DataCountStartedEvent;
import com.vaadin.flow.server.data.DataFetchEndedEvent;
import com.vaadin.flow.server.data.DataFetchStartedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Listens to the platform on the 25.3 service event bus and keeps the numbers a
 * generic monitoring tool cannot produce: how long session locks are held, how
 * chatty a view is, and how many data provider queries a page costs.
 *
 * This is deliberately small. It is not a monitoring product, it is the
 * instrument that makes two claims in the specifications checkable.
 */
@Component
public class PlatformEventRecorder implements VaadinServiceInitListener {

    private final AtomicLong lockRequests = new AtomicLong();
    private final AtomicLong lockAcquisitions = new AtomicLong();
    private final AtomicLong lockReleases = new AtomicLong();
    private final AtomicLong longestLockMillis = new AtomicLong();
    private final AtomicLong rpcInvocations = new AtomicLong();
    private final AtomicLong dataCountQueries = new AtomicLong();
    private final AtomicLong dataFetchQueries = new AtomicLong();
    private final AtomicLong undeliveredInvocations = new AtomicLong();
    private final Map<String, Long> fetchesByCaller = new ConcurrentHashMap<>();
    private final ThreadLocal<Instant> lockAcquiredAt = new ThreadLocal<>();

    @Override
    public void serviceInit(ServiceInitEvent event) {
        var bus = event.getSource().getEventBus();

        bus.addListener(SessionLockRequestedEvent.class, requested -> lockRequests.incrementAndGet());
        bus.addListener(SessionLockAcquiredEvent.class, acquired -> {
            lockAcquisitions.incrementAndGet();
            lockAcquiredAt.set(Instant.now());
        });
        bus.addListener(SessionLockReleasedEvent.class, released -> {
            lockReleases.incrementAndGet();
            var acquired = lockAcquiredAt.get();
            if (acquired != null) {
                long held = Duration.between(acquired, Instant.now()).toMillis();
                longestLockMillis.accumulateAndGet(held, Math::max);
                lockAcquiredAt.remove();
            }
        });

        bus.addListener(RpcInvocationEvent.class, rpc -> rpcInvocations.incrementAndGet());
        bus.addListener(RpcInvocationEndedEvent.class, rpc -> {
        });

        // The two that matter for the Grid claim: a hidden column should not
        // produce either of these.
        bus.addListener(DataCountStartedEvent.class, count -> dataCountQueries.incrementAndGet());
        bus.addListener(DataCountEndedEvent.class, count -> {
        });
        bus.addListener(DataFetchStartedEvent.class, fetch -> {
            dataFetchQueries.incrementAndGet();
            fetchesByCaller.merge(callerName(), 1L, Long::sum);
        });
        bus.addListener(DataFetchEndedEvent.class, fetch -> {
        });
    }

    /** Which of our own classes asked for the data, for the per view breakdown. */
    private String callerName() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .map(StackWalker.StackFrame::getClassName)
                        .filter(name -> name.startsWith("com.vaadin.bakery"))
                        .findFirst()
                        .orElse("unknown"));
    }

    public void countUndeliveredInvocation() {
        undeliveredInvocations.incrementAndGet();
    }

    public long lockRequests() {
        return lockRequests.get();
    }

    public long lockAcquisitions() {
        return lockAcquisitions.get();
    }

    public long lockReleases() {
        return lockReleases.get();
    }

    public long longestLockMillis() {
        return longestLockMillis.get();
    }

    public long rpcInvocations() {
        return rpcInvocations.get();
    }

    public long dataCountQueries() {
        return dataCountQueries.get();
    }

    public long dataFetchQueries() {
        return dataFetchQueries.get();
    }

    public long undeliveredInvocations() {
        return undeliveredInvocations.get();
    }

    public Map<String, Long> fetchesByCaller() {
        return Map.copyOf(fetchesByCaller);
    }

    public void reset() {
        lockRequests.set(0);
        lockAcquisitions.set(0);
        lockReleases.set(0);
        longestLockMillis.set(0);
        rpcInvocations.set(0);
        dataCountQueries.set(0);
        dataFetchQueries.set(0);
        undeliveredInvocations.set(0);
        fetchesByCaller.clear();
    }
}
