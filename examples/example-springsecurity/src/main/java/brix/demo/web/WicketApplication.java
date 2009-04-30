package brix.demo.web;

import brix.Brix;
import brix.Path;
import brix.config.BrixConfig;
import brix.config.PrefixUriMapper;
import brix.config.UriMapper;
import brix.demo.model.Member;
import brix.demo.model.Role;
import brix.demo.service.UserDAO;
import brix.demo.web.admin.AdminPage;
import brix.demo.web.auth.LogoutPage;
import brix.jcr.JcrSessionFactory;
import brix.jcr.api.JcrSession;
import brix.plugin.site.SitePlugin;
import brix.web.BrixRequestCycleProcessor;
import brix.web.nodepage.BrixNodePageUrlCodingStrategy;
import brix.workspace.Workspace;
import brix.workspace.WorkspaceManager;
import org.apache.wicket.Page;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ImportUUIDBehavior;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start class.
 *
 * @see wicket.myproject.Start#main(String[])
 */
public final class WicketApplication extends AbstractWicketApplication {
// ------------------------------ FIELDS ------------------------------

    private static final Logger log = LoggerFactory.getLogger(WicketApplication.class);

    /**
     * brix instance
     */
    private Brix brix;
    private UserDAO userDAO;

// --------------------- GETTER / SETTER METHODS ---------------------

    public Brix getBrix() {
        return brix;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Page> getHomePage() {
        // use special class so that the URL coding strategy knows we want to go home
        // it is not possible to just return null here because some pages (e.g. expired page)
        // rely on knowing the home page
        return BrixNodePageUrlCodingStrategy.HomePage.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() {
        addComponentInstantiationListener(new SpringComponentInjector(this));
        
        super.init();

        final JcrSessionFactory sf = getJcrSessionFactory();
        final WorkspaceManager wm = getWorkspaceManager();

        try {
            // create uri mapper for the cms
            // we are mounting the cms on the root, and getting the workspace name from the
            // application properties
            UriMapper mapper = new PrefixUriMapper(Path.ROOT) {
                public Workspace getWorkspaceForRequest(WebRequestCycle requestCycle, Brix brix) {
                    final String name = getProperties().getJcrDefaultWorkspace();
                    SitePlugin sitePlugin = SitePlugin.get(brix);
                    return sitePlugin.getSiteWorkspace(name, getProperties().getWorkspaceDefaultState());
                }
            };

            // create brix configuration
            BrixConfig config = new BrixConfig(sf, wm, mapper);
            config.setHttpPort(getProperties().getHttpPort());
            config.setHttpsPort(getProperties().getHttpsPort());

            // create brix instance and attach it to this application
            brix = new DemoBrix(config);
            brix.attachTo(this);
            initializeRepository();
            initDefaultWorkspace();
            createInitialUsers();
        }
        catch (Throwable e) {
            log.error("Exception in WicketApplication init()", e);
        }
        finally {
            // since we accessed session factory we also have to perform cleanup
            cleanupSessionFactory();
        }

        // mount admin page
        mount(new QueryStringHybridUrlCodingStrategy("/admin", AdminPage.class));
        mount(new QueryStringHybridUrlCodingStrategy("/logout", LogoutPage.class));

        // FIXME matej: do we need this?
        // mountBookmarkablePage("/NotFound", ResourceNotFoundPage.class);
        // mountBookmarkablePage("/Forbiden", ForbiddenPage.class);
    }

    /**
     * Allow Brix to perform repository initialization
     */
    private void initializeRepository() {
        try {
            brix.initRepository();
        }
        finally {
            // cleanup any sessions we might have created
            cleanupSessionFactory();
        }
    }

    private void initDefaultWorkspace() {
        try {
            final String defaultState = getProperties().getWorkspaceDefaultState();
            final String wn = getProperties().getJcrDefaultWorkspace();
            final SitePlugin sp = SitePlugin.get(brix);


            if (!sp.siteExists(wn, defaultState)) {
                Workspace w = sp.createSite(wn, defaultState);
                JcrSession session = brix.getCurrentSession(w.getId());

                session.importXML("/", getClass().getResourceAsStream("workspace.xml"), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);

                brix.initWorkspace(w, session);

                session.save();
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not initialize jackrabbit workspace with Brix", e);
        }
    }

    private void createInitialUsers() {
        Role superuser = new Role("ROLE_SUPERUSER");
        Role editor = new Role("ROLE_EDITOR");
        userDAO.saveOrUpdate(superuser);
        userDAO.saveOrUpdate(editor);
        Member member = new Member("sa", "sa", superuser, editor);
        userDAO.saveOrUpdate(member);
        member = new Member("editor", "editor", editor);
        userDAO.saveOrUpdate(member);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IRequestCycleProcessor newRequestCycleProcessor() {
        /*
         * install brix request cycle processor
         * 
         * this will allow brix to take over part of wicket's url space and handle requests
         */
        return new BrixRequestCycleProcessor(brix);
    }
}
