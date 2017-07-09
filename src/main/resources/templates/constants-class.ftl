<#function maybeEmpty str>
    <#if str?has_content>
        <#return str/>
    <#else>
        <#return "EMPTY"/>
    </#if>
</#function>

package ${packageName};

import org.alfresco.service.namespace.QName;

public interface ${className} {

    <#list namespaces as ns>
    public static final String ${ns.prefix?upper_case}_MODEL_URI = "${ns.uri}";
    </#list>

    <#list types as type>
        <#assign qname = type.name/>
    public static final QName TYPE_${get_qname_ns_prefix(qname)?upper_case}_${snake_upper_case(qname.localName)} = QName.createQName("${qname.namespaceURI}", "${qname.localName}");
    </#list>

    <#list properties as prop>
    <#assign qname = prop.name/>
    <#assign prefix = get_qname_ns_prefix(qname)?upper_case/>
    <#assign name = snake_upper_case(qname.localName)/>
    public static final QName PROP_${prefix}_${name} = QName.createQName("${qname.namespaceURI}", "${qname.localName}");
    <#list get_prop_allowed_values(prop) as value>
    public static final String PROP_${prefix}_${name}_${maybeEmpty(snake_upper_case(value))} = "${value}";
    </#list>
    </#list>

    <#list associations as assoc>
    <#assign qname = assoc.name/>
    public static final QName ASSOC_${get_qname_ns_prefix(qname)?upper_case}_${snake_upper_case(qname.localName)} = QName.createQName("${qname.namespaceURI}", "${qname.localName}");
    </#list>

}
