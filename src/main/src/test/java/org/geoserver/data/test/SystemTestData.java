package org.geoserver.data.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerPersister;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

public class SystemTestData extends CiteTestData {

    /**
     * Keys for overriding default layer properties
     */
    public static class LayerProperty<T> {

        T get(Map<LayerProperty,Object> map, T def) {
            return map.containsKey(this) ? (T) map.get(this) : def;
        }

        public static LayerProperty<String> NAME = new LayerProperty<String>(); 
        public static LayerProperty<ProjectionPolicy> PROJECTION_POLICY = new LayerProperty<ProjectionPolicy>();
        public static LayerProperty<String> STYLE = new LayerProperty<String>();
        public static LayerProperty<ReferencedEnvelope> ENVELOPE = new LayerProperty<ReferencedEnvelope>();
        public static LayerProperty<ReferencedEnvelope> LATLON_ENVELOPE = new LayerProperty<ReferencedEnvelope>();
    }

    /** data directory root */
    protected File data;

    /** internal catalog, used for setup before the real catalog available */
    Catalog catalog;

    public SystemTestData() throws IOException {
        // setup the root
        data = IOUtils.createRandomDirectory("./target", "default", "data");
        data.delete();
        data.mkdir();
    }

    @Override
    public void setUp() throws Exception {
        createCatalog();
        createConfig();
    }

    public void setUpDefaultLayers() throws IOException {
        for (QName layerName : TYPENAMES) {
            addVectorLayer(layerName, catalog);
        }
    }

    public void setUpDefaultRasterLayers() throws IOException {
        addWorkspace(WCS_PREFIX, WCS_URI, catalog);
        addDefaultRasterLayer(TASMANIA_DEM, catalog);
        addDefaultRasterLayer(TASMANIA_BM, catalog);
        addDefaultRasterLayer(ROTATED_CAD, catalog);
        addDefaultRasterLayer(WORLD, catalog);
    }

    public void addDefaultRasterLayer(QName name, Catalog catalog) throws IOException {
        if (name.equals(TASMANIA_DEM)) {
            addRasterLayer(name,  "tazdem.tiff", catalog);
        }
        else if (name.equals(TASMANIA_BM)) {
            addRasterLayer(name, "tazbm.tiff", catalog);
        }
        else if (name.equals(ROTATED_CAD)) {
            addRasterLayer(name, "rotated.tiff", catalog);
        }
        else if (name.equals(WORLD)) {
            addRasterLayer(name, "world.tiff", catalog);
        }
        else {
            throw new IllegalArgumentException("Unknown default raster layer: " + name);
        }
    }

    public void setUpWcs10RasterLayers() throws IOException {
    }

    public void setUpWcs11RasterLayers() throws IOException {
        setUpDefaultRasterLayers();
    }

    protected void createCatalog() throws IOException {
        CatalogImpl catalog = new CatalogImpl();
        catalog.setExtendedValidation(false);
        catalog.setResourceLoader(new GeoServerResourceLoader(data));
        
        catalog.addListener(new GeoServerPersister(catalog.getResourceLoader(), 
            createXStreamPersister()));

        //workspaces
        addWorkspace(DEFAULT_PREFIX, DEFAULT_URI, catalog);
        addWorkspace(SF_PREFIX, SF_URI, catalog);
        addWorkspace(CITE_PREFIX, CITE_URI, catalog);
        addWorkspace(CDF_PREFIX, CDF_URI, catalog);
        addWorkspace(CGF_PREFIX, CGF_URI, catalog);

        //default style
        addStyle(DEFAULT_VECTOR_STYLE, catalog);
        addStyle(DEFAULT_RASTER_STYLE, catalog);

        this.catalog = catalog;
    }

    protected void createConfig() {
        GeoServerImpl geoServer = new GeoServerImpl();
        geoServer.addListener(new GeoServerPersister(new GeoServerResourceLoader(data), 
            createXStreamPersister()));

        GeoServerInfo global = geoServer.getFactory().createGlobal();
        global.getSettings().getContact().setContactPerson("Andrea Aime");
        global.getSettings().setNumDecimals(8);
        global.getSettings().setOnlineResource("http://geoserver.org");
        global.getSettings().setVerbose(false);
        geoServer.setGlobal(global);

        LoggingInfo logging = geoServer.getFactory().createLogging();
        geoServer.setLogging(logging);
    }

    XStreamPersister createXStreamPersister() {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.setEncryptPasswordFields(false);
        xp.setVerbose(false);
        return xp;
    }

    public void addWorkspace(String name, String uri, Catalog catalog) {
        
        WorkspaceInfo ws = catalog.getWorkspaceByName(name);
        if (ws == null) {
            ws = catalog.getFactory().createWorkspace();
            ws.setName(name);
            catalog.add(ws);
        }

        NamespaceInfo ns =  catalog.getNamespaceByPrefix(name);
        if (ns == null) {
            ns = catalog.getFactory().createNamespace();
            ns.setPrefix(name);
            ns.setURI(uri);
            catalog.add(ns);
        }
        else {
            ns.setURI(uri);
            catalog.save(ns);
        }
    }

    public void addStyle(String name, Catalog catalog) throws IOException {
        File styles = catalog.getResourceLoader().findOrCreateDirectory(data, "styles");

        String filename = name + ".sld";
        catalog.getResourceLoader().copyFromClassPath(filename, new File(styles, filename), getClass());

        StyleInfo style = catalog. getStyleByName(name);
        if (style == null) {
            style = catalog.getFactory().createStyle();
            style.setName(name);
        }
        style.setFilename(filename);
        if (style.getId() == null) {
            catalog.add(style);
        }
        else {
            catalog.save(style);
        }
    }

    public void addDefaultVectorLayer(QName qName, Catalog catalog) throws IOException {
        addVectorLayer(qName, catalog);
    }

    public void addVectorLayer(QName qName, Catalog catalog) throws IOException {
        addVectorLayer(qName, new HashMap(), catalog);
    }

    public void addVectorLayer(QName qName, Map<LayerProperty,Object> props, Catalog catalog) throws IOException {
        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();
        String uri = qName.getNamespaceURI();

        //configure workspace if it doesn;t already exist
        if (catalog.getWorkspaceByName(prefix) == null) {
            addWorkspace(prefix, uri, catalog);
        }
        
        //configure store if it doesn't already exist

        File storeDir = catalog.getResourceLoader().findOrCreateDirectory(prefix);

        DataStoreInfo store = catalog.getDataStoreByName(prefix);
        if (store == null) {
            store = catalog.getFactory().createDataStore();
            store.setName(prefix);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);

            store.getConnectionParameters().put(PropertyDataStoreFactory.DIRECTORY.key, storeDir);
            store.getConnectionParameters().put(PropertyDataStoreFactory.NAMESPACE.key, uri);
            catalog.add(store);
        }

        //copy the properties file over
        String filename = name + ".properties";
        catalog.getResourceLoader().copyFromClassPath(filename, new File(storeDir, filename), getClass());

        //configure feature type
        FeatureTypeInfo featureType = catalog.getFactory().createFeatureType();
        featureType.setStore(store);
        featureType.setNamespace(catalog.getNamespaceByPrefix(prefix));
        featureType.setName(LayerProperty.NAME.get(props, name));
        featureType.setNativeName(name);
        featureType.setTitle(name);
        featureType.setAbstract("abstract about " + name);

        Integer srs = SRS.get( name );
        if ( srs == null ) {
            srs = 4326;
        }
        featureType.setSRS("EPSG:" + srs);
        try {
            featureType.setNativeCRS(CRS.decode("EPSG:" + srs));
        } catch (Exception e) {
            throw new IOException(e);
        }
        featureType.setNumDecimals(8);
        featureType.getKeywords().add(new Keyword(name));
        featureType.setEnabled(true);
        featureType.setProjectionPolicy(LayerProperty.PROJECTION_POLICY.get(props, ProjectionPolicy.NONE));
        featureType.setLatLonBoundingBox(LayerProperty.LATLON_ENVELOPE.get(props, DEFAULT_LATLON_ENVELOPE));
        featureType.setNativeBoundingBox(LayerProperty.ENVELOPE.get(props, null));

        FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore(store, name);
        if (ft == null) {
            ft = featureType;
            catalog.add(featureType);
        }
        else {
            new CatalogBuilder(catalog).updateFeatureType(ft, featureType);
            catalog.save(ft);
        }

        LayerInfo layer = catalog.getLayerByName(new NameImpl(prefix, name));
        if (layer == null) {
            layer = catalog.getFactory().createLayer();    
        }

        layer.setResource(ft);

        StyleInfo defaultStyle = null;
        if (props.containsKey(LayerProperty.STYLE)) {
            defaultStyle = catalog.getStyleByName(LayerProperty.STYLE.get(props, null));
        }
        else {
            //look for a style matching the layer name
            defaultStyle = catalog.getStyleByName(name);
            if (defaultStyle == null) {
                //see if the resource exists and we just need to create it
                if (getClass().getResource(name + ".sld") != null) {
                    addStyle(name, catalog);
                    defaultStyle = catalog.getStyleByName(name);
                }
            }
        }

        if (defaultStyle == null) {
            defaultStyle = catalog.getStyleByName(DEFAULT_VECTOR_STYLE);
        }

        layer.getStyles().clear();
        layer.setDefaultStyle(defaultStyle);
        layer.setType(LayerInfo.Type.VECTOR);
        layer.setEnabled(true);

        if (layer.getId() == null) {
            catalog.add(layer);
        }
        else {
            catalog.save(layer);
        }
    }


    public void addRasterLayer(QName qName, String filename, Catalog catalog) throws IOException {
        addRasterLayer(qName, filename, null, catalog);
    }

    public void addRasterLayer(QName qName, String filename, String format, Catalog catalog) throws IOException {
        addRasterLayer(qName, filename, format, new HashMap(), catalog);
    }

    public void addRasterLayer(QName qName, String filename, String extension, 
        Map<LayerProperty,Object> props, Catalog catalog) throws IOException {

        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();

        //setup the data
        File dir = new File(data, name);
        dir.mkdirs();

        File file = new File(dir, filename);
        catalog.getResourceLoader().copyFromClassPath(filename, file, getClass());

        String ext = FilenameUtils.getExtension(filename);
        if ("zip".equalsIgnoreCase(ext)) {
            if (extension == null) {
                throw new IllegalArgumentException("Raster data specified as archive but no " + 
                    "extension of coverage was specified");
            }

            //unpack the archive
            IOUtils.decompress(file, dir);

            //delete archive
            file.delete();

            file = new File(dir, FilenameUtils.getBaseName(filename) + "." + ext);
            if (!file.exists()) {
                throw new FileNotFoundException(file.getPath());
            }
        }

        //load the format/reader
        AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(file);
        if (format == null) {
            throw new RuntimeException("No format for " + file.getCanonicalPath());
        }
        AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) format.getReader(file);
        if (reader == null) {
            throw new RuntimeException("No reader for " + file.getCanonicalPath() + " with format " + format.getName());
        }

        //create the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName(prefix, name);
        if (store == null) {
            store = catalog.getFactory().createCoverageStore();
            store.setName(name);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);
            store.setURL(DataUtilities.fileToURL(file).toString());
            store.setType(format.getName());
            catalog.add(store);
        }

        //create the coverage
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(store);

        CoverageInfo coverage = null;
        
        try {
            coverage = builder.buildCoverage(reader, null);
        } catch (Exception e) {
            throw new IOException(e);
        }

        coverage.setName(name);
        coverage.setTitle(name);
        coverage.setDescription(name);
        coverage.setEnabled(true);

        CoverageInfo cov = catalog.getCoverageByCoverageStore(store, name);
        if (cov == null) {
            catalog.add(coverage);
        }
        else {
            builder.updateCoverage(cov, coverage);
            catalog.save(cov);
            coverage = cov;
        }

        LayerInfo layer = catalog.getLayerByName(new NameImpl(qName));
        if (layer == null) {
            layer = catalog.getFactory().createLayer();
        }
        layer.setResource(coverage);

        
        layer.setDefaultStyle(
            catalog.getStyleByName(LayerProperty.STYLE.get(props, DEFAULT_RASTER_STYLE)));
        layer.setType(LayerInfo.Type.RASTER);
        layer.setEnabled(true);

        if (layer.getId() == null) {
            catalog.add(layer);
        }
        else {
            catalog.save(layer);
        }
    }

    @Override
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(data);
    }
    
    @Override
    public File getDataDirectoryRoot() {
        return data;
    }
    
    @Override
    public boolean isTestDataAvailable() {
        return true;
    }

}
