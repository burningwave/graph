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
package org.burningwave.graph;

import static org.burningwave.core.assembler.StaticComponentContainer.Driver;
import static org.burningwave.core.assembler.StaticComponentContainer.ManagedLoggerRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.burningwave.core.Component;
import org.burningwave.core.classes.PropertyAccessor;
import org.burningwave.core.extension.CommandWrapper;
import org.burningwave.core.extension.Group;
import org.burningwave.core.iterable.IterableObjectHelper;
import org.burningwave.graph.ControllableContext.Directive;


public class Functions extends Group<CommandWrapper<?, ?, Context, Context>> {
	protected Map<String, Directive> onException;

	Functions() {
		super();
		onException = new LinkedHashMap<>();
	}

	void setOnException(Map<String, Directive> onException) {
		this.onException = onException;
	}

	static Functions create(
		PropertyAccessor byFieldOrByMethodPropertyAccessor,
		PropertyAccessor byMethodOrByFieldPropertyAccessor,
		IterableObjectHelper iterableObjectHelper
	) {
		return new Functions();
	}

	static Context.Abst castContext (Context context) {
		return ((Context.Abst)context);
	}


	public void executeOn(Object object) {
		Context context = (Context)object;
		ManagedLoggerRepository.logDebug(getClass()::getName, "Start executing functions group {}", getName());
		for (CommandWrapper<?, ?, Context, Context> functionWrapper : elements) {
			try {
				context = functionWrapper.executeOn(context);
			} catch (Throwable exc) {
				ManagedLoggerRepository.logError(getClass()::getName, "Exception occurred", exc);
				castContext(context).putAllDirectives(onException);
			}
			if (context.containsOneOf(getName(), Directive.Functions.STOP_PROCESSING)) {
				context.removeDirective(getName(), Directive.Functions.STOP_PROCESSING);
				ManagedLoggerRepository.logDebug(getClass()::getName, "Stopping processing functions group {}", Optional.ofNullable(getName()).orElse(""));
				break;
			}
		}
		ManagedLoggerRepository.logDebug(getClass()::getName, "End executing functions group {}", getName());
	}

	protected Function<Throwable, Void> getExceptionHandlingFunction(Context context) {
		return (exc) -> {
			Optional.ofNullable(exc).ifPresent((exception) -> {
				logError("Exception occurred", exception.getCause());
				Optional.ofNullable(onException).ifPresent((onExc) ->
					castContext(context).putAllDirectives(onExc)
				);
			});
			return null;
		};
	}


	public static class Async extends Functions {
		protected ExecutorService executor;

		private Async(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper, ExecutorService executor
		) {
			super();
			this.executor = executor;
		}

		public static Functions.Async create(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper
		) {
			return new Async(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, null);
		}

		public static Functions.Async create(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper,
				ExecutorService executor) {
			return new Async(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, executor);
		}

		public static Functions.Async create(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper,
				int threadsNumber) {
			return new Async(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, Executors.newFixedThreadPool(threadsNumber));
		}

		@Override
		public void executeOn(Object object) {
			Context context = (Context)object;
			logDebug("Start executing functions group {}", getName());
			List<CompletableFuture<?>> completableFutureList = new CopyOnWriteArrayList<>();
			elements.stream().filter(functionWrapper -> {
				Runnable runnableFunction = () -> functionWrapper.executeOn(context);
				CompletableFuture<?> completableFuture = (executor != null ?
					CompletableFuture.runAsync(runnableFunction, executor):
					CompletableFuture.runAsync(runnableFunction)
				).exceptionally(getExceptionHandlingFunction(context));
				completableFutureList.add(completableFuture);
				completableFuture.thenAcceptAsync(obj -> completableFutureList.remove(completableFuture));
				return context.containsOneOf(getName(), Directive.Functions.STOP_PROCESSING);
			}).findFirst().ifPresent(functionWrapper -> {
				context.removeDirective(getName(), Directive.Functions.STOP_PROCESSING);
				logDebug("Stopping processing functions group {}", Optional.ofNullable(getName()).orElse(""));
			});
			CompletableFuture.allOf(
				completableFutureList.stream().toArray(CompletableFuture<?>[]::new)
			).join();
			completableFutureList.clear();
			logDebug("End executing functions group {}", getName());
		}

		@Override
		public void close() {
			if (executor != null && !executor.isShutdown()) {
				executor.shutdownNow().clear();
				executor = null;
			}
			super.close();
		}
	}

	public static class ForCollection<T> extends Functions {

		protected AlgorithmsSupplier algorithmsSupplier;

		private ForCollection(AlgorithmsSupplier algorithmsSupplier) {
			super();
			this.algorithmsSupplier = algorithmsSupplier;
		}

		protected static <T> Functions.ForCollection<T> create(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper,
				AlgorithmsSupplier algorithmsSupplier) {
			return new Functions.ForCollection<>(algorithmsSupplier);
		}

		public static <T> Functions.ForCollection<T> create(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper,
				String iterableObjectContextKey,
				String loopResultContextKey) {
			return create(
					byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper,
					AlgorithmsSupplier.create(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, iterableObjectContextKey, loopResultContextKey)
			);
		}


		@Override
		@SuppressWarnings("unchecked")
		public void executeOn(Object object) {
			Context context = (Context)object;
			logDebug("Start executing functions group {}", getName());
			algorithmsSupplier.preLoopOperationsRetriever.accept(context);
			AtomicInteger counter = new AtomicInteger(0);
			algorithmsSupplier.iterableObjectStreamRetriever.apply(context).filter(item -> {
				final int idx = counter.getAndIncrement();
				executeOnItem(context, (T) item, idx);
				return context.containsOneOf(getName(), Directive.Functions.ForCollection.STOP_ITERATION);
			}).findFirst().ifPresent(functionWrapper -> {
				context.removeDirective(getName(), Directive.Functions.ForCollection.STOP_ITERATION);
				logDebug("Stopping iteration of functions group {}", Optional.ofNullable(getName()).orElse(""));
			});
			algorithmsSupplier.postLoopOperationsRetriever.accept(context);
			logDebug("End executing functions group {}", getName());
		}


		void executeOnItem(Context context, T item, int idx) {
			//Clone context
			Context clonedContext = algorithmsSupplier.putIteratedObjectInContextRetriever.apply(new Object[]{context, item, idx});
			elements.stream().filter(functionWrapper -> {
				functionWrapper.executeOn(clonedContext);
				return clonedContext.containsOneOf(getName(), Directive.Functions.STOP_PROCESSING);
			}).findFirst().ifPresent(functionWrapper -> {
				clonedContext.removeDirective(getName(), Directive.Functions.STOP_PROCESSING);
				logDebug("Stopping processing functions group {}", Optional.ofNullable(getName()).orElse(""));
			});
			try {
				clonedContext.close();
			} catch (Exception exc) {
				Driver.throwException(exc);
			}
		}

		@Override
		public void close() {
			algorithmsSupplier.close();
			algorithmsSupplier = null;
			super.close();
		}

		private static class AlgorithmsSupplier implements Component {
			@SuppressWarnings("unused")
			PropertyAccessor byFieldOrByMethodPropertyAccessor;
			PropertyAccessor byMethodOrByFieldPropertyAccessor;
			IterableObjectHelper iterableObjectHelper;
			String iterableObjectContextKey;
			String loopResultContextKey;

			//preLoopOperations
			Consumer<Context> preLoopOperationsRetriever = context -> {
				Optional.ofNullable(loopResultContextKey).ifPresent((oCDk) -> {
					Object[] resultsContainer =
						new Object[(int)iterableObjectHelper.getSize(retrieve(context, iterableObjectContextKey))];
					byMethodOrByFieldPropertyAccessor.set(context, oCDk, resultsContainer);
				});
			};

			// retrieveIterableObjectStream
			Function<Context, Stream<?>> iterableObjectStreamRetriever = context ->
				iterableObjectHelper.retrieveStream(retrieve(context, iterableObjectContextKey));

			// putIteratedObjectInContext
			Function<Object[], Context> putIteratedObjectInContextRetriever = (objects) -> {
				Context context = (Context)objects[0];
				Context clonedContext = context.createSymmetricClone();
				Object iterableObject = retrieve(context, iterableObjectContextKey);
				castContext(clonedContext).setCurrentIterationObjects(
					iterableObject,
					Optional.ofNullable(loopResultContextKey).map((oCDk) ->
						(Object[])retrieve(context, oCDk)
					).orElse(null),
					!(iterableObject instanceof Map)? objects[1] : ((Map<?,?>)iterableObject).get(objects[1]),
					(Integer)objects[2],
					!(iterableObject instanceof Map)? (Integer)objects[2] : objects[1]
				);
				return clonedContext;
			};

			// postLoopOperations
			Consumer<Context> postLoopOperationsRetriever = context -> {

			};


			private AlgorithmsSupplier(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper,
				String iterableObjectContextKey,
				String loopResultContextKey) {
				this.byFieldOrByMethodPropertyAccessor=byFieldOrByMethodPropertyAccessor;
				this.byMethodOrByFieldPropertyAccessor=byMethodOrByFieldPropertyAccessor;
				this.iterableObjectHelper = iterableObjectHelper;
				this.iterableObjectContextKey = iterableObjectContextKey;
				this.loopResultContextKey = loopResultContextKey;
			}

			static AlgorithmsSupplier create(
				PropertyAccessor byFieldOrByMethodPropertyAccessor,
				PropertyAccessor byMethodOrByFieldPropertyAccessor,
				IterableObjectHelper iterableObjectHelper,
				String iterableObjectContextKey,
				String loopResultContextKey) {
				return new AlgorithmsSupplier(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, iterableObjectContextKey, loopResultContextKey);
			}

			Object retrieve(Context context, String propertyPath) {
				return byMethodOrByFieldPropertyAccessor.get(context, propertyPath);
			}

			@Override
			public void close() {
				this.byFieldOrByMethodPropertyAccessor = null;
				this.byMethodOrByFieldPropertyAccessor = null;
				this.iterableObjectHelper = null;
				this.iterableObjectContextKey = null;
				this.loopResultContextKey = null;
			}
		}

		public static class Async<T> extends Functions.ForCollection<T> {
			protected ExecutorService executor;

			private Async(
					AlgorithmsSupplier algorithmsSupplier,
					ExecutorService executor) {
				super(algorithmsSupplier);
				this.executor = executor;
			}

			protected static <T> ForCollection.Async<T> create(
					AlgorithmsSupplier algorithmsSupplier,
					ExecutorService executor) {
				return new ForCollection.Async<>(algorithmsSupplier, executor);
			}

			public static <T> ForCollection.Async<T> create(
					PropertyAccessor byFieldOrByMethodPropertyAccessor,
					PropertyAccessor byMethodOrByFieldPropertyAccessor,
					IterableObjectHelper iterableObjectHelper,
					String iterableObjectContextKey,
					String loopResultContextKey,
					ExecutorService executor) {
				return create(
					AlgorithmsSupplier.create(
						byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, iterableObjectContextKey, loopResultContextKey
					)
					,executor
				);
			}

			public static <T> ForCollection.Async<T> create(
					PropertyAccessor byFieldOrByMethodPropertyAccessor,
					PropertyAccessor byMethodOrByFieldPropertyAccessor,
					IterableObjectHelper iterableObjectHelper,
					String collectionContextKey,
					String loopResultContextKey) {
				return create(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, collectionContextKey, loopResultContextKey, (ExecutorService)null);
			}

			public static <T> ForCollection.Async<T> create(
					PropertyAccessor byFieldOrByMethodPropertyAccessor,
					PropertyAccessor byMethodOrByFieldPropertyAccessor,
					IterableObjectHelper iterableObjectHelper,
					String collectionContextKey,
					String loopResultContextKey,
					Integer threadsNumber) {
				return create(byFieldOrByMethodPropertyAccessor, byMethodOrByFieldPropertyAccessor, iterableObjectHelper, collectionContextKey, loopResultContextKey, Executors.newFixedThreadPool(threadsNumber));
			}


			@Override
			@SuppressWarnings("unchecked")
			public void executeOn(Object object) {
				Context context = (Context)object;
				logDebug("Start executing functions group {}", getName());
				List<CompletableFuture<?>> completableFutureList = new CopyOnWriteArrayList<>();
				algorithmsSupplier.preLoopOperationsRetriever.accept(context);
				AtomicInteger counter = new AtomicInteger(0);
				algorithmsSupplier.iterableObjectStreamRetriever.apply(context).filter(item -> {
					final int idx = counter.getAndIncrement();
					Runnable executeOnItemFunction = () -> executeOnItem(context, (T) item, idx);
					CompletableFuture<?> completableFuture = (executor != null ?
						CompletableFuture.runAsync(executeOnItemFunction, executor):
						CompletableFuture.runAsync(executeOnItemFunction)
					).exceptionally(getExceptionHandlingFunction(context));
					completableFutureList.add(completableFuture);
					completableFuture.thenAcceptAsync(function -> completableFutureList.remove(completableFuture));
					return context.containsOneOf(getName(), Directive.Functions.ForCollection.STOP_ITERATION);
				}).findFirst().ifPresent(functionWrapper -> {
					context.removeDirective(getName(), Directive.Functions.ForCollection.STOP_ITERATION);
					logDebug("Stopping iteration of functions group {}", Optional.ofNullable(getName()).orElse(""));
				});
				algorithmsSupplier.postLoopOperationsRetriever.accept(context);
				CompletableFuture.allOf(
					completableFutureList.stream().toArray(CompletableFuture<?>[]::new)
				).join();
				completableFutureList.clear();
				logDebug("End executing functions group {}", getName());
			}

			@Override
			public void close() {
				if (executor != null && !executor.isShutdown()) {
					executor.shutdownNow().clear();
					executor = null;
				}
				super.close();
			}
		}
	}


	@Override
	public void close() {
		if (elements != null) {
			for (CommandWrapper<?, ?, Context, Context> functionWrapper : elements) {
				if (functionWrapper.getTarget() instanceof Functions) {
					((Functions)functionWrapper.getTarget()).close();
				}
				functionWrapper.close();
			}
		}
		if (onException != null) {
			onException.clear();
			onException = null;
		}
		super.close();
	}
}