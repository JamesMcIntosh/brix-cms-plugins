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

package brix.plugin.article.web.tile.message;

import org.brixcms.auth.Action.Context;
import org.brixcms.jcr.api.JcrNode;
import org.brixcms.jcr.api.JcrNodeIterator;
import org.brixcms.jcr.wrapper.BrixNode;
import brix.plugin.article.ArticlePlugin;
import brix.plugin.file.web.ConfirmAjaxCallDecorator;
import org.brixcms.web.generic.BrixGenericPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.convert.IConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author wickeria at gmail.com
 */
public class MessageListPanel extends BrixGenericPanel<BrixNode> {
    private static final long serialVersionUID = 1L;

    private boolean isDiscussion;

    public MessageListPanel(String id, IModel<BrixNode> model, final boolean isDiscussion) {
        super(id, model);
        this.isDiscussion = isDiscussion;
        setOutputMarkupId(true);
        final int messagesPerPage = isDiscussion ? Integer.MAX_VALUE : 10;
        final PageableListView<Message> listView = new PageableListView<Message>("entries", new EntriesModel(), messagesPerPage) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<Message> getListItemModel(IModel<? extends List<Message>> listViewModel, int index) {
                return new CompoundPropertyModel<Message>(super.getListItemModel(listViewModel, index));
            }

            @Override
            protected void populateItem(ListItem<Message> item) {
                item.add(new Label("index"));
                item.add(new Label("name"));
                item.add(new Label("title"));
                item.add(new Label("message"));
                item.add(new Label("timestamp") {
                    @Override
                    public IConverter getConverter(Class<?> type) {
                        return new PatternDateConverter("dd.MM.yyyy HH:mm:ss", false);
                    }
                });
                item.add(new AjaxLink<String>("nodeId") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new ConfirmAjaxCallDecorator();
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        BrixNode brixNode = (BrixNode) MessageListPanel.this.getDefaultModelObject();
                        brixNode.getSession().getNodeByIdentifier(getDefaultModelObjectAsString()).remove();
                        brixNode.save();
                        findParent(PageableListView.class).setModel(new EntriesModel());
                        target.addComponent(MessageListPanel.this);
                    }

                    @Override
                    public boolean isVisible() {
                        BrixNode brixNode = (BrixNode) MessageListPanel.this.getDefaultModelObject();
                        BrixNode node = (BrixNode) brixNode.getSession().getNodeByIdentifier(getDefaultModelObjectAsString());
                        return ArticlePlugin.get().canDeleteNode(node, Context.PRESENTATION);
                    }
                });
                item.add(new Label("email") {
                    @Override
                    public boolean isVisible() {
                        //only admin can see email
                        BrixNode brixNode = (BrixNode) MessageListPanel.this.getDefaultModelObject();
                        return ArticlePlugin.get().canDeleteNode(brixNode, Context.PRESENTATION);
                    }
                });
            }

        };
        add(listView);
        add(new AjaxPagingNavigator("navigator", listView) {
            @Override
            public boolean isVisible() {
                JcrNode tile = (JcrNode) MessageListPanel.this.getModelObject();
                JcrNodeIterator entryNodes = tile.getNodes("entry");
                return entryNodes.getSize() > messagesPerPage;
            }
        });
    }

    private class EntriesModel extends LoadableDetachableModel<List<Message>> {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<Message> load() {
            JcrNode tile = (JcrNode) getDefaultModelObject();
            JcrNodeIterator entryNodes = tile.getNodes("entry");
            ArrayList<Message> entries = new ArrayList<Message>((int) entryNodes.getSize());

            while (entryNodes.hasNext()) {
                JcrNode entryNode = entryNodes.nextNode();
                Message message = new Message();
                message.setIndex((int) entryNode.getProperty("index").getLong());
                message.setName(entryNode.getProperty("name").getString());
                message.setMessage(entryNode.getProperty("message").getString());
                if (entryNode.hasProperty("timestamp")) {
                    message.setTimestamp(new Date(entryNode.getProperty("timestamp").getLong()));
                }
                if (entryNode.hasProperty("email")) {
                    message.setEmail(entryNode.getProperty("email").getString());
                }
                if (entryNode.hasProperty("title")) {
                    message.setTitle(entryNode.getProperty("title").getString());
                }
                message.setNodeId(entryNode.getIdentifier());
                entries.add(message);
            }

            if (isDiscussion) {
                return entries;
            } else {
                Collections.reverse(entries);
                return entries;
            }
        }

    }

}
