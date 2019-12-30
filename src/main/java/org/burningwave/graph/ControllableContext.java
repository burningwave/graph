package org.burningwave.graph;

public interface ControllableContext {
	
	public boolean containsOneOf(String name, Directive... directives);
	
	public Context putDirective(String groupName, Directive directive);

	public Context removeDirective(String groupName, Directive directive);
		
	public interface Directive {
		public String getName();
		
		public enum Functions implements Directive {
			CONTINUE_PROCESSING,
			STOP_PROCESSING;

			@Override
			public String getName() {
				return this.name();
			}
			
			public enum ForCollection implements Directive {
				STOP_ITERATION();
				
				@Override
				public String getName() {
					return this.name();
				}
			}
		}
	}
}