/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.objectReferenceListToPrismReferenceValues;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Finds link targets based on a collection of selectors.
 */
class LinkTargetFinder implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(LinkTargetFinder.class);

    private static final String OP_GET_TARGETS = LinkTargetFinder.class.getName() + ".getTargets";

    @NotNull private final ActionContext actx;
    @NotNull private final PolicyRuleScriptExecutor beans;
    @NotNull private final OperationResult result;

    LinkTargetFinder(@NotNull ActionContext actx,
            @NotNull OperationResult parentResult) {
        this.actx = actx;
        this.beans = actx.beans;
        this.result = parentResult.createMinorSubresult(OP_GET_TARGETS);
    }

    List<PrismObject<? extends ObjectType>> getTargets(LinkTargetObjectSelectorType selector)
            throws SchemaException, ConfigurationException {
        try {
            return getTargetsInternal(resolveLinkType(selector));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    List<PrismObject<? extends ObjectType>> getTargets(String namedLinkTarget)
            throws SchemaException, ConfigurationException {
        try {
            return getTargetsInternal(resolveLinkType(namedLinkTarget));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    @NotNull
    private LinkTargetObjectSelectorType resolveLinkType(LinkTargetObjectSelectorType selector)
            throws SchemaException, ConfigurationException {
        String linkTypeName = selector.getLinkType();
        if (linkTypeName == null) {
            return selector;
        } else {
            LinkTargetObjectSelectorType mergedSelector = new LinkTargetObjectSelectorType();
            LinkedObjectSelectorType linkSelector = resolveLinkTypeInternal(linkTypeName);
            if (linkSelector != null) {
                ((PrismContainerValue<?>) mergedSelector.asPrismContainerValue()).mergeContent(linkSelector.asPrismContainerValue(), emptyList());
            }
            ((PrismContainerValue<?>) mergedSelector.asPrismContainerValue()).mergeContent(selector.asPrismContainerValue(), emptyList());
            mergedSelector.setLinkType(null);
            return mergedSelector;
        }
    }

    @NotNull
    private LinkTargetObjectSelectorType resolveLinkType(String linkTypeName)
            throws SchemaException, ConfigurationException {
        LinkTargetObjectSelectorType resultingSelector = new LinkTargetObjectSelectorType();
        LinkedObjectSelectorType linkSelector = resolveLinkTypeInternal(linkTypeName);
        if (linkSelector != null) {
            ((PrismContainerValue<?>) resultingSelector.asPrismContainerValue()).mergeContent(linkSelector.asPrismContainerValue(), emptyList());
        }
        return resultingSelector;
    }

    private LinkedObjectSelectorType resolveLinkTypeInternal(String linkTypeName)
            throws SchemaException, ConfigurationException {
        LensFocusContext<?> fc = actx.focusContext;
        LinkTypeDefinitionType definition = actx.beans.linkManager.getTargetLinkTypeDefinitionRequired(
                linkTypeName, Arrays.asList(fc.getObjectNew(), fc.getObjectCurrent(), fc.getObjectOld()), result);
        return definition.getSelector();
    }

    private List<PrismObject<? extends ObjectType>> getTargetsInternal(LinkTargetObjectSelectorType selector) {
        LOGGER.trace("Selecting matching link targets for {} with rule={}", selector, actx.rule);

        // We must create new set because we will remove links from it later.
        Set<PrismReferenceValue> links = new HashSet<>(getLinksInChangeSituation(selector));
        LOGGER.trace("Links matching change situation: {}", links);

        if (!selector.getRelation().isEmpty()) {
            applyRelationFilter(links, selector.getRelation());
            LOGGER.trace("Links matching also relation(s): {}", links);
        }

        if (isTrue(selector.isMatchesRuleAssignment())) {
            applyMatchingRuleAssignment(links);
            LOGGER.trace("Links matching also policy rule assignment: {}", links);
        }

        if (isTrue(selector.isMatchesConstraint())) {
            applyMatchingPolicyRuleConstraints(links);
            LOGGER.trace("Links matching also policy constraints: {}", links);
        }

        if (selector.getType() != null) {
            // This is just to avoid resolving objects with non-matching type
            applyObjectType(links, selector.getType());
            LOGGER.trace("Links matching also object type: {}", links);
        }

        Collection<PrismObject<? extends ObjectType>> resolvedObjects = resolveLinks(links);
        List<PrismObject<? extends ObjectType>> matchingObjects = getMatchingObjects(resolvedObjects, selector);

        LOGGER.trace("Final matching objects: {}", matchingObjects);
        return matchingObjects;
    }

    private List<PrismObject<? extends ObjectType>> getMatchingObjects(Collection<PrismObject<? extends ObjectType>> objects,
            ObjectSelectorType selector) {
        List<PrismObject<? extends ObjectType>> matching = new ArrayList<>();
        for (PrismObject<? extends ObjectType> object : objects) {
            try {
                if (beans.repositoryService.selectorMatches(selector, object, null, LOGGER, "")) {
                    matching.add(object);
                }
            } catch (CommonException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't match link target {} for {} when filtering link targets", e,
                        object, actx.focusContext.getObjectAny());
            }
        }
        return matching;
    }

    private Collection<PrismObject<? extends ObjectType>> resolveLinks(Set<PrismReferenceValue> links) {
        Map<String, PrismObject<? extends ObjectType>> objects = new HashMap<>();
        for (PrismReferenceValue link : links) {
            String oid = link.getOid();
            if (!objects.containsKey(oid)) {
                try {
                    Class<? extends ObjectType> clazz = getClassForType(link.getTargetType());
                    // TODO consider reading in read-only mode
                    objects.put(oid, beans.repositoryService.getObject(clazz, oid, null, result));
                } catch (SchemaException | ObjectNotFoundException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve reference {} in {} when applying script on link targets",
                            e, link, actx.focusContext.getObjectAny());
                }
            }
        }
        return objects.values();
    }

    private void applyObjectType(Set<PrismReferenceValue> links, QName type) {
        Class<?> clazz = getClassForType(type);
        links.removeIf(link -> !beans.prismContext.getSchemaRegistry().isAssignableFrom(clazz, link.getTargetType()));
    }

    @NotNull
    private Class<? extends ObjectType> getClassForType(QName type) {
        Class<?> clazz = beans.prismContext.getSchemaRegistry().getCompileTimeClass(type);
        if (clazz == null) {
            throw new IllegalStateException("Unknown type " + type);
        } else if (!ObjectType.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Type " + type + " is not an ObjectType, it is " + clazz);
        } else {
            //noinspection unchecked
            return (Class<? extends ObjectType>) clazz;
        }
    }

    private void applyMatchingRuleAssignment(Set<PrismReferenceValue> links) {
        EvaluatedAssignmentImpl<?> evaluatedAssignment = actx.rule.getEvaluatedAssignment();
        if (evaluatedAssignment == null) {
            throw new IllegalStateException("'matchesRuleAssignment' filter is selected but there's no evaluated assignment"
                    + " known for policy rule {}" + actx.rule);
        }
        Set<String> oids = evaluatedAssignment.getRoles().stream()
                .filter(EvaluatedAssignmentTargetImpl::appliesToFocus)
                .map(EvaluatedAssignmentTargetImpl::getOid)
                .collect(Collectors.toSet());
        links.removeIf(link -> !oids.contains(link.getOid()));
    }

    private void applyMatchingPolicyRuleConstraints(Set<PrismReferenceValue> links) {
        Set<String> oids = actx.rule.getAllTriggers().stream()
                .flatMap(trigger -> trigger.getTargetObjects().stream())
                .map(PrismObject::getOid)
                .collect(Collectors.toSet());
        links.removeIf(link -> !oids.contains(link.getOid()));
    }

    private void applyRelationFilter(Set<PrismReferenceValue> links, List<QName> relations) {
        links.removeIf(link -> !beans.prismContext.relationMatches(relations, link.getRelation()));
    }

    @NotNull
    private Set<PrismReferenceValue> getLinksInChangeSituation(LinkTargetObjectSelectorType selector) {
        switch (defaultIfNull(selector.getChangeSituation(), LinkChangeSituationType.ALWAYS)) {
            case ALWAYS:
                return SetUtils.union(getLinkedOld(), getLinkedNew());
            case ADDED:
                return SetUtils.difference(getLinkedNew(), getLinkedOld());
            case REMOVED:
                return SetUtils.difference(getLinkedOld(), getLinkedNew());
            case UNCHANGED:
                return SetUtils.intersection(getLinkedOld(), getLinkedNew());
            case CHANGED:
                return SetUtils.disjunction(getLinkedOld(), getLinkedNew());
            case IN_NEW:
                return getLinkedNew();
            case IN_OLD:
                return getLinkedOld();
            default:
                throw new AssertionError(selector.getChangeSituation());
        }
    }

    private Set<PrismReferenceValue> getLinkedOld() {
        ObjectType objectOld = asObjectable(actx.focusContext.getObjectOld());
        return objectOld instanceof AssignmentHolderType ?
                new HashSet<>(objectReferenceListToPrismReferenceValues(((AssignmentHolderType) objectOld).getRoleMembershipRef())) :
                emptySet();
    }

    private Set<PrismReferenceValue> getLinkedNew() {
        ObjectType objectNew = asObjectable(actx.focusContext.getObjectNew());
        return objectNew instanceof AssignmentHolderType ?
                new HashSet<>(objectReferenceListToPrismReferenceValues(((AssignmentHolderType) objectNew).getRoleMembershipRef())) :
                emptySet();
    }

    @Override
    public void close() {
        result.computeStatusIfUnknown();
    }
}
