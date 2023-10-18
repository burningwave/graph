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
 * Copyright (c) 2019-2023 Roberto Gentili
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
import static org.burningwave.core.assembler.StaticComponentContainer.Strings;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.burningwave.core.Component;
import org.burningwave.core.Strings;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.io.PathHelper;
import org.burningwave.graph.Config.Constraint.Violation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Config implements Serializable {

	private static final long serialVersionUID = -1420680417555794733L;

	private String name;
	private boolean async;
	private String threadsNumber;
	private OnException[] onException;
	private String iterableObject;
	private String loopResult;
	private String method;
	private Config parent;
	private Config[] functions;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isAsync() {
		return async;
	}
	public void setAsync(boolean async) {
		this.async = async;
	}
	public String getThreadsNumber() {
		return threadsNumber;
	}
	public Integer getThreadsNumberAsInteger() {
		return Optional.ofNullable(threadsNumber).map((thNum) -> Integer.valueOf(thNum)).orElse(null);
	}
	public void setThreadsNumber(String threadsNumber) {
		this.threadsNumber = threadsNumber;
	}

	public OnException[] getOnException() {
		return onException;
	}
	public void setOnException(OnException[] onException) {
		this.onException = onException;
	}
	public String getIterableObject() {
		return iterableObject;
	}
	public void setIterableObject(String iterableObject) {
		this.iterableObject = iterableObject;
	}
	public String getLoopResult() {
		return loopResult;
	}
	public void setLoopResult(String loopResult) {
		this.loopResult = loopResult;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Config getParent() {
		return parent;
	}
	public Config[] getFunctions() {
		return functions;
	}
	public void setFunctions(Config[] functions) {
		this.functions = functions;
		for (Config config : functions) {
			config.parent = this;
		}
	}

	public static class OnException implements Serializable  {
		private static final long serialVersionUID = -1638659620384801346L;

		private String directive;
		private String[] targets;
		public String getDirective() {
			return directive;
		}
		public void setDirective(String directive) {
			this.directive = directive;
		}
		public String[] getTargets() {
			return targets;
		}
		public void setTargets(String[] targets) {
			this.targets = targets;
		}
	}

	public static class Factory implements Component {
		private Config.Validator configValidator;
		private ObjectMapper objectMapper;
		private PathHelper pathHelper;

		private Factory(PathHelper pathHelper, Validator configValidator, ObjectMapper objectMapper) {
			this.configValidator = configValidator;
			this.pathHelper =  pathHelper;
			this.objectMapper = objectMapper;
		}


		public static Factory create(Strings stringHelper, PathHelper pathHelper, Validator configValidator, ObjectMapper objectMapper) {
			return new Factory(pathHelper, configValidator, objectMapper);
		}

		public static Factory getInstance() {
			ComponentSupplier componentSupplier = ComponentSupplier.getInstance();
			return componentSupplier.getOrCreate(Factory.class, () ->
				new Factory(
					componentSupplier.getPathHelper(),
					componentSupplier.getOrCreate(Validator.class, () ->
						Validator.create()
					),
					new ObjectMapper()
				)
			);
		}

		public Config build(String fileName) throws JsonParseException, JsonMappingException, IOException {
			Config config = objectMapper.readValue(
				Files.readAllBytes(Paths.get(pathHelper.getAbsolutePathOfResource(fileName))),
				Config.class
			);
			adjustFields(config);
			List<Constraint.Violation> violations = configValidator.validate(config);
			if (!violations.isEmpty()) {
				StringBuffer messages = new StringBuffer();
				violations.forEach(violation ->
					messages.append(violation.getMessage() + "\n")
				);
				Driver.throwException(messages.toString());
			}
			return config;
		}

		void adjustFields(Config config) {
			if (Strings.isNotEmpty(config.getIterableObject())) {
				if (config.getIterableObject().contains("currentIteratedObject")) {
					config.setIterableObject(config.getIterableObject().replace("context.", ""));
				}
				if (config.getIterableObject().contains("context[")) {
					config.setIterableObject(config.getIterableObject().replace("context[", "container["));
				}
			}
			if (Strings.isNotEmpty(config.getLoopResult())) {
				if (config.getLoopResult().contains("currentIterationResult")) {
					config.setLoopResult(config.getLoopResult().replace("context.", ""));
				}
				if (config.getLoopResult().contains("context[")) {
					config.setLoopResult(config.getLoopResult().replace("context[", "container["));
				}
			}
			if (config.isAsync() && isThreadNumberAutoSet(config)) {
				config.setThreadsNumber(
					Integer.toString(computeThreadsNumber(config))
				);
			}
			if (config.getFunctions() != null) {
				Stream.of(config.getFunctions()).forEach(conf -> {
					adjustFields(conf);
				});
			}
		}

		private Integer computeThreadsNumber(Config config) {
			Config parent = config.getParent();
			if (parent != null && parent.isAsync() && Strings.isNotEmpty(parent.getIterableObject())) {
				return (parent.getThreadsNumberAsInteger() * config.getFunctions().length) + 1;
			}
			if (config.isAsync() && Strings.isNotEmpty(config.getIterableObject()) && isThreadNumberAutoSet(config)) {
				return Runtime.getRuntime().availableProcessors();
			}
			return config.getFunctions().length;
		}

		private boolean isThreadNumberAutoSet(Config config) {
			return Strings.isEmpty(config.getThreadsNumber()) || "auto".equalsIgnoreCase(config.getThreadsNumber());
		}
	}

	static class Validator implements Component {
		private Validator() {
		}

		static Validator create() {
			return new Validator();
		}

		public List<Violation> validate(Config config){
			List<Constraint.Violation> constraintViolations = (check(config));
			if (config.getFunctions() != null && config.getFunctions().length > 0) {
				Stream.of(config.getFunctions()).forEach(confChild ->
					constraintViolations.addAll(validate(confChild))
				);
			}
			return constraintViolations;
		}

		private List<Violation> check(Config config) {
			List<Constraint.Violation> constraintViolations = new ArrayList<>();
			if (!config.isAsync() && Strings.isNotEmpty(config.getThreadsNumber())) {
				constraintViolations.add(
					Violation.create("\"threadsNumber\" property must be null when \"async\" property is true (" + config.getName() + ")")
				);
			}
			if (Strings.isEmpty(config.getIterableObject()) && Strings.isNotEmpty(config.getLoopResult())) {
				constraintViolations.add(
					Violation.create("\"inputCollection\" property must be valorized when \"loopResult\" property is valorized (" + config.getName() + ")")
				);
			}
			if (Strings.isNotEmpty(config.getMethod()) && config.getFunctions() != null) {
				constraintViolations.add(
					Violation.create("\"functions\" array property must be null when \"method\" property is valorized " + config.getName() + ")")
				);
			}
			if (Strings.isNotEmpty(config.getMethod()) && config.isAsync()) {
				constraintViolations.add(
					Violation.create("\"async\" array property must be false or null when \"method\" property is valorized " + config.getName() + ")")
				);
			}
			return constraintViolations;
		}

	}

	static class Constraint {

		static class Violation {
			private String message;

			private Violation(String message) {
				this.message = message;
			}

			static Violation create(String message) {
				return new Violation(message);
			}

			public String getMessage() {
				return message;
			}
		}

	}
}