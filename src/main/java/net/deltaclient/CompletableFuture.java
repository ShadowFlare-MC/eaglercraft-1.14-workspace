package net.deltaclient;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.*;

/*
    This class is a rewrite of CompletableFuture.
    Made by deltaclient.
    TeaVM does not support java.util.concurrent.CompletableFuture
*/
public class CompletableFuture<V> {

    private static final Object NIL = new Object();
    private static final int INCOMPLETE = 0;
    private static final int NORMAL = 1;
    private static final int EXCEPTIONAL = 2;
    private static final int CANCELLED = 3;

    volatile int state;
    volatile Object outcome;
    volatile Completion stack;

    public CompletableFuture() {
    }

    private CompletableFuture(Object unused) {
    }

    public static <U> CompletableFuture<U> completedFuture(U value) {
        CompletableFuture<U> f = new CompletableFuture<>(null);
        f.completeValue(value);
        return f;
    }

    public static <U> CompletableFuture<U> completedExceptionallyFuture(Throwable ex) {
        CompletableFuture<U> f = new CompletableFuture<>(null);
        f.completeThrowable(ex);
        return f;
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, Runnable::run);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(executor);
        CompletableFuture<U> f = new CompletableFuture<>(null);
        executor.execute(() -> {
            try {
                f.completeValue(supplier.get());
            } catch (Throwable ex) {
                f.completeThrowable(ex);
            }
        });
        return f;
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, Runnable::run);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        Objects.requireNonNull(runnable);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> f = new CompletableFuture<>(null);
        executor.execute(() -> {
            try {
                runnable.run();
                f.completeValue(null);
            } catch (Throwable ex) {
                f.completeThrowable(ex);
            }
        });
        return f;
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        Objects.requireNonNull(cfs);
        if (cfs.length == 0) return completedFuture(null);
        CompletableFuture<Void> result = new CompletableFuture<>(null);
        AllOf allOf = new AllOf(result, cfs.length);
        for (CompletableFuture<?> cf : cfs) {
            cf.push(new DualNotify(allOf));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <U> CompletableFuture<U> anyOf(CompletableFuture<?>... cfs) {
        Objects.requireNonNull(cfs);
        if (cfs.length == 0) throw new IllegalArgumentException();
        CompletableFuture<U> result = new CompletableFuture<>(null);
        for (CompletableFuture<?> cf : cfs) {
            cf.push(new AltAnyOf<>((CompletableFuture<U>) cf, result));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    V internalGet() {
        Object r = outcome;
        if (state == NORMAL) return (r == NIL) ? null : (V) r;
        if (state == EXCEPTIONAL) {
            RuntimeException rex = new RuntimeException();
            rex.initCause((Throwable) r);
            throw rex;
        }
        if (state == CANCELLED) throw new RuntimeException();
        throw new IllegalStateException("Not completed");
    }

    @SuppressWarnings("unchecked")
    V reportJoin() {
        Object r = outcome;
        if (state == EXCEPTIONAL) {
            Throwable x = (Throwable) r;
            if (x instanceof CompletionException) throw (CompletionException) x;
            throw new CompletionException(x);
        }
        if (state == CANCELLED) throw new RuntimeException();
        return (r == NIL) ? null : (V) r;
    }

    @SuppressWarnings("unchecked")
    V reportGet() throws ExecutionException, InterruptedException {
        Object r = outcome;
        if (state == EXCEPTIONAL) throw new ExecutionException((Throwable) r);
        if (state == CANCELLED) throw new RuntimeException();
        return (r == NIL) ? null : (V) r;
    }

    private boolean completeValue(Object r) {
        synchronized (this) {
            if (state != INCOMPLETE) return false;
            outcome = (r == null) ? NIL : r;
            state = NORMAL;
        }
        postComplete();
        return true;
    }

    private boolean completeThrowable(Throwable x) {
        synchronized (this) {
            if (state != INCOMPLETE) return false;
            outcome = x;
            state = EXCEPTIONAL;
        }
        postComplete();
        return true;
    }

    final void push(Completion c) {
        synchronized (this) {
            if (state != INCOMPLETE) {
                c.tryFire(false);
                return;
            }
            c.next = stack;
            stack = c;
        }
    }

    final void postComplete() {
        for (; ; ) {
            Completion c;
            synchronized (this) {
                c = stack;
                stack = null;
            }
            while (c != null) {
                Completion n = c.next;
                c.next = null;
                c.tryFire(false);
                c = n;
            }
            synchronized (this) {
                if (stack == null) return;
            }
        }
    }

    public <U> CompletableFuture<U> thenApply(Function<? super V, ? extends U> fn) {
        Objects.requireNonNull(fn);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        push(new UniApply<>(this, d, fn, null));
        return d;
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super V, ? extends U> fn) {
        return thenApplyAsync(fn, Runnable::run);
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super V, ? extends U> fn, Executor executor) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        push(new UniApply<>(this, d, fn, executor));
        return d;
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super V> action) {
        Objects.requireNonNull(action);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        push(new UniAccept<>(this, d, action, null));
        return d;
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super V> action) {
        return thenAcceptAsync(action, Runnable::run);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super V> action, Executor executor) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        push(new UniAccept<>(this, d, action, executor));
        return d;
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        Objects.requireNonNull(action);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        push(new UniRun(this, d, action, null));
        return d;
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, Runnable::run);
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        push(new UniRun(this, d, action, executor));
        return d;
    }

    public <U> CompletableFuture<U> thenCompose(Function<? super V, ? extends CompletableFuture<U>> fn) {
        Objects.requireNonNull(fn);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        push(new UniCompose<>(this, d, fn, null));
        return d;
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super V, ? extends CompletableFuture<U>> fn) {
        return thenComposeAsync(fn, Runnable::run);
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super V, ? extends CompletableFuture<U>> fn, Executor executor) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        push(new UniCompose<>(this, d, fn, executor));
        return d;
    }

    public <U, V1> CompletableFuture<V1> thenCombine(CompletableFuture<? extends U> other, BiFunction<? super V, ? super U, ? extends V1> fn) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(fn);
        CompletableFuture<V1> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        BiApply<V, U, V1> c = new BiApply<>(this, (CompletableFuture<U>) other, d, fn, null);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public <U, V1> CompletableFuture<V1> thenCombineAsync(CompletableFuture<? extends U> other, BiFunction<? super V, ? super U, ? extends V1> fn) {
        return thenCombineAsync(other, fn, Runnable::run);
    }

    public <U, V1> CompletableFuture<V1> thenCombineAsync(CompletableFuture<? extends U> other, BiFunction<? super V, ? super U, ? extends V1> fn, Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);
        CompletableFuture<V1> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        BiApply<V, U, V1> c = new BiApply<>(this, (CompletableFuture<U>) other, d, fn, executor);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public <U extends V> CompletableFuture<Void> thenAcceptBoth(CompletableFuture<? extends U> other, BiConsumer<? super V, ? super U> action) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        BiAccept<V, U> c = new BiAccept<>(this, (CompletableFuture<U>) other, d, action, null);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public <U extends V> CompletableFuture<Void> thenAcceptBothAsync(CompletableFuture<? extends U> other, BiConsumer<? super V, ? super U> action) {
        return thenAcceptBothAsync(other, action, Runnable::run);
    }

    public <U extends V> CompletableFuture<Void> thenAcceptBothAsync(CompletableFuture<? extends U> other, BiConsumer<? super V, ? super U> action, Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        BiAccept<V, U> c = new BiAccept<>(this, (CompletableFuture<U>) other, d, action, executor);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public CompletableFuture<Void> runAfterBoth(CompletableFuture<?> other, Runnable action) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        BiRun c = new BiRun(this, other, d, action, null);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletableFuture<?> other, Runnable action) {
        return runAfterBothAsync(other, action, Runnable::run);
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletableFuture<?> other, Runnable action, Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        BiRun c = new BiRun(this, other, d, action, executor);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public <U> CompletableFuture<U> applyToEither(CompletableFuture<? extends V> other, Function<? super V, ? extends U> fn) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(fn);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        AltApply<V, U> c = new AltApply<>(this, (CompletableFuture<V>) other, d, fn, null);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletableFuture<? extends V> other, Function<? super V, ? extends U> fn) {
        return applyToEitherAsync(other, fn, Runnable::run);
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletableFuture<? extends V> other, Function<? super V, ? extends U> fn, Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        AltApply<V, U> c = new AltApply<>(this, (CompletableFuture<V>) other, d, fn, executor);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public CompletableFuture<Void> acceptEither(CompletableFuture<? extends V> other, Consumer<? super V> action) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        AltAccept<V> c = new AltAccept<>(this, (CompletableFuture<V>) other, d, action, null);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletableFuture<? extends V> other, Consumer<? super V> action) {
        return acceptEitherAsync(other, action, Runnable::run);
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletableFuture<? extends V> other, Consumer<? super V> action, Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        @SuppressWarnings("unchecked")
        AltAccept<V> c = new AltAccept<>(this, (CompletableFuture<V>) other, d, action, executor);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public CompletableFuture<Void> runAfterEither(CompletableFuture<?> other, Runnable action) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        AltRun c = new AltRun(this, other, d, action, null);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletableFuture<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, Runnable::run);
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletableFuture<?> other, Runnable action, Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<Void> d = new CompletableFuture<>(null);
        AltRun c = new AltRun(this, other, d, action, executor);
        push(new DualNotify(c));
        other.push(new DualNotify(c));
        return d;
    }

    public <U> CompletableFuture<U> handle(BiFunction<? super V, Throwable, ? extends U> fn) {
        Objects.requireNonNull(fn);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        push(new UniHandle<>(this, d, fn, null));
        return d;
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super V, Throwable, ? extends U> fn) {
        return handleAsync(fn, Runnable::run);
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super V, Throwable, ? extends U> fn, Executor executor) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);
        CompletableFuture<U> d = new CompletableFuture<>(null);
        push(new UniHandle<>(this, d, fn, executor));
        return d;
    }

    public CompletableFuture<V> whenComplete(BiConsumer<? super V, ? super Throwable> action) {
        Objects.requireNonNull(action);
        CompletableFuture<V> d = new CompletableFuture<>(null);
        push(new UniWhenComplete<>(this, d, action, null));
        return d;
    }

    public CompletableFuture<V> whenCompleteAsync(BiConsumer<? super V, ? super Throwable> action) {
        return whenCompleteAsync(action, Runnable::run);
    }

    public CompletableFuture<V> whenCompleteAsync(BiConsumer<? super V, ? super Throwable> action, Executor executor) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);
        CompletableFuture<V> d = new CompletableFuture<>(null);
        push(new UniWhenComplete<>(this, d, action, executor));
        return d;
    }

    public CompletableFuture<V> exceptionally(Function<Throwable, ? extends V> fn) {
        Objects.requireNonNull(fn);
        CompletableFuture<V> d = new CompletableFuture<>(null);
        push(new UniExceptionally<>(this, d, fn, null));
        return d;
    }

    public CompletableFuture<V> exceptionallyCompose(Function<Throwable, ? extends CompletableFuture<V>> fn) {
        Objects.requireNonNull(fn);
        CompletableFuture<V> d = new CompletableFuture<>(null);
        push(new UniExceptionallyCompose<>(this, d, fn, null));
        return d;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            if (state != INCOMPLETE) return false;
            state = CANCELLED;
            outcome = null;
        }
        postComplete();
        return true;
    }

    public boolean isCancelled() {
        return state == CANCELLED;
    }

    public boolean isDone() {
        return state != INCOMPLETE;
    }

    public boolean isCompletedExceptionally() {
        return state == EXCEPTIONAL;
    }

    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s == INCOMPLETE) throw new InterruptedException();
        return reportGet();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        int s = state;
        if (s == INCOMPLETE) throw new InterruptedException();
        return reportGet();
    }

    public V join() {
        int s = state;
        if (s == INCOMPLETE) throw new IllegalStateException("Not completed");
        return reportJoin();
    }

    public V getNow(V valueIfAbsent) {
        int s = state;
        if (s == INCOMPLETE) return valueIfAbsent;
        if (s == EXCEPTIONAL) {
            RuntimeException rex = new RuntimeException();
            rex.initCause((Throwable) outcome);
            throw rex;
        }
        if (s == CANCELLED) throw new RuntimeException();
        Object r = outcome;
        return (r == NIL) ? null : (V) r;
    }

    public boolean complete(V value) {
        synchronized (this) {
            if (state != INCOMPLETE) return false;
            outcome = (value == null) ? NIL : value;
            state = NORMAL;
        }
        postComplete();
        return true;
    }

    public boolean completeExceptionally(Throwable ex) {
        synchronized (this) {
            if (state != INCOMPLETE) return false;
            outcome = ex;
            state = EXCEPTIONAL;
        }
        postComplete();
        return true;
    }

    public CompletableFuture<V> toCompletableFuture() {
        return this;
    }

    // ======================== Completion nodes ========================

    abstract static class Completion implements Runnable {
        volatile Completion next;

        abstract void tryFire(boolean fromExecutor);

        public final void run() {
            tryFire(true);
        }
    }

    static final class DualNotify extends Completion {
        final Completion target;

        DualNotify(Completion target) {
            this.target = target;
        }

        void tryFire(boolean fe) {
            target.tryFire(fe);
        }
    }

    static final class UniApply<T, V> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<V> dep;
        final Function<? super T, ? extends V> fn;
        final Executor executor;

        UniApply(CompletableFuture<T> src, CompletableFuture<V> dep, Function<? super T, ? extends V> fn, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src.outcome);
                return;
            }
            try {
                dep.completeValue(fn.apply(src.internalGet()));
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniAccept<T> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<Void> dep;
        final Consumer<? super T> action;
        final Executor executor;

        UniAccept(CompletableFuture<T> src, CompletableFuture<Void> dep, Consumer<? super T> action, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src.outcome);
                return;
            }
            try {
                action.accept(src.internalGet());
                dep.completeValue(null);
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniRun extends Completion {
        final CompletableFuture<?> src;
        final CompletableFuture<Void> dep;
        final Runnable action;
        final Executor executor;

        UniRun(CompletableFuture<?> src, CompletableFuture<Void> dep, Runnable action, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src.outcome);
                return;
            }
            try {
                action.run();
                dep.completeValue(null);
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniCompose<T, V> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<V> dep;
        final Function<? super T, ? extends CompletableFuture<V>> fn;
        final Executor executor;

        UniCompose(CompletableFuture<T> src, CompletableFuture<V> dep, Function<? super T, ? extends CompletableFuture<V>> fn, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src.outcome);
                return;
            }
            try {
                CompletableFuture<V> inner = fn.apply(src.internalGet());
                if (inner == null) {
                    dep.completeValue(null);
                } else {
                    inner.whenComplete((v, ex) -> {
                        if (ex != null) dep.completeThrowable(ex);
                        else dep.completeValue(v);
                    });
                }
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniHandle<T, V> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<V> dep;
        final BiFunction<? super T, Throwable, ? extends V> fn;
        final Executor executor;

        UniHandle(CompletableFuture<T> src, CompletableFuture<V> dep, BiFunction<? super T, Throwable, ? extends V> fn, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            try {
                if (s == EXCEPTIONAL) {
                    dep.completeValue(fn.apply(null, (Throwable) src.outcome));
                } else {
                    dep.completeValue(fn.apply(src.internalGet(), null));
                }
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniWhenComplete<T> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<T> dep;
        final BiConsumer<? super T, ? super Throwable> action;
        final Executor executor;

        UniWhenComplete(CompletableFuture<T> src, CompletableFuture<T> dep, BiConsumer<? super T, ? super Throwable> action, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        @SuppressWarnings("unchecked")
        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                try {
                    action.accept(null, null);
                } catch (Throwable ex) {
                    dep.completeThrowable(ex);
                    return;
                }
                dep.cancel(false);
                return;
            }
            try {
                if (s == EXCEPTIONAL) {
                    Throwable ex = (Throwable) src.outcome;
                    action.accept(null, ex);
                    dep.completeThrowable(ex);
                } else {
                    T v = src.internalGet();
                    action.accept(v, null);
                    dep.completeValue(v);
                }
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniExceptionally<T> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<T> dep;
        final Function<Throwable, ? extends T> fn;
        final Executor executor;

        UniExceptionally(CompletableFuture<T> src, CompletableFuture<T> dep, Function<Throwable, ? extends T> fn, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s == NORMAL) {
                dep.completeValue(src.internalGet());
                return;
            }
            try {
                dep.completeValue(fn.apply((Throwable) src.outcome));
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class UniExceptionallyCompose<T> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<T> dep;
        final Function<Throwable, ? extends CompletableFuture<T>> fn;
        final Executor executor;

        UniExceptionallyCompose(CompletableFuture<T> src, CompletableFuture<T> dep, Function<Throwable, ? extends CompletableFuture<T>> fn, Executor executor) {
            this.src = src;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s == NORMAL) {
                dep.completeValue(src.internalGet());
                return;
            }
            try {
                CompletableFuture<T> inner = fn.apply((Throwable) src.outcome);
                if (inner == null) {
                    dep.completeValue(null);
                } else {
                    inner.whenComplete((v, ex) -> {
                        if (ex != null) dep.completeThrowable(ex);
                        else dep.completeValue(v);
                    });
                }
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class BiApply<T, U, V> extends Completion {
        final CompletableFuture<T> src1;
        final CompletableFuture<U> src2;
        final CompletableFuture<V> dep;
        final BiFunction<? super T, ? super U, ? extends V> fn;
        final Executor executor;
        volatile boolean fired;

        BiApply(CompletableFuture<T> src1, CompletableFuture<U> src2, CompletableFuture<V> dep, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s1 = src1.state;
            int s2 = src2.state;
            if (s1 == INCOMPLETE || s2 == INCOMPLETE) return;
            if (fired) return;
            fired = true;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s1 == CANCELLED || s2 == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s1 == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src1.outcome);
                return;
            }
            if (s2 == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src2.outcome);
                return;
            }
            try {
                dep.completeValue(fn.apply(src1.internalGet(), src2.internalGet()));
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class BiAccept<T, U> extends Completion {
        final CompletableFuture<T> src1;
        final CompletableFuture<U> src2;
        final CompletableFuture<Void> dep;
        final BiConsumer<? super T, ? super U> action;
        final Executor executor;
        volatile boolean fired;

        BiAccept(CompletableFuture<T> src1, CompletableFuture<U> src2, CompletableFuture<Void> dep, BiConsumer<? super T, ? super U> action, Executor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s1 = src1.state;
            int s2 = src2.state;
            if (s1 == INCOMPLETE || s2 == INCOMPLETE) return;
            if (fired) return;
            fired = true;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s1 == CANCELLED || s2 == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s1 == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src1.outcome);
                return;
            }
            if (s2 == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src2.outcome);
                return;
            }
            try {
                action.accept(src1.internalGet(), src2.internalGet());
                dep.completeValue(null);
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class BiRun extends Completion {
        final CompletableFuture<?> src1;
        final CompletableFuture<?> src2;
        final CompletableFuture<Void> dep;
        final Runnable action;
        final Executor executor;
        volatile boolean fired;

        BiRun(CompletableFuture<?> src1, CompletableFuture<?> src2, CompletableFuture<Void> dep, Runnable action, Executor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s1 = src1.state;
            int s2 = src2.state;
            if (s1 == INCOMPLETE || s2 == INCOMPLETE) return;
            if (fired) return;
            fired = true;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s1 == CANCELLED || s2 == CANCELLED) {
                dep.cancel(false);
                return;
            }
            if (s1 == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src1.outcome);
                return;
            }
            if (s2 == EXCEPTIONAL) {
                dep.completeThrowable((Throwable) src2.outcome);
                return;
            }
            try {
                action.run();
                dep.completeValue(null);
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class AltApply<T, V> extends Completion {
        final CompletableFuture<T> src1;
        final CompletableFuture<T> src2;
        final CompletableFuture<V> dep;
        final Function<? super T, ? extends V> fn;
        final Executor executor;
        volatile boolean fired;

        AltApply(CompletableFuture<T> src1, CompletableFuture<T> src2, CompletableFuture<V> dep, Function<? super T, ? extends V> fn, Executor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.dep = dep;
            this.fn = fn;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s1 = src1.state;
            int s2 = src2.state;
            if (s1 == INCOMPLETE && s2 == INCOMPLETE) return;
            if (fired) return;
            fired = true;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            T r;
            if (s1 != INCOMPLETE) {
                if (s1 == CANCELLED) {
                    dep.cancel(false);
                    return;
                }
                if (s1 == EXCEPTIONAL) {
                    dep.completeThrowable((Throwable) src1.outcome);
                    return;
                }
                r = src1.internalGet();
            } else {
                if (s2 == CANCELLED) {
                    dep.cancel(false);
                    return;
                }
                if (s2 == EXCEPTIONAL) {
                    dep.completeThrowable((Throwable) src2.outcome);
                    return;
                }
                r = src2.internalGet();
            }
            try {
                dep.completeValue(fn.apply(r));
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class AltAccept<T> extends Completion {
        final CompletableFuture<T> src1;
        final CompletableFuture<T> src2;
        final CompletableFuture<Void> dep;
        final Consumer<? super T> action;
        final Executor executor;
        volatile boolean fired;

        AltAccept(CompletableFuture<T> src1, CompletableFuture<T> src2, CompletableFuture<Void> dep, Consumer<? super T> action, Executor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s1 = src1.state;
            int s2 = src2.state;
            if (s1 == INCOMPLETE && s2 == INCOMPLETE) return;
            if (fired) return;
            fired = true;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            T r;
            if (s1 != INCOMPLETE) {
                if (s1 == CANCELLED) {
                    dep.cancel(false);
                    return;
                }
                if (s1 == EXCEPTIONAL) {
                    dep.completeThrowable((Throwable) src1.outcome);
                    return;
                }
                r = src1.internalGet();
            } else {
                if (s2 == CANCELLED) {
                    dep.cancel(false);
                    return;
                }
                if (s2 == EXCEPTIONAL) {
                    dep.completeThrowable((Throwable) src2.outcome);
                    return;
                }
                r = src2.internalGet();
            }
            try {
                action.accept(r);
                dep.completeValue(null);
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class AltRun extends Completion {
        final CompletableFuture<?> src1;
        final CompletableFuture<?> src2;
        final CompletableFuture<Void> dep;
        final Runnable action;
        final Executor executor;
        volatile boolean fired;

        AltRun(CompletableFuture<?> src1, CompletableFuture<?> src2, CompletableFuture<Void> dep, Runnable action, Executor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.dep = dep;
            this.action = action;
            this.executor = executor;
        }

        void tryFire(boolean fe) {
            int s1 = src1.state;
            int s2 = src2.state;
            if (s1 == INCOMPLETE && s2 == INCOMPLETE) return;
            if (fired) return;
            fired = true;
            if (!fe && executor != null) {
                executor.execute(this);
                return;
            }
            if (s1 != INCOMPLETE) {
                if (s1 == CANCELLED) {
                    dep.cancel(false);
                    return;
                }
                if (s1 == EXCEPTIONAL) {
                    dep.completeThrowable((Throwable) src1.outcome);
                    return;
                }
            } else {
                if (s2 == CANCELLED) {
                    dep.cancel(false);
                    return;
                }
                if (s2 == EXCEPTIONAL) {
                    dep.completeThrowable((Throwable) src2.outcome);
                    return;
                }
            }
            try {
                action.run();
                dep.completeValue(null);
            } catch (Throwable ex) {
                dep.completeThrowable(ex);
            }
        }
    }

    static final class AltAnyOf<T> extends Completion {
        final CompletableFuture<T> src;
        final CompletableFuture<T> dep;

        AltAnyOf(CompletableFuture<T> src, CompletableFuture<T> dep) {
            this.src = src;
            this.dep = dep;
        }

        void tryFire(boolean fe) {
            int s = src.state;
            if (s == INCOMPLETE) return;
            if (s == CANCELLED) return;
            if (s == EXCEPTIONAL) return;
            T val = src.internalGet();
            dep.completeValue(val);
        }
    }

    static final class AllOf extends Completion {
        final CompletableFuture<Void> dep;
        volatile int remaining;


        AllOf(CompletableFuture<Void> dep, int count) {
            this.dep = dep;
            this.remaining = count;
        }

        void tryFire(boolean fe) {
            if (dep.state == CANCELLED) return;
            synchronized (this) {
                if (remaining <= 0) return;
                --remaining;
                if (remaining > 0) return;
            }
            dep.completeValue(null);
        }
    }
}
