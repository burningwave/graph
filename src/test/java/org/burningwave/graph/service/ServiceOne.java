package org.burningwave.graph.service;

import static org.burningwave.core.assembler.StaticComponentContainer.ManagedLoggersRepository;

import java.util.List;

import org.burningwave.core.ManagedLogger;
import org.burningwave.graph.IterableObjectSupport;
import org.burningwave.graph.ListenableContext;

public class ServiceOne implements ManagedLogger {

	public static int loadCounter = 1;
	public static int updateCounter = 1;


	public Context load(Context ctx) {
		Object obj = ctx.getCurrentIteratedObject();
		int idx = ctx.getInputCollection().indexOf(obj);
		if (idx == 30000) {
			ctx.setName("Sam");
		}
		idx = -1;
		if (obj != null) {
			try {
				idx = ctx.getInputCollection().indexOf(obj);
			} catch (NullPointerException exc) {
				exc.printStackTrace();
			}
		}
		logInfo("LOAD object at index {} {}", idx, ctx.getCurrentIteratedObject());
		return ctx;
	}


	public static Context staticLoad(Context ctx) {
		Object obj = ctx.getCurrentIteratedObject();
		int idx = -1;
		if (obj != null) {
			idx = ctx.getInputCollection().indexOf(obj);
		}
		ManagedLoggersRepository.logInfo(() -> ServiceOne.class.getName(), "LOAD object at index {} {}", idx, ctx.getCurrentIteratedObject());
		return ctx;
	}


	public void load() {
		logInfo("void load entered");
	}


	public void update(Context ctx) throws Exception {
		Object obj = ctx.getCurrentIteratedObject();
		int idx = -1;
		if (obj != null) {
			idx = ctx.getInputCollection().indexOf(obj);
			int number = (Math.random() <= 0.5) ? 1 : 2;
			if (number == 2) {
				//Thread.sleep(20);
				ctx.setCurrentIterationResult(obj);
			}
			if (idx == 12345) {
				obj = null;
				//ctx.putDirective("thirdLevelGroup[2]", Directive.Functions.STOP_PROCESSING);
				//obj.getClass();
			}

		}
		logInfo("UPDATE object at index {}, result: {}", idx, ctx.getCurrentIterationResult());
	}

	public void waitFor(Context ctx) throws InterruptedException {
		//Attenzione perchè blocca l'esecuzione in caso non venga inserito l'elemento per cui si aspetta
		String sam = ctx.waitForPut("name", (value) -> value != null && value.equals("Sam"));
		logInfo("ended wait for " + sam);
		//TODO: aggiungere funzionalità di ascolto eventi
		ctx.setName(null);
	}

	public interface Context extends IterableObjectSupport, ListenableContext {
		public List<?> getInputCollection();

		public void setName(String string);

		public void setInputCollection(List<?> value);

		public boolean isValid();
	}

}