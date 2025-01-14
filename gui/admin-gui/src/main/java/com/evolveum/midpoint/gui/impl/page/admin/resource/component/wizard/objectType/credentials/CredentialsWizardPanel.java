/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceCredentialsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

@Experimental
public class CredentialsWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    public CredentialsWizardPanel(String id, ResourceWizardPanelHelper<ResourceObjectTypeDefinitionType> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createCredentialsSteps(getValueModel())))));
    }

    private List<WizardStep> createCredentialsSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        PasswordStepPanel panel = new PasswordStepPanel(
                getResourceModel(),
                PrismContainerWrapperModel.fromContainerValueWrapper(
                        valueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_CREDENTIALS,
                                ResourceCredentialsDefinitionType.F_PASSWORD))) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                CredentialsWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                if (!isSavedAfterWizard()) {
                    onExitPerformed(target);
                    return;
                }
                OperationResult result = CredentialsWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    WebComponentUtil.createToastForUpdateResource(target, this);
                    onExitPerformed(target);
                }
            }

            @Override
            protected IModel<String> getSubmitLabelModel() {
                if (isSavedAfterWizard()) {
                    return super.getSubmitLabelModel();
                }
                return getPageBase().createStringResource("WizardPanel.confirm");
            }
        };
        panel.setOutputMarkupId(true);
        steps.add(panel);

        return steps;
    }
}
