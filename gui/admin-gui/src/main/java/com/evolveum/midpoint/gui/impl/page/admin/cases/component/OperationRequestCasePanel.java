/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.wf.api.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by honchar
 */
@PanelType(name = "operationRequestCase")
@PanelInstance(identifier = "operationRequestCase",
        display = @PanelDisplay(label = "PageCase.operationRequestTab", order = 1))
public class OperationRequestCasePanel extends AbstractObjectMainPanel<CaseType, AssignmentHolderDetailsModel<CaseType>> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = OperationRequestCasePanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(OperationRequestCasePanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";

    private static final String ID_REQUEST_DETAILS_PANELS = "requestDetailsPanels";
    private static final String ID_OPERATIONAL_REQUEST_CASE_PANEL = "operationRequestCasePanel";
    private IModel<List<SceneDto>> sceneModel;

    public OperationRequestCasePanel(String id, AssignmentHolderDetailsModel objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
        initModels(); //TODO: should be in CaseDetailsModel???
    }

    private void initModels(){
        sceneModel = new LoadableModel<List<SceneDto>>(false) {
            @Override
            protected List<SceneDto> load() {
                PageBase pageBase = OperationRequestCasePanel.this.getPageBase();

                CaseType caseObject =  getObjectWrapper().getObject().asObjectable();
                OperationResult result = new OperationResult(OPERATION_PREPARE_DELTA_VISUALIZATION);
                Task task = pageBase.createSimpleTask(OPERATION_PREPARE_DELTA_VISUALIZATION);
                try {
                    ChangesByState<?> changesByState = pageBase.getApprovalsManager().getChangesByState(caseObject,
                            pageBase.getModelInteractionService(), pageBase.getPrismContext(), task, result);
                    List<SceneDto> sceneDtoList = WebComponentUtil.computeChangesCategorizationList(changesByState, caseObject.getObjectRef(),
                             pageBase.getModelInteractionService(), pageBase.getPrismContext(), task, result);
                    return sceneDtoList;
                } catch (Exception ex){
                    LOGGER.error("Couldn't prepare delta visualization: {}", ex.getLocalizedMessage());
                }
                return null;
            }
        };
    }


    protected void initLayout(){
        ListView<SceneDto> requestDetailsPanels = new ListView<SceneDto>(ID_REQUEST_DETAILS_PANELS, sceneModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                ScenePanel scenePanel = new ScenePanel(ID_OPERATIONAL_REQUEST_CASE_PANEL, item.getModel());
                scenePanel.setOutputMarkupId(true);
                item.add(scenePanel);
            }
        };
        requestDetailsPanels.setOutputMarkupId(true);
        add(requestDetailsPanels);
    }

}
