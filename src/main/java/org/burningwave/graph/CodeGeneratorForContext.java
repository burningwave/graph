package org.burningwave.graph;

import org.burningwave.core.classes.CodeGenerator;
import org.burningwave.core.classes.MemberFinder;
import org.burningwave.core.common.Strings;
import org.burningwave.core.io.StreamHelper;

public class CodeGeneratorForContext extends CodeGenerator.ForPojo {
	private CodeGeneratorForContext(
			MemberFinder memberFinder,
			StreamHelper streamHelper) {
		super(memberFinder, streamHelper);
	}
	
	public static CodeGeneratorForContext create(
			MemberFinder memberFinder,
			StreamHelper streamHelper) {
		return new CodeGeneratorForContext(memberFinder, streamHelper);
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
