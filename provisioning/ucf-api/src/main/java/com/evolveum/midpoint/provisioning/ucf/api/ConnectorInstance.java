/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncChangeListener;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

/**
 * Connector instance configured for a specific resource.
 *
 * This is kind of connector facade. It is an API provided by
 * the "Unified Connector Framework" to the midPoint provisioning
 * component. There is no associated SPI yet. That may come in the
 * future when this interface stabilizes a bit.
 *
 * This interface provides an unified facade to a connector capabilities
 * in the Unified Connector Framework interface. The connector is configured
 * to a specific resource instance and therefore can execute operations on
 * resource.
 *
 * Calls to this interface always try to reach the resource and get the
 * actual state on resource. The connectors are not supposed to cache any
 * information. Therefore the methods do not follow get/set java convention
 * as the data are not regular javabean properties.
 *
 * @see ConnectorFactory
 *
 * @author Radovan Semancik
 *
 */
public interface ConnectorInstance {

    String OPERATION_CONFIGURE = ConnectorInstance.class.getName() + ".configure";
    String OPERATION_INITIALIZE = ConnectorInstance.class.getName() + ".initialize";
    String OPERATION_DISPOSE = ConnectorInstance.class.getName() + ".dispose";

    /**
     * The connector instance will be configured to the state that it can
     * immediately access the resource. The resource configuration is provided as
     * a parameter to this method.
     *
     * This method may be invoked on connector instance that is already configured.
     * In that case re-configuration of the connector instance is requested.
     * The connector instance must be operational at all times, even during re-configuration.
     * Operations cannot be interrupted or refused due to missing configuration.
     *
     * @param configuration new connector configuration (prism container value)
     * @param generateObjectClasses the list of the object classes which should be generated in schema
     */
    void configure(@NotNull PrismContainerValue<?> configuration, List<QName> generateObjectClasses, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException;

    ConnectorOperationalStatus getOperationalStatus() throws ObjectNotFoundException;

    /**
     * Get necessary information from the remote system.
     *
     * This method will initialize the configured connector. It may contact the remote system in order to do so,
     * e.g. to download the schema. It will cache the information inside connector instance until this method
     * is called again. It must be called after configure() and before any other method that is accessing the
     * resource.
     *
     * If resource schema and capabilities are already cached by midPoint they may be passed to the connector instance.
     * Otherwise the instance may need to fetch them from the resource which may be less efficient.
     *
     * NOTE: the capabilities and schema that are used here are NOT necessarily those that are detected by the resource.
     *       The detected schema will come later. The schema here is the one that is stored in the resource
     *       definition (ResourceType). This may be schema that was detected previously. But it may also be a schema
     *       that was manually defined. This is needed to be passed to the connector in case that the connector
     *       cannot detect the schema and needs schema/capabilities definition to establish a connection.
     *       Most connectors will just ignore the schema and capabilities that are provided here.
     *       But some connectors may need it (e.g. CSV connector working with CSV file without a header).
     *
     * TODO: caseIgnoreAttributeNames is probably not correct here. It should be provided in schema or capabilities?
     */
    void initialize(ResourceSchema previousResourceSchema, Collection<Object> previousCapabilities,
            boolean caseIgnoreAttributeNames, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException;

    /**
     * Updates stored resource schema and capabilities.
     */
    void updateSchema(ResourceSchema resourceSchema);

    /**
     * Retrieves native connector capabilities.
     *
     * The capabilities specify what the connector can do without any kind of simulation or other workarounds.
     * The set of capabilities may depend on the connector configuration (e.g. if a "disable" or password attribute
     * was specified in the configuration or not).
     *
     * It may return null. Such case means that the capabilities cannot be determined.
     */
    Collection<Object> fetchCapabilities(OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException;

    /**
     * Retrieves the schema from the resource.
     *
     * The schema may be considered to be an XSD schema, but it is returned in a
     * "parsed" format and it is in fact a bit stricter and richer midPoint
     * schema.
     *
     * It may return null. Such case means that the schema cannot be determined.
     *
     * @see PrismSchema
     *
     * @return Up-to-date resource schema.
     * @throws CommunicationException error in communication to the resource
     *                - nothing was fetched.
     */
    ResourceSchema fetchResourceSchema(OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException;

    /**
     * Retrieves a specific object from the resource.
     *
     * This method is fetching an object from the resource that is identified
     * by its primary identifier. It is a "targeted" method in this aspect and
     * it will fail if the object is not found.
     *
     * The objectClass provided as a parameter to this method must correspond
     * to one of the object classes in the schema. The object class must match
     * the object. If it does not, the behavior of this operation is undefined.
     *
     * The returned ResourceObject is "disconnected" from schema. It means that
     * any call to the getDefinition() method of the returned object will
     * return null.
     *
     * TODO: object not found error
     *
     * @param resourceObjectIdentification objectClass+identifiers of the object to fetch
     * @return object fetched from the resource (no schema)
     * @throws CommunicationException error in communication to the resource
     *                - nothing was fetched.
     * @throws SchemaException error converting object from native (connector) format
     */
    PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification,
            AttributesToReturn attributesToReturn, StateReporter reporter, OperationResult parentResult)
        throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
        SecurityViolationException, ConfigurationException;

    /**
     * Execute iterative search operation.
     *
     * This method will execute search operation on the resource and will pass
     * any objects that are found. A "handler" callback will be called for each
     * of the objects found (depending also on the error reporting method).
     *
     * The call to this method will return only after all the callbacks were
     * called, therefore it is not asynchronous in a strict sense.
     *
     * If nothing is found the method should behave as if there is an empty result set
     * (handler is never called) and the call should result in a success. So the ObjectNotFoundException
     * should be thrown only if there is an error in search parameters, e.g. if search base points to an non-existent object.
     *
     * BEWARE: The implementation of the handler should be consistent with the value of errorReportingMethod parameter:
     * if the method is FETCH_RESULT, the handler must be ready to process also incomplete/malformed objects (flagged
     * by appropriate fetchResult).
     *
     * @param objectClassDefinition Definition of the object class of the objects being searched for.
     * @param query Object query to be used.
     * @param handler Handler that is called for each object found.
     * @param attributesToReturn Attributes that are to be returned; TODO describe exact semantics
     * @param pagedSearchConfiguration Configuration (capability) describing how paged searches are to be done.
     * @param searchHierarchyConstraints Specifies in what parts of hierarchy the search should be executed.
     * @param errorReportingMethod How should errors during processing individual objects be reported.
     *                             If EXCEPTION (the default), an appropriate exception is thrown.
     *                             If FETCH_RESULT, the error is reported within the shadow affected.
     *
     * @throws SchemaException if the search couldn't be executed because of a problem with the schema; or there is a schema
     *                         problem with an object returned (and error reporting method is EXCEPTION).
     * @throws ObjectNotFoundException if something from the search parameters refers non-existent object,
     *                                 e.g. if search base points to an non-existent object.
     */
    SearchResultMetadata search(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            ShadowResultHandler handler, AttributesToReturn attributesToReturn, PagedSearchCapabilityType pagedSearchConfiguration,
            SearchHierarchyConstraints searchHierarchyConstraints, FetchErrorReportingMethodType errorReportingMethod,
            StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, SecurityViolationException,
                    ObjectNotFoundException;

    /**
     * Counts objects on resource.
     *
     * This method will count objects on the resource by executing a paged search operation,
     * returning the "estimated objects count" information.
     *
     * If paging is not available, it throws an exception.
     */
    int count(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            PagedSearchCapabilityType pagedSearchConfigurationType, StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException;

    /**
     * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
     *  and what were not determined.
     *
     * The exception should be thrown only if the connector is sure that nothing was done on the resource.
     * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
     * exception as something might have been done already.
     *
     * The connector may return some (or all) of the attributes of created object. The connector should do
     * this only such operation is efficient, e.g. in case that the created object is normal return value from
     * the create operation. The connector must not execute additional operation to fetch the state of
     * created resource. In case that the new state is not such a normal result, the connector must
     * return null. Returning empty set means that the connector supports returning of new state, but nothing
     * was returned (e.g. due to a limiting configuration). Returning null means that connector does not support
     * returning of new object state and the caller should explicitly invoke fetchObject() in case that the
     * information is needed.
     *
     * @throws SchemaException resource schema violation
     * @return created object attributes. May be null.
     * @throws ObjectAlreadyExistsException object already exists on the resource
     */
    AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> object, StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException;

    /**
     * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
     *  and what results are we not sure about.
     *
     * Returns a set of attributes that were changed as a result of the operation. This may include attributes
     * that were changed as a side effect of the operations, e.g. attributes that were not originally specified
     * in the "changes" parameter.
     *
     * The exception should be thrown only if the connector is sure that nothing was done on the resource.
     * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
     * exception as something might have been done already.
     *
     * @throws ObjectAlreadyExistsException in case that the modified object conflicts with another existing object (e.g. while renaming an object)
     */
    AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
            ResourceObjectIdentification identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            StateReporter reporter, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ConfigurationException;

    AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass, PrismObject<ShadowType> shadow,
            Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException;

    Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException;

    /**
     * Creates a live Java object from a token previously serialized to string.
     *
     * Serialized token is not portable to other connectors or other resources.
     * However, newer versions of the connector should understand tokens generated
     * by previous connector version.
     */
    PrismProperty<?> deserializeToken(Object serializedToken);

    /**
     * Returns the latest token. In other words, returns a token that
     * corresponds to a current state of the resource. If fetchChanges
     * is immediately called with this token, nothing should be returned
     * (Figuratively speaking, neglecting concurrent resource modifications).
     */
    <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass, StateReporter reporter, OperationResult parentResult) throws CommunicationException, GenericFrameworkException;

    /**
     * Token may be null. That means "from the beginning of history".
     */
    void fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
            AttributesToReturn attrsToReturn, Integer maxChanges, StateReporter reporter,
            LiveSyncChangeListener changeHandler, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException;

    //public ValidationResult validateConfiguration(ResourceConfiguration newConfiguration);

    //public void applyConfiguration(ResourceConfiguration newConfiguration) throws MisconfigurationException;

    // Maybe this should be moved to ConnectorManager? In that way it can also test connector instantiation.
    void test(OperationResult parentResult);

    /**
     * Dispose of the connector instance. Dispose is a brutal operation. Once the instance is disposed of, it cannot execute
     * any operation, it may not be (re)configured, any operation in progress may fail.
     * Dispose is usually invoked only if the system is shutting down. It is not invoked while the system is running, not even
     * if the connector instance configuration is out of date. There still may be some operations running. Disposing of the instance
     * will make those operations to fail.
     * MidPoint prefers to re-configure existing connector instance instead of disposing of it.
     * However, this approach may change in the future.
     */
    void dispose();

    /**
     * Listens for asynchronous updates. The connector should (indefinitely) listen for updates coming from the resource.
     * Caller thread can be used for listening to and processing of changes. Or, the connector is free to create its own
     * threads to listen and/or to execute changeListener in. But the control should return back to the caller after the
     * listening is over.
     *
     * In the future we could create a similar method that would simply start the listening process and return the control
     * immediately -- if needed.
     *
     * @param changeListener Listener to invoke when a change arrives
     * @param canRunSupplier Supplier of "canRun" information. If it returns false we should stop listening.
     * @param parentResult Operation result to use for listening for changes.
     */
    default void listenForChanges(@NotNull AsyncChangeListener changeListener, @NotNull Supplier<Boolean> canRunSupplier,
            @NotNull OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException();
    }
}
