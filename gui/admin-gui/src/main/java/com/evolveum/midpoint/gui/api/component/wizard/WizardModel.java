/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardModel implements IClusterable {

    private static final long serialVersionUID = 1L;

    public static final String PARAM_STEP = "step";

    private List<WizardListener> wizardListeners = new ArrayList<>();

    private WizardPanel panel;

    private List<WizardStep> steps;
    private int activeStepIndex;

    public WizardModel(@NotNull List<WizardStep> steps) {
        this.steps = steps;
    }

    public void addWizardListener(@NotNull WizardListener listener) {
        wizardListeners.add(listener);
    }

    public void removeWizardListener(@NotNull WizardListener listener) {
        wizardListeners.remove(listener);
    }

    protected final void fireActiveStepChanged(final WizardStep step) {
        wizardListeners.forEach(listener -> listener.onStepChanged(step));
    }

    protected final void fireWizardCancelled() {
        wizardListeners.forEach(listener -> listener.onCancel());
    }

    protected final void fireWizardFinished() {
        wizardListeners.forEach(listener -> listener.onFinish());
    }

    public void init(Page page) {
        steps.forEach(s -> s.init(this));

        String stepId = getStepIdFromParams(page);
        if (stepId != null) {
            setActiveStepById(stepId);
        }

        fireActiveStepChanged(getActiveStep());
    }

    private String getStepIdFromParams(Page page) {
        if (page == null) {
            return null;
        }

        PageParameters params = page.getPageParameters();
        if (params == null) {
            return null;
        }

        StringValue step = params.get(PARAM_STEP);
        return step != null ? step.toString() : null;
    }

    public Component getPanel() {
        return panel;
    }

    public Component getHeader() {
        return panel.getHeader();
    }

    public void setPanel(WizardPanel panel) {
        this.panel = panel;
    }

    public List<WizardStep> getSteps() {
        return steps;
    }

    public WizardStep getActiveStep() {
        return steps.get(activeStepIndex);
    }

    public void setActiveStepById(String id) {
        if (id == null) {
            return;
        }

        for (int i = 0; i < steps.size(); i++) {
            WizardStep step = steps.get(i);

            if (Objects.equals(id, step.getStepId()) && BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                setActiveStepIndex(i);
                break;
            }
        }
    }

    public int getActiveStepIndex() {
        return activeStepIndex;
    }

    public int getActiveStepVisibleIndex() {
        int index = 0;
        for (int i = 0; i < activeStepIndex; i++) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                index++;
            }
        }
        return index;
    }

    private void setActiveStepIndex(int activeStepIndex) {
        if (activeStepIndex < 0) {
            return;
        }
        if (activeStepIndex >= steps.size()) {
            return;
        }

        this.activeStepIndex = activeStepIndex;
    }

    public void next() {
        setActiveStepIndex(activeStepIndex + 1);

        fireActiveStepChanged(getActiveStep());
    }

    public void previous() {
        setActiveStepIndex(activeStepIndex - 1);

        fireActiveStepChanged(getActiveStep());
    }

    public WizardStep getNextPanel() {
        int nextIndex = activeStepIndex + 1;
        if (steps.size() <= nextIndex) {
            return null;
        }

        return steps.get(nextIndex);
    }
}
