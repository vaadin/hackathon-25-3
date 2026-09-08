package com.vaadin.bakery.diagnostics;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionLockAcquiredEvent;
import com.vaadin.flow.server.SessionLockReleasedEvent;
import com.vaadin.flow.server.SessionLockRequestedEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.RpcInvocationEndedEvent;
import com.vaadin.flow.server.communication.RpcInvocationStartedEvent;
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

        bus.addListener(RpcInvocationStartedEvent.class, rpc -> rpcInvocations.incrementAndGet());
        bus.addListener(RpcInvocationEndedEvent.class, rpc -> {
        });

        // The two that matter for the Grid claim: a hidden column should not
        // produce either of these.
        bus.addListener(DataCountStartedEvent.class, count -> dataCountQueries.incrementAndGet());
        bus.addListener(DataCountEndedEvent.class, count -> {
        });
        bus.addListener(DataFetchStartedEvent.class, fetch -> {
            dataFetchQueries.incrementAndGet();
            fetchesByCaller.merge(callerName(fetch), 1L, Long::sum);
        });
        bus.addListener(DataFetchEndedEvent.class, fetch -> {
        });
    }

    /**
     * Which component asked for the data, for the per view breakdown.
     *
     * The event carries it, which is the whole reason the 25.3 fetch events
     * take a component: a query can be attributed to the grid or the combo box
     * that issued it rather than only to the request it arrived in. This used
     * to walk the stack for the first `com.vaadin.bakery` frame, which is a
     * frame of this class, because a listener runs inside the listener: the
     * column read "PlatformEventRecorder" for every fetch in the application
     * and had never read anything else.
     *
     * A filtered fetch is named as one, because a combo box loading matches for
     * what somebody typed and a grid loading its next page are the same event
     * and not the same cost.
     */
    private String callerName(DataFetchStartedEvent fetch) {
        var component = fetch.getComponent()
                .map(item -> item.getClass().getSimpleName())
                .orElse("unattributed");
        return fetch.isFiltered() ? component + " (filtered)" : component;
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
