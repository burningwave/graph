package org.burningwave.graph;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.burningwave.core.Component;
import org.burningwave.core.concurrent.Mutex;

public interface Context extends 
	Component, 
	IterableObjectSupport, 
	ControllableContext,
	ListenableContext,
	Clearable, 
	Serializable {
	
	static enum Operation {
		PUT, REMOVE
	}
	
	public <T> T get(Object key);
	
	public Context removeAll(Object... keys);

	public <K, V> Context put(K key, V value);
	
	public Context putAll(Map<?, ?> inputContainer);

	public Context putAll(Context input);

	public void close();
	
	public Context createSymmetricClone();
	
	static class IterationContext<T> implements Serializable {

		private static final long serialVersionUID = 9013466754089134552L;
		
		private T iterableObject;
		private Object[] loopResult;
		private Object currentIteratedObject;
		private Integer index;
		private Object currentIteratedObjectKey;
		
		private IterationContext(T iterableObject, Object[] loopResult, Object input, Integer index, Object key){
			this.iterableObject = iterableObject;
			this.currentIteratedObject = input;
			this.index = index;
			this.loopResult = loopResult;
			this.currentIteratedObjectKey = key;
		}
		
		static <T> IterationContext<T> create(T iterableObject, Object[] loopResult, Object input, Integer index, Object key) {
			return new IterationContext<T>(iterableObject, loopResult, input, index, key);
		}
		
		Object getCurrentIteratedObject() {
			return currentIteratedObject;
		}
		void setCurrentIteratedObject(Object input) {
			this.currentIteratedObject = input;
		}
		
		Object getCurrentIterationResult() {
			return loopResult[index];
		}
		void setCurrentIterationResult(Object output) {
			this.loopResult[index] = output;
		}
		T getIterableObject() {
			return iterableObject;
		}
		void setIterableObject(T iterableObject) {
			this.iterableObject = iterableObject;
		}
		Object[] getLoopResult() {
			return loopResult;
		}
		void setLoopResult(Object[] loopResult) {
			this.loopResult = loopResult;
		}

		void setIndex(Integer index) {
			this.index = index;			
		}
		Integer getIndex() {
			return this.index;
		}

		@SuppressWarnings("unchecked")
		<K> K getKey() {
			return (K)currentIteratedObjectKey;
		}

		void setKey(Object key) {
			this.currentIteratedObjectKey = key;
		}
	}


	static abstract class Abst implements Context {
		private static final long serialVersionUID = 8260204603417876527L;
		
		protected Map<Object, Object> container;
		protected Map<String, Directive> executionDirectiveForGroupName;
		protected IterationContext<Object> iterationContext;
		protected Mutex.Manager.ForMap<Operation, Object, Object> mutexManager;

		Abst() {
			container = new ConcurrentHashMap<Object, Object>() {
				private static final long serialVersionUID = -4473137080512706444L;

				@Override
				public Object put(Object key, Object value) {
					Object val = null;
					if (value != null) {
						val = super.put(key, value);
					} else {
						val = super.remove(key, value);
					}
					mutexManager.unlockMutexes(Operation.PUT, key, value);
					return val;
				}
				
				@Override
				public Object remove(Object key) {
					Object value = super.remove(key);
					mutexManager.unlockMutexes(Operation.REMOVE, key, value);
					return value;
				}
				
			};
			executionDirectiveForGroupName = new ConcurrentHashMap<String, Directive>();
			mutexManager = Mutex.Manager.ForMap.create(this::get);

		}
		
		
		public Abst(Map<Object, Object> container, Map<String, Directive> executionDirectiveForGroupName,
				Mutex.Manager.ForMap<Operation, Object, Object> mutexManager) {
			super();
			this.container = container;
			this.executionDirectiveForGroupName = executionDirectiveForGroupName;
			this.mutexManager = mutexManager;
		}


		abstract Context putAllDirectives(Map<String, Directive> directives);	
		
		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Object key) {
			try {
				return (T)container.get(key);
			} catch (NullPointerException exc) {}
			return null;
		}		
		
		@SuppressWarnings("unchecked")
		<T> IterationContext<T> removeIterationContext() {
			IterationContext<T> itrCnt = (IterationContext<T>)iterationContext;
			setCurrentIterationContext(null);
			return itrCnt;
		}
		
		@SuppressWarnings("unchecked")
		void setCurrentIterationContext(IterationContext<?> itrCnt) {
			iterationContext = (IterationContext<Object>)itrCnt;
		}
		
		@SuppressWarnings("unchecked")
		<T> IterationContext<T> getCurrentIteratedContainer() {
			return (IterationContext<T>)iterationContext;
		}
		
		public boolean containsOneOf(String name, Directive... directives) {
			return 
				Stream.of(directives).filter((directive) -> 
					executionDirectiveForGroupName.get(name) == directive
				).findFirst().isPresent();
		}	
		
		void setCurrentIterationObjects(
				Object iterableObject,
				Object[] loopResult,
				Object currentIterationObject,
				Integer index,
				Object key) {
			Optional.of(
				Optional.ofNullable(iterationContext).orElseGet(() -> 
					this.iterationContext = IterationContext.create(iterableObject, loopResult, currentIterationObject, index, key)
				)
			).ifPresent(itrCnt -> {
				itrCnt.setIterableObject(iterableObject);
				itrCnt.setLoopResult(loopResult);
				itrCnt.setCurrentIteratedObject(currentIterationObject);
				itrCnt.setIndex(index);
				itrCnt.setKey(key);
			});
		}
		
		@Override
		public Integer getCurrentIterationIndex() {
			return Optional.ofNullable(iterationContext)
					.map((iterationContext) -> iterationContext.getIndex()).orElse(null);
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCurrentIteratedObject() {
			return (T)Optional.ofNullable(iterationContext)
					.map((iterationContext) -> iterationContext.getCurrentIteratedObject()).orElse(null);
		}

		@Override
		public void setCurrentIterationResult(Object obj) {
			Objects.requireNonNull(
				Objects.requireNonNull(
					iterationContext,
					"setCurrentIterationResult calling failed cause " + getClass() + " not contains currentIterationContext"
				),
				"setCurrentIterationResult calling failed cause " + getClass() + " not contains currentIterationContext.loopResult"
			).setCurrentIterationResult(obj);
		}
			

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCurrentIterationResult() {
			return (T)Optional.ofNullable(iterationContext)
				.map((iterationContext) -> iterationContext.getCurrentIterationResult()).orElse(null);
		}
		
		Context.Abst cast(Context context) {
			return ((Context.Abst)context);
		}

		
		@Override
		public void clear() {
			clearContainer();
			mutexManager.clearMutexes();
			executionDirectiveForGroupName.clear();
			iterationContext = null;
		}
	

		<K, V> void clearContainer() {
			container.forEach((key, value) ->
				mutexManager.unlockMutexes(Operation.REMOVE, key, container.remove(key))
			);
		}
		
		@Override
		public void close() {
			clear();
			container = null;
			executionDirectiveForGroupName = null;
			mutexManager.close();
			mutexManager = null;
		}
	}
	
	static class Simple extends Abst  {

		private static final long serialVersionUID = -7459443347382714306L;
		protected Context parent;
		
		
		protected Simple(Map<Object, Object> container, Map<String, Directive> executionDirectiveForGroupName,
				Mutex.Manager.ForMap<Operation, Object, Object> mutexManager) {
			super(container, executionDirectiveForGroupName, mutexManager);
		}

		protected Simple() {
			super();
		}

		public static Context create() {
			return new Simple();
		}
		
		@Override
		public <K, V> Context put(K key, V value) {
			container.put(key, value);
			return this;
		}
		
		@Override
		public Context removeAll(Object... keys) {
			if (keys != null && keys.length > 0) {
				for (Object key : keys) {
					container.remove(key);
				}
			} else {
				clearContainer();
			}
			return this;
		}
		
		@Override
		public Context putAll(Map<?, ?> inputContainer) {
			if (container != inputContainer) {
				inputContainer.forEach((key, value) -> put(key, value));
			}
			return this;
		}

		@Override
		public Context putDirective(String groupName, Directive directive) {
			executionDirectiveForGroupName.put(groupName, directive);
			return this;
		}

		@Override
		public Context removeDirective(String groupName, Directive directive) {
			executionDirectiveForGroupName.remove(groupName, directive);	
			return this;
		}

		@Override
		public Context putAll(Context input) {
			putAll(cast(input).container);
			putAllDirectives((cast(input)).executionDirectiveForGroupName);
			return this;
		}

		@Override
		public Context createSymmetricClone() {
			Simple context = new Simple(container, executionDirectiveForGroupName, mutexManager);
			context.parent = this;
			return context;
		}

		@Override
		Context putAllDirectives(Map<String, Directive> directives) {
			if (executionDirectiveForGroupName != directives) {
				executionDirectiveForGroupName.putAll(directives);
			}
			return this;
		}
		
		@Override
		public void close() {
			if (parent == null) {
				super.close();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <V> V waitForPut(Object key, Predicate<V> predicate, int... timeout) throws InterruptedException {
			return (V)mutexManager.waitFor(Operation.PUT, (Object)key, (Predicate<Object>)predicate);
		}


		@Override
		@SuppressWarnings("unchecked")
		public <V> V waitForRemove(Object key, Predicate<V> predicate, int... timeout) throws InterruptedException {
			return (V)mutexManager.waitFor(Operation.REMOVE, (Object)key, (Predicate<Object>)predicate);
		}
	}
}