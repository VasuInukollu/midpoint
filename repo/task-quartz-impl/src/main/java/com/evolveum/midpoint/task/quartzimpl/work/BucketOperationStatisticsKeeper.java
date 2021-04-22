/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work;

import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.task.quartzimpl.statistics.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class BucketOperationStatisticsKeeper {

    private final WorkBucketStatisticsCollector collector;

    final long start = System.currentTimeMillis();

    int conflictCount = 0;
    private long conflictWastedTime = 0;
    private int bucketWaitCount = 0;
    private long bucketWaitTime = 0;
    private int bucketsReclaimed = 0;

    BucketOperationStatisticsKeeper(WorkBucketStatisticsCollector collector) {
        this.collector = collector;
    }

    public void register(String situation) {
        if (collector != null) {
            collector.register(situation, System.currentTimeMillis() - start,
                    conflictCount, conflictWastedTime, bucketWaitCount, bucketWaitTime, bucketsReclaimed);
        }
    }

    void addReclaims(int count) {
        bucketsReclaimed += count;
    }

    void addToConflictCounts(ModifyObjectResult<TaskType> modifyObjectResult) {
        conflictCount += modifyObjectResult.getRetries();
        conflictWastedTime += modifyObjectResult.getWastedTime();
    }

    void setConflictCounts(ModifyObjectResult<TaskType> modifyObjectResult) {
        conflictCount = modifyObjectResult.getRetries();
        conflictWastedTime = modifyObjectResult.getWastedTime();
    }

    void addWaitTime(long waitTime) {
        bucketWaitCount++;
        bucketWaitTime += waitTime;
    }
}
