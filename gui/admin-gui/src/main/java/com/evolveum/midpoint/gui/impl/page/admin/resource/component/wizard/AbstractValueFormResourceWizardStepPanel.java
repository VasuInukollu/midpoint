/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.*;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
public abstract class AbstractValueFormResourceWizardStepPanel<C extends Containerable> extends AbstractResourceWizardStepPanel {

    private static final String ID_VALUE = "value";
    private final IModel<PrismContainerValueWrapper<C>> newValueModel;

    public AbstractValueFormResourceWizardStepPanel(
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<C>> newValueModel) {
        super(model);
        this.newValueModel = newValueModel;
        newValueModel.getObject().setExpanded(true);
        newValueModel.getObject().setShowEmpty(true);
    }

    protected static <C extends Containerable, T extends Containerable> IModel<PrismContainerValueWrapper<C>> createNewValueModel(
            IModel<PrismContainerValueWrapper<T>> parentValue, ItemName itemName) {
        LoadableDetachableModel<PrismContainerValueWrapper<C>> model = new LoadableDetachableModel<>() {

            @Override
            protected PrismContainerValueWrapper<C> load() {
                PrismContainerWrapperModel<T, C> model
                        = PrismContainerWrapperModel.fromContainerValueWrapper(
                        parentValue,
                        itemName);
                return model.getObject().getValues().get(0);
            }
        };
        return model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler()).build();
        settings.setConfig(getContainerConfiguration());
        VerticalFormPrismContainerValuePanel panel
                = new VerticalFormPrismContainerValuePanel(ID_VALUE, newValueModel, settings){

            @Override
            protected WebMarkupContainer createHeaderPanel() {
                return super.createHeaderPanel();
            }

            @Override
            protected LoadableDetachableModel<String> getLabelModel() {
                return (LoadableDetachableModel<String>) getTitle();
            }

            @Override
            protected String getIcon() {
                return AbstractValueFormResourceWizardStepPanel.this.getIcon();
            }
        };
        add(panel);
    }

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return WebComponentUtil.getContainerConfiguration(
                getResourceModel().getObjectDetailsPageConfiguration().getObject(), getPanelType());
    }

    protected abstract String getPanelType();

    protected ItemVisibilityHandler getVisibilityHandler() {
        return null;
    }

    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        getValuePanel().visitChildren(VerticalFormPrismPropertyValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismPropertyValuePanel) component).updateFeedbackPanel(target);
        });
        getValuePanel().visitChildren(VerticalFormPrismReferenceValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismReferenceValuePanel) component).updateFeedbackPanel(target);
        });
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    private VerticalFormDefaultContainerablePanel getValuePanel() {
        return (VerticalFormDefaultContainerablePanel) get(ID_VALUE);
    }

    protected IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return newValueModel;
    }
}