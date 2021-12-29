package pink.zak.minestom.towerdefence.game;

import com.google.common.collect.Sets;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import pink.zak.minestom.towerdefence.TowerDefencePlugin;
import pink.zak.minestom.towerdefence.cache.DamageIndicatorCache;
import pink.zak.minestom.towerdefence.enums.Team;
import pink.zak.minestom.towerdefence.model.GameUser;
import pink.zak.minestom.towerdefence.model.map.TowerMap;
import pink.zak.minestom.towerdefence.model.mob.QueuedEnemyMob;
import pink.zak.minestom.towerdefence.model.mob.living.LivingEnemyMob;
import pink.zak.minestom.towerdefence.model.tower.config.AttackingTowerLevel;
import pink.zak.minestom.towerdefence.model.tower.placed.PlacedAttackingTower;

import java.util.Set;
import java.util.stream.Collectors;

public class MobHandler {
    public static DamageIndicatorCache DAMAGE_INDICATOR_CACHE; // todo spend time to not use static here.

    private final Set<LivingEnemyMob> redSideMobs = Sets.newConcurrentHashSet();
    private final Set<LivingEnemyMob> blueSideMobs = Sets.newConcurrentHashSet();

    private final TowerDefencePlugin plugin;
    private final TowerHandler towerHandler;
    private final TowerMap map;
    private Instance instance;

    private Task attackUpdateTask;

    public MobHandler(GameHandler gameHandler, TowerDefencePlugin plugin) {
        this.plugin = plugin;
        this.towerHandler = gameHandler.getTowerHandler();
        this.map = gameHandler.getMap();

        DAMAGE_INDICATOR_CACHE = new DamageIndicatorCache(plugin);

        this.startUpdatingAttackingTowers();
    }

    public void spawnMob(QueuedEnemyMob queuedEnemyMob, GameUser spawner) {
        LivingEnemyMob mob = LivingEnemyMob.create(this.towerHandler, this, queuedEnemyMob.mob(), queuedEnemyMob.level().getLevel(), this.instance, this.map, spawner);
        if (spawner.getTeam() == Team.RED)
            this.redSideMobs.add(mob); // todo change later
        else
            this.redSideMobs.add(mob);
    }

    private void startUpdatingAttackingTowers() {
        this.attackUpdateTask = MinecraftServer.getSchedulerManager()
            .buildTask(this::updateAttackingTowers)
            .repeat(10, TimeUnit.SERVER_TICK)
            .schedule();
    }

    // todo support both sides
    private void updateAttackingTowers() {
        for (LivingEnemyMob enemyMob : this.redSideMobs) {
            Pos position = enemyMob.getPosition();
            Set<PlacedAttackingTower<?>> newTowersInRange = Sets.newConcurrentHashSet();
            Set<PlacedAttackingTower<?>> oldTowersInRange = enemyMob.getAttackingTowers();
            for (PlacedAttackingTower<?> tower : this.towerHandler.getRedTowers().stream()
                .filter(tower -> tower instanceof PlacedAttackingTower)
                .map(tower -> (PlacedAttackingTower<?>) tower)
                .collect(Collectors.toSet())
            ) {
                double distance = tower.getBasePoint().distance(position);
                if (distance < tower.getLevel().getRange())
                    newTowersInRange.add(tower);
                else if (oldTowersInRange.contains(tower) && tower.getTarget() == enemyMob)
                    tower.setTarget(null);
            }
            enemyMob.setAttackingTowers(newTowersInRange);
        }
        for (LivingEnemyMob enemyMob : this.redSideMobs) {
            for (PlacedAttackingTower<?> tower : enemyMob.getAttackingTowers()) {
                if (
                    (tower.getTarget() == null || tower.getTarget().isDead() || enemyMob.getTotalDistanceMoved() > tower.getTarget().getTotalDistanceMoved())
                        && (!enemyMob.getEnemyMob().isFlying() || tower.getTower().getType().isTargetAir())
                ) {
                    tower.setTarget(enemyMob);
                }
            }
        }
        /*
         * for every mob
         *   -> calculate the towers that can shoot it
         *   -> compare towers that can shoot it to towers that could shoot it before
         *   -> if new tower is added
         *     -> recalculate tower target
         *   -> if tower is removed
         *     -> recalculate tower target
         * */
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Set<LivingEnemyMob> getRedSideMobs() {
        return this.redSideMobs;
    }

    public Set<LivingEnemyMob> getBlueSideMobs() {
        return this.blueSideMobs;
    }
}
