package ${packageName};

import org.alfresco.service.namespace.QName;

public interface ${name} {

    <#list templateVariables as variable>
    public static QName ${variable.name} = QName.createQName("${variable.URI}", "${variable.localName}");
    </#list>

}
