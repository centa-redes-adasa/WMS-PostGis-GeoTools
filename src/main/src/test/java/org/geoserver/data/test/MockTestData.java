package org.geoserver.data.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.security.SecurityUtils.toBytes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.easymock.internal.LastControl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerEmptyPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.opengis.feature.type.FeatureType;
import org.springframework.context.ApplicationContext;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Test setup uses for GeoServer mock tests.
 * <p>
 * This is the default test setup used by {@link GeoServerMockTestSupport}. During setup this 
 * class creates a catalog whose contents contain all the layers defined by {@link CiteTestData}
 * </p>
 * <p>
 * Customizing the setup, adding layers, etc... is done from 
 * {@link GeoServerSystemTestSupport#setUpTestData}. 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class MockTestData extends CiteTestData {

    File data;
    Catalog catalog;
    GeoServerSecurityManager secMgr;
    MockCreator mockCreator;
    boolean includeRaster;

    public MockTestData() throws IOException {
        // setup the root
        data = IOUtils.createRandomDirectory("./target", "mock", "data");
        data.delete();
        data.mkdir();

        mockCreator = new MockCreator();
    }

    public void setMockCreator(MockCreator mockCreator) {
        this.mockCreator = mockCreator;
    }

    public boolean isInludeRaster() {
        return includeRaster;
    }

    public void setIncludeRaster(boolean includeRaster) {
        this.includeRaster = includeRaster;
    }

    public Catalog getCatalog() {
        if (catalog == null) {
            try {
                catalog = mockCreator.createCatalog(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return catalog;
    }

    public GeoServerSecurityManager getSecurityManager() {
        if (secMgr == null) {
            try {
                secMgr = mockCreator.createSecurityManager(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return secMgr;
    }

    @Override
    public void setUp() throws Exception {
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

    public static class MockCreator {

        public Catalog createCatalog(MockTestData testData) throws Exception {

            File data = testData.getDataDirectoryRoot();

            GeoServerResourceLoader loader = new GeoServerResourceLoader(data);
            GeoserverDataDirectory.setResourceLoader(loader);

            final Catalog catalog = createMock(Catalog.class);
            expect(catalog.getFactory()).andReturn(new CatalogFactoryImpl(catalog)).anyTimes();
            expect(catalog.getResourceLoader()).andReturn(loader).anyTimes();
            
            catalog.addListener((CatalogListener) EasyMock.anyObject());
            expectLastCall().anyTimes();
            
            expect(catalog.getResourcePool()).andAnswer(new IAnswer<ResourcePool>() {
                @Override
                public ResourcePool answer() throws Throwable {
                    return new ResourcePool(catalog);
                }
            }).anyTimes();

            MockCatalogBuilder b = new MockCatalogBuilder(catalog, data);

            b.style(DEFAULT_VECTOR_STYLE);

            createWorkspace(DEFAULT_PREFIX, DEFAULT_URI, null, b);
            createWorkspace(CGF_PREFIX, CGF_URI, CGF_TYPENAMES, b);
            createWorkspace(CDF_PREFIX, CDF_URI, CDF_TYPENAMES, b);
            createWorkspace(SF_PREFIX, SF_URI, SF_TYPENAMES, b);
            createWorkspace(CITE_PREFIX, CITE_URI, CITE_TYPENAMES, b);

            if (testData.isInludeRaster()) {
                b.style(DEFAULT_RASTER_STYLE);
        
                createWorkspace(WCS_PREFIX, WCS_URI, null, WCS_TYPENAMES, b);
            }

            b.commit();
            return catalog;
        }

        void createWorkspace(String wsName, String nsURI, QName[] typeNames, MockCatalogBuilder b) {
            createWorkspace(wsName, nsURI, typeNames, null, b);
        }
        
        void createWorkspace(String wsName, String nsURI, QName[] ftTypeNames, QName[] covTypeNames, 
            MockCatalogBuilder b) {
            b.workspace(wsName, nsURI);

            if (ftTypeNames != null && ftTypeNames.length > 0) {
                b.dataStore(wsName);
                for (QName typeName : ftTypeNames) {
                    String local = typeName.getLocalPart();
                    b.style(local);
                    b.featureType(local);
                }
                b.commit().commit();
            }
            if (covTypeNames != null && covTypeNames.length > 0) {
                for (QName typeName : covTypeNames) {
                    String local = typeName.getLocalPart();
                    String[] fileNameAndFormat = COVERAGES.get(typeName);
                    
                    b.coverageStore(local, fileNameAndFormat[0], fileNameAndFormat[1]);
                    b.coverage(typeName);
                    b.commit();
                }
                b.commit();
            }
        }

        public GeoServerSecurityManager createSecurityManager(MockTestData testData) throws Exception {
            final GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);

            //application context
            ApplicationContext appContext = createNiceMock(ApplicationContext.class);
            expect(secMgr.getApplicationContext()).andReturn(appContext).anyTimes();

            //master password provider
            MasterPasswordProvider masterPasswdProvider = createNiceMock(MasterPasswordProvider.class);
            expect(masterPasswdProvider.getName()).andReturn(MasterPasswordProvider.DEFAULT_NAME).anyTimes();
            expect(secMgr.listMasterPasswordProviders()).andReturn(
                new TreeSet<String>(Arrays.asList(MasterPasswordProvider.DEFAULT_NAME))).anyTimes();

            //password validators
            PasswordValidator passwdValidator = createNiceMock(PasswordValidator.class);
            expect(secMgr.loadPasswordValidator(PasswordValidator.DEFAULT_NAME))
                .andReturn(passwdValidator).anyTimes();

            PasswordPolicyConfig masterPasswdPolicyConfig = createNiceMock(PasswordPolicyConfig.class);
            expect(masterPasswdPolicyConfig.getMinLength()).andReturn(8).anyTimes();
            expect(masterPasswdPolicyConfig.getMaxLength()).andReturn(-1).anyTimes();
            

            PasswordValidatorImpl masterPasswdValidator = new PasswordValidatorImpl(secMgr);
            masterPasswdValidator.setConfig(masterPasswdPolicyConfig);

            expect(secMgr.loadPasswordValidator(PasswordValidator.MASTERPASSWORD_NAME))
                .andReturn(masterPasswdValidator).anyTimes();
            expect(secMgr.listPasswordValidators()).andReturn(
                    new TreeSet<String>(Arrays.asList(PasswordValidator.DEFAULT_NAME, PasswordValidator.MASTERPASSWORD_NAME))).anyTimes();;

            //default user group store
            GeoServerUserGroupStore ugStore = 
                createUserGroupStore(XMLUserGroupService.DEFAULT_NAME, secMgr);
            expect(secMgr.listUserGroupServices()).andReturn(
                new TreeSet<String>(Arrays.asList(XMLUserGroupService.DEFAULT_NAME))).anyTimes();

            SecurityUserGroupServiceConfig ugConfig = createNiceMock(SecurityUserGroupServiceConfig.class);
            expect(ugConfig.getName()).andReturn(XMLUserGroupService.DEFAULT_NAME).anyTimes();
            expect(ugConfig.getPasswordPolicyName()).andReturn(PasswordValidator.DEFAULT_NAME).anyTimes();
            expect(secMgr.loadUserGroupServiceConfig(XMLUserGroupService.DEFAULT_NAME))
                .andReturn(ugConfig).anyTimes();

            //default role store
            GeoServerRoleStore roleStore = 
                createRoleStore(XMLRoleService.DEFAULT_NAME, secMgr);
            expect(secMgr.listRoleServices()).andReturn(
                new TreeSet<String>(Arrays.asList(XMLRoleService.DEFAULT_NAME))).anyTimes();
            expect(secMgr.getActiveRoleService()).andReturn(roleStore).anyTimes();

            //auth providers
            SecurityAuthProviderConfig authProviderConfig = createNiceMock(SecurityAuthProviderConfig.class);
            expect(authProviderConfig.getName()).andReturn(GeoServerAuthenticationProvider.DEFAULT_NAME).anyTimes();
            expect(authProviderConfig.getUserGroupServiceName()).andReturn(XMLUserGroupService.DEFAULT_NAME).anyTimes();
            expect(secMgr.loadAuthenticationProviderConfig(GeoServerAuthenticationProvider.DEFAULT_NAME))
                .andReturn(authProviderConfig).anyTimes();
            
            GeoServerAuthenticationProvider authProvider = createNiceMock(GeoServerAuthenticationProvider.class);
            expect(authProvider.getName()).andReturn(GeoServerAuthenticationProvider.DEFAULT_NAME).anyTimes();
            expect(secMgr.loadAuthenticationProvider(GeoServerAuthenticationProvider.DEFAULT_NAME))
                .andReturn(authProvider).anyTimes();
            expect(secMgr.listAuthenticationProviders()).andReturn(
                new TreeSet<String>(Arrays.asList(GeoServerAuthenticationProvider.DEFAULT_NAME))).anyTimes();
            expect(secMgr.getAuthenticationProviders()).andReturn(Arrays.asList(authProvider)).anyTimes();

            //security filters
            SecurityFilterConfig filterConfig = createNiceMock(SecurityFilterConfig.class); 
            expect(secMgr.loadFilterConfig(
                GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR)).andReturn(filterConfig).anyTimes();

            //password encoders
            expect(secMgr.loadPasswordEncoder(GeoServerEmptyPasswordEncoder.class)).andAnswer(
                new IAnswer<GeoServerEmptyPasswordEncoder>() {
                    @Override
                    public GeoServerEmptyPasswordEncoder answer() throws Throwable {
                        return createEmptyPasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder("emptyPasswordEncoder")).andAnswer(
                new IAnswer<GeoServerPasswordEncoder>() {
                    @Override
                    public GeoServerPasswordEncoder answer() throws Throwable {
                        return createEmptyPasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class)).andAnswer(
                new IAnswer<GeoServerPlainTextPasswordEncoder>() {
                    @Override
                    public GeoServerPlainTextPasswordEncoder answer() throws Throwable {
                        return createPlainTextPasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder("plainTextPasswordEncoder")).andAnswer(
                new IAnswer<GeoServerPasswordEncoder>() {
                    @Override
                    public GeoServerPasswordEncoder answer() throws Throwable {
                        return createPlainTextPasswordEncoder(secMgr);
                    }
                }).anyTimes();

            expect(secMgr.loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null,false)).andAnswer(
                new IAnswer<GeoServerPBEPasswordEncoder>() {
                    @Override
                    public GeoServerPBEPasswordEncoder answer() throws Throwable {
                        return createPbePasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder("pbePasswordEncoder")).andAnswer(
                new IAnswer<GeoServerPasswordEncoder>() {
                    @Override
                    public GeoServerPasswordEncoder answer() throws Throwable {
                        return createPbePasswordEncoder(secMgr);
                    }
                }).anyTimes();
            
            expect(secMgr.loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null,true)).andAnswer(
                new IAnswer<GeoServerPBEPasswordEncoder>() {
                    @Override
                    public GeoServerPBEPasswordEncoder answer() throws Throwable {
                        return createStrongPbePasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder("strongPbePasswordEncoder")).andAnswer(
                new IAnswer<GeoServerPasswordEncoder>() {
                    @Override
                    public GeoServerPasswordEncoder answer() throws Throwable {
                        return createStrongPbePasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder(GeoServerDigestPasswordEncoder.class, null,true)).andAnswer(
                new IAnswer<GeoServerDigestPasswordEncoder>() {
                    @Override
                    public GeoServerDigestPasswordEncoder answer() throws Throwable {
                        return createDigestPasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder(GeoServerDigestPasswordEncoder.class)).andAnswer(
                new IAnswer<GeoServerDigestPasswordEncoder>() {
                    @Override
                    public GeoServerDigestPasswordEncoder answer() throws Throwable {
                        return createDigestPasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoder("digestPasswordEncoder")).andAnswer(
                new IAnswer<GeoServerPasswordEncoder>() {
                    @Override
                    public GeoServerPasswordEncoder answer() throws Throwable {
                        return createDigestPasswordEncoder(secMgr);
                    }
                }).anyTimes();
            expect(secMgr.loadPasswordEncoders()).andAnswer(
                new IAnswer<List<GeoServerPasswordEncoder>>() {
                    @Override
                    public List<GeoServerPasswordEncoder> answer()
                            throws Throwable {
                        return (List) Arrays.asList(
                            createEmptyPasswordEncoder(secMgr),
                            createPlainTextPasswordEncoder(secMgr), createPbePasswordEncoder(secMgr), 
                            createStrongPbePasswordEncoder(secMgr), createDigestPasswordEncoder(secMgr));
                    }
                }).anyTimes();

            //keystore provider
            KeyStoreProvider keyStoreProvider = createNiceMock(KeyStoreProvider.class);
            expect(keyStoreProvider.isKeyStorePassword(aryEq("geoserver".toCharArray())))
                .andReturn(true).anyTimes();
            expect(keyStoreProvider.containsAlias(KeyStoreProviderImpl.CONFIGPASSWORDKEY))
                .andReturn(true).anyTimes();;
            expect(keyStoreProvider.getSecretKey(KeyStoreProviderImpl.CONFIGPASSWORDKEY))
                .andReturn(new SecretKeySpec(toBytes("geoserver".toCharArray()),"PBE")).anyTimes();
            expect(keyStoreProvider.hasUserGroupKey(XMLUserGroupService.DEFAULT_NAME))
                .andReturn(true).anyTimes();

            String alias = "ugServiceAlias";
            expect(keyStoreProvider.aliasForGroupService(XMLUserGroupService.DEFAULT_NAME))
                .andReturn(alias).anyTimes();
            expect(keyStoreProvider.containsAlias(alias)).andReturn(true).anyTimes();;
            expect(keyStoreProvider.getSecretKey(alias)).andReturn(
                    new SecretKeySpec(toBytes("geoserver".toCharArray()),"PBE")).anyTimes();
            expect(secMgr.getKeyStoreProvider()).andReturn(keyStoreProvider).anyTimes();
            
            replay(keyStoreProvider, masterPasswdProvider, ugStore, ugConfig, roleStore, authProvider, 
                authProviderConfig, filterConfig, passwdValidator, masterPasswdPolicyConfig, appContext, 
                secMgr);
            return secMgr;
        }

        protected GeoServerEmptyPasswordEncoder createEmptyPasswordEncoder( 
            GeoServerSecurityManager secMgr) throws IOException {
            GeoServerEmptyPasswordEncoder emptyPwe = new GeoServerEmptyPasswordEncoder();
            emptyPwe.setBeanName("emptyPasswordEncoder");
            emptyPwe.setPrefix("empty");
            return emptyPwe;
        }
        
        protected GeoServerDigestPasswordEncoder createDigestPasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {
            GeoServerDigestPasswordEncoder digestPwe = new GeoServerDigestPasswordEncoder();
            digestPwe.setBeanName("digestPasswordEncoder");
            digestPwe.setPrefix("digest1");
            return digestPwe;
        }

        protected GeoServerPBEPasswordEncoder createStrongPbePasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {
            GeoServerPBEPasswordEncoder strongPbePwe = new GeoServerPBEPasswordEncoder();
            strongPbePwe.setBeanName("strongPbePasswordEncoder");
            strongPbePwe.setPrefix("crypt2");
            strongPbePwe.setProviderName("BC");
            strongPbePwe.setAvailableWithoutStrongCryptogaphy(false);
            strongPbePwe.initialize(secMgr);
            return strongPbePwe;
        }

        protected GeoServerPBEPasswordEncoder createPbePasswordEncoder(GeoServerSecurityManager secMgr) 
            throws IOException {
            GeoServerPBEPasswordEncoder pbePwe = new GeoServerPBEPasswordEncoder();
            pbePwe.setBeanName("pbePasswordEncoder");
            pbePwe.setPrefix("crypt1");
            pbePwe.setAlgorithm("PBEWITHMD5ANDDES");
            pbePwe.initialize(secMgr);
            return pbePwe;
        }

        protected GeoServerPlainTextPasswordEncoder createPlainTextPasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {

            GeoServerPlainTextPasswordEncoder plainPwe = new GeoServerPlainTextPasswordEncoder();
            plainPwe.setBeanName("plainTextPasswordEncoder");
            plainPwe.setPrefix("plain");
            plainPwe.initialize(secMgr);
            return plainPwe;
        }

        protected GeoServerUserGroupStore createUserGroupStore(String name, GeoServerSecurityManager secMgr) 
            throws IOException {
            GeoServerUserGroupStore ugStore = createNiceMock(GeoServerUserGroupStore.class);
            expect(ugStore.getName()).andReturn(name).anyTimes();
            
            expect(secMgr.loadUserGroupService(name)).andReturn(ugStore).anyTimes();
            return ugStore;
        }

        protected void addUsers(GeoServerUserGroupStore ugStore, String... up) throws IOException {
            for (int i = 0; i < up.length; i += 2) {
                GeoServerUser user = new GeoServerUser(up[i]);
                user.setPassword(up[i+1]);

                expect(ugStore.getUserByUsername(up[i])).andReturn(user).anyTimes();
            }
        }

        protected  void addGroups(GeoServerUserGroupStore ugStore, String... groupNames) throws IOException {
            for (String groupName : groupNames) {
                GeoServerUserGroup grp = new GeoServerUserGroup(groupName);
                expect(ugStore.getGroupByGroupname(groupName)).andReturn(grp).anyTimes();
            }
        }

        protected GeoServerRoleStore createRoleStore(String name, GeoServerSecurityManager secMgr, String... roleNames) 
            throws IOException {

            GeoServerRoleStore roleStore = createNiceMock(GeoServerRoleStore.class);
            expect(roleStore.getSecurityManager()).andReturn(secMgr).anyTimes();
            expect(roleStore.getName()).andReturn(name).anyTimes();

            for (String roleName : roleNames) {
                expect(roleStore.getRoleByName(roleName)).andReturn(new GeoServerRole(roleName)).anyTimes();
            }

            for (GeoServerRole role : GeoServerRole.SystemRoles) {
                String roleName = role.getAuthority();
                expect(roleStore.createRoleObject(roleName)).andReturn(new GeoServerRole(roleName)).anyTimes();
            }

            expect(secMgr.loadRoleService(name)).andReturn(roleStore).anyTimes();
            return roleStore;
        }

        protected void addRolesToCreate(GeoServerRoleStore roleStore, String... roleNames) throws IOException {
            for (String roleName : roleNames) {
                expect(roleStore.createRoleObject(roleName)).andReturn(new GeoServerRole(roleName)).anyTimes();
            }
        }

    }

    public static class MockCatalogBuilder {
        Catalog catalog;
        File dataDirRoot;

        LinkedList<WorkspaceInfo> workspaces = new LinkedList();
        LinkedList<NamespaceInfo> namespaces = new LinkedList();
        LinkedList<DataStoreInfo> dataStores = new LinkedList();
        LinkedList<CoverageStoreInfo> coverageStores = new LinkedList();
        LinkedList<FeatureTypeInfo> featureTypes = new LinkedList();
        LinkedList<CoverageInfo> coverages = new LinkedList();
        LinkedList<LayerInfo> layers = new LinkedList();
        LinkedList<StyleInfo> styles = new LinkedList();

        LinkedList<FeatureTypeInfo> featureTypesByNamespace = new LinkedList();
        LinkedList<FeatureTypeInfo> featureTypesAll = new LinkedList();
        LinkedList<CoverageInfo> coveragesByNamespace = new LinkedList();
        LinkedList<CoverageInfo> coveragesAll = new LinkedList();
        LinkedList<DataStoreInfo> dataStoresAll = new LinkedList();
        LinkedList<CoverageStoreInfo> coverageStoresAll = new LinkedList();

        public MockCatalogBuilder(Catalog catalog, File dataDirRoot) {
            this.catalog = catalog;
            this.dataDirRoot = dataDirRoot;
        }

        public Catalog getCatalog() {
            return catalog;
        }

        public MockCatalogBuilder workspace(String name, String uri) {
            String wsId = newId();
            String nsId = newId();

            WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
            workspaces.add(ws);
            expect(ws.getId()).andReturn(wsId).anyTimes();
            expect(ws.getName()).andReturn(name).anyTimes();
            expect(ws.getMetadata()).andReturn(new MetadataMap()).anyTimes();

            expect(catalog.getWorkspace(wsId)).andReturn(ws).anyTimes();
            expect(catalog.getWorkspaceByName(name)).andReturn(ws).anyTimes();

            NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
            namespaces.add(ns);

            expect(ns.getId()).andReturn(nsId).anyTimes();
            expect(ns.getName()).andReturn(name).anyTimes();
            expect(ns.getPrefix()).andReturn(name).anyTimes();
            expect(ns.getMetadata()).andReturn(new MetadataMap()).anyTimes();

            expect(catalog.getNamespace(nsId)).andReturn(ns).anyTimes();
            expect(catalog.getNamespaceByPrefix(name)).andReturn(ns).anyTimes();
            expect(catalog.getNamespaceByURI(uri)).andReturn(ns).anyTimes();

            replay(ws, ns);
            return this;
        }

        public MockCatalogBuilder dataStore(String name) {
            String dsId = newId();
            final WorkspaceInfo ws = workspaces.peekLast();
            final NamespaceInfo ns = namespaces.peekLast();

            DataStoreInfo ds = createNiceMock(DataStoreInfo.class);
            dataStores.add(ds);

            initStore(ds, DataStoreInfo.class, dsId, name, ws);

            //setup the property data store
            final File propDir = new File(dataDirRoot, name);
            
            HashMap cxParams = new HashMap();
            cxParams.put(PropertyDataStoreFactory.DIRECTORY.key, propDir);
            cxParams.put(PropertyDataStoreFactory.NAMESPACE.key, ns.getURI());
            expect(ds.getConnectionParameters()).andReturn(cxParams).anyTimes();
            
            try {
                expect(ds.getDataStore(null)).andAnswer((IAnswer)new IAnswer<DataAccess>() {
                    @Override
                    public DataAccess answer() throws Throwable {
                        return new PropertyDataStore(propDir, ns.getURI());
                    }
                }).anyTimes();
            } catch (IOException e) {
            }

            expect(catalog.getDataStore(dsId)).andReturn(ds).anyTimes();
            expect(catalog.getDataStoreByName(name)).andReturn(ds).anyTimes();
            expect(catalog.getDataStoreByName(ws.getName(), name)).andReturn(ds).anyTimes();
            expect(catalog.getDataStoreByName(ws, name)).andReturn(ds).anyTimes();

            replay(ds);
            return this;
        }

        public MockCatalogBuilder coverageStore(String name, String filename, String format) {
            String csId = newId();
            WorkspaceInfo ws = workspaces.peekLast();
            NamespaceInfo ns = namespaces.peekLast();

            CoverageStoreInfo cs = createNiceMock(CoverageStoreInfo.class);
            coverageStores.add(cs);

            initStore(cs, CoverageStoreInfo.class, csId, name, ws);

            File covDir = new File(dataDirRoot, name);
            final File covFile = new File(covDir, filename);
            expect(cs.getURL()).andReturn(DataUtilities.fileToURL(covFile).toString()).anyTimes();
            expect(cs.getType()).andAnswer(new IAnswer<String>() {
                @Override
                public String answer() throws Throwable {
                    return lookupGridFormat(covFile).getName();
                }
            }).anyTimes();
            expect(cs.getFormat()).andAnswer(new IAnswer<AbstractGridFormat>() {
                @Override
                public AbstractGridFormat answer() throws Throwable {
                    return lookupGridFormat(covFile);
                }
            }).anyTimes();
            expect(cs.getConnectionParameters()).andReturn(new HashMap()).anyTimes();

            expect(catalog.getCoverageStore(csId)).andReturn(cs).anyTimes();
            expect(catalog.getCoverageStoreByName(name)).andReturn(cs).anyTimes();
            expect(catalog.getCoverageStoreByName(ws.getName(), name)).andReturn(cs).anyTimes();
            expect(catalog.getCoverageStoreByName(ws, name)).andReturn(cs).anyTimes();

            replay(cs);
            return this;
        }

        AbstractGridFormat lookupGridFormat(Object obj) {
            AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(obj);
            if (format == null) {
                throw new RuntimeException("No format for " + obj);
            }
            return format;
        }
       
        <T extends StoreInfo> void initStore(T s, Class<T> clazz, String sId, String name, 
            WorkspaceInfo ws) {
            expect(s.getId()).andReturn(sId).anyTimes();
            expect(s.getName()).andReturn(name).anyTimes();
            expect(s.getWorkspace()).andReturn(ws).anyTimes();
            expect(s.getCatalog()).andReturn(catalog).anyTimes();
            expect(s.isEnabled()).andReturn(true).anyTimes();

            expect(catalog.getStore(sId, clazz)).andReturn(s).anyTimes();
            expect(catalog.getStoreByName(name, clazz)).andReturn(s).anyTimes();
            expect(catalog.getStoreByName(ws.getName(), name, clazz)).andReturn(s).anyTimes();
            expect(catalog.getStoreByName(ws, name, clazz)).andReturn(s).anyTimes();
            
        }

        public MockCatalogBuilder featureType(String name) {
            return featureType(name, ProjectionPolicy.NONE, null, DEFAULT_LATLON_ENVELOPE);
        }
        
        public MockCatalogBuilder featureType(final String name, ProjectionPolicy projPolicy, 
            ReferencedEnvelope envelope, ReferencedEnvelope latLonEnvelope) {

            String ftId = newId();
            final DataStoreInfo ds = dataStores.peekLast();
            NamespaceInfo ns = namespaces.peekLast();

            FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
            featureTypes.add(ft);

            initResource(ft, FeatureTypeInfo.class, ftId, name, ds, ns, projPolicy, envelope, latLonEnvelope);

            expect(ft.getNumDecimals()).andReturn(8);

            //setup the property file data
            File propDir = new File(dataDirRoot, ds.getName());
            propDir.mkdirs();

            String fileName = name + ".properties";
            try {
                IOUtils.copy(getClass().getResourceAsStream(fileName), new File(propDir, fileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                expect(ft.getFeatureType()).andAnswer(new IAnswer<FeatureType>() {
                    @Override
                    public FeatureType answer() throws Throwable {
                        return ((DataStore)ds.getDataStore(null)).getSchema(name);
                    }
                }).anyTimes();
                expect(ft.getFeatureSource(null, null)).andAnswer((IAnswer)new IAnswer<FeatureSource>() {
                    @Override
                    public FeatureSource answer() throws Throwable {
                        return ((DataStore)ds.getDataStore(null)).getFeatureSource(name);
                    }
                }).anyTimes();
            } catch (IOException e) {
            }
            
            expect(catalog.getFeatureTypeByName(or(eq(name), eq(ns.getPrefix() + ":" + name))))
                .andReturn(ft).anyTimes();
            
            expect(catalog.getFeatureTypeByName(or(eq(new NameImpl(ns.getPrefix(), name)), 
                eq(new NameImpl(ns.getURI(), name))))).andReturn(ft).anyTimes();
            expect(catalog.getFeatureTypeByName(ns, name)).andReturn(ft).anyTimes();

            expect(catalog.getFeatureTypeByName(ns.getPrefix(), name))
                .andReturn(ft).anyTimes();
            //expect(catalog.getFeatureTypeByName(or(eq(ns.getPrefix()), eq(ns.getURI())), name))
            //    .andReturn(ft).anyTimes();

            expect(catalog.getFeatureTypeByStore(ds, name)).andReturn(ft).anyTimes();
            expect(catalog.getFeatureTypeByDataStore(ds, name)).andReturn(ft).anyTimes();

            replay(ft, createLayer(ft, name, ns));
            return this;
        }

        public MockCatalogBuilder coverage(QName qName) {

            String cId = newId();
            final CoverageStoreInfo cs = coverageStores.peekLast();
            NamespaceInfo ns = namespaces.peekLast();

            final String name = qName.getLocalPart();
            File dir = new File(dataDirRoot, name);
            dir.mkdir();

            String fileName = COVERAGES.get(qName)[0];
            try {
                IOUtils.copy(getClass().getResourceAsStream(fileName), new File(dir, fileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            //initialize the mock by actually building a real one first
            CatalogBuilder cb = new CatalogBuilder(new CatalogImpl());
            cb.setStore(cs);

            AbstractGridCoverage2DReader reader = cs.getFormat().getReader(cs.getURL());
            if (reader == null) {
                throw new RuntimeException("No reader for " + cs.getURL());
            }

            CoverageInfo real = null;
            try {
                real = cb.buildCoverage(reader, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            CoverageInfo c = createNiceMock(CoverageInfo.class);
            coverages.add(c);

            initResource(c, CoverageInfo.class, cId, name, cs, ns, real.getProjectionPolicy(), 
                real.getNativeBoundingBox(), real.getLatLonBoundingBox());

            expect(c.getDefaultInterpolationMethod()).andReturn(real.getDefaultInterpolationMethod())
                .anyTimes();
            expect(c.getDimensions()).andReturn(real.getDimensions()).anyTimes();
            expect(c.getGrid()).andReturn(real.getGrid()).anyTimes();

            expect(c.getInterpolationMethods()).andReturn(real.getInterpolationMethods()).anyTimes();
            expect(c.getRequestSRS()).andReturn(real.getRequestSRS()).anyTimes();
            expect(c.getResponseSRS()).andReturn(real.getResponseSRS()).anyTimes();

            try {
                expect(c.getGridCoverageReader(null, null)).andReturn(reader).anyTimes();
            } catch (IOException e) {
            }

            expect(catalog.getCoverageByName(or(eq(name), eq(ns.getPrefix() + ":" + name))))
                .andReturn(c).anyTimes();
            expect(catalog.getCoverageByName(or(eq(new NameImpl(ns.getPrefix(), name)), 
                eq(new NameImpl(ns.getURI(), name))))).andReturn(c).anyTimes();
            expect(catalog.getCoverageByName(ns, name)).andReturn(c).anyTimes();

            expect(catalog.getCoverageByName(ns.getPrefix(), name)).andReturn(c).anyTimes();
            //expect(catalog.getFeatureTypeByName(or(eq(ns.getPrefix()), eq(ns.getURI())), name))
            //    .andReturn(ft).anyTimes();

            //expect(catalog.getCoverageByStore(cs, name)).andReturn(c).anyTimes();
            expect(catalog.getCoverageByCoverageStore(cs, name)).andReturn(c).anyTimes();

            replay(c, createLayer(c, name, ns));
            return this;
        }

        <T extends ResourceInfo> void initResource(T r, Class<T> clazz, String rId, String name, StoreInfo s, NamespaceInfo ns, 
            ProjectionPolicy projPolicy, ReferencedEnvelope envelope, ReferencedEnvelope latLonEnvelope) {
            
            expect(r.getId()).andReturn(rId).anyTimes();
            expect(r.getName()).andReturn(name).anyTimes();
            expect(r.getQualifiedName()).andReturn(new NameImpl(ns.getURI(), name)).anyTimes();
            expect(r.getNativeName()).andReturn(name).anyTimes();
            expect(r.getQualifiedNativeName()).andReturn(new NameImpl(ns.getURI(), name)).anyTimes();
            expect(r.getTitle()).andReturn(name).anyTimes();
            expect(r.getAbstract()).andReturn("abstract about " + name).anyTimes();
            expect(r.getStore()).andReturn(s).anyTimes();
            expect(r.getNamespace()).andReturn(ns).anyTimes();

            Integer srs = SRS.get( name );
            if ( srs == null ) {
                srs = 4326;
            }
            expect(r.getSRS()).andReturn("EPSG:" + srs).anyTimes();
            try {
                expect(r.getNativeCRS()).andReturn(CRS.decode("EPSG:" + srs));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            expect(r.getKeywords()).andReturn((List)Arrays.asList(new Keyword(name))).anyTimes();
            expect(r.isEnabled()).andReturn(true).anyTimes();
            expect(r.isAdvertised()).andReturn(true).anyTimes();
            expect(r.getProjectionPolicy()).andReturn(projPolicy).anyTimes();
            expect(r.getLatLonBoundingBox()).andReturn(latLonEnvelope).anyTimes();;
            expect(r.getNativeBoundingBox()).andReturn(envelope).anyTimes();

            expect(catalog.getResource(rId, clazz)).andReturn(r).anyTimes();
            expect(catalog.getResourceByName(name, clazz)).andReturn(r).anyTimes();
            expect(catalog.getResourceByName(new NameImpl(ns.getPrefix(), name), clazz))
                .andReturn(r).anyTimes();
            expect(catalog.getResourceByName(
                new NameImpl(ns.getURI(), name), clazz)).andReturn(r).anyTimes();
            expect(catalog.getResourceByName(ns, name,clazz)).andReturn(r).andReturn(r).anyTimes();

            expect(catalog.getResourceByName(ns.getPrefix(), name,
                    clazz)).andReturn(r).anyTimes();
            //expect(catalog.getResourceByName(or(eq(ns.getPrefix()), eq(ns.getURI())), name,
            //    clazz)).andReturn(r).anyTimes();
            expect(catalog.getResourceByStore(s, name, clazz)).andReturn(r).anyTimes();
        }

        LayerInfo createLayer(ResourceInfo r, String name, NamespaceInfo ns) {
            String lId = newId();
            StyleInfo s = styles.peekLast();

            LayerInfo l = createNiceMock(LayerInfo.class);
            layers.add(l);

            expect(l.getId()).andReturn(lId).anyTimes();
            expect(l.getName()).andReturn(name).anyTimes();
            expect(l.getType()).andReturn(LayerInfo.Type.VECTOR).anyTimes();
            expect(l.getResource()).andReturn(r).anyTimes();
            expect(l.getDefaultStyle()).andReturn(s).anyTimes();
            expect(l.isEnabled()).andReturn(true).anyTimes();
            expect(l.isAdvertised()).andReturn(true).anyTimes();

            expect(catalog.getLayer(lId)).andReturn(l).anyTimes();
            expect(catalog.getLayerByName(name)).andReturn(l).anyTimes();
            expect(catalog.getLayerByName(ns.getPrefix() + ":" + name)).andReturn(l).anyTimes();
            expect(catalog.getLayerByName(new NameImpl(ns.getPrefix(), name))).andReturn(l).anyTimes();
            expect(catalog.getLayerByName(new NameImpl(ns.getURI(), name))).andReturn(l).anyTimes();

            return l;
        }

        public MockCatalogBuilder style(String name) {
            String filename = name + ".sld";
            if (getClass().getResourceAsStream(filename) == null) {
                return this;
            }
            
            String sId = newId();
            Version version = Styles.Handler.SLD_10.getVersion();

            StyleInfo s = createNiceMock(StyleInfo.class);
            styles.add(s);

            expect(s.getId()).andReturn(sId);
            expect(s.getName()).andReturn(name).anyTimes();
            expect(s.getFilename()).andReturn(filename).anyTimes();
            expect(s.getSLDVersion()).andReturn(version).anyTimes();
            try {
                expect(s.getStyle()).andReturn(Styles.style(Styles.parse(
                    getClass().getResourceAsStream(filename), version))).anyTimes();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
            
            expect(catalog.getStyle(sId)).andReturn(s).anyTimes();
            expect(catalog.getStyleByName(name)).andReturn(s).anyTimes();
            return this;
        }

        public MockCatalogBuilder commit() {
            if (!featureTypes.isEmpty() || !coverages.isEmpty()) {
                if (!featureTypes.isEmpty()) {
                    DataStoreInfo ds = dataStores.peekLast();
                    
                    expect(catalog.getResourcesByStore(ds, FeatureTypeInfo.class))
                        .andReturn(featureTypes).anyTimes();
                    expect(catalog.getFeatureTypesByDataStore(ds)).andReturn(featureTypes).anyTimes();
                }

                if (!coverages.isEmpty()) {
                    CoverageStoreInfo cs = coverageStores.peekLast();

                    expect(catalog.getResourcesByStore(cs, CoverageInfo.class))
                        .andReturn(coverages).anyTimes();
                    expect(catalog.getCoveragesByCoverageStore(cs)).andReturn(coverages).anyTimes();
                }

                //clear out local lists but push up to be included when this workspace is complete
                featureTypesByNamespace.addAll(featureTypes);
                featureTypes = new LinkedList<FeatureTypeInfo>();

                coveragesByNamespace.addAll(coverages);
                coverages = new LinkedList<CoverageInfo>();

            }
            else if (!dataStores.isEmpty() || !coverageStores.isEmpty()) {
                WorkspaceInfo ws = workspaces.peekLast();
                NamespaceInfo ns = namespaces.peekLast();
                
                expect(catalog.getStoresByWorkspace(ws.getName(), DataStoreInfo.class))
                    .andReturn(dataStores).anyTimes();
                expect(catalog.getStoresByWorkspace(ws, DataStoreInfo.class))
                    .andReturn(dataStores).anyTimes();
                expect(catalog.getDataStoresByWorkspace(ws.getName())).andReturn(dataStores).anyTimes();
                expect(catalog.getDataStoresByWorkspace(ws)).andReturn(dataStores).anyTimes();

                expect(catalog.getStoresByWorkspace(ws.getName(), CoverageStoreInfo.class))
                    .andReturn(coverageStores).anyTimes();
                expect(catalog.getStoresByWorkspace(ws, CoverageStoreInfo.class))
                    .andReturn(coverageStores).anyTimes();
                expect(catalog.getCoverageStoresByWorkspace(ws.getName())).andReturn(coverageStores).anyTimes();
                expect(catalog.getCoverageStoresByWorkspace(ws)).andReturn(coverageStores).anyTimes();

                List<StoreInfo> l = new LinkedList<StoreInfo>(dataStores);
                l.addAll(coverageStores);

                expect(catalog.getStoresByWorkspace(ws.getName(), StoreInfo.class)).andReturn(l).anyTimes();
                expect(catalog.getStoresByWorkspace(ws, StoreInfo.class)).andReturn(l).anyTimes();

                //add all the resources for this workspace
                List<ResourceInfo> m = new LinkedList(featureTypesByNamespace);
                m.addAll(coveragesByNamespace);
                expect(catalog.getResourcesByNamespace(ns, ResourceInfo.class)).andReturn(m).anyTimes();
                //expect(catalog.getResourcesByNamespace(ns.getPrefix(), ResourceInfo.class)).andReturn(m).anyTimes();
                expect(catalog.getResourcesByNamespace(ns, FeatureTypeInfo.class))
                    .andReturn(featureTypesByNamespace).anyTimes();
                //expect(catalog.getResourcesByNamespace(ns.getPrefix(), FeatureTypeInfo.class))
                //    .andReturn(featureTypesByNamespace).anyTimes();
                expect(catalog.getResourcesByNamespace(ns, CoverageInfo.class))
                    .andReturn(coveragesByNamespace).anyTimes();
                //expect(catalog.getResourcesByNamespace(ns.getPrefix(), CoverageInfo.class))
                //    .andReturn(coveragesByNamespace).anyTimes();

                dataStoresAll.addAll(dataStores);
                dataStores = new LinkedList();

                coverageStoresAll.addAll(coverageStores);
                coverageStores = new LinkedList();

                featureTypesAll.addAll(featureTypesByNamespace);
                featureTypesByNamespace = new LinkedList();

                coveragesAll.addAll(coveragesByNamespace);
                coveragesByNamespace = new LinkedList();
            }
            else if (!workspaces.isEmpty()) {

                //all the resources
                List<ResourceInfo> l = new LinkedList<ResourceInfo>(featureTypesAll);
                l.addAll(coveragesAll);
                expect(catalog.getResources(ResourceInfo.class)).andReturn(l).anyTimes();
                expect(catalog.getResources(FeatureTypeInfo.class)).andReturn(featureTypesAll).anyTimes();
                expect(catalog.getResources(CoverageInfo.class)).andReturn(coverages).anyTimes();
                expect(catalog.getFeatureTypes()).andReturn(featureTypesAll).anyTimes();
                expect(catalog.getCoverages()).andReturn(coveragesAll).anyTimes();
                
                //add all the stores
                List<StoreInfo> m = new LinkedList<StoreInfo>(dataStoresAll);
                m.addAll(coverageStoresAll);
                expect(catalog.getStores(StoreInfo.class)).andReturn(m).anyTimes();
                expect(catalog.getStores(DataStoreInfo.class)).andReturn(dataStoresAll).anyTimes();
                expect(catalog.getStores(CoverageStoreInfo.class)).andReturn(coverageStoresAll).anyTimes();

                //add all the styles
                expect(catalog.getStyles()).andReturn(styles).anyTimes();

                //add all the workspaces/namespaces
                expect(catalog.getWorkspaces()).andReturn(workspaces).anyTimes();
                expect(catalog.getNamespaces()).andReturn(namespaces).anyTimes();

                //default workspace/namespace
                expect(catalog.getDefaultWorkspace()).andReturn(workspaces.peekFirst()).anyTimes();
                expect(catalog.getDefaultNamespace()).andReturn(namespaces.peekFirst()).anyTimes();

                replay(catalog);

                featureTypesAll = new LinkedList();
                coveragesAll = new LinkedList();
                dataStoresAll = new LinkedList();
                coverageStoresAll = new LinkedList();
                styles = new LinkedList();
                workspaces = new LinkedList();
                namespaces = new LinkedList();
            }

            return this;
        }
        protected String newId() {
            return UUID.randomUUID().toString();
        }
    }
}
