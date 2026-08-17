package net.minecraft.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.Dynamic;
import it.unimi.dsi.fastutil.Hash.Strategy;
import net.deltaclient.CompletableFuture;
import net.lax1dude.eaglercraft.EaglercraftUUID;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;
import net.minecraft.state.IProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
    private static final AtomicInteger NEXT_SERVER_WORKER_ID = new AtomicInteger(1);
    private static final Executor SERVER_EXECUTOR = createServerExecutor();
    public static LongSupplier nanoTimeSupplier = System::nanoTime;
    private static final Logger LOGGER = LogManager.getLogger();

    public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMapCollector() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <T extends Comparable<T>> String getValueName(IProperty<T> property, Object value) {
        return property.getName((T) (value));
    }

    public static String makeTranslationKey(String type, ResourceLocation id) {
        return id == null ? type + ".unregistered_sadface" : type + '.' + id.getNamespace() + '.' + id.getPath().replace('/', '.');
    }

    public static long milliTime() {
        return nanoTime() / 1000000L;
    }

    public static long nanoTime() {
        return nanoTimeSupplier.getAsLong();
    }

    public static long millisecondsSinceEpoch() {
        return Instant.now().toEpochMilli();
    }

    private static Executor createServerExecutor() {
        return command -> command.run();
    }

    public static Executor getServerExecutor() {
        return SERVER_EXECUTOR;
    }

    public static void shutdownServerExecutor() {


    }

    @OnlyIn(Dist.CLIENT)
    public static <T> CompletableFuture<T> completedExceptionallyFuture(Throwable p_215087_0_) {
        CompletableFuture<T> completablefuture = new CompletableFuture<>();
        completablefuture.completeExceptionally(p_215087_0_);
        return completablefuture;
    }

    public static Util.OS getOSType() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (s.contains("win")) {
            return Util.OS.WINDOWS;
        } else if (s.contains("mac")) {
            return Util.OS.OSX;
        } else if (s.contains("solaris")) {
            return Util.OS.SOLARIS;
        } else if (s.contains("sunos")) {
            return Util.OS.SOLARIS;
        } else if (s.contains("linux")) {
            return Util.OS.LINUX;
        } else {
            return s.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
        }
    }

    public static Stream<String> getJvmFlags() {
        return Stream.empty();
    }

    public static <T> T func_223378_a(List<T> p_223378_0_) {
        return p_223378_0_.get(p_223378_0_.size() - 1);
    }

    public static <T> T getElementAfter(Iterable<T> iterable, T element) {
        Iterator<T> iterator = iterable.iterator();
        T t = iterator.next();
        if (element != null) {
            T t1 = t;

            while (t1 != element) {
                if (iterator.hasNext()) {
                    t1 = iterator.next();
                }
            }

            if (iterator.hasNext()) {
                return iterator.next();
            }
        }

        return t;
    }

    public static <T> T getElementBefore(Iterable<T> iterable, T current) {
        Iterator<T> iterator = iterable.iterator();

        T t;
        T t1;
        for (t = null; iterator.hasNext(); t = t1) {
            t1 = iterator.next();
            if (t1 == current) {
                if (t == null) {
                    t = (T) (iterator.hasNext() ? Iterators.getLast(iterator) : current);
                }
                break;
            }
        }

        return t;
    }

    public static <T> T make(Supplier<T> supplier) {
        return supplier.get();
    }

    public static <T> T make(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    public static <K> Strategy<K> identityHashStrategy() {
        return (Strategy<K>) Util.IdentityStrategy.INSTANCE;
    }

    public static <V> CompletableFuture<List<V>> gather(List<? extends CompletableFuture<? extends V>> p_215079_0_) {
        List<V> list = Lists.newArrayListWithCapacity(p_215079_0_.size());
        CompletableFuture<?>[] completablefuture = new CompletableFuture[p_215079_0_.size()];
        CompletableFuture<Void> completablefuture1 = new CompletableFuture<>();
        p_215079_0_.forEach((p_215083_3_) -> {
            int i = list.size();
            list.add((V) null);
            completablefuture[i] = p_215083_3_.whenComplete((p_215085_3_, p_215085_4_) -> {
                if (p_215085_4_ != null) {
                    completablefuture1.completeExceptionally(p_215085_4_);
                } else {
                    list.set(i, p_215085_3_);
                }

            });
        });
        return CompletableFuture.allOf(completablefuture).applyToEither(completablefuture1, (p_215089_1_) -> {
            return list;
        });
    }

    public static <T> Stream<T> streamOptional(Optional<? extends T> p_215081_0_) {
        return p_215081_0_.isPresent() ? Stream.of(p_215081_0_.get()) : Stream.empty();
    }

    public static <T> Optional<T> acceptOrElse(Optional<T> opt, Consumer<T> consumer, Runnable orElse) {
        if (opt.isPresent()) {
            consumer.accept(opt.get());
        } else {
            orElse.run();
        }

        return opt;
    }

    public static Runnable namedRunnable(Runnable p_215075_0_, Supplier<String> p_215075_1_) {
        return p_215075_0_;
    }

    public static Optional<EaglercraftUUID> readUUID(String p_215074_0_, Dynamic<?> p_215074_1_) {
        return null;
    }

    static enum IdentityStrategy implements Strategy<Object> {
        INSTANCE;

        public int hashCode(Object p_hashCode_1_) {
            return System.identityHashCode(p_hashCode_1_);
        }

        public boolean equals(Object p_equals_1_, Object p_equals_2_) {
            return p_equals_1_ == p_equals_2_;
        }
    }

    public static enum OS {
        LINUX,
        SOLARIS,
        WINDOWS {
            @OnlyIn(Dist.CLIENT)
            protected String[] getOpenCommandLine(URL url) {
                return new String[]{"rundll32", "url.dll,FileProtocolHandler", url.toString()};
            }
        },
        OSX {
            @OnlyIn(Dist.CLIENT)
            protected String[] getOpenCommandLine(URL url) {
                return new String[]{"open", url.toString()};
            }
        },
        UNKNOWN;

        private OS() {
        }

        @OnlyIn(Dist.CLIENT)
        public void openURL(URL url) {
            net.lax1dude.eaglercraft.EagRuntime.openLink(url.toString());

        }

        @OnlyIn(Dist.CLIENT)
        public void openURI(URI uri) {
            try {
                this.openURL(uri.toURL());
            } catch (MalformedURLException malformedurlexception) {
                Util.LOGGER.error("Couldn't open uri '{}'", uri, malformedurlexception);
            }

        }

        @OnlyIn(Dist.CLIENT)
        public void openFile(VFile2 fileIn) {
            try {
                this.openURL(new java.net.URL("file:///" + fileIn.getPath()));
            } catch (MalformedURLException malformedurlexception) {
                Util.LOGGER.error("Couldn't open file '{}'", fileIn, malformedurlexception);
            }

        }

        @OnlyIn(Dist.CLIENT)
        protected String[] getOpenCommandLine(URL url) {
            String s = url.toString();
            if ("file".equals(url.getProtocol())) {
                s = s.replace("file:", "file://");
            }

            return new String[]{"xdg-open", s};
        }

        @OnlyIn(Dist.CLIENT)
        public void openURI(String uri) {
            try {
                this.openURL((new URI(uri)).toURL());
            } catch (MalformedURLException | IllegalArgumentException | URISyntaxException urisyntaxexception) {
                Util.LOGGER.error("Couldn't open uri '{}'", uri, urisyntaxexception);
            }

        }
    }
}
