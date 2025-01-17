/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import org.jetbrains.annotations.Nullable;

public enum Capability {

    ACTIVATION(ActivationCapabilityType.class, "Activation", "activation"),
    CREDENTIALS(CredentialsCapabilityType.class, "Credentials", "credentials"),
    TEST_CONNECTION(TestConnectionCapabilityType.class, "Test connection", "testConnection"),
    READ(ReadCapabilityType.class, "Read", "read"),
    CREATE(CreateCapabilityType.class, "Create", "create"),
    UPDATE(UpdateCapabilityType.class, "Update", "update"),
    DELETE(DeleteCapabilityType.class, "Delete", "delete"),
    LIVE_SYNC(LiveSyncCapabilityType.class, "Live sync", "liveSync"),
    SCRIPT(ScriptCapabilityType.class, "Script", "script"),
    //PAGED_SEARCH(PagedSearchCapabilityType.class, "Paged search", "pagedSearch"),                    EXPERIMENTAL
    //COUNT_OBJECTS(CountObjectsCapabilityType.class, "Count objects", "countObjects"),                EXPERIMENTAL
    AUXILIARY_OBJECT_CLASSES(AuxiliaryObjectClassesCapabilityType.class, "Auxiliary object classes", "auxiliaryObjectClasses"),
    ADD_REMOVE_ATTRIBUTE_VALUES(AddRemoveAttributeValuesCapabilityType.class, "Add/remove attribute values", "addRemoveAttributeValues");

    private Class<? extends CapabilityType> clazz;
    private String displayName;
    private String resourceKey;

    Capability(Class<? extends CapabilityType> clazz, String displayName, String resourceKey) {
        this.clazz = clazz;
        this.displayName = displayName;
        this.resourceKey = resourceKey;
    }

    public Class<? extends CapabilityType> getClazz() {
        return clazz;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    @Nullable
    public static String getDisplayNameForClass(Class<? extends CapabilityType> clazz) {
        Capability cap = getByClass(clazz);
        return cap != null ? cap.getDisplayName() : null;
    }

    @Nullable
    private static Capability getByClass(Class<? extends CapabilityType> clazz) {
        for (Capability cap : Capability.values()) {
            if (cap.clazz.equals(clazz)) {
                return cap;
            }
        }
        return null;
    }

    @Nullable
    public static String getResourceKeyForClass(Class<? extends CapabilityType> clazz) {
        Capability cap = getByClass(clazz);
        return cap != null ? cap.getResourceKey() : null;
    }

    public static boolean supports(Class<? extends CapabilityType> clazz) {
        return getByClass(clazz) != null;
    }

    public CapabilityType newInstance() {
        try {
            return clazz.newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            throw new SystemException("Couldn't instantiate " + clazz, e);
        }
    }
}
