/*
 * Copyright 2020 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.monitor.metric.custom;

import com.navercorp.pinpoint.profiler.context.monitor.metric.LongGaugeWrapper;

/**
 * @author Taejin Koo
 */
public class LongGaugeMetricVo extends AbstractCustomMetricVo {

    private final long value;

    public LongGaugeMetricVo(LongGaugeWrapper longGaugeMetricWrapper) {
        super(longGaugeMetricWrapper);
        this.value = longGaugeMetricWrapper.getValue();
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LongGaugeMetricVo{");
        sb.append("id=").append(getId());
        sb.append(", metricName='").append(getName()).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
