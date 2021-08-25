/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Controller
public class AdminGuiConfigurationMergeManagerImpl implements AdminGuiConfigurationMergeManager {

    @Autowired ArchetypeManager archetypeManager;

    @Override
    public List<ContainerPanelConfigurationType> mergeContainerPanelConfigurationType(List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> configuredPanels) {
        List<ContainerPanelConfigurationType> mergedPanels = new ArrayList<>(defaultPanels);
        for (ContainerPanelConfigurationType configuredPanel : configuredPanels) {
            mergePanelConfigurations(configuredPanel, defaultPanels, mergedPanels);
        }
        return mergedPanels;
    }

    @Override
    public GuiObjectDetailsPageType mergeObjectDetailsPageConfiguration(GuiObjectDetailsPageType defaultPageConfiguration, List<ObjectReferenceType> archetypes, OperationResult result) {
        GuiObjectDetailsPageType mergedPageConfiguration = defaultPageConfiguration.cloneWithoutId();
        for (ObjectReferenceType archetypeRef : archetypes) {
            ArchetypeType archetypeType = null;
            try {
                archetypeType = archetypeManager.getArchetype(archetypeRef.getOid(), result).asObjectable();
            } catch (ObjectNotFoundException | SchemaException e) {
                //TODO only log excpetion
            }

            mergeObjectDetailsPageConfiguration(mergedPageConfiguration, archetypeType);
        }
        return mergedPageConfiguration;
    }

    private void mergeObjectDetailsPageConfiguration(GuiObjectDetailsPageType defaultPageConfiguration, ArchetypeType archetypeType) {
        if (archetypeType == null) {
            return;
        }

        ArchetypePolicyType archetypePolicyType = archetypeType.getArchetypePolicy();
        if (archetypePolicyType == null) {
            return;
        }

        ArchetypeAdminGuiConfigurationType archetypeAdminGuiConfigurationType = archetypePolicyType.getAdminGuiConfiguration();
        if (archetypeAdminGuiConfigurationType == null) {
            return;
        }

        GuiObjectDetailsPageType archetypePageConfiguration = archetypeAdminGuiConfigurationType.getObjectDetails();
        if (archetypePageConfiguration == null) {
            return;
        }

        mergeObjectDetailsPageConfiguration(defaultPageConfiguration, archetypePageConfiguration);
    }

    @Override
    public void mergeObjectDetailsPageConfiguration(GuiObjectDetailsPageType defaultPageConfiguration, GuiObjectDetailsPageType compiledPageType) {
        if (compiledPageType == null) {
            return;
        }
        List<ContainerPanelConfigurationType> mergedPanels = mergeContainerPanelConfigurationType(defaultPageConfiguration.getPanel(), compiledPageType.getPanel());
        defaultPageConfiguration.getPanel().clear();
        defaultPageConfiguration.getPanel().addAll(CloneUtil.cloneCollectionMembersWithoutIds(mergedPanels));
    }

    private void mergePanelConfigurations(ContainerPanelConfigurationType configuredPanel, List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> mergedPanels) {
        for (ContainerPanelConfigurationType defaultPanel : defaultPanels) {
            if (defaultPanel.getIdentifier().equals(configuredPanel.getIdentifier())) {
                mergePanels(defaultPanel, configuredPanel);
                return;
            }
        }
        mergedPanels.add(configuredPanel.cloneWithoutId());
    }

    private void mergePanels(ContainerPanelConfigurationType mergedPanel, ContainerPanelConfigurationType configuredPanel) {
        if (configuredPanel.getPanelType() != null) {
            mergedPanel.setPanelType(configuredPanel.getPanelType());
        }

        DisplayType mergedDisplayType = mergeDisplayType(configuredPanel.getDisplay(), mergedPanel.getDisplay());
        mergedPanel.setDisplay(mergedDisplayType);

        if (configuredPanel.getPath() != null) {
            mergedPanel.setPath(configuredPanel.getPath());
        }

        if (configuredPanel.getListView() != null) {
            mergedPanel.setListView(configuredPanel.getListView().cloneWithoutId());
        }

        if (!configuredPanel.getContainer().isEmpty()) {
            List<VirtualContainersSpecificationType> virtualContainers = mergeVirtualContainers(configuredPanel.getContainer(), mergedPanel.getContainer());
            mergedPanel.getContainer().clear();
            mergedPanel.getContainer().addAll(virtualContainers);
        }

        if (configuredPanel.getType() != null) {
            mergedPanel.setType(configuredPanel.getType());
        }

        if (configuredPanel.getVisibility() != null) {
            mergedPanel.setVisibility(configuredPanel.getVisibility());
        }

        if (!configuredPanel.getPanel().isEmpty()) {
            List<ContainerPanelConfigurationType> mergedConfigs = mergeContainerPanelConfigurationType(mergedPanel.getPanel(), configuredPanel.getPanel());
            mergedPanel.getPanel().clear();
            mergedPanel.getPanel().addAll(mergedConfigs);
        }
    }

    public List<VirtualContainersSpecificationType> mergeVirtualContainers(GuiObjectDetailsPageType currentObjectDetails, GuiObjectDetailsPageType superObjectDetails) {
        return mergeContainers(currentObjectDetails.getContainer(), superObjectDetails.getContainer(),
                this::createVirtualContainersPredicate, this::mergeVirtualContainer);
    }

    private List<VirtualContainersSpecificationType> mergeVirtualContainers(List<VirtualContainersSpecificationType> currentVirtualContainers, List<VirtualContainersSpecificationType> superObjectDetails) {
        return mergeContainers(currentVirtualContainers, superObjectDetails,
                this::createVirtualContainersPredicate, this::mergeVirtualContainer);
    }

    private Predicate<VirtualContainersSpecificationType> createVirtualContainersPredicate(VirtualContainersSpecificationType superContainer) {
        return c -> identifiersMatch(c.getIdentifier(), superContainer.getIdentifier()) || pathsMatch(superContainer.getPath(), c.getPath());
    }

    public <C extends Containerable> List<C> mergeContainers(List<C> currentContainers, List<C> superContainers, Function<C, Predicate<C>> predicate, BiFunction<C, C, C> mergeFunction) {
        if (currentContainers.isEmpty()) {
            if (superContainers.isEmpty()) {
                return Collections.emptyList();
            }
            return superContainers.stream().map(this::cloneComplex).collect(Collectors.toList());
        }

        if (superContainers.isEmpty()) {
            return currentContainers.stream().map(this::cloneComplex).collect(Collectors.toList());
        }

        List<C> mergedContainers = new ArrayList<>();
        for (C superContainer : superContainers) {
            C matchedContainer = find(predicate.apply(superContainer), currentContainers);
            if (matchedContainer != null) {
                C mergedContainer = mergeFunction.apply(matchedContainer, superContainer);
                mergedContainers.add(mergedContainer);
            } else {
                mergedContainers.add(cloneComplex(superContainer));
            }
        }

        for (C currentContainer : currentContainers) {
            if (!findAny(predicate.apply(currentContainer), mergedContainers)) {
                mergedContainers.add(cloneComplex(currentContainer));
            }
        }

        return mergedContainers;
    }

    private <C extends Containerable> C find(Predicate<C> predicate, List<C> currentContainers) {
        List<C> matchedContainers = currentContainers.stream()
                .filter(predicate)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(matchedContainers)) {
            return null;
        }

        if (matchedContainers.size() > 1) {
            throw new IllegalStateException("Cannot merge virtual containers. More containers with same identifier specified.");
        }

        return matchedContainers.iterator().next();
    }

    private <C extends Containerable> boolean findAny(Predicate<C> predicate, List<C> mergedContainers) {
        return mergedContainers.stream().anyMatch(predicate);
    }


    private boolean identifiersMatch(String id1, String id2) {
        return id1 != null && id1.equals(id2);
    }

    private VirtualContainersSpecificationType mergeVirtualContainer(VirtualContainersSpecificationType currentContainer, VirtualContainersSpecificationType superContainer) {
        VirtualContainersSpecificationType mergedContainer = currentContainer.clone();
        if (currentContainer.getDescription() == null) {
            mergedContainer.setDescription(superContainer.getDescription());
        }

        DisplayType mergedDisplayType = mergeDisplayType(currentContainer.getDisplay(), superContainer.getDisplay());
        mergedContainer.setDisplay(mergedDisplayType);

        if (currentContainer.getDisplayOrder() == null) {
            mergedContainer.setDisplayOrder(superContainer.getDisplayOrder());
        }

        if (currentContainer.getVisibility() == null) {
            mergedContainer.setVisibility(superContainer.getVisibility());
        }

        for (VirtualContainerItemSpecificationType virtualItem : superContainer.getItem()) {
            if (currentContainer.getItem().stream().noneMatch(i -> pathsMatch(i.getPath(), virtualItem.getPath()))) {
                mergedContainer.getItem().add(cloneComplex(virtualItem));
            }
        }

        return mergedContainer;
    }

    private <C extends Containerable> C cloneComplex(C containerable) {
        return containerable.cloneWithoutId();
    }

    public DisplayType mergeDisplayType(DisplayType currentDisplayType, DisplayType superDisplayType) {
        if (currentDisplayType == null) {
            if (superDisplayType == null) {
                return null;
            }
            return superDisplayType.clone();
        }

        if (superDisplayType == null) {
            return currentDisplayType.clone();
        }

        DisplayType mergedDisplayType = currentDisplayType.clone();
        if (currentDisplayType.getLabel() == null) {
            mergedDisplayType.setLabel(superDisplayType.getLabel());
        }

        if (currentDisplayType.getColor() == null) {
            mergedDisplayType.setColor(superDisplayType.getColor());
        }

        if (currentDisplayType.getCssClass() == null) {
            mergedDisplayType.setCssClass(superDisplayType.getCssClass());
        }

        if (currentDisplayType.getCssStyle() == null) {
            mergedDisplayType.setCssStyle(superDisplayType.getCssStyle());
        }

        if (currentDisplayType.getHelp() == null) {
            mergedDisplayType.setHelp(superDisplayType.getHelp());
        }

        IconType mergedIcon = mergeIcon(currentDisplayType.getIcon(), superDisplayType.getIcon());
        mergedDisplayType.setIcon(mergedIcon);

        if (currentDisplayType.getPluralLabel() == null) {
            mergedDisplayType.setPluralLabel(superDisplayType.getPluralLabel());
        }

        if (currentDisplayType.getSingularLabel() == null) {
            mergedDisplayType.setSingularLabel(superDisplayType.getSingularLabel());
        }

        if (currentDisplayType.getTooltip() == null) {
            mergedDisplayType.setTooltip(superDisplayType.getTooltip());
        }

        return mergedDisplayType;
    }

    private IconType mergeIcon(IconType currentIcon, IconType superIcon) {
        if (currentIcon == null) {
            if (superIcon == null) {
                return null;
            }
            return superIcon.clone();
        }

        if (superIcon == null) {
            return currentIcon.clone();
        }

        IconType mergedIcon = currentIcon.clone();
        if (currentIcon.getCssClass() == null) {
            mergedIcon.setCssClass(superIcon.getCssClass());
        }

        if (currentIcon.getColor() == null) {
            mergedIcon.setColor(superIcon.getColor());
        }

        if (currentIcon.getImageUrl() == null) {
            mergedIcon.setImageUrl(superIcon.getImageUrl());
        }

        return mergedIcon;
    }

    private boolean pathsMatch(ItemPathType supperPath, ItemPathType currentPath) {
        return supperPath != null && currentPath != null && supperPath.equivalent(currentPath);
    }

}