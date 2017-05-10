package ${packageName};

import org.alfresco.service.namespace.QName;

public interface ${name} {

    <#list uris as uri>
    public static final String ${uri.name} = "${uri.value}";
    </#list>
    <#list properties as property>
    public static final QName ${property.name} = QName.createQName(${property.URI}, "${property.localName}");
    </#list>

}
