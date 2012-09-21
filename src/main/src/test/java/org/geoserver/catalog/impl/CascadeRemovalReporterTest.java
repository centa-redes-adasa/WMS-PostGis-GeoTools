package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.CascadeRemovalReporter.ModificationType;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

public class CascadeRemovalReporterTest extends GeoServerMockTestSupport {
    
    static final String LAKES_GROUP = "lakesGroup";
    CascadeRemovalReporter visitor;
    Catalog catalog;

//    @Override
//    protected void setUp(MockTestData testData) throws Exception {
//        super.setUp(testData);
//        
//        catalog = getCatalog();
//        visitor = new CascadeRemovalReporter(catalog);
//        
//        // setup a group, see GEOS-3040
//        Catalog catalog = getCatalog();
//        String lakes = MockData.LAKES.getLocalPart();
//        String forests = MockData.FORESTS.getLocalPart();
//        String bridges = MockData.BRIDGES.getLocalPart();
//        
//        setNativeBox(catalog, lakes);
//        setNativeBox(catalog, forests);
//        setNativeBox(catalog, bridges);
//        
//        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
//        lg.setName(LAKES_GROUP);
//        lg.getLayers().add(catalog.getLayerByName(lakes));
//        lg.getStyles().add(catalog.getStyleByName(lakes));
//        lg.getLayers().add(catalog.getLayerByName(forests));
//        lg.getStyles().add(catalog.getStyleByName(forests));
//        lg.getLayers().add(catalog.getLayerByName(bridges));
//        lg.getStyles().add(catalog.getStyleByName(bridges));
//        CatalogBuilder builder = new CatalogBuilder(catalog);
//        builder.calculateLayerGroupBounds(lg);
//        catalog.add(lg);
//    }

    @Before
    public void init() {
        catalog = getCatalog();
        visitor = new CascadeRemovalReporter(catalog);
    }

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    @Test
    public void testCascadeLayer() {
        String name = getLayerId(MockData.LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);
        visitor.visit(layer);
        //layer.accept(visitor);
        
        // we expect a layer, a resource and a group
        assertEquals(3, visitor.getObjects(null).size());
        
        // check the layer and resource have been marked to delete (and
        assertEquals(catalog.getLayerByName(name), 
                visitor.getObjects(LayerInfo.class, ModificationType.DELETE).get(0));
        assertEquals(catalog.getResourceByName(name, ResourceInfo.class), 
                visitor.getObjects(ResourceInfo.class, ModificationType.DELETE).get(0));
        
        // the group has been marked to update? (we need to compare by id as the
        // objects won't compare properly by equality)
        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(group.getId(), visitor.getObjects(LayerGroupInfo.class, 
                ModificationType.GROUP_CHANGED).get(0).getId());
    }
    
    @Test
    public void testCascadeStore() {
        String citeStore = MockData.CITE_PREFIX;
        StoreInfo store = catalog.getStoreByName(citeStore, StoreInfo.class);
        String buildings = getLayerId(MockData.BUILDINGS);
        String lakes = getLayerId(MockData.LAKES);
        LayerInfo bl = catalog.getLayerByName(buildings);
        ResourceInfo br = catalog.getResourceByName(buildings, ResourceInfo.class);
        LayerInfo ll = catalog.getLayerByName(lakes);
        ResourceInfo lr = catalog.getResourceByName(lakes, ResourceInfo.class);
        
        store.accept(visitor);
        
        assertEquals(store, visitor.getObjects(StoreInfo.class, ModificationType.DELETE).get(0));
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, ModificationType.DELETE);
        assertTrue(layers.contains(bl));
        assertTrue(layers.contains(ll));
        List<ResourceInfo> resources = visitor.getObjects(ResourceInfo.class, ModificationType.DELETE);
        assertTrue(resources.contains(br));
        assertTrue(resources.contains(lr));
    }
    
    @Test
    public void testCascadeWorkspace() {
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        assertNotNull(ws);
        List<StoreInfo> stores = getCatalog().getStoresByWorkspace(ws, StoreInfo.class);
        
        ws.accept(visitor);
        
        assertTrue(stores.containsAll(visitor.getObjects(StoreInfo.class, ModificationType.DELETE)));
    }
    
    @Test
    public void testCascadeStyle() {
        String styleName = MockData.LAKES.getLocalPart();
        String layerName = getLayerId(MockData.LAKES);
        StyleInfo style = catalog.getStyleByName(styleName);
        assertNotNull(style);
        
        // add the lakes style to builds as an alternate style
        LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        buildings.getStyles().add(style);
        catalog.save(buildings);
        buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        assertTrue(buildings.getStyles().contains(style));
        
        style.accept(visitor);
        
        // test style reset
        assertEquals(style.getId(), visitor.getObjects(StyleInfo.class, ModificationType.DELETE).get(0).getId());
        String lakesId = catalog.getLayerByName(layerName).getId();
        assertEquals(lakesId, visitor.getObjects(LayerInfo.class, ModificationType.STYLE_RESET).get(0).getId());
        
        // test style removal
        String buildingsId = catalog.getLayerByName(getLayerId(MockData.BUILDINGS)).getId();
        assertEquals(buildingsId, visitor.getObjects(LayerInfo.class, ModificationType.EXTRA_STYLE_REMOVED).get(0).getId());
    }

    String getLayerId(QName name) {
        return toString(name);
    }
}
