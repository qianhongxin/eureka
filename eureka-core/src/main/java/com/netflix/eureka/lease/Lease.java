/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.eureka.lease;

import com.netflix.eureka.registry.AbstractInstanceRegistry;

/**
 * Describes a time-based availability of a {@link T}. Purpose is to avoid
 * accumulation of instances in {@link AbstractInstanceRegistry} as result of ungraceful
 * shutdowns that is not uncommon in AWS environments.
 *
 * If a lease elapses without renewals, it will eventually expire consequently
 * marking the associated {@link T} for immediate eviction - this is similar to
 * an explicit cancellation except that there is no communication between the
 * {@link T} and {@link LeaseManager}.
 *
 * @author Karthik Ranganathan, Greg Kim
 */
public class Lease<T> {

    enum Action {
        Register, Cancel, Renew
    };

    public static final int DEFAULT_DURATION_IN_SECS = 90;

    private T holder;
    private long evictionTimestamp;
    private long registrationTimestamp;
    private long serviceUpTimestamp;
    // Make it volatile so that the expiration task would see this quicker
    private volatile long lastUpdateTimestamp;
    private long duration;

    public Lease(T r, int durationInSecs) {
        holder = r;
        registrationTimestamp = System.currentTimeMillis();
        lastUpdateTimestamp = registrationTimestamp;
        duration = (durationInSecs * 1000);

    }

    /**
     * Renew the lease, use renewal duration if it was specified by the
     * associated {@link T} during registration, otherwise default duration is
     * {@link #DEFAULT_DURATION_IN_SECS}.
     */
    // 更新下lastUpdateTimestamp
    public void renew() {
        lastUpdateTimestamp = System.currentTimeMillis() + duration;

    }

    /**
     * Cancels the lease by updating the eviction time.
     */
    // 设置服务下线时间戳
    public void cancel() {
        if (evictionTimestamp <= 0) {
            evictionTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Mark the service as up. This will only take affect the first time called,
     * subsequent calls will be ignored.
     */
    public void serviceUp() {
        if (serviceUpTimestamp == 0) {
            serviceUpTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Set the leases service UP timestamp.
     */
    public void setServiceUpTimestamp(long serviceUpTimestamp) {
        this.serviceUpTimestamp = serviceUpTimestamp;
    }

    /**
     * Checks if the lease of a given {@link com.netflix.appinfo.InstanceInfo} has expired or not.
     */
    public boolean isExpired() {
        return isExpired(0l);
    }

    /**
     * Checks if the lease of a given {@link com.netflix.appinfo.InstanceInfo} has expired or not.
     *
     * Note that due to renew() doing the 'wrong" thing and setting lastUpdateTimestamp to +duration more than
     * what it should be, the expiry will actually be 2 * duration. This is a minor bug and should only affect
     * instances that ungracefully shutdown. Due to possible wide ranging impact to existing usage, this will
     * not be fixed.
     *
     *    *
     * @param additionalLeaseMs any additional lease time to add to the lease evaluation in ms.
     */
    //这里有eureka  的bug，我们设置过期时间时间是90s，实际是90s+duration才会过期。即180s。还有只读缓存，读写缓存的更新时间，所以
    // 实际上更慢一点，大于3min

    // 闪光点：为了防止因为jvm的stop the world等导致延迟调度二导致的误判，加了补偿时间。但是也会导致新问题
    public boolean isExpired(long additionalLeaseMs) {
        //lastUpdateTimestamp是心跳更新的时间，additionalLeaseMs是补偿时间，防止stop the world等导致的主动过期任务延迟调度导致的问题。但也会导致整体过期时间大于
        // lastUpdateTimestamp + duration，即lastUpdateTimestamp + duration+ additionalLeaseMs·
        return (evictionTimestamp > 0 || System.currentTimeMillis() > (lastUpdateTimestamp + duration + additionalLeaseMs));
    }

    /**
     * Gets the milliseconds since epoch when the lease was registered.
     *
     * @return the milliseconds since epoch when the lease was registered.
     */
    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    /**
     * Gets the milliseconds since epoch when the lease was last renewed.
     * Note that the value returned here is actually not the last lease renewal time but the renewal + duration.
     *
     * @return the milliseconds since epoch when the lease was last renewed.
     */
    public long getLastRenewalTimestamp() {
        return lastUpdateTimestamp;
    }

    /**
     * Gets the milliseconds since epoch when the lease was evicted.
     *
     * @return the milliseconds since epoch when the lease was evicted.
     */
    public long getEvictionTimestamp() {
        return evictionTimestamp;
    }

    /**
     * Gets the milliseconds since epoch when the service for the lease was marked as up.
     *
     * @return the milliseconds since epoch when the service for the lease was marked as up.
     */
    public long getServiceUpTimestamp() {
        return serviceUpTimestamp;
    }

    /**
     * Returns the holder of the lease.
     */
    public T getHolder() {
        return holder;
    }

}
