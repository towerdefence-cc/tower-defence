package pink.zak.minestom.towerdefence.model.mob.living;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.NotNull;
import pink.zak.minestom.towerdefence.game.MobHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DamageIndicator {
    private static final Cache<Double, Component> NAME_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    private static final int VIEW_DISTANCE = 10;
    private static final int VIEW_DISTANCE_SQUARED = VIEW_DISTANCE * VIEW_DISTANCE;

    private static final Vec[] VECTORS = MobHandler.DAMAGE_INDICATOR_CACHE.getPreCalculatedVelocity();

    private final @NotNull Set<Player> viewers;
    private final int entityId;

    private final @NotNull AtomicInteger aliveTicks = new AtomicInteger(0);
    private final @NotNull Task tickTask;

    private @NotNull Pos position;

    private DamageIndicator(@NotNull Set<Player> viewers, @NotNull Pos spawnPosition, @NotNull Component text) {
        this.viewers = viewers;
        this.entityId = Entity.generateId();

        SpawnEntityPacket spawnPacket = new SpawnEntityPacket(
                this.entityId, UUID.randomUUID(),
                EntityType.AREA_EFFECT_CLOUD.id(), spawnPosition,
                0, 0, (short) 0, (short) 0, (short) 0
        );

        int entityOffset = EntityMeta.OFFSET;
        int areaEffectCloudOffset = AreaEffectCloudMeta.OFFSET;

        EntityMetaDataPacket metaDataPacket = new EntityMetaDataPacket(
                this.entityId, Map.of(
                areaEffectCloudOffset, Metadata.Float(0f), // Radius
                entityOffset + 4, Metadata.Boolean(true), // Silent
                entityOffset + 2, Metadata.OptChat(text), // Custom name
                entityOffset + 3, Metadata.Boolean(true), // Custom name visible
                entityOffset + 5, Metadata.Boolean(true) // Has no gravity
        ));

        PacketUtils.sendGroupedPacket(this.viewers, spawnPacket);
        PacketUtils.sendGroupedPacket(this.viewers, metaDataPacket);

        this.tickTask = MinecraftServer.getSchedulerManager().buildTask(this::tick)
                .repeat(TaskSchedule.nextTick())
                .schedule();

        this.position = spawnPosition;
    }

    /**
     * Creates a damage indicator for the given mob with the given damage
     * NOTE: A damage indicator will not be created if it will not be visible to any players.
     *
     * @param enemyMob The mob to create the damage indicator for
     * @param damage   The damage to display
     */
    public static void create(@NotNull LivingTDEnemyMob enemyMob, double damage) {
        Component text = NAME_CACHE.get(damage, key -> Component.text(damage, NamedTextColor.RED));

        Set<Player> players = new HashSet<>();
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (player.getPosition().distanceSquared(enemyMob.getPosition()) < VIEW_DISTANCE_SQUARED) {
                players.add(player);
            }
        }

        if (!players.isEmpty())
            new DamageIndicator(players, enemyMob.getPosition().add(0, enemyMob.getTDEntityType().height(), 0), text);
    }

    public void tick() {
        int aliveTicks = this.aliveTicks.incrementAndGet();
        if (aliveTicks == 16) {
            this.remove();
            return;
        }

        Vec velocity = VECTORS[aliveTicks];

        double prevX = this.position.x();
        double prevY = this.position.y();
        double prevZ = this.position.z();

        this.position = this.position.add(velocity.div(20));

        double newX = this.position.x();
        double newY = this.position.y();
        double newZ = this.position.z();

        short deltaX = (short) ((newX * 32 - prevX * 32) * 128);
        short deltaY = (short) ((newY * 32 - prevY * 32) * 128);
        short deltaZ = (short) ((newZ * 32 - prevZ * 32) * 128);

        EntityVelocityPacket velocityPacket = new EntityVelocityPacket(this.entityId, velocity);
        EntityPositionPacket positionPacket = new EntityPositionPacket(this.entityId, deltaX, deltaY, deltaZ, false);

        // Remove players that have left the game
        this.viewers.removeIf(viewer -> !viewer.isOnline());

        PacketUtils.sendGroupedPacket(this.viewers, positionPacket);
        PacketUtils.sendGroupedPacket(this.viewers, velocityPacket);
    }

    private void remove() {
        this.tickTask.cancel();

        DestroyEntitiesPacket destroyEntitiesPacket = new DestroyEntitiesPacket(this.entityId);
        PacketUtils.sendGroupedPacket(this.viewers, destroyEntitiesPacket);
    }
}
