package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import java.sql.Blob;

import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditDelta;

/**
 * Querydsl "row bean" type related to {@link QAuditDelta}.
 */
@SuppressWarnings("unused")
public class MAuditDelta {

    // TODO why is checksum part of PK? why not (recordId, deltaNumber) or even totally unique single ID?
    public Long recordId;
    public String checksum;
    public Blob delta;
    public String deltaoid;
    public Integer deltatype;
    public Blob fullresult;
    public String objectnameNorm;
    public String objectnameOrig;
    public String resourcenameNorm;
    public String resourcenameOrig;
    public String resourceoid;
    public Integer status;

    @Override
    public String toString() {
        return "MAuditDelta{" +
                "recordId=" + recordId +
                ", checksum='" + checksum + '\'' +
//                ", delta=" + delta +
//                ", deltaoid='" + deltaoid + '\'' +
//                ", deltatype=" + deltatype +
//                ", fullresult=" + fullresult +
//                ", objectnameNorm='" + objectnameNorm + '\'' +
//                ", objectnameOrig='" + objectnameOrig + '\'' +
//                ", resourcenameNorm='" + resourcenameNorm + '\'' +
//                ", resourcenameOrig='" + resourcenameOrig + '\'' +
//                ", resourceoid='" + resourceoid + '\'' +
                ", status=" + status +
                '}';
    }
}
