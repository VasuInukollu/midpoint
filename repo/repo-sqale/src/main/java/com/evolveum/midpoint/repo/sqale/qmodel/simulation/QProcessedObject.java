package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.sql.Types;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

public class QProcessedObject extends QContainer<MProcessedObject, MSimulationResult> {

    public static final String TABLE_NAME = "m_simulation_result_processed_object";


    public static final ColumnMetadata OID =
            ColumnMetadata.named("oid").ofType(UuidPath.UUID_TYPE).notNull();
    public static final ColumnMetadata OBJECT_TYPE =
            ColumnMetadata.named("objectType").ofType(Types.OTHER).notNull();
    public static final ColumnMetadata NAME_ORIG =
            ColumnMetadata.named("nameOrig").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata NAME_NORM =
            ColumnMetadata.named("nameNorm").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata STATE =
            ColumnMetadata.named("state").ofType(Types.OTHER);

    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);
    public static final ColumnMetadata OBJECT_BEFORE =
            ColumnMetadata.named("objectBefore").ofType(Types.BINARY);

    public static final ColumnMetadata OBJECT_AFTER =
            ColumnMetadata.named("objectAfter").ofType(Types.BINARY);

    public static final ColumnMetadata METRIC_IDENTIFIERS =
            ColumnMetadata.named("metricIdentifiers").ofType(Types.ARRAY);

    public final UuidPath oid = createUuid("oid", OID);
    public final EnumPath<MObjectType> objectType = createEnum("objectType", MObjectType.class, OBJECT_TYPE);
    public final StringPath nameOrig = createString("nameOrig", NAME_ORIG);
    public final StringPath nameNorm = createString("nameNorm", NAME_NORM);
    public final EnumPath<ObjectProcessingStateType> state =
            createEnum("outcome", ObjectProcessingStateType.class, STATE);

    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);
    public final ArrayPath<byte[], Byte> objectBefore = createByteArray("objectBefore", OBJECT_BEFORE);
    public final ArrayPath<byte[], Byte> objectAfter = createByteArray("objectAfter", OBJECT_AFTER);


    public final ArrayPath<String[], String> metricIdentifiers =
            createArray("metricIdentifiers", String[].class, METRIC_IDENTIFIERS);

    public QProcessedObject(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QProcessedObject(String variable, String schema, String table) {
        super(MProcessedObject.class, variable, schema, table);
    }



}
