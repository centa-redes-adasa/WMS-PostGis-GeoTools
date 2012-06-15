package org.geoserver.data.test;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerPersister;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;

public class DefaultTestData implements TestData {

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

    // //// WMS 1.1.1
    /**
     * WMS 1.1.1 cite namespace + uri
     */
    public static String CITE_PREFIX = "cite";
    public static String CITE_URI = "http://www.opengis.net/cite";
    
    /** featuretype name for WMS 1.1.1 CITE BasicPolygons features */
    public static QName BASIC_POLYGONS = new QName(CITE_URI, "BasicPolygons", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Bridges features */
    public static QName BRIDGES = new QName(CITE_URI, "Bridges", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Buildings features */
    public static QName BUILDINGS = new QName(CITE_URI, "Buildings", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Divided Routes features */
    public static QName DIVIDED_ROUTES = new QName(CITE_URI, "DividedRoutes", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Forests features */
    public static QName FORESTS = new QName(CITE_URI, "Forests", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Lakes features */
    public static QName LAKES = new QName(CITE_URI, "Lakes", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Map Neatliine features */
    public static QName MAP_NEATLINE = new QName(CITE_URI, "MapNeatline", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Named Places features */
    public static QName NAMED_PLACES = new QName(CITE_URI, "NamedPlaces", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Ponds features */
    public static QName PONDS = new QName(CITE_URI, "Ponds", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Road Segments features */
    public static QName ROAD_SEGMENTS = new QName(CITE_URI, "RoadSegments", CITE_PREFIX);
    
    /** featuretype name for WMS 1.1.1 CITE Streams features */
    public static QName STREAMS = new QName(CITE_URI, "Streams", CITE_PREFIX);
    
    // /// WFS 1.0
    /**
     * WFS 1.0 cdf namespace + uri
     */
    public static String CDF_PREFIX = "cdf";
    public static String CDF_URI = "http://www.opengis.net/cite/data";
    
    /** featuretype name for WFS 1.0 CITE Deletes features */
    public static QName DELETES = new QName(CDF_URI, "Deletes", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Fifteen features */
    public static QName FIFTEEN = new QName(CDF_URI, "Fifteen", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Inserts features */
    public static QName INSERTS = new QName(CDF_URI, "Inserts", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Inserts features */
    public static QName LOCKS = new QName(CDF_URI, "Locks", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Nulls features */
    public static QName NULLS = new QName(CDF_URI, "Nulls", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Other features */
    public static QName OTHER = new QName(CDF_URI, "Other", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Nulls features */
    public static QName SEVEN = new QName(CDF_URI, "Seven", CDF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Updates features */
    public static QName UPDATES = new QName(CDF_URI, "Updates", CDF_PREFIX);
    
    /**
     * cgf namespace + uri
     */
    public static String CGF_PREFIX = "cgf";
    public static String CGF_URI = "http://www.opengis.net/cite/geometry";
    
    /** featuretype name for WFS 1.0 CITE Lines features */
    public static QName LINES = new QName(CGF_URI, "Lines", CGF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE MLines features */
    public static QName MLINES = new QName(CGF_URI, "MLines", CGF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE MPoints features */
    public static QName MPOINTS = new QName(CGF_URI, "MPoints", CGF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE MPolygons features */
    public static QName MPOLYGONS = new QName(CGF_URI, "MPolygons", CGF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Points features */
    public static QName POINTS = new QName(CGF_URI, "Points", CGF_PREFIX);
    
    /** featuretype name for WFS 1.0 CITE Polygons features */
    public static QName POLYGONS = new QName(CGF_URI, "Polygons", CGF_PREFIX);
    
    // //// WFS 1.1
    /**
     * sf namespace + uri
     */
    public static String SF_PREFIX = "sf";
    public static String SF_URI = "http://cite.opengeospatial.org/gmlsf";
    public static QName PRIMITIVEGEOFEATURE = new QName(SF_URI, "PrimitiveGeoFeature", SF_PREFIX);
    public static QName AGGREGATEGEOFEATURE = new QName(SF_URI, "AggregateGeoFeature", SF_PREFIX);
    public static QName GENERICENTITY = new QName(SF_URI, "GenericEntity", SF_PREFIX);
    
    // WCS 1.0
    public static QName GTOPO_DEM = new QName(CDF_URI, "W020N90", CDF_PREFIX);
    public static QName USA_WORLDIMG = new QName(CDF_URI, "usa", CDF_PREFIX);
    public static String DEM = "dem";
    public static String PNG = "png";
    // WCS 1.1  
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";
    
    // DEFAULT
    public static String DEFAULT_PREFIX = "gs";
    public static String DEFAULT_URI = "http://geoserver.org";
    
    // public static QName ENTIT\u00C9G\u00C9N\u00C9RIQUE = new QName( SF_URI,
    // "Entit\u00E9G\u00E9n\u00E9rique", SF_PREFIX );
    
    // Extra types
    public static QName GEOMETRYLESS = new QName(CITE_URI, "Geometryless", CITE_PREFIX);
    
    /**
     * List of all cite types names
     */
    public static QName[] TYPENAMES = new QName[] {
            // WMS 1.1.1
            BASIC_POLYGONS, BRIDGES, BUILDINGS, DIVIDED_ROUTES, FORESTS, LAKES, MAP_NEATLINE,
            NAMED_PLACES, PONDS, ROAD_SEGMENTS, STREAMS, // WFS 1.0
            DELETES, FIFTEEN, INSERTS, LOCKS, NULLS, OTHER, SEVEN, UPDATES, LINES, MLINES, MPOINTS,
            MPOLYGONS, POINTS, POLYGONS, // WFS 1.1
            PRIMITIVEGEOFEATURE, AGGREGATEGEOFEATURE, GENERICENTITY, GEOMETRYLESS /* ENTIT\u00C9G\u00C9N\u00C9RIQUE */
        };
    
    /**
     * List of wms type names.
     */
    public static QName[] WMS_TYPENAMES = new QName[] {
            BASIC_POLYGONS, BRIDGES, BUILDINGS, DIVIDED_ROUTES, FORESTS, LAKES, MAP_NEATLINE,
            NAMED_PLACES, PONDS, ROAD_SEGMENTS, STREAMS
        };
    
    /**
     * List of wfs 1.0 type names.
     */
    public static QName[] WFS10_TYPENAMES = new QName[] {
            DELETES, FIFTEEN, INSERTS, LOCKS, NULLS, OTHER, SEVEN, UPDATES, LINES, MLINES, MPOINTS,
            MPOLYGONS, POINTS, POLYGONS
        };
    
    /**
     * List of wfs 1.1 type names.
     */
    public static QName[] WFS11_TYPENAMES = new QName[] {
            PRIMITIVEGEOFEATURE, AGGREGATEGEOFEATURE, GENERICENTITY /* ENTIT\u00C9G\u00C9N\u00C9RIQUE */
        };
    
    /**
     * map of qname to srs
     */
    public static HashMap<QName,Integer> SRS = new HashMap<QName, Integer>();
    static {
        for ( int i = 0; i < WFS10_TYPENAMES.length; i++ ) {
            SRS.put( WFS10_TYPENAMES[i], 32615);
        }
        for ( int i = 0; i < WFS11_TYPENAMES.length; i++ ) {
            SRS.put( WFS11_TYPENAMES[i], 4326 );
        }
    }

    public static String DEFAULT_VECTOR_STYLE = "Default";
    public static String DEFAULT_RASTER_STYLE = "raster";

    /**
     * Default lon/lat envelope
     */
    static final ReferencedEnvelope DEFAULT_ENVELOPE = 
        new ReferencedEnvelope(-180,180,-90,90, DefaultGeographicCRS.WGS84);

    /** data directory root */
    protected File data;

    /** internal catalog, used for setup before the real catalog available */
    Catalog catalog;

    public DefaultTestData() throws IOException {
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
        featureType.setLatLonBoundingBox(LayerProperty.LATLON_ENVELOPE.get(props, DEFAULT_ENVELOPE));
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
