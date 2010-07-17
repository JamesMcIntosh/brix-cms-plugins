package brix.demo.gallery.webdav;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.AbstractLocatorFactory;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.apache.wicket.Application;

import brix.Brix;
import brix.Plugin;
import brix.SessionAwarePlugin;
import brix.demo.web.WicketApplication;
import brix.jcr.base.BrixSession;
import brix.jcr.base.EventUtil;
import brix.plugin.gallery.webdav.GalleryRootLocatorFactory;

public class GalleryServlet extends SimpleWebdavServlet {

	private static final long serialVersionUID = 1L;
	private AbstractLocatorFactory locatorFactory;

	/**
	 * Constructor
	 */
	public GalleryServlet() {

	}

	/**
	 * Returns the <code>DavLocatorFactory</code>. If no locator factory has
	 * been set or created a new instance of
	 * {@link org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl} is
	 * returned.
	 * 
	 * @return the locator factory
	 * @see AbstractWebdavServlet#getLocatorFactory()
	 */
	@Override
	public DavLocatorFactory getLocatorFactory() {
		if (locatorFactory == null) {
			locatorFactory = new GalleryRootLocatorFactory(getPathPrefix());
		}
		return locatorFactory;

	}

	@Override
	public synchronized SessionProvider getSessionProvider() {
		final SessionProvider original = super.getSessionProvider();

		return new SessionProvider() {
			public Session getSession(HttpServletRequest request, Repository rep, String workspace)
					throws LoginException, ServletException, RepositoryException {

				final String key = Brix.NS_PREFIX + "jcr-session";
				BrixSession s = (BrixSession) request.getAttribute(key);
				if (s == null) {
					s = EventUtil.wrapSession(original.getSession(request, rep, workspace));
					for (Plugin p : getBrix().getPlugins()) {
						if (p instanceof SessionAwarePlugin) {
							((SessionAwarePlugin) p).onWebDavSession(s);
						}
					}
					request.setAttribute(key, s);
				}
				return s;
			}

			public void releaseSession(Session session) {
				original.releaseSession(EventUtil.unwrapSession(session));
			}
		};
	}

	private Brix getBrix() {
		WicketApplication app = (WicketApplication) Application.get("wicket.brix-demo");
		return app.getBrix();
	}

	@Override
	public Repository getRepository() {
		Application.getApplicationKeys();
		WicketApplication app = (WicketApplication) Application.get("wicket.brix-demo");
		return app.getRepository();
	}

}
