package com.evolveum.midpoint.prism.impl.query.lang;

import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.axiom.lang.antlr.AxiomAntlrLiterals;
import com.evolveum.axiom.lang.antlr.AxiomQuerySource;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.AndFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.DoubleQuoteStringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.FilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.FilterNameContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.GenFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.ItemFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.MultilineStringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.OrFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.PathContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.PrefixedNameContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.SingleQuoteStringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.StringContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.SubFilterContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.SubfilterOrValueContext;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.ValueSpecificationContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.impl.query.EqualFilterImpl;
import com.evolveum.midpoint.prism.impl.query.ExistsFilterImpl;
import com.evolveum.midpoint.prism.impl.query.NotFilterImpl;
import com.evolveum.midpoint.prism.impl.query.SubstringFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

public class PrismQueryLanguageParser {


    public static final String QUERY_NS = "http://prism.evolveum.com/xml/ns/public/query-3";
    public static final String MATCHING_RULE_NS = "http://prism.evolveum.com/xml/ns/public/matching-rule-3";


    public interface ItemFilterFactory {

        ObjectFilter create(PrismContainerDefinition<?> parentDef, ItemPath itemPath, ItemDefinition<?> itemDef, QName matchingRule, SubfilterOrValueContext subfilterOrValue) throws SchemaException;

    }

    private abstract class PropertyFilterFactory implements ItemFilterFactory {

        @Override
        public ObjectFilter create(PrismContainerDefinition<?> parentDef, ItemPath path, ItemDefinition<?> definition, QName matchingRule,
                SubfilterOrValueContext subfilterOrValue) throws SchemaException {
            Preconditions.checkArgument(subfilterOrValue != null);
            schemaCheck(definition instanceof PrismPropertyDefinition<?>, "Definition %s is not property", definition);
            PrismPropertyDefinition<?> propDef = (PrismPropertyDefinition<?>) definition;
            ValueSpecificationContext valueSpec = subfilterOrValue.valueSpecification();
            if(valueSpec.path() != null) {
                throw new UnsupportedOperationException("FIXME: Implement right side lookup");
            } else if (valueSpec.string() != null) {
                Object parsedValue = parseLiteral(propDef, valueSpec.string());
                return valueFilter(propDef, path, matchingRule, parsedValue);
            }
            throw new IllegalStateException();
        }

        abstract ObjectFilter valueFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule, Object value);

        abstract ObjectFilter propertyFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule, ItemPath rightPath, PrismPropertyDefinition<?> rightDef);


    }


    private class SubstringFilterFactory extends PropertyFilterFactory {

        private final boolean anchorStart;
        private final boolean anchorEnd;



        public SubstringFilterFactory(boolean anchorStart, boolean anchorEnd) {
            this.anchorStart = anchorStart;
            this.anchorEnd = anchorEnd;
        }

        @Override
        ObjectFilter propertyFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                ItemPath rightPath, PrismPropertyDefinition<?> rightDef) {
            throw new UnsupportedOperationException();
        }

        @Override
        ObjectFilter valueFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                Object value) {
            return SubstringFilterImpl.createSubstring(path, definition, context, matchingRule, value, anchorStart, anchorEnd);
        }
    };


    private static final Map<String, QName> ALIASES_TO_NAME = ImmutableMap.<String, QName>builder()
            .put("=", queryName("equal"))
            .put("<", queryName("less"))
            .put(">", queryName("greater"))
            .put("<=", queryName("lessOrEquals"))
            .put(">=", queryName("greaterOrEquals"))
            .build();


    private final Map<QName, ItemFilterFactory> filterFactories = ImmutableMap.<QName, ItemFilterFactory>builder()
            .put(queryName("equal"), new PropertyFilterFactory() {

                @Override
                public ObjectFilter valueFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                        Object value) {
                    return EqualFilterImpl.createEqual(path, definition, matchingRule, context, value);
                }

                @Override
                public ObjectFilter propertyFilter(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule,
                        ItemPath rightPath, PrismPropertyDefinition<?> rightDef) {
                    return EqualFilterImpl.createEqual(path, definition, matchingRule, rightPath, rightDef);
                }
            })
            .put(queryName("contains"), new SubstringFilterFactory(false, false))
            .put(queryName("startsWith"), new SubstringFilterFactory(true, false))
            .put(queryName("endsWith"), new SubstringFilterFactory(false, true))
            .put(queryName("matches"), this::matchesFilter)
            .put(queryName("exists") , new ItemFilterFactory() {

                @Override
                public ObjectFilter create(PrismContainerDefinition<?> parentDef, ItemPath itemPath, ItemDefinition<?> itemDef,
                        QName matchingRule, SubfilterOrValueContext subfilterOrValue) throws SchemaException {
                    return ExistsFilterImpl.createExists(itemPath, parentDef, null);
                }
            })
            .build()
            ;

    private final Map<QName, ItemFilterFactory> notFilterFactories = ImmutableMap.<QName, ItemFilterFactory>builder()
            .put(queryName("exists") , new ItemFilterFactory() {
                @Override
                public ObjectFilter create(PrismContainerDefinition<?> parentDef, ItemPath itemPath, ItemDefinition<?> itemDef,
                        QName matchingRule, SubfilterOrValueContext subfilterOrValue) throws SchemaException {
                    if(itemDef instanceof PrismPropertyDefinition<?>) {
                        return EqualFilterImpl.createEqual(itemPath, (PrismPropertyDefinition<?>) itemDef, matchingRule);
                    }
                    return NotFilterImpl.createNot(ExistsFilterImpl.createExists(itemPath, parentDef, null));
                }
            })
            .build()
            ;



    private final PrismContext context;

    public PrismQueryLanguageParser(PrismContext context) {
        this.context = context;
    }

    public <C extends Containerable> ObjectFilter parseQuery(Class<C> typeClass, String query) throws SchemaException {
        return parseQuery(typeClass, AxiomQuerySource.from(query));
    }

    public <C extends Containerable> ObjectFilter parseQuery(Class<C> typeClass, AxiomQuerySource source) throws SchemaException {
        PrismContainerDefinition<?> complexType = context.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(typeClass);
        if (complexType == null) {
            throw new IllegalArgumentException("Couldn't find definition for complex type " + typeClass);
        }

        ObjectFilter rootFilter = parseFilter(complexType, source.root());
        return rootFilter;
    }


    private static QName queryName(String localName) {
        return new QName(QUERY_NS,localName);
    }

    private ObjectFilter parseFilter(PrismContainerDefinition<?> complexType, FilterContext root) throws SchemaException {
        if (root instanceof AndFilterContext) {
            return andFilter(complexType, (AndFilterContext) root);
        } else if (root instanceof OrFilterContext) {
            return orFilter(complexType, (OrFilterContext) root);
        } else if (root instanceof GenFilterContext) {
            return itemFilter(complexType, ((GenFilterContext) root).itemFilter());
        } else if (root instanceof SubFilterContext) {
            return parseFilter(complexType, ((SubFilterContext) root).subfilterSpec().filter());
        }
        throw new IllegalStateException("Unsupported Filter Context");
    }

    private ObjectFilter andFilter(PrismContainerDefinition<?> complexType, AndFilterContext root) throws SchemaException {
        Builder<ObjectFilter> filters = ImmutableList.builder();
        filters.addAll(expandIf(AndFilter.class, parseFilter(complexType,root.left)));
        filters.addAll(expandIf(AndFilter.class, parseFilter(complexType, root.right)));
        return context.queryFactory().createAndOptimized(filters.build());
    }


    private Iterable<? extends ObjectFilter> expandIf(Class<? extends NaryLogicalFilter> expandable, ObjectFilter filter) {
        if (expandable.isInstance(filter)) {
            return ((LogicalFilter) filter).getConditions();
        }
        return Collections.singletonList(filter);
    }

    private ObjectFilter orFilter(PrismContainerDefinition<?> complexType, OrFilterContext root) throws SchemaException {
        Builder<ObjectFilter> filters = ImmutableList.builder();
        filters.addAll(expandIf(OrFilter.class,parseFilter(complexType,root.left)));
        filters.addAll(expandIf(OrFilter.class, parseFilter(complexType,root.right)));
        return context.queryFactory().createOrOptimized(filters.build());
    }

    private ObjectFilter itemFilter(PrismContainerDefinition<?> parent, ItemFilterContext itemFilter) throws SchemaException {
        // TODO Auto-generated method stub
        QName filterName = filterName(itemFilter.filterName());
        QName matchingRule = itemFilter.matchingRule() != null ? toFilterName(MATCHING_RULE_NS, itemFilter.matchingRule().prefixedName()) : null;
        ItemPath path = path(parent, itemFilter.path());
        ItemDefinition<?> itemDefinition = parent.findItemDefinition(path);
        ItemFilterFactory factory = filterFactories.get(filterName);
        schemaCheck(factory != null, "Unknown filter %s", filterName);

        if(itemFilter.negation() != null) {
            ItemFilterFactory notFactory = notFilterFactories.get(filterName);
            if(notFactory != null) {
                return notFactory.create(parent, path, itemDefinition, matchingRule, itemFilter.subfilterOrValue());
            }
        }
        ObjectFilter filter = createItemFilter(factory,parent, path, itemDefinition, matchingRule,itemFilter.subfilterOrValue());
        if(itemFilter.negation() != null) {
            return new NotFilterImpl(filter);
        }
        return filter;

    }

    static void schemaCheck(boolean condition, String template, Object... arguments) throws SchemaException {
        if(! condition) {
            throw new SchemaException(Strings.lenientFormat(template, arguments));
        }
    }

    private ObjectFilter createItemFilter(ItemFilterFactory factory, PrismContainerDefinition<?> parent, ItemPath path, ItemDefinition<?> itemDef, QName matchingRule,
            SubfilterOrValueContext subfilterOrValue) throws SchemaException {
        return factory.create(parent, path, itemDef, matchingRule, subfilterOrValue);
    }

    private ItemPath path(PrismContainerDefinition<?> complexType, PathContext path) {
        // FIXME: Implement proper parsing of decomposed item path from Antlr
        UniformItemPath ret = ItemPathHolder.parseFromString(path.getText());
        return ret;
    }

    private QName filterName(FilterNameContext filterName) {
        if ( filterName.filterNameAlias() != null) {
            return ALIASES_TO_NAME.get(filterName.filterNameAlias().getText());
        }
        return toFilterName(QUERY_NS, filterName.prefixedName());
    }

    private QName toFilterName(String defaultNs, PrefixedNameContext itemName) {
        String ns = defaultNs;
        // FIXME: Add namespace detection
        return new QName(ns,itemName.localName.getText());
    }

    ObjectFilter createEqual(PrismPropertyDefinition<?> definition, ItemPath path, QName matchingRule, ValueSpecificationContext value) {
        if(value.path() != null) {
            throw new UnsupportedOperationException("FIXME: Implement right side lookup");
        } else if (value.string() != null) {
            Object parsedValue = parseLiteral(definition, value.string());
            return EqualFilterImpl.createEqual(path, definition, matchingRule, context, parsedValue);
        }
        throw new IllegalStateException();
    }

    private Object parseLiteral(PrismPropertyDefinition<?> definition, StringContext string) {
        // FIXME: Use property definition for parsing (date, name, qname, etc)
        if (string instanceof DoubleQuoteStringContext) {
            return AxiomAntlrLiterals.convertDoubleQuote(string.getText());
        } else if (string instanceof SingleQuoteStringContext) {
            return AxiomAntlrLiterals.convertSingleQuote(string.getText());
        } else if (string instanceof MultilineStringContext) {
            return AxiomAntlrLiterals.convertMultiline(string.getText());
        }
        return null;
    }


    private ObjectFilter matchesFilter(PrismContainerDefinition<?> parent, ItemPath path,ItemDefinition<?> definition,  QName matchingRule, SubfilterOrValueContext subfilterOrValue) throws SchemaException {
        Preconditions.checkArgument(definition instanceof PrismContainerDefinition<?>);
        PrismContainerDefinition<?> containerDef = (PrismContainerDefinition<?>) definition;
        FilterContext subfilterTree = subfilterOrValue.subfilterSpec().filter();
        ObjectFilter subfilter = parseFilter(containerDef, subfilterTree);
        return ExistsFilterImpl.createExists(path, (PrismContainerDefinition<?>) parent, subfilter);

    }

    public static PrismQueryLanguageParser create(PrismContext prismContext) {
        return new PrismQueryLanguageParser(prismContext);
    }
}


