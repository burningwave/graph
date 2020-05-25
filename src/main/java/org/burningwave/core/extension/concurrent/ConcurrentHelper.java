/*
 * This file is part of Burningwave Core.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.burningwave.core.Component;

public class ConcurrentHelper implements Component {
	
	private ConcurrentHelper() {}
	
	public static ConcurrentHelper create() {
		return new ConcurrentHelper();
	}
	
	protected void joinAll(CompletableFuture<?>... completableFutures) {
		for (int i = 0; i < completableFutures.length; i++) {
			if (completableFutures[i] != null) {
				completableFutures[i].join();
			}
		}
	}

	public boolean removeAllTerminated(Collection<CompletableFuture<?>> completableFutureList) {
		Iterator<CompletableFuture<?>> itr = completableFutureList.iterator();
		boolean removed = false;
		while (itr.hasNext()) {
			CompletableFuture<?> cF = itr.next();
			if (cF.isDone() || cF.isCancelled() || cF.isCompletedExceptionally()) {
				completableFutureList.remove(cF);
				removed = true;
			}
		}
		return removed;
	}
	
	
	public void waitFor(long interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException exc) {
			logError("Exception occurred", exc);
		}
	}
	
}
