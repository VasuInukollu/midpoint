/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qbean;

import com.evolveum.midpoint.repo.sqale.qmodel.QRole;

/**
 * Querydsl "row bean" type related to {@link QRole}.
 */
public class MRole extends MAbstractRole {

    public String roleType;
}