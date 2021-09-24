/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;
import static com.evolveum.midpoint.schema.result.OperationResult.DEFAULT;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.*;

/**
 * Loads the full resource object for a projection context.
 *
 * Note that full object can be loaded also in {@link ProjectionUpdateOperation}.
 */
public class ProjectionFullLoadOperation<F extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionFullLoadOperation.class);

    // Backwards-compatible name
    private static final String OP_LOAD_FULL_SHADOW = ContextLoader.class.getName() + "." + "loadFullShadow";

    @NotNull private final LensContext<F> context;
    @NotNull private final LensProjectionContext projCtx;
    @NotNull private final String reason;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans;

    private FullShadowLoadedTraceType trace;

    public ProjectionFullLoadOperation(
            @NotNull LensContext<F> context,
            @NotNull LensProjectionContext projCtx,
            @NotNull String reason,
            @NotNull Task task) {
        this.context = context;
        this.projCtx = projCtx;
        this.reason = reason;
        this.task = task;
        this.beans = ModelBeans.get();
    }

    public void loadFullShadow(OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (shouldSkipLoading()) {
            return;
        }

        OperationResult result = parentResult.subresult(OP_LOAD_FULL_SHADOW)
                .setMinor()
                .addParam("context", String.valueOf(projCtx))
                .addParam("reason", reason)
                .build();
        createTraceIfNeeded(result);

        try {
            String oid = projCtx.getOid();

            if (projCtx.isHigherOrder()) {
                // It may be just too early to load the projection
                if (LensUtil.hasLowerOrderContext(context, projCtx) && context.getExecutionWave() < projCtx.getWave()) {
                    // We cannot reliably load the context now
                    result.addReturn(DEFAULT, "too early");
                    return;
                }
            }

            Collection<SelectorOptions<GetOperationOptions>> options = createOptions();
            try {
                if (oid == null) {
                    throw new IllegalStateException("Trying to load shadow with null OID (reason for load: " + reason + ") for "
                            + projCtx.getHumanReadableName());
                }
                PrismObject<ShadowType> objectCurrent = beans.provisioningService
                        .getObject(ShadowType.class, oid, options, task, result);
                Validate.notNull(objectCurrent.getOid());
                if (trace != null) {
                    trace.setShadowLoadedRef(ObjectTypeUtil.createObjectRefWithFullObject(objectCurrent, beans.prismContext));
                }
                projCtx.setCurrentObject(objectCurrent);
                projCtx.determineFullShadowFlag(objectCurrent);
                if (ShadowUtil.isExists(objectCurrent.asObjectable()) || isInMaintenance(projCtx.getResource())) {
                    result.addReturn(DEFAULT, "found");
                } else {
                    LOGGER.debug("Load of full resource object {} ended with non-existent shadow (options={})", projCtx, options);
                    projCtx.setExists(false);
                    refreshContextAfterShadowNotFound(options, result);
                    result.addReturn(DEFAULT, "not found");
                }

            } catch (ObjectNotFoundException ex) {
                LOGGER.debug("Load of full resource object {} ended with ObjectNotFoundException (options={})", projCtx, options);
                result.muteLastSubresultError();
                projCtx.setShadowExistsInRepo(false);
                refreshContextAfterShadowNotFound(options, result);
                result.addReturn(DEFAULT, "not found");
            }

            projCtx.recompute();

            LOGGER.trace("Loaded full resource object:\n{}", projCtx.debugDumpLazily(1));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            if (trace != null) {
                if (result.isTracingNormal(FullShadowLoadedTraceType.class)) {
                    trace.setOutputLensContextText(context.debugDump());
                }
                trace.setOutputLensContext(context.toLensContextType(getExportType(trace, result)));
            }
            result.computeStatusIfUnknown();
        }
    }

    private void refreshContextAfterShadowNotFound(Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        new MissingShadowContextRefresher<>(context, projCtx, options, task)
                .refresh(result);
    }

    @NotNull
    private Collection<SelectorOptions<GetOperationOptions>> createOptions() throws SchemaException {
        GetOperationOptions getOptions = GetOperationOptions.createAllowNotFound();
        //getOptions.setReadOnly(true);
        getOptions.setPointInTimeType(PointInTimeType.FUTURE);
        if (projCtx.isDoReconciliation()) {
            getOptions.setForceRefresh(true);
        }
        if (SchemaConstants.CHANNEL_DISCOVERY_URI.equals(context.getChannel())) {
            LOGGER.trace("Loading full resource object {} from provisioning - with doNotDiscover to avoid loops; reason: {}",
                    projCtx, reason);
            // Avoid discovery loops
            getOptions.setDoNotDiscovery(true);
        } else {
            LOGGER.trace("Loading full resource object {} from provisioning (discovery enabled), reason: {}, channel: {}",
                    projCtx, reason, context.getChannel());
        }
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(getOptions);
        addRetrievePasswordIfNeeded(options);
        return options;
    }

    private void createTraceIfNeeded(OperationResult result) throws SchemaException {
        if (result.isTracingAny(FullShadowLoadedTraceType.class)) {
            trace = new FullShadowLoadedTraceType(beans.prismContext);
            if (result.isTracingNormal(FullShadowLoadedTraceType.class)) {
                trace.setInputLensContextText(context.debugDump());
                ResourceType resource = projCtx.getResource();
                PolyStringType name = resource != null ? resource.getName() : null;
                trace.setResourceName(name != null ? name : PolyStringType.fromOrig(projCtx.getResourceOid()));
            }
            trace.setInputLensContext(context.toLensContextType(getExportType(trace, result)));
            trace.setReason(reason);
            result.addTrace(trace);
        } else {
            trace = null;
        }
    }

    private boolean shouldSkipLoading() {
        if (projCtx.isFullShadow()) {
            LOGGER.trace("Skipping loading full shadow: The shadow is already loaded.");
            return true;
        }
        if (projCtx.isTombstone()) {
            // loading is futile
            LOGGER.trace("Skipping loading full shadow: The shadow is tombstone.");
            return true;
        }

        if (projCtx.getOid() == null) {
            if (projCtx.isAdd()) {
                LOGGER.trace("Skipping loading full shadow: Nothing to load yet (oid=null, isAdd).");
                return true;
            }
            if (projCtx.getWave() > context.getExecutionWave()) {
                LOGGER.trace("Skipping loading full shadow: oid=null, and wave is greater than current one:"
                        + " will be dealt with later.");
                return true;
            }
            if (projCtx.getWave() == context.getExecutionWave() && projCtx.getSynchronizationPolicyDecision() == null) {
                LOGGER.trace("Skipping loading full shadow: oid=null, and wave is current but no sync policy decision "
                        + "(activation was not run yet, probably will be created later)");
                return true;
            }
        }
        return false;
    }

    private void addRetrievePasswordIfNeeded(Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
        if (!LensUtil.isPasswordReturnedByDefault(projCtx)
                && LensUtil.needsFullShadowForCredentialProcessing(projCtx)) {
            options.add(
                    SelectorOptions.create(
                            beans.prismContext.toUniformPath(SchemaConstants.PATH_PASSWORD_VALUE),
                            GetOperationOptions.createRetrieve()));
        }
    }
}
