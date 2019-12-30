package org.burningwave.graph;

import java.util.function.Predicate;

public interface ListenableContext {

	<V> V waitForPut(Object key, Predicate<V> predicate, int... timeout) throws InterruptedException;

	<V> V waitForRemove(Object key, Predicate<V> predicate, int... timeout) throws InterruptedException;

}