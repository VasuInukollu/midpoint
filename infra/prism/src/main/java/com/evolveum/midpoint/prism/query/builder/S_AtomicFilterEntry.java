/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface S_AtomicFilterEntry {
    S_AtomicFilterExit all() ;
    S_AtomicFilterExit none() ;
    S_AtomicFilterExit undefined() ;
    S_ConditionEntry item(QName... names) ;
    S_ConditionEntry item(ItemPath path) ;
    S_ConditionEntry item(ItemPath itemPath, ItemDefinition itemDefinition);
    S_ConditionEntry itemWithDef(ItemDefinition itemDefinition, QName... names);        // experimental
    S_ConditionEntry item(PrismContainerDefinition containerDefinition, QName... names);
    S_ConditionEntry item(PrismContainerDefinition containerDefinition, ItemPath itemPath);
    S_AtomicFilterExit id(String... identifiers) ;
    S_AtomicFilterExit id(long... identifiers) ;
    S_AtomicFilterExit ownerId(String... identifiers) ;
    S_AtomicFilterExit ownerId(long... identifiers) ;
    S_AtomicFilterExit isDirectChildOf(PrismReferenceValue value) ;
    S_AtomicFilterExit isChildOf(PrismReferenceValue value) ;
    S_AtomicFilterExit isDirectChildOf(String oid) ;
    S_AtomicFilterExit isChildOf(String oid) ;
    S_AtomicFilterExit isParentOf(PrismReferenceValue value) ;            // reference should point to OrgType
    S_AtomicFilterExit isParentOf(String oid) ;                           // oid should be of an OrgType
    S_AtomicFilterExit isRoot() ;
    S_FilterEntryOrEmpty block();
    S_FilterEntry type(Class<? extends Containerable> type) ;
    S_FilterEntry exists(QName... names) ;
}
