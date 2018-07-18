/*
 * $Id: HttpServletContext.java, 2018年7月15日 下午9:56:53 XiuYu.Ge Exp $
 * 
 * Copyright (c) 2012 zzcode Technologies Co.,Ltd 
 * All rights reserved.
 * 
 * This software is copyrighted and owned by zzcode or the copyright holder
 * specified, unless otherwise noted, and may not be reproduced or distributed
 * in whole or in part in any form or medium without express written permission.
 */
package cn.zzcode.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.FileTypeMap;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.WebUtils;

/**
 * <p>
 * Title: HttpServletContext
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author XiuYu.Ge
 * @created 2018年7月15日 下午9:56:53
 * @modified [who date description]
 * @check [who date description]
 */
public class HttpServletContext implements ServletContext {

    private Map<String, String> classNameMap = new HashMap<String, String>();

    private Map<String, Servlet> servletMap = new HashMap<String, Servlet>();

    private Map<String, Class<? extends Servlet>> servletClassMap = new HashMap<String, Class<? extends Servlet>>();

    public Map<String, ServletRegistration> servletRegistrationMap = new HashMap<String, ServletRegistration>();

    public String getVirtualServerName() {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        classNameMap.put(servletName, className);
        HttpServletRegistration myServletRegistration = new HttpServletRegistration(this);
        servletRegistrationMap.put(servletName, myServletRegistration);
        return new HttpDynamic(myServletRegistration);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        servletMap.put(servletName, servlet);
        HttpServletRegistration myServletRegistration = new HttpServletRegistration(this);
        servletRegistrationMap.put(servletName, myServletRegistration);
        return new HttpDynamic(myServletRegistration);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        servletClassMap.put(servletName, servletClass);
        HttpServletRegistration myServletRegistration = new HttpServletRegistration(this);
        servletRegistrationMap.put(servletName, myServletRegistration);
        return new HttpDynamic(myServletRegistration);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return servletRegistrationMap.get(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return servletRegistrationMap;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        return;
    }

    @Override
    public void addListener(String className) {
        return;
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        return;
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> c) throws ServletException {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.emptyMap();
    }

    /**
     * Default Servlet name used by Tomcat, Jetty, JBoss, and GlassFish:
     * {@value}.
     */
    private static final String COMMON_DEFAULT_SERVLET_NAME = "default";

    private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";

    private static final Set<SessionTrackingMode> DEFAULT_SESSION_TRACKING_MODES = new LinkedHashSet<SessionTrackingMode>(
            3);

    static {
        DEFAULT_SESSION_TRACKING_MODES.add(SessionTrackingMode.COOKIE);
        DEFAULT_SESSION_TRACKING_MODES.add(SessionTrackingMode.URL);
        DEFAULT_SESSION_TRACKING_MODES.add(SessionTrackingMode.SSL);
    }

    private final Log logger = LogFactory.getLog(getClass());

    private final ResourceLoader resourceLoader;

    private final String resourceBasePath;

    private String contextPath = "";

    private final Map<String, ServletContext> contexts = new HashMap<String, ServletContext>();

    private int majorVersion = 3;

    private int minorVersion = 0;

    private int effectiveMajorVersion = 3;

    private int effectiveMinorVersion = 0;

    private final Map<String, RequestDispatcher> namedRequestDispatchers = new HashMap<String, RequestDispatcher>();

    private String defaultServletName = COMMON_DEFAULT_SERVLET_NAME;

    private final Map<String, String> initParameters = new LinkedHashMap<String, String>();

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    private String servletContextName = "MockServletContext";

    private final Set<String> declaredRoles = new LinkedHashSet<String>();

    private Set<SessionTrackingMode> sessionTrackingModes;

    // private final SessionCookieConfig sessionCookieConfig = new
    // MockSessionCookieConfig();

    /**
     * Create a new {@code MockServletContext}, using no base path and a
     * {@link DefaultResourceLoader} (i.e. the classpath root as WAR root).
     * 
     * @see org.springframework.core.io.DefaultResourceLoader
     */
    public HttpServletContext() {
        this("", null);
    }

    /**
     * Create a new {@code MockServletContext}, using a
     * {@link DefaultResourceLoader}.
     * 
     * @param resourceBasePath
     *            the root directory of the WAR (should not end with a slash)
     * @see org.springframework.core.io.DefaultResourceLoader
     */
    public HttpServletContext(String resourceBasePath) {
        this(resourceBasePath, null);
    }

    /**
     * Create a new {@code MockServletContext}, using the specified
     * {@link ResourceLoader} and no base path.
     * 
     * @param resourceLoader
     *            the ResourceLoader to use (or null for the default)
     */
    public HttpServletContext(ResourceLoader resourceLoader) {
        this("", resourceLoader);
    }

    /**
     * Create a new {@code MockServletContext} using the supplied resource base
     * path and resource loader.
     * <p>
     * Registers a {@link MockRequestDispatcher} for the Servlet named
     * {@literal 'default'}.
     * 
     * @param resourceBasePath
     *            the root directory of the WAR (should not end with a slash)
     * @param resourceLoader
     *            the ResourceLoader to use (or null for the default)
     * @see #registerNamedDispatcher
     */
    public HttpServletContext(String resourceBasePath, ResourceLoader resourceLoader) {
        this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
        this.resourceBasePath = (resourceBasePath != null ? resourceBasePath : "");

        // Use JVM temp dir as ServletContext temp dir.
        String tempDir = System.getProperty(TEMP_DIR_SYSTEM_PROPERTY);
        if (tempDir != null) {
            this.attributes.put(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File(tempDir));
        }

        registerNamedDispatcher(this.defaultServletName, new HttpRequestDispatcher(this.defaultServletName));
    }

    /**
     * Build a full resource location for the given path, prepending the
     * resource base path of this {@code MockServletContext}.
     * 
     * @param path
     *            the path as specified
     * @return the full resource path
     */
    protected String getResourceLocation(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return this.resourceBasePath + path;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = (contextPath != null ? contextPath : "");
    }

    @Override
    public String getContextPath() {
        return this.contextPath;
    }

    public void registerContext(String contextPath, ServletContext context) {
        this.contexts.put(contextPath, context);
    }

    @Override
    public ServletContext getContext(String contextPath) {
        if (this.contextPath.equals(contextPath)) {
            return this;
        }
        return this.contexts.get(contextPath);
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    @Override
    public int getMajorVersion() {
        return this.majorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    @Override
    public int getMinorVersion() {
        return this.minorVersion;
    }

    public void setEffectiveMajorVersion(int effectiveMajorVersion) {
        this.effectiveMajorVersion = effectiveMajorVersion;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return this.effectiveMajorVersion;
    }

    public void setEffectiveMinorVersion(int effectiveMinorVersion) {
        this.effectiveMinorVersion = effectiveMinorVersion;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return this.effectiveMinorVersion;
    }

    /**
     * This method uses the default
     * {@link javax.activation.FileTypeMap#getDefaultFileTypeMap() FileTypeMap}
     * from the Java Activation Framework to resolve MIME types.
     * <p>
     * The Java Activation Framework returns {@code "application/octet-stream"}
     * if the MIME type is unknown (i.e., it never returns {@code null}). Thus,
     * in order to honor the {@link ServletContext#getMimeType(String)}
     * contract, this method returns {@code null} if the MIME type is
     * {@code "application/octet-stream"}.
     * <p>
     * {@code MockServletContext} does not provide a direct mechanism for
     * setting a custom MIME type; however, if the default {@code FileTypeMap}
     * is an instance of {@code javax.activation.MimetypesFileTypeMap}, a custom
     * MIME type named {@code text/enigma} can be registered for a custom
     * {@code .puzzle} file extension in the following manner:
     * 
     * <pre style="code">
     * MimetypesFileTypeMap mimetypesFileTypeMap = (MimetypesFileTypeMap) FileTypeMap.getDefaultFileTypeMap();
     * mimetypesFileTypeMap.addMimeTypes("text/enigma    puzzle");
     * </pre>
     */
    @Override
    public String getMimeType(String filePath) {
        String mimeType = FileTypeMap.getDefaultFileTypeMap().getContentType(filePath);
        return ("application/octet-stream".equals(mimeType) ? null : mimeType);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        String actualPath = (path.endsWith("/") ? path : path + "/");
        Resource resource = this.resourceLoader.getResource(getResourceLocation(actualPath));
        try {
            File file = resource.getFile();
            String[] fileList = file.list();
            if (ObjectUtils.isEmpty(fileList)) {
                return null;
            }
            Set<String> resourcePaths = new LinkedHashSet<String>(fileList.length);
            for (String fileEntry : fileList) {
                String resultPath = actualPath + fileEntry;
                if (resource.createRelative(fileEntry).getFile().isDirectory()) {
                    resultPath += "/";
                }
                resourcePaths.add(resultPath);
            }
            return resourcePaths;
        } catch (IOException ex) {
            logger.warn("Couldn't get resource paths for " + resource, ex);
            return null;
        }
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
        if (!resource.exists()) {
            return null;
        }
        try {
            return resource.getURL();
        } catch (MalformedURLException ex) {
            throw ex;
        } catch (IOException ex) {
            logger.warn("Couldn't get URL for " + resource, ex);
            return null;
        }
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
        if (!resource.exists()) {
            return null;
        }
        try {
            return resource.getInputStream();
        } catch (IOException ex) {
            logger.warn("Couldn't open InputStream for " + resource, ex);
            return null;
        }
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("RequestDispatcher path at ServletContext level must start with '/'");
        }
        return new HttpRequestDispatcher(path);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String path) {
        return this.namedRequestDispatchers.get(path);
    }

    /**
     * Register a {@link RequestDispatcher} (typically a
     * {@link MockRequestDispatcher}) that acts as a wrapper for the named
     * Servlet.
     * 
     * @param name
     *            the name of the wrapped Servlet
     * @param requestDispatcher
     *            the dispatcher that wraps the named Servlet
     * @see #getNamedDispatcher
     * @see #unregisterNamedDispatcher
     */
    public void registerNamedDispatcher(String name, RequestDispatcher requestDispatcher) {
        Assert.notNull(name, "RequestDispatcher name must not be null");
        Assert.notNull(requestDispatcher, "RequestDispatcher must not be null");
        this.namedRequestDispatchers.put(name, requestDispatcher);
    }

    /**
     * Unregister the {@link RequestDispatcher} with the given name.
     * 
     * @param name
     *            the name of the dispatcher to unregister
     * @see #getNamedDispatcher
     * @see #registerNamedDispatcher
     */
    public void unregisterNamedDispatcher(String name) {
        Assert.notNull(name, "RequestDispatcher name must not be null");
        this.namedRequestDispatchers.remove(name);
    }

    /**
     * Get the name of the <em>default</em> {@code Servlet}.
     * <p>
     * Defaults to {@literal 'default'}.
     * 
     * @see #setDefaultServletName
     */
    public String getDefaultServletName() {
        return this.defaultServletName;
    }

    /**
     * Set the name of the <em>default</em> {@code Servlet}.
     * <p>
     * Also {@link #unregisterNamedDispatcher unregisters} the current default
     * {@link RequestDispatcher} and {@link #registerNamedDispatcher replaces}
     * it with a {@link MockRequestDispatcher} for the provided
     * {@code defaultServletName}.
     * 
     * @param defaultServletName
     *            the name of the <em>default</em> {@code Servlet}; never
     *            {@code null} or empty
     * @see #getDefaultServletName
     */
    public void setDefaultServletName(String defaultServletName) {
        Assert.hasText(defaultServletName, "defaultServletName must not be null or empty");
        unregisterNamedDispatcher(this.defaultServletName);
        this.defaultServletName = defaultServletName;
        registerNamedDispatcher(this.defaultServletName, new HttpRequestDispatcher(this.defaultServletName));
    }

    @Override
    @Deprecated
    public Servlet getServlet(String name) {
        return null;
    }

    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return Collections.enumeration(Collections.<Servlet> emptySet());
    }

    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        return Collections.enumeration(Collections.<String> emptySet());
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    @Deprecated
    public void log(Exception ex, String message) {
        logger.info(message, ex);
    }

    @Override
    public void log(String message, Throwable ex) {
        logger.info(message, ex);
    }

    @Override
    public String getRealPath(String path) {
        Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
        try {
            return resource.getFile().getAbsolutePath();
        } catch (IOException ex) {
            logger.warn("Couldn't determine real path of resource " + resource, ex);
            return null;
        }
    }

    @Override
    public String getServerInfo() {
        return "MockServletContext";
    }

    @Override
    public String getInitParameter(String name) {
        Assert.notNull(name, "Parameter name must not be null");
        return this.initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        Assert.notNull(name, "Parameter name must not be null");
        if (this.initParameters.containsKey(name)) {
            return false;
        }
        this.initParameters.put(name, value);
        return true;
    }

    public void addInitParameter(String name, String value) {
        Assert.notNull(name, "Parameter name must not be null");
        this.initParameters.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        Assert.notNull(name, "Attribute name must not be null");
        return this.attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
    }

    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "Attribute name must not be null");
        if (value != null) {
            this.attributes.put(name, value);
        } else {
            this.attributes.remove(name);
        }
    }

    @Override
    public void removeAttribute(String name) {
        Assert.notNull(name, "Attribute name must not be null");
        this.attributes.remove(name);
    }

    public void setServletContextName(String servletContextName) {
        this.servletContextName = servletContextName;
    }

    @Override
    public String getServletContextName() {
        return this.servletContextName;
    }

    @Override
    public ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        Assert.notNull(roleNames, "Role names array must not be null");
        for (String roleName : roleNames) {
            Assert.hasLength(roleName, "Role name must not be empty");
            this.declaredRoles.add(roleName);
        }
    }

    public Set<String> getDeclaredRoles() {
        return Collections.unmodifiableSet(this.declaredRoles);
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes)
            throws IllegalStateException, IllegalArgumentException {
        this.sessionTrackingModes = sessionTrackingModes;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return DEFAULT_SESSION_TRACKING_MODES;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return (this.sessionTrackingModes != null ? Collections.unmodifiableSet(this.sessionTrackingModes)
                : DEFAULT_SESSION_TRACKING_MODES);
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#addJspFile(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Dynamic addJspFile(String servletName, String jspFile) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getSessionTimeout()
     */
    @Override
    public int getSessionTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see javax.servlet.ServletContext#setSessionTimeout(int)
     */
    @Override
    public void setSessionTimeout(int sessionTimeout) {
        // TODO Auto-generated method stub

    }

    /**
     * @see javax.servlet.ServletContext#getRequestCharacterEncoding()
     */
    @Override
    public String getRequestCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#setRequestCharacterEncoding(java.lang.String)
     */
    @Override
    public void setRequestCharacterEncoding(String encoding) {
        // TODO Auto-generated method stub

    }

    /**
     * @see javax.servlet.ServletContext#getResponseCharacterEncoding()
     */
    @Override
    public String getResponseCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#setResponseCharacterEncoding(java.lang.String)
     */
    @Override
    public void setResponseCharacterEncoding(String encoding) {
        // TODO Auto-generated method stub

    }

    // ---------------------------------------------------------------------
    // Unsupported Servlet 3.0 registration methods
    // ---------------------------------------------------------------------

}
