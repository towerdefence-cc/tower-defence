package pink.zak.minestom.towerdefence.model.mob.living;

import dev.emortal.minestom.core.Environment;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pink.zak.minestom.towerdefence.enums.Team;
import pink.zak.minestom.towerdefence.game.GameHandler;
import pink.zak.minestom.towerdefence.game.MobHandler;
import pink.zak.minestom.towerdefence.game.TowerHandler;
import pink.zak.minestom.towerdefence.model.DamageSource;
import pink.zak.minestom.towerdefence.model.map.PathCorner;
import pink.zak.minestom.towerdefence.model.map.TowerMap;
import pink.zak.minestom.towerdefence.model.mob.config.EnemyMob;
import pink.zak.minestom.towerdefence.model.mob.config.EnemyMobLevel;
import pink.zak.minestom.towerdefence.model.mob.modifier.SpeedModifier;
import pink.zak.minestom.towerdefence.model.mob.statuseffect.StatusEffect;
import pink.zak.minestom.towerdefence.model.mob.statuseffect.StatusEffectType;
import pink.zak.minestom.towerdefence.model.tower.placed.PlacedAttackingTower;
import pink.zak.minestom.towerdefence.model.tower.placed.PlacedTower;
import pink.zak.minestom.towerdefence.model.tower.placed.types.CharityTower;
import pink.zak.minestom.towerdefence.model.tower.placed.types.NecromancerTower;
import pink.zak.minestom.towerdefence.model.user.GameUser;
import pink.zak.minestom.towerdefence.utils.DirectionUtil;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;

public class SingleEnemyTDMob extends SingleTDMob implements LivingTDEnemyMob {
    protected final TowerHandler towerHandler;
    protected final MobHandler mobHandler;
    protected final EnemyMob enemyMob;
    protected final EnemyMobLevel level;
    protected final Team team;
    protected final GameUser sender;

    protected final int positionModifier;
    protected final List<PathCorner> corners;

    protected final Map<StatusEffectType, StatusEffect<?>> statusEffects = Collections.synchronizedMap(new EnumMap<>(StatusEffectType.class));
    protected final Set<SpeedModifier> speedModifiers = new CopyOnWriteArraySet<>();

    private final GameHandler gameHandler;

    protected int currentCornerIndex;
    protected PathCorner currentCorner;
    protected PathCorner nextCorner;
    protected int currentCornerLengthModifier;
    protected double moveDistance;
    protected double totalDistanceMoved;

    protected Set<PlacedAttackingTower<?>> attackingTowers = ConcurrentHashMap.newKeySet();
    protected float health;

    private Task attackTask;

    public SingleEnemyTDMob(@NotNull GameHandler gameHandler, @NotNull EnemyMob enemyMob, int level,
                            @NotNull Instance instance, @NotNull TowerMap map,
                            @NotNull GameUser gameUser) {
        this(gameHandler, enemyMob, level, instance, map, gameUser, enemyMob.getLevel(level).getEntityType());
    }

    public SingleEnemyTDMob(@NotNull GameHandler gameHandler, @NotNull EnemyMob enemyMob, int level,
                            @NotNull Instance instance, @NotNull TowerMap map,
                            @NotNull GameUser gameUser, @NotNull EntityType entityType) {

        super(entityType, level);

        this.gameHandler = gameHandler;

        this.towerHandler = gameHandler.getTowerHandler();
        this.mobHandler = gameHandler.getMobHandler();
        this.enemyMob = enemyMob;
        this.level = enemyMob.getLevel(level);

        if (Environment.isProduction()) this.team = gameUser.getTeam() == Team.RED ? Team.BLUE : Team.RED;
        else this.team = gameUser.getTeam();

        this.sender = gameUser;

        this.positionModifier = ThreadLocalRandom.current().nextInt(-map.getRandomValue(), map.getRandomValue() + 1);
        this.corners = map.getCorners(this.team);
        this.currentCornerIndex = 0;
        this.currentCorner = this.corners.get(0);
        this.nextCorner = this.corners.get(1);

        this.currentCornerLengthModifier = this.getRandomLengthModifier();

        this.getAttribute(Attribute.MAX_HEALTH).setBaseValue(this.level.getHealth());
        this.health = this.level.getHealth();

        this.setCustomNameVisible(true);
        if (enemyMob.isFlying())
            this.setNoGravity(true);

        Pos spawnPos = this.team == Team.RED ? map.getRedMobSpawn() : map.getBlueMobSpawn();
        this.setInstance(instance, spawnPos.add(this.positionModifier, enemyMob.isFlying() ? 5 : 0, this.positionModifier));
    }

    @Override
    public void tick(long time) {
        for (StatusEffect<?> statusEffect : this.statusEffects.values()) statusEffect.tick(time);

        if (this.attackTask == null && !this.isDead())
            this.updatePos();
        super.tick(time);
    }

    private void updatePos() {
        double movement = this.level.getMovementSpeed();
        for (SpeedModifier speedModifier : this.speedModifiers) movement *= speedModifier.getModifier();

        Pos oldPos = this.getPosition();
        Pos newPos = this.modifyPosition(movement);
        this.teleport(newPos);
        if (oldPos.yaw() != newPos.yaw() || oldPos.pitch() != newPos.pitch())
            for (Entity passenger : this.getPassengers())
                passenger.refreshPosition(passenger.getPosition().withView(newPos.yaw(), newPos.pitch()));

        this.moveDistance += movement;
        this.totalDistanceMoved += movement;
        if (this.nextCorner == null) {
            // detect if we've reached the end
            int modifier = this.positionModifier;
            if (this.currentCorner.modify()) {
                if (this.currentCorner.negativeModifier()) modifier = -modifier;
            }
            if (this.moveDistance >= this.currentCorner.distance() - modifier) {
                this.nextCorner();
            }
        } else if (this.moveDistance >= this.currentCorner.distance() + this.currentCornerLengthModifier) {
            this.nextCorner();
        }
    }

    private Pos modifyPosition(double movement) {
        Pos currentPos = this.getPosition();

        Pos newPos = switch (this.currentCorner.direction()) {
            case EAST -> currentPos.add(movement, 0, 0);
            case SOUTH -> currentPos.add(0, 0, movement);
            case WEST -> currentPos.sub(movement, 0, 0);
            case NORTH -> currentPos.sub(0, 0, movement);
            default ->
                    throw new IllegalArgumentException("Direction must be NORTH, EAST, SOUTH or WEST. Provided direction was %s".formatted(this.currentCorner.direction()));
        };
        // todo this can be removed in the majority of cases to reduce pos creations
        return newPos.withView(DirectionUtil.getYaw(this.currentCorner.direction()), 0);
    }

    private void nextCorner() {
        int newCornerIndex = ++this.currentCornerIndex;
        int cornerSize = this.corners.size();
        if (this.nextCorner == null) {
            this.currentCornerIndex = -1;
            this.startAttackingCastle();
            return;
        }
        this.currentCorner = this.nextCorner;
        if (++newCornerIndex == cornerSize)
            this.nextCorner = null;
        else
            this.nextCorner = this.corners.get(newCornerIndex);

        this.currentCornerLengthModifier = this.getRandomLengthModifier();
        this.moveDistance = 0;
    }

    protected void attackCastle() {
        this.swingMainHand();
        this.swingOffHand();
        this.damageCastle();
    }

    protected void damageCastle() {
        this.gameHandler.damageTower(this.team, this.level.getDamage());
    }

    protected void startAttackingCastle() {
        this.attackTask = MinecraftServer.getSchedulerManager()
                .buildTask(this::attackCastle)
                .repeat(2, ChronoUnit.SECONDS)
                .schedule();
    }

    @Override
    public void kill() {
        this.refreshIsDead(true);
        if (this.attackTask != null)
            this.attackTask.cancel();

        for (PlacedAttackingTower<?> tower : this.attackingTowers)
            tower.getTargets().remove(this);

        this.getMobHandler().getMobs(this.getTDTeam()).remove(this);

        super.kill();
    }

    public float damage(@NotNull DamageSource source, float value) {
        if (this.isDead) return 0;
        if (this.enemyMob.isDamageTypeIgnored(source.getDamageType())) return 0;

        DamageIndicator.create(this, value);

        this.sendPacketToViewersAndSelf(new EntityAnimationPacket(this.getEntityId(), EntityAnimationPacket.Animation.TAKE_DAMAGE));
        this.setHealth(this.health - value);
        float damageDealt = this.isDead ? value : value - Math.abs(this.health);

        if (this.isDead) {
            double multiplier = 1;
            boolean necromanced = false;

            for (PlacedTower<?> tower : this.towerHandler.getTowers(this.team)) {
                if (tower.getBasePoint().distance(this.position) > tower.getLevel().getRange()) continue;
                if (tower instanceof CharityTower charityTower) {
                    double tempMultiplier = charityTower.getLevel().getMultiplier();
                    if (tempMultiplier > multiplier) multiplier = tempMultiplier;
                } else if (!necromanced && tower instanceof NecromancerTower necromancerTower) {
                    necromancerTower.createNecromancedMob(this);
                    necromanced = true;
                }
            }
            double finalMultiplier = multiplier;
            source.getOwningUser().updateAndGetCoins(current -> (int) Math.floor(current + (this.level.getKillReward() * finalMultiplier)));
            source.getOwningUser().updateAndGetMana(current -> current + this.level.getManaKillReward());
        }

        final SoundEvent sound = DamageType.VOID.getSound(this);
        if (sound != null) {
            Sound.Source soundCategory = Sound.Source.PLAYER;

            SoundEffectPacket damageSoundPacket = new SoundEffectPacket(sound, soundCategory, this.getPosition(), 1.0f, 1.0f);
            this.sendPacketToViewersAndSelf(damageSoundPacket);
        }

        return damageDealt;
    }

    @Override
    public void updateCustomName() {
        for (Player player : this.getViewers()) {
            Component value = this.createNameComponent(player);
            Metadata.Entry<?> nameEntry = Metadata.OptChat(value);
            player.sendPacket(new EntityMetaDataPacket(this.getEntityId(), Map.of(2, nameEntry)));
        }
    }

    @Override
    public boolean damage(@NotNull DamageType type, float value) {
        throw new UnsupportedOperationException("Use damage(DamageSource, float) instead");
    }

    @Override
    public float getHealth() {
        return this.health;
    }

    @Override
    public void setHealth(float health) {
        this.health = health;
        if (this.health <= 0 && !this.isDead)
            this.kill();

        if (this.level != null && this.isCustomNameVisible())
            this.updateCustomName();
    }

    private int getRandomLengthModifier() {
        if (this.positionModifier == 0 || !this.currentCorner.modify())
            return 0;
        int value = this.positionModifier;
        if (this.currentCorner.multiplyModifier())
            value *= 2;
        if (this.currentCorner.negativeModifier())
            value = -value;
        return value;
    }

    @Override
    public @NotNull Team getTDTeam() {
        return this.team;
    }

    @Override
    public @NotNull EnemyMob getEnemyMob() {
        return this.enemyMob;
    }

    @Override
    public @NotNull EnemyMobLevel getEnemyMobLevel() {
        return this.level;
    }

    @Override
    public double getTotalDistanceMoved() {
        return this.totalDistanceMoved;
    }

    @Override
    public @NotNull Map<StatusEffectType, StatusEffect<?>> getStatusEffects() {
        return this.statusEffects;
    }

    @Override
    public @NotNull Set<SpeedModifier> getSpeedModifiers() {
        return this.speedModifiers;
    }

    @Override
    public @Nullable Task getAttackTask() {
        return this.attackTask;
    }

    @Override
    public @NotNull Set<PlacedAttackingTower<?>> getAttackingTowers() {
        return attackingTowers;
    }

    @Override
    public @NotNull MobHandler getMobHandler() {
        return mobHandler;
    }
}
