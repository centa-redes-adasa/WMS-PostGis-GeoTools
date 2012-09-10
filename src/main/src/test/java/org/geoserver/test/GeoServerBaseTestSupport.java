package org.geoserver.test;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geoserver.data.test.TestData;
import org.geotools.factory.Hints;
import org.geotools.feature.NameImpl;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.opengis.feature.type.Name;

public abstract class GeoServerBaseTestSupport<T extends TestData> {

    /**
     * Common logger for test cases
     */
    protected static final Logger LOGGER = 
        org.geotools.util.logging.Logging.getLogger("org.geoserver.test"); 

    /**
     * test data
     */
    protected static TestData testData;

    /**
     * test instance, used to give subclass hooks for one time setup/teardown
     */
    protected static GeoServerBaseTestSupport test;

    protected static TestSetupPolicy testSetupPolicy = null;

//  @Rule
//  public TestWatcher watcher = new TestWatcher() {
//      protected void finished(org.junit.runner.Description description) {
//          System.out.println(description);
//      };
//  };

    @BeforeClass
    public static void setUpReferencing() throws Exception {
        // do we need to reset the referencing subsystem and reorient it with lon/lat order?
        if (System.getProperty("org.geotools.referencing.forceXY") == null
                || !"http".equals(Hints.getSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING))) {
            System.setProperty("org.geotools.referencing.forceXY", "true");
            Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
            CRS.reset("all");
        }
    }

    @Before
    public final void setUpTestData() throws Exception {
        if (testData == null) {
            test = this;
            testData = createTestData();
            testData.setUp();
            
            setUpTestData((T) testData);
            setUp((T) testData);
        }
    }

    protected T getTestData() {
        return (T) testData;
    }

    protected abstract T createTestData() throws Exception;

    protected void setUpTestData(T testData) throws Exception {
    }

    protected void setUp(T testData) throws Exception {
    }

    @After
    public void tearDownTestData() throws Exception {
        if (testSetupPolicy == null) {
            testSetupPolicy = lookupTestSetupPolicy();
        }
        if (testSetupPolicy != TestSetupPolicy.ONCE) {
            doTearDownTestData();
        }
    }

    private TestSetupPolicy lookupTestSetupPolicy() {
        Class clazz = getClass();
        while(clazz != null && !Object.class.equals(clazz)) {
            TestSetup testSetup = (TestSetup) clazz.getAnnotation(TestSetup.class);
            if (testSetup != null) {
                return testSetup.run();
            }
            clazz = clazz.getSuperclass();
        }
        return TestSetupPolicy.REPEAT;
    }

    @AfterClass
    public static void doTearDownTestData() throws Exception {
        if (testData != null) {
            test.tearDown(testData);
            testData.tearDown();
            testData = null;
            test = null;
        }
    }

    protected void tearDown(T testData) throws Exception {
    }

    //common convenience methods
    /**
     * Returns a qualified name into a string of the form "[<prefix>:]<localPart>". 
     */
    protected String toString(QName qName) {
        if(qName.getPrefix() != null) {
            return qName.getPrefix() + ":" + qName.getLocalPart();
        }
        else {
            return qName.getLocalPart();
        }
    }

    /**
     * Returns a qualified name into a GeoTools type name. 
     */
    protected Name toName(QName qName) {
        return qName.getNamespaceURI() != null ? 
            new NameImpl(qName.getNamespaceURI(), qName.getLocalPart()) : new NameImpl(qName.getLocalPart());
    }

}
