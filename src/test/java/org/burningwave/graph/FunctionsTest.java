package org.burningwave.graph;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.burningwave.core.ManagedLogger;
import org.burningwave.graph.bean.Person;
import org.burningwave.graph.service.ServiceOne;
import org.burningwave.graph.service.ServiceTwo;
import org.junit.jupiter.api.Test;


public class FunctionsTest implements ManagedLogger {

	@Test
	public void testOne() {
		try {
			logInfo(new Object() {}.getClass().getEnclosingMethod().getName() + " started");
			Map<String, Object> objs = new LinkedHashMap<>();
			objs.put("sharedService", new ServiceOne());

			List<Object> list = new ArrayList<>();

			int inputCollectionSize = 100000;
			for (int i = 0; i < inputCollectionSize; i++) {
				list.add(new Object());
			}
			Config graphConfig = Config.Factory.getInstance().build(
				"graphConfig/FunctionsTestChainConfig.graph"
			);
			Factory factory = Factory.getInstance();
			Functions functions = factory.build(
				graphConfig, objs
			);

			ServiceOne.Context data = factory.createContext(ServiceOne.Context.class);
			data.setInputCollection(list);

			functions.executeOn(data);
			factory.close(functions);
			factory.close(data);
			logInfo(new Object() {}.getClass().getEnclosingMethod().getName() + " succesfully completed");
		} catch (Throwable exc) {
			logError("", exc);
		}
	}


	@Test
	public void iterableFunctionsTest() {
		try {
			logInfo(new Object() {}.getClass().getEnclosingMethod().getName() + " started");
			Map<String, Object> services = new LinkedHashMap<>();
			services.put("service", new ServiceTwo());

			Config graphConfig = Config.Factory.getInstance().build(
				"graphConfig/IterableFunctionsTestChainConfig.graph"
			);
			Factory factory = Factory.getInstance();
			Functions functions = factory.build(
				graphConfig, services
			);

			Context data = factory.createContext();
			functions.executeOn(data);
			List<Person> persons = data.get("persons");
			factory.close(functions);
			assertEquals(ServiceTwo.PERSONS_COLLECTION_SIZE, persons.size());
			logInfo(new Object() {}.getClass().getEnclosingMethod().getName() + " succesfully completed");
		} catch (Throwable exc) {
			logError("Exception occurred", exc);
		}
	}

	@Test
	public void nestedIterableFunctionsTest() {
		try {
			logInfo(new Object() {}.getClass().getEnclosingMethod().getName() + " started");
			Map<String, Object> services = new LinkedHashMap<>();
			services.put("service", new ServiceTwo());

			Config graphConfig = Config.Factory.getInstance().build(
				"graphConfig/NestedIterableFunctionsTestChainConfig.graph"
			);
			Factory factory = Factory.getInstance();
			Functions functions = factory.build(
				graphConfig, services
			);

			Context data = factory.createContext();
			functions.executeOn(data);
			int total = data.get("total");
			factory.close(functions);
			assertEquals(ServiceTwo.PERSONS_COLLECTION_SIZE * ServiceTwo.PERSONS_COLLECTIONS_NUMBER, total);
			logInfo(new Object() {}.getClass().getEnclosingMethod().getName() + " succesfully completed");
		} catch (Throwable exc) {
			logError("Exception occurred", exc);
		}
	}
}
