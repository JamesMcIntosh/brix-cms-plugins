/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package brix.plugin.article.articlenode.admin;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

import org.brixcms.jcr.wrapper.BrixNode;
import org.brixcms.web.BrixRequestCycleProcessor;
import org.brixcms.web.generic.BrixGenericWebMarkupContainer;
import org.brixcms.web.nodepage.BrixNodeRequestTarget;
import org.brixcms.web.nodepage.BrixPageParameters;

/**
 * @author wickeria at gmail.com
 */
public class PreviewArticleIFrame extends BrixGenericWebMarkupContainer<BrixNode> {

	private static final long serialVersionUID = 1L;

	public PreviewArticleIFrame(String id, IModel<BrixNode> model) {
		super(id, model);
		setOutputMarkupId(true);
	}

	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		tag.put("src", getUrl());
	}

	private CharSequence getUrl() {
		BrixPageParameters parameters = new BrixPageParameters();
		IModel<BrixNode> nodeModel = getModel();
		String workspace = nodeModel.getObject().getSession().getWorkspace().getName();
		parameters.setQueryParam(BrixRequestCycleProcessor.WORKSPACE_PARAM, workspace);
		StringBuilder url = new StringBuilder(getRequestCycle()
				.urlFor(new BrixNodeRequestTarget(nodeModel, parameters)));
		return Strings.replaceAll(url, "brix%3Aroot/Articles/", "");
	}
}
