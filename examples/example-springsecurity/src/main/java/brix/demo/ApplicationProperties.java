package brix.demo;

import org.brixcms.demo.util.FileUtils;
import org.brixcms.demo.util.PropertyUtils;
import org.brixcms.demo.util.PropertyUtils.MergeMode;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.util.Properties;

/**
 * Application-wide configuration settings for Brix Demo Application
 *
 * @author igor.vaynberg
 */
public class ApplicationProperties {
// ------------------------------ FIELDS ------------------------------

    private final Properties properties;

// --------------------------- CONSTRUCTORS ---------------------------

    public ApplicationProperties() {
        // load base properties
        String baseProperties = "brix/demo/application.properties";
        Properties base = PropertyUtils.loadFromClassPath(baseProperties, false);

        // load user-specific property overrides
        String username = System.getProperty("user.name");
        String userProperties = "brix/demo/application." + username + ".properties";
        Properties user = PropertyUtils.loadFromClassPath(userProperties, false);

        // load system properties
        Properties system = System.getProperties();

        // merge properties
        properties = PropertyUtils.merge(MergeMode.OVERRIDE_ONLY, base, user, system);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * @return jcr {@link Credentials} built from username and password
     */

    public Credentials buildSimpleCredentials() {
        return new SimpleCredentials(getJcrLogin(), getJcrPassword().toString().toCharArray());
    }

    /**
     * @return jcr login name
     */
    public String getJcrLogin() {
        return properties.getProperty("brixdemo.jcr.login");
    }

    /**
     * @return http port the server is using
     */
    public int getHttpPort() {
        return Integer.parseInt(properties.getProperty("brixdemo.httpPort"));
    }

    /**
     * @return https port the server is using
     */
    public int getHttpsPort() {
        return Integer.parseInt(properties.getProperty("brixdemo.httpsPort"));
    }

    /**
     * @return jcr default workspace
     */

    public String getJcrDefaultWorkspace() {
        return properties.getProperty("brixdemo.jcr.defaultWorkspace");
    }

    /**
     * @return jcr login password
     */
    public String getJcrPassword() {
        return properties.getProperty("brixdemo.jcr.password");
    }

    /**
     * @return jcr repository url
     */
    public String getJcrRepositoryUrl() {
        String url = properties.getProperty("brixdemo.jcr.url");
        if (url == null || url.trim().length() == 0) {
            // if no url was specified generate a unique temporary one
            url = "file://" + FileUtils.getDefaultRepositoryFileName();
            properties.setProperty("brixdemo.jcr.url", url);
        }
        return url;
    }

    public String getWorkspaceDefaultState() {
        return properties.getProperty("brixdemo.jcr.defaultWorkspaceState");
    }

    /**
     * @return workspace manager url
     */
    public String getWorkspaceManagerUrl() {
        return properties.getProperty("brixdemo.workspaceManagerUrl");
    }
}
