/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangeExecutor;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations.AssociationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.session.ObjectDetailsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.page.admin.resources.ResourceSummaryPanel;

import org.jetbrains.annotations.Nullable;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resource")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        label = "PageAdminResources.auth.resourcesAll.label",
                        description = "PageAdminResources.auth.resourcesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                        label = "PageResource.auth.resource.label",
                        description = "PageResource.auth.resource.description")
        })
public class PageResource extends PageAssignmentHolderDetails<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_WIZARD = "wizard";
    private List<Breadcrumb> wizardBreadcrumbs = new ArrayList<>();


    public PageResource(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageResource(PrismObject<ResourceType> resource) {
        super(resource);
    }

    @Override
    public Class<ResourceType> getType() {
        return ResourceType.class;
    }

    protected boolean isApplicableTemplate() {
        return true;
    }

    protected WebMarkupContainer createTemplatePanel(String id) {
        return new ResourceWizardPanel(id, getObjectDetailsModels()) {

            @Override
            protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                OperationResult result = new OperationResult(OPERATION_SAVE);
                saveOrPreviewPerformed(target, result, false);
                return result;
            }
        };
    }

    @Override
    protected void showResultAfterExecuteChanges(ObjectChangeExecutor changeExecutor, OperationResult result) {
        if (changeExecutor instanceof ObjectChangesExecutorImpl
                && (isEditObject() || !result.isSuccess())) {
            showResult(result);
        }
    }

    @Override
    protected void postProcessResult(OperationResult result, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, AjaxRequestTarget target) {
        if (isEditObject()) {
            super.postProcessResult(result, executedDeltas, target);
            return;
        }
        if (!result.isError()) {
            if (executedDeltas != null) {
                String resourceOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
                if (resourceOid != null) {
                    Task task = createSimpleTask("load resource after save");
                    @Nullable PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(
                            ResourceType.class,
                            resourceOid,
                            PageResource.this,
                            task,
                            task.getResult());
                    if (resource != null) {
                        getObjectDetailsModels().reset();
                        getObjectDetailsModels().reloadPrismObjectModel(resource);
                    }
                }
            }

            result.computeStatusIfUnknown();
        } else {
            target.add(getFeedbackPanel());
        }
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ResourceType> summaryModel) {
        return new ResourceSummaryPanel(id,
                summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected ResourceOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> wrapperModel) {
        return new ResourceOperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected void refreshStatus(AjaxRequestTarget target) {
                target.add(PageResource.this.get(ID_DETAILS_VIEW));
                PageResource.this.refresh(target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageResource.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageResource.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected ResourceDetailsModel createObjectDetailsModels(PrismObject<ResourceType> object) {
        return new ResourceDetailsModel(createPrismObjectModel(object), this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return getOperationOptionsBuilder()
                .noFetch()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
    }

    public void showSynchronizationWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, SynchronizationWizardPanel.class);
    }

    public void showCorrelationWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, CorrelationWizardPanel.class);
    }

    public void showCapabilitiesWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, CapabilitiesWizardPanel.class);
    }

    public void showCredentialsWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, CredentialsWizardPanel.class);
    }

    public void showActivationsWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, ActivationsWizardPanel.class);
    }

    public void showAssociationsWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, AssociationsWizardPanel.class);
    }

    public void showObjectTypeWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, ResourceObjectTypeWizardPanel.class);
    }

    public void showAttributeMappingWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showResourceWizard(target, valueModel, AttributeMappingWizardPanel.class);
    }

    private void showResourceWizard(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            Class<? extends AbstractResourceWizardPanel> clazz) {
        Fragment fragment = new Fragment(ID_DETAILS_VIEW, ID_WIZARD_FRAGMENT, PageResource.this);
        fragment.setOutputMarkupId(true);
        addOrReplace(fragment);

        try {
            Constructor<? extends AbstractResourceWizardPanel> constructor = clazz.getConstructor(String.class, ResourceWizardPanelHelper.class);
            AbstractResourceWizardPanel wizard = constructor.newInstance(ID_WIZARD, createResourceWizardPanelHelper(valueModel));
            wizard.setOutputMarkupId(true);
            fragment.add(wizard);
            target.add(fragment);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Couldn't create panel by constructor for class " + clazz.getSimpleName()
                    + " with parameters type: String, ResourceWizardPanelHelper");
        }
    }

    private ResourceWizardPanelHelper createResourceWizardPanelHelper(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        return new ResourceWizardPanelHelper(getObjectDetailsModels(), valueModel) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                backToDetailsFromWizard(target);
            }

            @Override
            public boolean isSavedAfterWizard() {
                return false;
            }
        };
    }

    private void backToDetailsFromWizard(AjaxRequestTarget target) {
        //TODO change it and use parameter, when it will be implemented
        ObjectDetailsStorage storage =
                getSessionStorage().getObjectDetailsStorage("details" + ResourceType.class.getSimpleName());
        ContainerPanelConfigurationType defaultConfig = null;
        if (storage != null) {
            defaultConfig = storage.getDefaultConfiguration();
        }
        DetailsFragment detailsFragment = createDetailsFragment();
        PageResource.this.addOrReplace(detailsFragment);
        if (defaultConfig != null) {
            replacePanel(defaultConfig, target);
        }
        target.add(detailsFragment);
    }

    public List<Breadcrumb> getWizardBreadcrumbs() {
        return wizardBreadcrumbs;
    }

    @Override
    protected void recordNoChangesWarning(OperationResult result) {
        if (isEditObject()) {
            super.recordNoChangesWarning(result);
        } else {
            result.recordSuccess();
        }
    }

    @Override
    protected void showResultNoChangesWarning(OperationResult result) {
        if (isEditObject()) {
            super.showResultNoChangesWarning(result);
        }
    }
}
