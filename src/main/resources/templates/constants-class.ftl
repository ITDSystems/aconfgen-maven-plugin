package ${packageName};

import org.alfresco.service.namespace.QName;

public interface ${name} {

    <#list stringConstants as constant>
    public static final String ${constant.name} = "${constant.value}";
    </#list>
    <#list paramsConstants as constant>
    public static final QName ${constant.name} = QName.createQName(${constant.URI}, "${constant.localName}");
    </#list>

}
