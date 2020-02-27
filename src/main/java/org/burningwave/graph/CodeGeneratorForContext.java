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

import static org.burningwave.core.assembler.StaticComponentsContainer.Strings;

import org.burningwave.core.classes.CodeGenerator;
import org.burningwave.core.io.StreamHelper;

public class CodeGeneratorForContext extends CodeGenerator.ForPojo {
	private CodeGeneratorForContext(
		StreamHelper streamHelper
	) {
		super(streamHelper);
	}
	
	public static CodeGeneratorForContext create(
		StreamHelper streamHelper
	) {
		return new CodeGeneratorForContext(streamHelper);
	}
	
	
	@Override
	protected String generateConstructors(String packageName, String classSimpleName, Class<?> superClass) {
		String members = super.generateConstructors(packageName, classSimpleName, superClass);
		members += generateCreateMethods(members);
		members += generateCloneMethods(classSimpleName.substring(classSimpleName.lastIndexOf(".") + 1, classSimpleName.length()));
		return members;
	}

	
	@Override
	protected String generateGetter(String methodAsString) {
		return "\tpublic " + Strings.extractAllGroups(METHOD_RETURN_PATTERN, methodAsString).get(1).get(0) + " "
				+ Strings.extractAllGroups(METHOD_NAME_AND_INPUT_PATTERN_WITHOUT_LAST_BRACKET, methodAsString).get(1).get(0) + ") {\n"
				+ "\t\treturn (" + Strings.extractAllGroups(METHOD_RETURN_PATTERN, methodAsString).get(1).get(0) + ")get(\""
				+ Strings.lowerCaseFirstCharacter(Strings.extractAllGroups(PROPERTY_NAME_PATTERN_FOR_GET, methodAsString).get(1).get(0)) + "\");\n" + "\t}\n\n";
	}
	
	@Override
	protected String generateChecker(String methodAsString) {
		return "\tpublic " + Strings.extractAllGroups(METHOD_RETURN_PATTERN, methodAsString).get(1).get(0) + " "
				+ Strings.extractAllGroups(METHOD_NAME_AND_INPUT_PATTERN_WITHOUT_LAST_BRACKET, methodAsString).get(1).get(0) + ") {\n"
				+ "\t\treturn (" + Strings.extractAllGroups(METHOD_RETURN_PATTERN, methodAsString).get(1).get(0) + ")get(\""
				+ Strings.lowerCaseFirstCharacter(Strings.extractAllGroups(PROPERTY_NAME_PATTERN_FOR_IS, methodAsString).get(1).get(0)) + "\");\n" + "\t}\n\n";
	}
	@Override
	protected Object generateSetter(String methodAsString) {
		return "\tpublic " + Strings.extractAllGroups(METHOD_RETURN_PATTERN, methodAsString).get(1).get(0) + " "
				+ Strings.extractAllGroups(METHOD_NAME_AND_INPUT_PATTERN_WITHOUT_LAST_BRACKET, methodAsString).get(1).get(0) + " value) {\n"
				+ "\t\tput(\"" + Strings.lowerCaseFirstCharacter(Strings.extractAllGroups(PROPERTY_NAME_PATTERN_FOR_SET, methodAsString).get(1).get(0)) + "\", value" + ");\n"
				+ "\t}\n\n";
	}
	
	private String generateCloneMethods(String classSimpleName) {
		return 
			"\t@Override\n" + 
			"\tpublic " + classSimpleName +" createSymmetricClone() {\n" + 
				"\t\t" + classSimpleName + " data = new " + classSimpleName + "(container, executionDirectiveForGroupName, mutexManager);\n" + 
				"\t\tdata.parent = this;\n" + 
				"\t\treturn data;\n" + 
			"\t}\n\n";
	}

}
