/*
 * Copyright 2024 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.otlp.web.config.mysql;

import com.navercorp.pinpoint.mybatis.MyBatisRegistryHandler;
import com.navercorp.pinpoint.otlp.common.web.defined.AppMetricDefinition;
import com.navercorp.pinpoint.otlp.web.dao.model.AppMetricDefDto;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author minwoo-jung
 */
public class OtlpMetricMysqlRegistryHandler implements MyBatisRegistryHandler {
    @Override
    public void registerTypeAlias(TypeAliasRegistry typeAliasRegistry) {
        typeAliasRegistry.registerAlias(AppMetricDefinition.class);
        typeAliasRegistry.registerAlias(AppMetricDefDto.class);
    }

    @Override
    public void registerTypeHandler(TypeHandlerRegistry typeHandlerRegistry) {

    }
}