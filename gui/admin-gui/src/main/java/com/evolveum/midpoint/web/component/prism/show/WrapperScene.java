/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Artificial implementation of a scene used to hold a list of deltas.
 * (A bit of hack, unfortunately.)
 */
public class WrapperScene implements Visualization {

    private String displayNameKey;
    private Object[] displayNameParameters;
    private List<? extends Visualization> partialScenes;

    public WrapperScene(List<? extends Visualization> partialScenes, String displayNameKey, Object... displayNameParameters) {
        this.partialScenes = partialScenes;
        this.displayNameKey = displayNameKey;
        this.displayNameParameters = displayNameParameters;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public Object[] getDisplayNameParameters() {
        return displayNameParameters;
    }

    @Override
    public Name getName() {
        return new Name() {

            @Override
            public String getSimpleName() {
                return "";
            }

            @Override
            public String getDisplayName() {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public boolean namesAreResourceKeys() {
                return false;
            }
        };
    }

    @Override
    public ChangeType getChangeType() {
        return null;
    }

    @NotNull
    @Override
    public List<? extends Visualization> getPartialVisualizations() {
        return partialScenes;
    }

    @NotNull
    @Override
    public List<? extends VisualizationItem> getItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public Visualization getOwner() {
        return null;
    }

    @Override
    public ItemPath getSourceRelPath() {
        return null;
    }

    @Override
    public ItemPath getSourceAbsPath() {
        return null;
    }

    @Override
    public PrismContainerValue<?> getSourceValue() {
        return null;
    }

    @Override
    public PrismContainerDefinition<?> getSourceDefinition() {
        return null;
    }

    @Override
    public ObjectDelta<?> getSourceDelta() {
        return null;
    }

    @Override
    public boolean isBroken() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        if (partialScenes == null) {
            return true;
        }
        for (Visualization scene : partialScenes) {
            if (!scene.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(partialScenes, indent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WrapperScene that = (WrapperScene) o;

        if (displayNameKey != null ? !displayNameKey.equals(that.displayNameKey) : that.displayNameKey != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(displayNameParameters, that.displayNameParameters)) return false;
        return !(partialScenes != null ? !partialScenes.equals(that.partialScenes) : that.partialScenes != null);

    }

    @Override
    public int hashCode() {
        int result = displayNameKey != null ? displayNameKey.hashCode() : 0;
        result = 31 * result + (displayNameParameters != null ? Arrays.hashCode(displayNameParameters) : 0);
        result = 31 * result + (partialScenes != null ? partialScenes.hashCode() : 0);
        return result;
    }
}
