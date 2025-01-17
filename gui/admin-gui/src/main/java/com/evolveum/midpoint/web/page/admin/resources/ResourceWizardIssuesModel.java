/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.wizard.resource.dto.WizardIssuesDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.jetbrains.annotations.NotNull;

public class ResourceWizardIssuesModel extends NonEmptyLoadableModel<WizardIssuesDto> {

    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel;
    @NotNull private final PageResourceWizard wizardPage;

    ResourceWizardIssuesModel(@NotNull NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel, @NotNull PageResourceWizard wizardPage) {
        super(false);
        this.resourceModel = resourceModel;
        this.wizardPage = wizardPage;
    }

    @NotNull
    @Override
    protected WizardIssuesDto load() {
        final WizardIssuesDto issuesDto = new WizardIssuesDto();
        if (!resourceModel.isLoaded()) {
            return issuesDto;        // e.g. in first two wizard steps (IT PROBABLY DOES NOT WORK AS EXPECTED)
        }
        ResourceValidator validator = wizardPage.getResourceValidator();
        ValidationResult validationResult = validator
                .validate(resourceModel.getObject(), Scope.QUICK, WebComponentUtil.getCurrentLocale(), wizardPage.createSimpleTask("validate"), new OperationResult("validate"));

        issuesDto.fillFrom(validationResult);
        issuesDto.sortIssues();
        return issuesDto;
    }

}
