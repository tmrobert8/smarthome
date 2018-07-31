package org.eclipse.smarthome.config.discovery.child;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.core.thing.ThingUID;

@NonNullByDefault
public interface ChildDiscoveryCallback {
    void thingDiscovered(DiscoveryResult discoveryResult);

    void thingRemoved(ThingUID thingUid);
}
