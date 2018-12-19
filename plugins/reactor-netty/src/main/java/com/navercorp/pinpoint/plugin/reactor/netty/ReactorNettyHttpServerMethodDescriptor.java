package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.common.trace.MethodType;

/**
 * Created by linxiao on 2018/12/18.
 */
public class ReactorNettyHttpServerMethodDescriptor implements MethodDescriptor {
    private int apiId = 0;
    private int type = MethodType.WEB_REQUEST;

    @Override
    public String getMethodName() {
        return "";
    }

    @Override
    public String getClassName() {
        return "";
    }

    @Override
    public String[] getParameterTypes() {
        return  null;
    }

    @Override
    public String[] getParameterVariableName() {
        return null;
    }

    @Override
    public String getParameterDescriptor() {
        return "()";
    }

    @Override
    public int getLineNumber() {
        return -1;
    }

    @Override
    public String getFullName() {
        return ReactorNettyHttpServerMethodDescriptor.class.getName();
    }

    @Override
    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    @Override
    public int getApiId() {
        return apiId;
    }

    @Override
    public String getApiDescriptor() {
        return "ReactorNetty HTTP Server";
    }

    @Override
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
}
