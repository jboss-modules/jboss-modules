package org.jboss.modules.xml;

import static org.junit.Assert.*;

import org.jboss.modules.util.ModulesTestBase;
import org.junit.Before;
import org.junit.Test;

public class PropertyExpanderTestCase extends ModulesTestBase{
    private static final String FILE_SEPATATOR = System.getProperty("file.separator");
    private static final String SOME_PROPERTY = "some.property";
    private static final String SOME_PROPERTY_VALUE = "someProperty";
    private static final String OTHER_PROPERTY = "other.property";
    private static final String OTHER_PROPERTY_VALUE ="otherProperty";
    private static final String JBOSS_HOME = "jboss.home.dir";
    private static final String JBOSS_HOME_VALUE = "/Users/home/jshepher/eap/jboss-eap-7";

    @Before
    public void setup() throws Exception{
        super.setUp();
        System.setProperty(SOME_PROPERTY, SOME_PROPERTY_VALUE);
        System.setProperty(OTHER_PROPERTY, OTHER_PROPERTY_VALUE);
        System.setProperty(JBOSS_HOME, JBOSS_HOME_VALUE);
    }
    @Test
    public void systemProp(){
        String result = PolicyExpander.expand(String.format("${%s}", SOME_PROPERTY));
        assertEquals(SOME_PROPERTY_VALUE, result);
    }
    @Test
    public void multiSystemProp(){
        String result = PolicyExpander.expand(String.format("${%s}${%s}", SOME_PROPERTY, OTHER_PROPERTY));
        assertEquals(String.format("%s%s", SOME_PROPERTY_VALUE, OTHER_PROPERTY_VALUE), result);
    }
    @Test
    public void multiSystemPropWithNonSpecial(){
        String result = PolicyExpander.expand(String.format("${%s}abc${%s}", SOME_PROPERTY, OTHER_PROPERTY));
        assertEquals(String.format("%sabc%s", SOME_PROPERTY_VALUE, OTHER_PROPERTY_VALUE), result);
    }
    @Test
    public void multiSystemPropWithSpecial(){
        String result = PolicyExpander.expand(String.format("${%s}a$bc${%s}", SOME_PROPERTY, OTHER_PROPERTY));
        assertEquals(String.format("%sa$bc%s", SOME_PROPERTY_VALUE, OTHER_PROPERTY_VALUE), result);
    }
    @Test
    public void fileSeperator(){
        String result = PolicyExpander.expand("${/}");
        assertEquals(FILE_SEPATATOR, result);
    }
    @Test
    public void fileSeperatorInExpression(){
        String result = PolicyExpander.expand("${/abc}");
        if(result != null)
            System.out.println(result);
        assertNull(result);
    }
    @Test
    public void propAndFileSeperator(){
        String result = PolicyExpander.expand(String.format("${%s}${/}abc${/}", SOME_PROPERTY));
        assertEquals(String.format("%s%sabc%s", SOME_PROPERTY_VALUE, FILE_SEPATATOR, FILE_SEPATATOR), result);
    }

    @Test
    public void propAndPath(){
        String result = PolicyExpander.expand(String.format("${%s}/standalone/configuration/logging.properties", JBOSS_HOME));
        assertEquals(String.format("%s/standalone/configuration/logging.properties", JBOSS_HOME_VALUE), result);
    }
}
