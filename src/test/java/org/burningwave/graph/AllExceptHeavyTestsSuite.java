package org.burningwave.graph;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@SelectPackages("org.burningwave.graph")
@ExcludeTags("Heavy")
public class AllExceptHeavyTestsSuite {

}
