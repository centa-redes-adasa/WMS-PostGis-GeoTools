package org.geoserver.security.jdbc;

import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASSNAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASS_NOT_FOUND_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JNDINAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.USERNAME_REQUIRED;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidatorTest;
import org.geotools.util.logging.Logging;

public class JdbcSecurityConfigValidatorTest extends SecurityConfigValidatorTest  {

    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");
    
    
    @Override
    protected SecurityUserGroupServiceConfig createUGConfig(String name, Class<?> aClass,
            String encoder, String policyName) {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setPasswordEncoderName(encoder);
        config.setPasswordPolicyName(policyName);
        config.setCreatingTables(false);
        return config;
    }
    
    @Override
    protected SecurityRoleServiceConfig createRoleConfig(String name, Class<?> aClass,String adminRole) {
        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setAdminRoleName(adminRole);
        config.setCreatingTables(false);
        return config;
    }
    
    @Override
    protected SecurityAuthProviderConfig createAuthConfig(String name, Class<?> aClass,String userGroupServiceName) {
        JDBCConnectAuthProviderConfig config = new JDBCConnectAuthProviderConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setUserGroupServiceName(userGroupServiceName);        
        return config;
    }

    @Override
    public void testRoleConfig() throws IOException {
        
        super.testRoleConfig();
        
        JDBCRoleServiceConfig  config = 
                (JDBCRoleServiceConfig)createRoleConfig("jdbc", JDBCRoleService.class, 
                AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        
        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        
        JDBCRoleServiceConfig  configJNDI = (JDBCRoleServiceConfig) 
                createRoleConfig("jndi", JDBCRoleService.class, 
                		AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        configJNDI.setJndi(true);
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        
        boolean fail;

        
        
        fail=false;
        try {            
            configJNDI.setJndiName("");
            getSecurityManager().saveRoleService(configJNDI);
        } catch (SecurityConfigException ex) {
            assert( JNDINAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        fail=false;
        try {            
            config.setDriverClassName("");
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            assert( DRIVER_CLASSNAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setDriverClassName("a.b.c");
        fail=false;
        try {            
            config.setUserName("");
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assert( USERNAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setUserName("user");
        fail=false;
        try {            
            config.setConnectURL(null);
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assert( JDBCURL_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);
        
        config.setConnectURL("jdbc:connect");
        try {            
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assert( DRIVER_CLASS_NOT_FOUND_$1.equals(ex.getId()));
            assert("a.b.c".equals(ex.getArgs()[0]));
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);
        
        config.setDriverClassName("java.lang.String");
                
        config.setPropertyFileNameDDL(null);
        try {
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }
        config.setPropertyFileNameDML(null);
        try {            
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            assert( DML_FILE_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveRoleService(config);
            } catch (SecurityConfigException ex) {
                assert(DDL_FILE_INVALID.equals( ex.getId()));
                assert(invalidPath.equals( ex.getArgs()[0]));
                fail=true;
            }
            assert(fail);
        }
 
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
 
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDML(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveRoleService(config);
            } catch (SecurityConfigException ex) {
                assert(DML_FILE_INVALID.equals( ex.getId()));
                assert(invalidPath.equals( ex.getArgs()[0]));
                fail=true;
            }
            assert(fail);
        }

        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        
        try {
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            assert(DDL_FILE_REQUIRED.equals( ex.getId()));
            assert(0== ex.getArgs().length);
            fail=true;
        }
        assert(fail);


    }

    @Override
    public void testUserGroupConfig() throws IOException {

        super.testUserGroupConfig();
        
        JDBCUserGroupServiceConfig  config = 
                (JDBCUserGroupServiceConfig)createUGConfig("jdbc", JDBCUserGroupService.class,
                getPlainTextPasswordEncoder().getName() ,PasswordValidator.DEFAULT_NAME);

        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);


        JDBCUserGroupServiceConfig  configJNDI = (JDBCUserGroupServiceConfig) 
                createUGConfig("jdbc", JDBCUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME);
        configJNDI.setJndi(true);                        
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        
        boolean fail;

        
        
        fail=false;
        try {            
            configJNDI.setJndiName("");
            getSecurityManager().saveUserGroupService(configJNDI);                                     
        } catch (SecurityConfigException ex) {
            assert(JNDINAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        fail=false;
        try {            
            config.setDriverClassName("");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assert( DRIVER_CLASSNAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setDriverClassName("a.b.c");
        fail=false;
        try {            
            config.setUserName("");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assert( USERNAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setUserName("user");
        fail=false;
        try {            
            config.setConnectURL(null);
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assert( JDBCURL_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);
        
        config.setConnectURL("jdbc:connect");
        try {            
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assert( DRIVER_CLASS_NOT_FOUND_$1.equals(ex.getId()));
            assert("a.b.c".equals(ex.getArgs()[0]));
            LOGGER.info(ex.getMessage());
            fail=true;
        }

        config.setDriverClassName("java.lang.String");
        
        config.setPropertyFileNameDDL(null);
        try {
            getSecurityManager().saveUserGroupService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }
        config.setPropertyFileNameDML(null);
        try {            
            getSecurityManager().saveUserGroupService(config);
        } catch (SecurityConfigException ex) {
            assert( DML_FILE_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveUserGroupService(config);
            } catch (SecurityConfigException ex) {
                assert(DDL_FILE_INVALID.equals( ex.getId()));
                assert(invalidPath.equals( ex.getArgs()[0]));
                fail=true;
            }
            assert(fail);
        }
 
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
 
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDML(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveUserGroupService(config);
            } catch (SecurityConfigException ex) {
                assert(DML_FILE_INVALID.equals( ex.getId()));
                assert(invalidPath.equals( ex.getArgs()[0]));
                fail=true;
            }
            assert(fail);
        }

        
        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        
        try {
            getSecurityManager().saveUserGroupService(config);
        } catch (SecurityConfigException ex) {
            assert(DDL_FILE_REQUIRED.equals( ex.getId()));
            assert(0== ex.getArgs().length);
            fail=true;
        }
        assert(fail);        
    }

    @Override
    public void testAuthenticationProvider() throws IOException {
        super.testAuthenticationProvider();
        JDBCConnectAuthProviderConfig config = 
                (JDBCConnectAuthProviderConfig) createAuthConfig("jdbcprov", JDBCConnectAuthProvider.class, "default");
        
        config.setConnectURL("jdbc:connect");
        
        boolean fail=false;
        try {            
            config.setDriverClassName("");
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assert( DRIVER_CLASSNAME_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

        config.setDriverClassName("a.b.c");
        fail=false;
        try {            
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assert( DRIVER_CLASS_NOT_FOUND_$1.equals(ex.getId()));
            assert("a.b.c".equals(ex.getArgs()[0]));
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);
        
        fail=false;
        try {            
            config.setConnectURL(null);
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assert( JDBCURL_REQUIRED.equals(ex.getId()));
            assert(0==ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assert(fail);

    }

}
