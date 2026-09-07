package com.vaadin.bakery.assistant;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.ai.provider.ToolException;
import com.vaadin.flow.function.SerializableSupplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Running a tool's work where a component may be touched.
 *
 * A provider answers on a worker thread, so a tool body runs there too, and
 * reading or writing a component from a thread that does not hold the session
 * lock is not allowed. Everything a tool does to the form therefore goes
 * through {@code UI.access} and waits for its turn.
 *
 * The wait has a timeout on purpose. The platform's own form controller does
 * the same thing with an unbounded {@code get()}, and when that wait cannot be
 * satisfied the thread hangs for ever with nothing in the log. A tool that
 * gives up after ten seconds tells the model something, which is the whole
 * point of a tool refusal.
 */
public final class UiWork {

    private static final int TIMEOUT_SECONDS = 10;

    private UiWork() {
    }

    public static <T> T on(UI ui, SerializableSupplier<T> work) {
        if (ui == null) {
            throw new ToolException("This screen is no longer open, so nothing can be filled in.");
        }
        // Already holding the lock: do it here. This is what stops a tool that
        // is called on the UI thread from queueing work behind the very thread
        // that would have to run it, which is the deadlock the platform's own
        // form controller falls into.
        var session = ui.getSession();
        if (session != null && session.hasLock()) {
            return work.get();
        }

        var done = new CompletableFuture<T>();
        ui.access(() -> {
            try {
                done.complete(work.get());
            } catch (RuntimeException failure) {
                done.completeExceptionally(failure);
            }
        });
        try {
            return done.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException failure) {
            // The tool's own refusal, raised where the model can read it.
            if (failure.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new ToolException("The form could not be updated.", failure.getCause());
        } catch (TimeoutException tooSlow) {
            throw new ToolException("The form did not respond in time, so nothing was changed.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ToolException("The form update was interrupted, so nothing was changed.");
        }
    }
}
