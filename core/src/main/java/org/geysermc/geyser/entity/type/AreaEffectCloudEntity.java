/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.entity.type;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.level.particle.ParticleMapping;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.MathUtils;
import net.minecraft.core.particles.ColorParticleOption;
import org.geysermc.geyser.vanilla.protocol.VanillaParticles;
import net.minecraft.core.particles.ParticleOptions;

public class AreaEffectCloudEntity extends Entity {

    public AreaEffectCloudEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // Without this the cloud doesn't appear,
        metadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_DURATION, Integer.MAX_VALUE);

        // This disabled client side shrink of the cloud
        metadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_RADIUS, 3.0f);
        metadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP, Float.MIN_VALUE);

        //noinspection deprecation - still needed for these to show up
        metadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_CHANGE_RATE, Float.MIN_VALUE);
        setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    public void setRadius(DataValue<Float> entityMetadata) {
        // Anything less than 0.5 will cause the cloud to despawn
        float value = MathUtils.clamp(entityMetadata.value(), 0.5f, 32.0f);
        metadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_RADIUS, value);
        metadata.put(EntityDataTypes.WIDTH, 2.0f * value);
    }

    public void setParticle(DataValue<ParticleOptions> entityMetadata) {
        ParticleOptions particle = entityMetadata.value();
        Registries.PARTICLES.flatMap(particle.getType(), ParticleMapping::particleType)
            .ifPresent(type -> metadata.put(EntityDataTypes.AREA_EFFECT_CLOUD_PARTICLE, type));

        if (particle instanceof ColorParticleOption effectParticleData) {
            metadata.put(EntityDataTypes.EFFECT_COLOR, VanillaParticles.color(effectParticleData));
        }
    }
}
