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

package org.geysermc.geyser.translator.protocol.java.entity;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.MathUtils;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.Objects;

@Translator(packet = ClientboundTeleportEntityPacket.class)
public class JavaTeleportEntityTranslator extends PacketTranslator<ClientboundTeleportEntityPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundTeleportEntityPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.id());

        boolean isPreviouslyRemovedVehicle = false;
        if (entity == null && Objects.equals(session.getPlayerEntity().getRemovedPlayerVehicleId(), packet.id())) {
            entity = session.getPlayerEntity();
            isPreviouslyRemovedVehicle = true;
        }
        if (entity == null) {
            return;
        }

        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
            new PositionMoveRotation(MathUtils.toVec3(entity.position()), MathUtils.toVec3(entity.getMotion()), entity.getYaw(), entity.getPitch()),
            packet.change(), packet.relatives());
        Vector3d position = MathUtils.toVector3d(absolute.position());
        float newPitch = MathUtils.clamp(absolute.xRot(), -90, 90);
        float newYaw = absolute.yRot();

        boolean hasRelative = packet.relatives().contains(Relative.X) || packet.relatives().contains(Relative.Y) || packet.relatives().contains(Relative.Z);
        boolean interpolate = (entity instanceof LivingEntity || hasRelative) && entity.position().distance(position.toFloat()) < 4096.0;

        if (!interpolate) {
            entity.teleport(position.toFloat(), newYaw, newPitch, packet.onGround());
        } else {
            final Vector3d currentPosition = entity.position().toDouble();
            entity.moveRelative(position.getX() - currentPosition.getX(), position.getY() - currentPosition.getY(), position.getZ() - currentPosition.getZ(), newYaw, newPitch, newYaw, packet.onGround());
        }

        Vector3f deltaMovement = MathUtils.toVector3f(absolute.deltaMovement());

        entity.setMotion(deltaMovement);
        if (deltaMovement.distanceSquared(Vector3f.ZERO) > 1.0E-8F) {
            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(entity.geyserId());
            entityMotionPacket.setMotion(entity.getMotion());
            session.sendUpstreamPacket(entityMotionPacket);
        }

        if (!interpolate && !entity.getPassengers().isEmpty() && entity.getPassengers().getFirst() == session.getPlayerEntity() && !isPreviouslyRemovedVehicle) {
            ServerboundMoveVehiclePacket vehiclePacket = new ServerboundMoveVehiclePacket(net.minecraft.core.PositionAndRotation.of(org.geysermc.geyser.util.MathUtils.toVec3(position), newYaw, newPitch), entity.isOnGround());
            session.sendDownstreamGamePacket(vehiclePacket);
        }

        if (isPreviouslyRemovedVehicle) {
            ServerboundMovePlayerPacket.PosRot positionPacket = new ServerboundMovePlayerPacket.PosRot(position.getX(), position.getY(), position.getZ(), newYaw, newPitch, false, false);
            session.sendDownstreamGamePacket(positionPacket);
        }
    }
}
