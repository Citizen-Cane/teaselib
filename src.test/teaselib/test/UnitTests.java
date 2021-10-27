package teaselib.test;

import org.junit.Ignore;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.junit.runners.Suite;

@Ignore
@RunWith(Categories.class)
@Categories.ExcludeCategory({ IntegrationTests.class })
@Suite.SuiteClasses({ AllTests.class })
public final class UnitTests {
    //
}
