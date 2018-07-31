package org.eclipse.smarthome.binding.hue.internal.discovery;

import static org.eclipse.smarthome.binding.hue.HueBindingConstants.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.binding.hue.handler.HueBridgeHandler;
import org.eclipse.smarthome.binding.hue.handler.HueLightHandler;
import org.eclipse.smarthome.binding.hue.handler.LightStatusListener;
import org.eclipse.smarthome.binding.hue.internal.FullLight;
import org.eclipse.smarthome.binding.hue.internal.HueBridge;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.child.ChildDiscoveryCallback;
import org.eclipse.smarthome.config.discovery.child.ChildDiscoveryServiceModule;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;

@NonNullByDefault
public class HueLightChildDiscoveryImpl implements ChildDiscoveryServiceModule {

    // @formatter:off
    private static final Map<String, @Nullable String> TYPE_TO_ZIGBEE_ID_MAP = Stream.of(
            new SimpleEntry<>("on_off_light", "0000"),
            new SimpleEntry<>("on_off_plug_in_unit", "0010"),
            new SimpleEntry<>("dimmable_light", "0100"),
            new SimpleEntry<>("dimmable_plug_in_unit", "0110"),
            new SimpleEntry<>("color_light", "0200"),
            new SimpleEntry<>("extended_color_light", "0210"),
            new SimpleEntry<>("color_temperature_light", "0220")
        ).collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue()));
    // @formatter:on

    @Override
    public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(THING_TYPE_BRIDGE);
    }

    @Override
    public void createResults(Bridge bridge, ChildDiscoveryCallback callback) {
        if (bridge.getHandler() instanceof HueBridgeHandler) {
            final HueBridgeHandler hueBridgeHandler = (HueBridgeHandler) bridge.getHandler();

            hueBridgeHandler.registerLightStatusListener(new LightStatusListener() {

                @Override
                public void onLightStateChanged(@Nullable HueBridge bridge, @NonNull FullLight light) {
                }

                @Override
                public void onLightRemoved(@Nullable HueBridge bridge, @NonNull FullLight light) {
                    ThingUID thingUID = getThingUID(hueBridgeHandler, light);

                    if (thingUID != null) {
                        callback.thingRemoved(thingUID);
                    }
                }

                @Override
                public void onLightAdded(@Nullable HueBridge bridge, @NonNull FullLight light) {
                    onLightAddedInternal(hueBridgeHandler, callback, light);

                }

            });

            final List<FullLight> lights = hueBridgeHandler.getFullLights();
            for (FullLight l : lights) {
                onLightAddedInternal(hueBridgeHandler, callback, l);
            }
            hueBridgeHandler.startSearch();
        }

    }

    private void onLightAddedInternal(HueBridgeHandler hueBridgeHandler, ChildDiscoveryCallback callback,
            FullLight light) {
        ThingUID thingUID = getThingUID(hueBridgeHandler, light);
        ThingTypeUID thingTypeUID = getThingTypeUID(light);

        String modelId = light.getModelID().replaceAll(HueLightHandler.NORMALIZE_ID_REGEX, "_");

        if (thingUID != null && thingTypeUID != null) {
            ThingUID bridgeUID = hueBridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(LIGHT_ID, light.getId());
            properties.put(MODEL_ID, modelId);
            properties.put(LIGHT_UNIQUE_ID, light.getUniqueID());

            callback.thingDiscovered(DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withBridge(bridgeUID).withRepresentationProperty(LIGHT_UNIQUE_ID)
                    .withLabel(light.getName()).build());

        } else {
            // logger.debug("discovered unsupported light of type '{}' and model '{}' with id {}", light.getType(),
            // modelId, light.getId());
        }
    }

    private @Nullable ThingUID getThingUID(HueBridgeHandler hueBridgeHandler, FullLight light) {
        ThingUID bridgeUID = hueBridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = getThingTypeUID(light);

        if (thingTypeUID != null && getSupportedThingTypeUIDs().contains(thingTypeUID)) {
            return new ThingUID(thingTypeUID, bridgeUID, light.getId());
        } else {
            return null;
        }
    }

    private @Nullable ThingTypeUID getThingTypeUID(FullLight light) {
        String thingTypeId = TYPE_TO_ZIGBEE_ID_MAP
                .get(light.getType().replaceAll(HueLightHandler.NORMALIZE_ID_REGEX, "_").toLowerCase());
        return thingTypeId != null ? new ThingTypeUID(BINDING_ID, thingTypeId) : null;
    }

}
