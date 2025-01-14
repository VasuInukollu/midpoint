/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

/**
 * Throw-away helper for matching target reference in constraints.
 * It encapsulates the common state for a single constraint evaluation.
 * Filter - if used - is evaluated lazily and only once.
 */
public class ConstraintReferenceMatcher<AH extends AssignmentHolderType> {

    private final Trace logger;

    private final PolicyRuleEvaluationContext<AH> evalContext;
    private final ExpressionFactory expressionFactory;
    private final OperationResult operationResult;

    /** Reference defined in the exclusion or other constraint. We match it against the object. */
    @Nullable private final ObjectReferenceType targetReference;

    private ObjectFilter filter; // lazily initialized

    public ConstraintReferenceMatcher(
            @NotNull PolicyRuleEvaluationContext<AH> evalContext,
            @Nullable ObjectReferenceType targetReference,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull OperationResult operationResult,
            @NotNull Trace logger) {
        this.evalContext = evalContext;
        this.targetReference = targetReference;
        this.expressionFactory = expressionFactory;
        this.operationResult = operationResult;
        this.logger = logger;
    }

    /**
     * @param object Object we want to match against the reference.
     */
    public boolean refMatchesTarget(PrismObject<?> object, String context)
            throws SchemaException {
        if (targetReference == null) {
            // this means we rely on comparing relations (represented by order constraints in exclusion case)
            return true;
        }
        if (object.getOid() == null) {
            logger.warn("OID-less object to be matched against reference in a constraint: {}", object);
            return false;
        }
        if (targetReference.getOid() != null) {
            return object.getOid().equals(targetReference.getOid());
        }
        if (targetReference.getResolutionTime() == EvaluationTimeType.RUN) {
            return filterMatches(object, context);
        }
        throw new SchemaException("Neither OID nor filter in " + context);
    }

    private boolean filterMatches(PrismObject<?> object, String contextDescription)
            throws SchemaException {
        assert targetReference != null;
        QName typeNameLookingFor = MoreObjects.firstNonNull(targetReference.getType(), ObjectType.COMPLEX_TYPE);
        PrismObjectDefinition<?> objectDefLookingFor = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByType(typeNameLookingFor);
        if (!object.canRepresent(objectDefLookingFor.getCompileTimeClass())) {
            return false;
        }
        SearchFilterType filterBean = MiscUtil.requireNonNull(
                targetReference.getFilter(), () -> "No filter in " + contextDescription);
        if (filter == null) {
            filter = PrismContext.get().getQueryConverter().parseFilter(filterBean, objectDefLookingFor);

            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_POLICY_RULE, evalContext.policyRule, EvaluatedPolicyRule.class);
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                    new ModelExpressionEnvironment<>(evalContext.lensContext, null, evalContext.task, operationResult));
            try {
                filter = ExpressionUtil.evaluateFilterExpressions(filter,
                        variables, MiscSchemaUtil.getExpressionProfile(),
                        expressionFactory, expressionFactory.getPrismContext(), contextDescription,
                        evalContext.task, operationResult);
            } catch (ObjectNotFoundException | SecurityViolationException | ConfigurationException
                    | CommunicationException | ExpressionEvaluationException e) {
                throw new SystemException("Error occurred during expression evaluation", e);
            } finally {
                ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            }
        }
        return filter.match(object.getValue(), SchemaService.get().matchingRuleRegistry());
    }
}
