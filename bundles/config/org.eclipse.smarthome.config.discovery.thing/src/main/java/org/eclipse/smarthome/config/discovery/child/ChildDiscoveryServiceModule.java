/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.discovery.child;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * A {@link ChildDiscoveryServiceModule} that is registered as a service is picked up by the ThingDiscoveryService
 * and can thus contribute {@link DiscoveryResult}s from things being added or updated in the {@link ThingRegistry}
 *
 * @author Tim Roberts - Initial contribution
 *
 */
@NonNullByDefault
public interface ChildDiscoveryServiceModule {

    /**
     * Defines the list of thing types that this participant can identify
     *
     * @return a set of thing type UIDs for which results can be created
     */
    public Set<ThingTypeUID> getSupportedThingTypeUIDs();

    /**
     * Creates discovery results based on the {@link Bridge}
     *
     * @param bridge the non-null bridge to be evaluated
     * @param callback the non-null callback to use to discover results
     */
    public void createResults(Bridge bridge, ChildDiscoveryCallback callback);
}
