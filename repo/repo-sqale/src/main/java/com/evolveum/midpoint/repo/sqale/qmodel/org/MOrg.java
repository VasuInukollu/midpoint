/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.org;

import com.evolveum.midpoint.repo.sqale.qmodel.role.MAbstractRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QRole;

/**
 * Querydsl "row bean" type related to {@link QRole}.
 */
public class MOrg extends MAbstractRole {

    public Integer displayOrder;
    public Boolean tenant;
}