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
package com.navercorp.pinpoint.collector.monitor.micrometer;

import com.navercorp.pinpoint.collector.monitor.dao.hbase.BulkOperationReporter;
import com.navercorp.pinpoint.common.util.StringUtils;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Objects;

/**
 * @author Taejin Koo
 */
public class BulkOperationMetrics {

    private static final String FLUSH_COUNT = ".flush.count";
    private static final String FLUSH_LAST_TIME_MILLIS = ".flush.lasttimemillis";
    private static final String INCREMENT_REJECT_COUNT= ".increment.reject.count";

    private final List<BulkOperationReporter> bulkOperationReporters;
    private final MeterRegistry meterRegistry;

    public BulkOperationMetrics(List<BulkOperationReporter> bulkOperationReporters, MeterRegistry meterRegistry) {
        this.bulkOperationReporters = Objects.requireNonNull(bulkOperationReporters, "bulkOperationReporters");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        registerMetrics();
    }

    private void registerMetrics() {
        for (BulkOperationReporter bulkOperationReporter : bulkOperationReporters) {
            String clazzName = bulkOperationReporter.getClass().getSimpleName();

            String[] splittedName = clazzName.split("\\$", 2);
            if (splittedName.length > 1 && StringUtils.hasText(splittedName[0])) {
                clazzName = splittedName[0];
            }

            Gauge.builder(clazzName + FLUSH_COUNT, bulkOperationReporter, BulkOperationReporter::getFlushAllCount)
                    .register(meterRegistry);

            Gauge.builder(clazzName + FLUSH_LAST_TIME_MILLIS, bulkOperationReporter, BulkOperationReporter::getLastFlushTimeMillis)
                    .register(meterRegistry);

            Gauge.builder(clazzName + INCREMENT_REJECT_COUNT, bulkOperationReporter, BulkOperationReporter::getRejectedCount)
                    .register(meterRegistry);
        }
    }
}
