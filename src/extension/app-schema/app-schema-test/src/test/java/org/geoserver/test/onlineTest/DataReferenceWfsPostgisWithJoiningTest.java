package org.geoserver.test.onlineTest;

import org.junit.Test;

import org.geotools.data.complex.AppSchemaDataAccessRegistry;

public class DataReferenceWfsPostgisWithJoiningTest extends DataReferenceWfsPostgisTest {

    public DataReferenceWfsPostgisWithJoiningTest() throws Exception {
        super();
    }

    protected void onSetUp(org.geoserver.test.NamespaceTestData testData) throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties().setProperty ("app-schema.joining", "true");
        super.onSetUp(testData);
    };
    
    @Override
    @Test
    public void testFilteringSplit() throws Exception {
        //this is a non joining test
    }

}
