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
package org.burningwave.graph;

import static org.burningwave.core.assembler.StaticComponentContainer.ByFieldOrByMethodPropertyAccessor;
import static org.burningwave.core.assembler.StaticComponentContainer.ByMethodOrByFieldPropertyAccessor;
import static org.burningwave.core.assembler.StaticComponentContainer.Classes;
import static org.burningwave.core.assembler.StaticComponentContainer.IterableObjectHelper;
import static org.burningwave.core.assembler.StaticComponentContainer.Members;
import static org.burningwave.core.assembler.StaticComponentContainer.Strings;
import static org.burningwave.core.assembler.StaticComponentContainer.Throwables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.burningwave.core.Component;
import org.burningwave.core.Virtual;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ClassFactory;
import org.burningwave.core.classes.ClassSourceGenerator;
import org.burningwave.core.classes.FunctionSourceGenerator;
import org.burningwave.core.classes.MethodCriteria;
import org.burningwave.core.classes.PojoSourceGenerator;
import org.burningwave.core.classes.TypeDeclarationSourceGenerator;
import org.burningwave.core.extension.CommandWrapper;
import org.burningwave.graph.ControllableContext.Directive;


public class Factory implements Component {
	ComponentSupplier componentSupplier;
	List<Functions> functionList;
	List<Context> contextList;
	
	private Factory(ComponentSupplier componentSupplier) {
		this.componentSupplier = componentSupplier;
		functionList = new CopyOnWriteArrayList<>();
		contextList = new CopyOnWriteArrayList<>();
	}
	
	public static Factory create(ComponentSupplier componentSupplier) {
		return new Factory(componentSupplier);
	}
	
	public static Factory getOrCreateFrom(ComponentSupplier componentSupplier) {
		return componentSupplier.getOrCreate(Factory.class, () -> new Factory(componentSupplier));
	}	
	
	public static Factory getInstance() {
		return getOrCreateFrom(ComponentSupplier.getInstance());
	}
	
	List<Directive> getAllDirectives() {
		List<Directive> directives = new ArrayList<Directive>();
		directives.addAll(
			Stream.of(Directive.Functions.values()).collect(Collectors.toList()));
		directives.addAll(
			Stream.of(Directive.Functions.ForCollection.values()).collect(Collectors.toList()));
		return directives;
	}			

	Map<String, Directive> getDirectives(Config.OnException[] onException) {
		Map<String, Directive> directives = new LinkedHashMap<>();
		if (onException != null) {					
			for (Config.OnException temp : onException) {
				for (Directive directive : getAllDirectives()) {
					if (directive.getName().equals(temp.getDirective())) {
						for (String target : temp.getTargets()) {
							directives.put(target, directive);
						}								
						break;
					}
				}
			}
		}
		return directives;
	}

	Object retrieveBean(Object beanContainer, String beanClassNameOrContextName)
			throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object instance = null;
		if (beanContainer instanceof Map) {
			beanClassNameOrContextName = beanClassNameOrContextName.split("#")[1];
			instance = ((Map<?, ?>) beanContainer).get(beanClassNameOrContextName);
		} else if (Class.forName("org.springframework.context.ApplicationContext").isInstance(beanContainer)) {
			instance = Members.findOne(
				MethodCriteria.forName(
					methodName -> methodName.matches("getBean")
				).and().returnType(
					returnType -> returnType == Object.class
				).and().parameterTypes(
					parameterTypes -> parameterTypes.length == 1
				).and().parameterType(
					(parameterTypes, idx) -> idx == 0 && parameterTypes[idx] == String.class
				),
				beanContainer
			).invoke(beanContainer, beanClassNameOrContextName.split("#")[1]);
		}
		
		return instance;
	}	

	Function<Config, Functions> createAsyncFunctionsForCollection() {
		return (config) -> Functions.ForCollection.Async.create(
			ByFieldOrByMethodPropertyAccessor,
			ByMethodOrByFieldPropertyAccessor,
			IterableObjectHelper,
			config.getIterableObject(),
			config.getLoopResult(),
			config.getThreadsNumberAsInteger()
		);
	}		

	Function<Config, Functions> createAsyncFunctionsForCollectionWithSystemManagedThreads() {
		return (config) -> Functions.ForCollection.Async.create(
			ByFieldOrByMethodPropertyAccessor,
			ByMethodOrByFieldPropertyAccessor,
			IterableObjectHelper,
			config.getIterableObject(), config.getLoopResult()
		);
	}
	

	Function<Config, Functions> createFunctionsForCollection() {
		return (config) -> Functions.ForCollection.create(
			ByFieldOrByMethodPropertyAccessor,
			ByMethodOrByFieldPropertyAccessor,
			IterableObjectHelper,
			config.getIterableObject(), config.getLoopResult());
	}		
	

	Function<Config, Functions> createAsyncFunctions() {
		return (config) -> Functions.Async.create(
			ByFieldOrByMethodPropertyAccessor,
			ByMethodOrByFieldPropertyAccessor,
			IterableObjectHelper,
			config.getThreadsNumberAsInteger()); 
	}
	

	java.util.function.Supplier<Functions> createAsyncFunctionsWithSystemManagedThreads() {
		return () -> Functions.Async.create(
			ByFieldOrByMethodPropertyAccessor,
			ByMethodOrByFieldPropertyAccessor,
			IterableObjectHelper
		); 
	}
	

	java.util.function.Supplier<Functions> createFunctions() {
		return () -> Functions.create(
			ByFieldOrByMethodPropertyAccessor,
			ByMethodOrByFieldPropertyAccessor,
			IterableObjectHelper
		); 
	}
	
	
	public Context createContext() {
		Context context = Context.Simple.create();
		contextList.add(context);
		return context;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T createContext(Class<?>... interfaces) {
		ClassFactory classFactory = componentSupplier.getClassFactory();
		String className =
			Factory.class.getPackage().getName() + "." + 
			Virtual.class.getSimpleName().toLowerCase() + "." +
			String.join("", Stream.of(interfaces).map(interf -> interf.getSimpleName()).toArray(String[]::new)) + "Impl";
		List<java.lang.Class<?>> classes = new ArrayList<>(Arrays.asList(interfaces));
		classes.add(Context.Simple.class);
		PojoSourceGenerator pojoSourceSG = PojoSourceGenerator.createDefault();
		Class<?> cls = classFactory.createPojoSubTypeRetriever(
			pojoSourceSG.setSetterMethodsBodyBuilder(
				(methodSG, method, options) -> {
					String paramName = Strings.lowerCaseFirstCharacter(method.getName().replaceFirst("set", ""));
					methodSG.addBodyCodeRow("put(\"" + paramName + "\", "+ paramName + ");");
				}
			).setGetterMethodsBodyBuilder(
				(methodSG, method, options) -> {
					String prefix = method.getName().startsWith("get")? "get" : "is";
					String paramName = Strings.lowerCaseFirstCharacter(method.getName().replaceFirst(prefix, ""));
					methodSG.addBodyCodeRow("return (" + 
						(pojoSourceSG.isUseFullyQualifiedClassNamesEnabled(options)?
							method.getReturnType().getName():
							method.getReturnType().getSimpleName()
						) + 
					")get(\"" + paramName + "\");");
				}
			).setExtraElementsBuilder(
				(unit, superClass, interfs, options) -> {
					ClassSourceGenerator classSource = unit.getClass(className);
					FunctionSourceGenerator createSimmetricCloneMethod =
						FunctionSourceGenerator.create("createSymmetricClone")
						.addModifier(Modifier.PUBLIC).setReturnType(TypeDeclarationSourceGenerator.create(Context.class.getName()))
						.addBodyCodeRow(Classes.retrieveSimpleName(className)).addBodyCode("data = new")
						.addBodyCode(Classes.retrieveSimpleName(className)).addBodyCode("(container, executionDirectiveForGroupName, mutexManager);")
						.addBodyCodeRow("data.parent = this;")
						.addBodyCodeRow("return data;").addOuterCodeRow("@Override");
					classSource.addMethod(createSimmetricCloneMethod);
				}
		).setFieldsBuilder(
			null
		)).loadOrBuildAndDefine(
			this.getClass().getClassLoader(),
			className, 
			(PojoSourceGenerator.BUILDING_METHODS_CREATION_ENABLED | PojoSourceGenerator.USE_OF_FULLY_QUALIFIED_CLASS_NAMES_ENABLED), 
			classes.toArray(new java.lang.Class<?>[classes.size()])
		);
		//Class<?> cls = classFactory.getOrBuild(codeGeneratorForContext.generate(className, Context.Simple.class, interfaces), this.getClass().getClassLoader());
		try {
			return (T)Members.findOne(
				MethodCriteria.on(cls).name(
					"create"::equals
				).and().parameterTypes(
					paramsType -> paramsType.length == 0
				),
				cls
			).invoke(null);
		} catch (Exception exc) {
			throw Throwables.toRuntimeException(exc);
		}
	}
	
	
	public Functions build(Config config, Object... beanContainers) throws Throwable {
		Functions functions = buildFunctions(config, beanContainers);
		functionList.add(functions);
		return functions;
	}
	
	private Functions buildFunctions(Config config, Object... beanContainers) throws Throwable {
		Functions functions = createMainFunctions(config);
		createChildren(config, functions, beanContainers);
		return functions;
	}

	private void createChildren(Config config, Functions functions, Object... beanContainers) throws Throwable {
		if (config.getFunctions() != null && config.getFunctions().length > 0) {
			for (Config innerConfig : config.getFunctions()) {
				Object instance = null;
				final AtomicReference<Class<?>> superClassWrapper = new AtomicReference<>(Object.class);
				final AtomicReference<String> methodNameWrapper = new AtomicReference<>();
				if (Strings.isNotEmpty(innerConfig.getMethod())) {
					String beanClassNameOrContextName = innerConfig.getMethod().split("::")[0];
					methodNameWrapper.set(innerConfig.getMethod().split("::")[1]);
					if ("new".equalsIgnoreCase(beanClassNameOrContextName.split("\\s+")[0])) {
						beanClassNameOrContextName = beanClassNameOrContextName.split("\\s+")[1];
						instance = Class.forName(beanClassNameOrContextName).getConstructor().newInstance();
					} else if (beanClassNameOrContextName.startsWith("#")) {
						for (Object beanContainer : beanContainers) {
							instance = retrieveBean(beanContainer, beanClassNameOrContextName);
							if (instance != null) {
								break;
							}
						}
					} else {
						instance = Class.forName(beanClassNameOrContextName);
					}
				} else if(Strings.isEmpty(innerConfig.getMethod())) {
					instance = buildFunctions(innerConfig, beanContainers);
					superClassWrapper.set(instance.getClass());
					methodNameWrapper.set(
						Strings.isNotEmpty(innerConfig.getMethod())?
							innerConfig.getMethod() : "executeOn");
				}
				Objects.requireNonNull(instance, "Object " + innerConfig.getMethod() + " not found");
				final String methodName = methodNameWrapper.get();
				Method mth = Optional.ofNullable(
					Members.findOne(
						MethodCriteria.byScanUpTo(c ->
							c.getName().equals(superClassWrapper.get().getName())
						).and().name(
							methodName::equals
						).and().parameterTypes((parameterTypes) ->
							parameterTypes.length == 1
						),
						instance
					)
				).orElse(
					Members.findOne(
						MethodCriteria.byScanUpTo(c ->
							c.getName().equals(superClassWrapper.get().getName())
						).and().name(
							methodName::equals
						).and().parameterTypes((parameterTypes) ->
							parameterTypes.length == 0
						),
						instance
					)
				);
				
				Object functionalInterface = componentSupplier.getFunctionalInterfaceFactory().create(
					Objects.requireNonNull(
						mth, "Could not bind function " + instance.getClass().getName() + "::" + mth.getName() + " to any Wrapper"
					)
				);
				functions.add(
					CommandWrapper.create(
						functionalInterface, instance
					)
				);
			}
		}
	}

	private Functions createMainFunctions(Config config) {
		Functions functions = null;
		if (config.isAsync() && Strings.isNotEmpty(config.getIterableObject())) {
			functions = 
				Optional.ofNullable(config.getThreadsNumberAsInteger()).map((threadsNumber) ->
					createAsyncFunctionsForCollection().apply(config)
				).orElseGet(() ->
					createAsyncFunctionsForCollectionWithSystemManagedThreads().apply(config)
				);	
		} else if (Strings.isNotEmpty(config.getIterableObject())) {
			functions = createFunctionsForCollection().apply(config);
		} else if (config.isAsync()) {
			functions = 
				Optional.ofNullable(config.getThreadsNumberAsInteger()).map((threadsNumber) -> 
					createAsyncFunctions().apply(config)
				).orElseGet(() ->
					createAsyncFunctionsWithSystemManagedThreads().get()
				);
		} else {
			functions = createFunctions().get();
		}
		functions.setName(config.getName());
		functions.setOnException(getDirectives(config.getOnException()));
		return functions;
	}
	
	public void close(Functions... functions) {
		for (Functions function : functions) {
			function.close();
			functionList.remove(function);
		}				
	}
	
	public void close(Object... contextes) {
		for (Object context : contextes) {
			((Context)context).close();
			contextList.remove(context);
		}				
	}
	
	public void close() {
		if (functionList != null) {
			for (Functions function : functionList) {
				close(function);
			}
			functionList.clear();
			functionList = null;
		}
		if (contextList != null) {
			for (Context context : contextList) {
				close(context);
			}
			contextList.clear();
			contextList = null;
		}
	}
}