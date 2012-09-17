package org.geoserver.test;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.MockTestData.MockCreator;

/**
 * Base test class for GeoServer mock tests that work from mocked up configuration. 
 * <h2>Test Setup Frequency</h2>
 * <p>
 * By default the setup cycle is executed once for extensions of this class. Subclasses that require
 * a different test setup frequency should annotate themselves with the appropriate {@link TestSetup}
 * annotation. For example to implement a repeated setup:
 * <code><pre> 
 *  {@literal @}TestSetup(run=TestSetupFrequency.REPEATED}
 *  public class MyTest extends GeoServerMockTestSupport {
 *  
 *  }
 * </pre></code>
 * </p>
 *  * <h2>Mock Customization</h2>
 * <p>
 * Subclasses extending this base class may customize the mock setup by setting a custom 
 * {@link MockCreator} object to {@link #setMockCreator(MockCreator)}. Tests that utilize the 
 * one time setup (which is the default for this class) may call this method from the 
 * {@link GeoServerBaseTestSupport#setUp(TestData)} hook. For test classes requiring per test case
 * mock customization this method should be called from the test method itself, but the test class
 * must declare a setup frequency of {@link TestSetupFrequency#REPEAT}.  
 *
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class GeoServerMockTestSupport extends GeoServerBaseTestSupport<MockTestData> {

    @Override
    protected MockTestData createTestData() throws Exception {
        return new MockTestData();
    }

    public Catalog getCatalog() {
        return getTestData().getCatalog();
    }

    /**
     * Forwards through to {@link MockTestData#setMockCreator(MockCreator)}
     */
    protected void setMockCreator(MockCreator mockCreator) {
        getTestData().setMockCreator(mockCreator);
    }
}
