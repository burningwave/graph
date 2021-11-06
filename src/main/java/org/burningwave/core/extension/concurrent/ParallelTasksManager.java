/*
 * This file is part of Burningwave Graph.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/core
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Roberto Gentili
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
package org.burningwave.core.extension.concurrent;

import static org.burningwave.core.assembler.StaticComponentContainer.ManagedLoggersRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.burningwave.core.Component;
import org.burningwave.core.function.ThrowingRunnable;

public class ParallelTasksManager implements Component {

	protected Collection<CompletableFuture<Void>> tasks;
	protected ExecutorService executorService;
	private int maxParallelTasks;

	private ParallelTasksManager(int maxParallelTasks) {
		tasks = new CopyOnWriteArrayList<>();
		this.maxParallelTasks = maxParallelTasks;
	}

	public static ParallelTasksManager create(int maxParallelTasks) {
		return new ParallelTasksManager(maxParallelTasks);
	}

	public static ParallelTasksManager create() {
		return new ParallelTasksManager(Runtime.getRuntime().availableProcessors());
	}

	public void execute(ThrowingRunnable<Throwable> task) {
		if (executorService == null) {
			this.executorService = Executors.newFixedThreadPool(maxParallelTasks);
		}
		tasks.add(CompletableFuture.runAsync(() -> {
			try {
				task.run();
			} catch (Throwable exc) {
				ManagedLoggersRepository.logError(getClass()::getName, "Exception occurred", exc);
			}
		}, executorService));
	}

	public void waitForTasksEnding() {
		Iterator<CompletableFuture<Void>> iterator = tasks.iterator();
		Collection<CompletableFuture<Void>> tasks = new ArrayList<>();
		while (iterator.hasNext()) {
			CompletableFuture<Void> task= iterator.next();
			task.join();
			tasks.add(task);
		}
		tasks.removeAll(tasks);
	}

	@Override
	public void close() {
		waitForTasksEnding();
		tasks.clear();
		tasks = null;
		if (executorService != null) {
			executorService.shutdown();
			executorService = null;
		}
	}
}