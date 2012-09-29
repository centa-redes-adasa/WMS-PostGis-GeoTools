package org.geoserver.test;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.geoserver.config.ServiceInfo;
import org.geoserver.data.test.SystemTestData;

/**
 * Base test class for GeoServer system tests that require a fully configured spring context and
 * work off a real data directory provided by {@link SystemTestData}.
 * <h2>Subclass Hooks</h2>
 * <p>
 * Subclasses extending this base class have the following hooks avaialble:
 * <ul>
 *   <li>{@link #setUpTestData(SystemTestData)} - Perform post configuration of the {@link SystemTestData} 
 *   instance</li>
 *   <li>{@link #onSetUp(SystemTestData)} - Perform setup after the system has been fully initialized
 *   <li>{@link #onTearDown(SystemTestData)} - Perform teardown before the system is to be shutdown 
 * </ul>
 * </p>
 * <h2>Test Setup Frequency</h2>
 * <p>
 * By default the setup cycle is executed once for extensions of this class. Subclasses that require
 * a different test setup frequency should annotate themselves with the appropriate {@link TestSetup}
 * annotation. For example to implement a repeated setup:
 * <code><pre> 
 *  {@literal @}TestSetup(run=TestSetupFrequency.REPEATED}
 *  public class MyTest extends GeoServerSystemTestSupport {
 *  
 *  }
 * </pre></code>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
@TestSetup(run=TestSetupFrequency.ONCE)
public class GeoServerSystemTestSupport extends GeoServerSpringTestSupport<SystemTestData> {

    protected SystemTestData createTestData() throws Exception {
        return new SystemTestData();
    }

    //
    // subclass hooks
    //
    /**
     * Sets up the {@link SystemTestData} used for this test.
     * <p>
     * This method is used to add any additional data or configuration to the test setup and may 
     * be overridden or extended. The default implementation calls 
     * {@link SystemTestData#setUpDefaultLayers()} to add the default layers for the test.  
     * </p>
     */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultLayers();
        testData.setUpSecurity();
    }

    /**
     * Subclass hook called after the system (ie spring context) has been fully initialized.
     * <p>
     * Subclasses should override for post setup that is needed. The default implementation does 
     * nothing. 
     * </p>
     */
    protected void onSetUp(SystemTestData testData) throws Exception {
    }

    /**
     * Subclass hook called before the system (ie spring context) is to be shut down.
     * <p>
     * Subclasses should override for any cleanup / teardown that should occur on system shutdown. 
     * </p>
     */
    protected void onTearDown(SystemTestData testData) throws Exception {
    }

    protected void revertLayer(String workspace, String layerName) throws IOException {
        revertLayer(new QName(workspace, layerName));
    }

    protected void revertLayer(QName qName) throws IOException {
        getTestData().addVectorLayer(qName, getCatalog());
    }
    
    public void revertService(String workspace, Class<? extends ServiceInfo> service) {
        getTestData().addService(workspace, service, getGeoServer(), getCatalog(), applicationContext);
    }

}
