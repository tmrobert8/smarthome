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
package org.eclipse.smarthome.config.discovery.child.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.child.ChildDiscoveryCallback;
import org.eclipse.smarthome.config.discovery.child.ChildDiscoveryServiceModule;
import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingRegistryChangeListener;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link DiscoveryService} implementation, which can find things that are based on other things (
 * Support for further devices can be added by implementing and registering a {@link ChildDiscoveryServiceModule}.
 *
 * @author Tim Roberts - Initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true, service = { DiscoveryService.class,
        ThingRegistryChangeListener.class, }, configurationPid = "discovery.child")
public class ChildDiscoveryService extends AbstractDiscoveryService
        implements ThingRegistryChangeListener, ChildDiscoveryCallback {

    private final Logger logger = LoggerFactory.getLogger(ChildDiscoveryService.class);

    private final Set<ChildDiscoveryServiceModule> participants = new CopyOnWriteArraySet<>();

    private @NonNullByDefault({}) ThingRegistry thingRegistry;

    private @NonNullByDefault({}) SafeCaller safeCaller;

    public ChildDiscoveryService() {
        super(null, 5, true);
    }

    @Override
    protected void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        super.activate(configProperties);
        startScan();
    }

    @Override
    @Modified
    protected void modified(@Nullable Map<String, @Nullable Object> configProperties) {
        super.modified(configProperties);
    }

    @Reference
    protected void setSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = safeCaller;
    }

    protected void unsetSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingDiscoveryParticipant(ChildDiscoveryServiceModule participant) {
        this.participants.add(participant);

        if (thingRegistry != null && isBackgroundDiscoveryEnabled()) {
            thingRegistry.stream().filter(t -> t instanceof Bridge)
                    .forEach(b -> discoverThings(participant, (Bridge) b));
        }
    }

    protected void removeThingDiscoveryParticipant(ChildDiscoveryServiceModule participant) {
        this.participants.remove(participant);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        final Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        for (ChildDiscoveryServiceModule participant : participants) {
            supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
        }
        return supportedThingTypes;
    }

    private void discoverThings(ChildDiscoveryServiceModule participant, Bridge bridge) {
        if (safeCaller != null) {
            ChildDiscoveryServiceModule safeListener = safeCaller.create(participant, ChildDiscoveryServiceModule.class)
                    .withTimeout(15000)
                    .onException(exception -> logger.debug("ThingDiscoveryParticipant exception {}", exception))
                    .build();
            safeListener.createResults(bridge, this);
        }
    }

    @Override
    public void added(Thing thing) {
        if (thing instanceof Bridge) {
            for (ChildDiscoveryServiceModule participant : participants) {
                discoverThings(participant, (Bridge) thing);
            }
        }
    }

    @Override
    public void removed(Thing thing) {
        if (thing instanceof Bridge) {
            thingRemoved(thing.getUID());
        }
    }

    @Override
    public void updated(Thing oldThing, Thing thing) {
        added(thing);
    }

    @Override
    protected void startBackgroundDiscovery() {
        thingRegistry.addRegistryChangeListener(this);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        thingRegistry.removeRegistryChangeListener(this);
    }

    @Override
    protected void startScan() {
        for (ChildDiscoveryServiceModule participant : participants) {
            thingRegistry.stream().filter(t -> t instanceof Bridge)
                    .forEach(b -> discoverThings(participant, (Bridge) b));
        }
        thingRegistry.addRegistryChangeListener(this);
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
        if (!isBackgroundDiscoveryEnabled()) {
            thingRegistry.removeRegistryChangeListener(this);
        }
    }

    @Override
    public void thingDiscovered(@Nullable DiscoveryResult discoveryResult) {
        if (discoveryResult != null) {
            thingDiscovered(discoveryResult);
        }
    }

    @Override
    public void thingRemoved(ThingUID thingUID) {
        super.thingRemoved(thingUID);
    }
}
