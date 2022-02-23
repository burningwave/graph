/*
 * This file is part of Burningwave Graph.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/graph
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019-2022 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.core.extension;

import org.burningwave.core.Component;

public abstract class CommandWrapper<T, C, I, O>  implements Component {
	C command;
	T target;

	public T getTarget() {
		return target;
	}

	public abstract O executeOn(I data);

	@SuppressWarnings("unchecked")
	public static <T, F, I, O, W extends CommandWrapper<T, F ,I, O>> W create(
		F functionInterface, T instance) throws Throwable {
		if (functionInterface instanceof java.lang.Runnable) {
			return (W)new Runnable<T, I, O>(instance, (java.lang.Runnable)functionInterface);
		} else if (functionInterface instanceof java.util.function.Predicate) {
			return (W)new Predicate<T, I, O>(instance, (java.util.function.Predicate<I>)functionInterface);
		} else if (functionInterface instanceof java.util.function.BiPredicate) {
			return (W)new BiPredicate<T, I, O>(instance, (java.util.function.BiPredicate<T, I>)functionInterface);
		} else if (functionInterface instanceof java.util.function.Consumer) {
			return (W)new Consumer<T, I, O>(instance, (java.util.function.Consumer<I>)functionInterface);
		} else if (functionInterface instanceof java.util.function.BiConsumer) {
			return (W)new BiConsumer<T, I, O>(instance, (java.util.function.BiConsumer<T, I>)functionInterface);
		} else if (functionInterface instanceof java.util.function.Supplier) {
			return (W)new Supplier<T, I, O>(instance, ((java.util.function.Supplier<O>)functionInterface));
		} else if (functionInterface instanceof java.util.function.Function) {
			return (W)new Function<>(instance, ((java.util.function.Function<I, O>)functionInterface));
		} else if (functionInterface instanceof java.util.function.BiFunction) {
			return (W)new BiFunction<>(instance, ((java.util.function.BiFunction<T, I, O>)functionInterface));
		}
		return null;
	}

	CommandWrapper(T target, C t) {
		this.target = target;
		this.command = t;
	}

	static class Function<T, I, O> extends CommandWrapper<T, java.util.function.Function<I, O>, I, O> {
		Function(T target, java.util.function.Function<I, O> t) {
			super(target, t);
		}

		@Override
		public O executeOn(I data) {
			return command.apply(data);
		}

	}

	static class BiFunction<T, I, O> extends CommandWrapper<T, java.util.function.BiFunction<T, I, O>, I, O> {
		BiFunction(T target, java.util.function.BiFunction<T, I, O> t) {
			super(target, t);
		}

		@Override
		public O executeOn(I data) {
			return command.apply(target, data);
		}

	}

	static class Consumer<T, I, O> extends CommandWrapper<T, java.util.function.Consumer<I>, I, O> {
		Consumer(T target, java.util.function.Consumer<I> t) {
			super(target, t);
		}

		@SuppressWarnings("unchecked")
		@Override
		public O executeOn(I data) {
			command.accept(data);
			return (O)data;
		}
	}

	static class BiConsumer<T, I, O> extends CommandWrapper<T, java.util.function.BiConsumer<T, I>, I, O> {
		BiConsumer(T target, java.util.function.BiConsumer<T, I> t) {
			super(target, t);
		}

		@SuppressWarnings("unchecked")
		@Override
		public O executeOn(I data) {
			command.accept(target, data);
			return (O)data;
		}
	}

	static class Supplier<T, I, O> extends CommandWrapper<T, java.util.function.Supplier<O>, I, O> {
		Supplier(T target, java.util.function.Supplier<O> t) {
			super(target, t);
		}


		@Override
		public O executeOn(I data) {
			return command.get();
		}
	}

	static class Predicate<T, I, O> extends CommandWrapper<T, java.util.function.Predicate<I>, I, O> {

		Predicate(T target, java.util.function.Predicate<I> t) {
			super(target, t);
		}

		@SuppressWarnings("unchecked")
		@Override
		public O executeOn(I data) {
			command.test(data);
			return (O)data;
		}
	}

	static class BiPredicate<T, I, O> extends CommandWrapper<T, java.util.function.BiPredicate<T, I>, I, O> {

		BiPredicate(T target, java.util.function.BiPredicate<T, I> t) {
			super(target, t);
		}

		@SuppressWarnings("unchecked")
		@Override
		public O executeOn(I data) {
			command.test(target, data);
			return (O)data;
		}
	}

	static class Runnable<T, I, O> extends CommandWrapper<T, java.lang.Runnable, I, O> {

		Runnable(T target, java.lang.Runnable t) {
			super(target, t);
		}

		@SuppressWarnings("unchecked")
		@Override
		public O executeOn(I data) {
			command.run();
			return (O)data;
		}

	}

	@Override
	public void close() {
		command = null;
		target = null;
	}

}