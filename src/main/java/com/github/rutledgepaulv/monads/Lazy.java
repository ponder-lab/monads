package com.github.rutledgepaulv.monads;

import com.github.rutledgepaulv.monads.supporting.ToOptional;
import com.github.rutledgepaulv.monads.supporting.ToStream;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * A lazy evaluation monad that wraps a supplying function for one-time execution
 * and then caching of the generated value. Supports lazy mapping and flat mapping
 * over the contents.
 *
 * Supports conversion to an optional and a stream. An empty optional and empty stream
 * will be produced if the value is none. Getting as an optional immediately evaluates
 * the underlying supplier, however getting as a stream will not execute the supplier
 * until a terminal operation is performed on the stream. Either way, once the supplier
 * has been executed just once the value will be cached and the generating function
 * dereferenced.
 *
 * @param <T> The result type contained by the monad.
 */
public final class Lazy<T> implements Supplier<T>, ToOptional<T>, ToStream<T> {


    private static <S, T extends Lazy<?>> Lazy<S> flatten(Lazy<T> nested, Function<? super T, Lazy<S>> func) {
        return nested.stream().map(func).findFirst().orElseGet(Lazy::empty);
    }

    public static <S> Lazy<S> flatten2(Lazy<Lazy<S>> nested) {
        return flatten(nested, identity());
    }

    public static <S> Lazy<S> flatten3(Lazy<Lazy<Lazy<S>>> nested) {
        return flatten(nested, Lazy::flatten2);
    }

    public static <S> Lazy<S> flatten4(Lazy<Lazy<Lazy<Lazy<S>>>> nested) {
        return flatten(nested, Lazy::flatten3);
    }

    public static <S> Lazy<S> flatten5(Lazy<Lazy<Lazy<Lazy<Lazy<S>>>>> nested) {
        return flatten(nested, Lazy::flatten4);
    }

    public static <S> Lazy<S> flatten6(Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<S>>>>>> nested) {
        return flatten(nested, Lazy::flatten5);
    }

    public static <S> Lazy<S> flatten7(Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<S>>>>>>> nested) {
        return flatten(nested, Lazy::flatten6);
    }

    public static <S> Lazy<S> flatten8(Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<Lazy<S>>>>>>>> nested) {
        return flatten(nested, Lazy::flatten7);
    }


    /**
     * @return Create a pre-evaluated lazy of null value.
     */
    public static <S> Lazy<S> empty() {
        return new Lazy<>();
    }

    /**
     * @return Create a pre-evaluated lazy of the given value.
     */
    public static <S> Lazy<S> of(S value) {
        return new Lazy<>(value);
    }

    /**
     * @return Create a lazy wrapping the generating function. The function
     *         will be executed at most once and then the value will cache as
     *         long as the lazy is referenced. The generating function is
     *         dereferenced after first execution.
     */
    public static <S> Lazy<S> of(Supplier<S> supplier) {
        return new Lazy<>(supplier);
    }




    private final AtomicReference<T> value;
    private final AtomicReference<Supplier<T>> supplier;

    private Lazy(T value) {
        this.value = new AtomicReference<>(value);
        this.supplier = new AtomicReference<>();
    }

    private Lazy(Supplier<T> supplier) {
        this.supplier = new AtomicReference<>(supplier);
        this.value = new AtomicReference<>();
    }

    private Lazy() {
        this.supplier = new AtomicReference<>();
        this.value = new AtomicReference<>();
    }


    /**
     * Map across the lazy by wrapping it as a new lazy who upon execution
     * will evaluate the original and apply the function to the result.
     *
     * @param mapper The mapping function.
     * @return The composed lazy
     */
    public final <S> Lazy<S> map(Function<? super T,? extends S> mapper) {
        return of(() -> optional().map(mapper).orElse(null));
    }

    /**
     * Map across the lazy by wrapping it as a new lazy who upon execution
     * will evaluate the original and apply the function to the result and
     * then unwrap the function's lazy result.
     *
     * @param mapper The mapping function.
     * @return The composed lazy
     */
    public final <S> Lazy<S> flatMap(Function<? super T, Lazy<S>> mapper) {
        return of(() -> optional().map(mapper).map(Lazy::get).orElse(null));
    }

    /**
     * @return Get the value contained herein.
     */
    @Override
    public final T get() {
        Supplier<T> supplier = this.supplier.get();
        if(supplier != null && this.value.compareAndSet(null, supplier.get())) {
            this.supplier.compareAndSet(supplier, null);
        }
        return this.value.get();
    }

    /**
     * @return Get a stream that will lazily load the value
     *         contained herein.
     */
    @Override
    public final Stream<T> stream() {
        return Stream.of(this).map(Lazy::optional)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * @return Get this lazy as an optional wrapping its value. If it
     *         was an empty lazy or the generating function evaluated
     *         to null, then the optional will be empty.
     */
    @Override
    public final Optional<T> optional() {
        return Optional.ofNullable(get());
    }

}