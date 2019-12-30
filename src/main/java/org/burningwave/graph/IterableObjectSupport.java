package org.burningwave.graph;

public interface IterableObjectSupport {
	
	public Integer getCurrentIterationIndex();
	
	public <T> T getCurrentIteratedObject();
	
	public void setCurrentIterationResult(Object obj);

	public <T> T getCurrentIterationResult();
	
}
