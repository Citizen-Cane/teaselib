package teaselib.test;

import org.junit.Ignore;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import teaselib.core.ShowChoicesTest;
import teaselib.core.ShowChoicesTestErrorHandling;
import teaselib.core.ShowChoicesTestScriptFunctionReply;
import teaselib.core.ShowChoicesTestThrowScriptInterruptedException;

@Ignore
@RunWith(Categories.class)
@Categories.IncludeCategory({ IntegrationTests.class })
@Suite.SuiteClasses({ ShowChoicesTest.class, ShowChoicesTestErrorHandling.class,
        ShowChoicesTestScriptFunctionReply.class, ShowChoicesTestThrowScriptInterruptedException.class })
public final class IntegrationTests {
    //
}
