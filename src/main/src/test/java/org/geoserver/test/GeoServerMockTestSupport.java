package org.geoserver.test;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockTestData;

public class GeoServerMockTestSupport extends GeoServerBaseTestSupport<MockTestData> {

    @Override
    protected MockTestData createTestData() throws Exception {
        return new MockTestData();
    }

    public Catalog getCatalog() {
        return getTestData().getCatalog();
    }
}
