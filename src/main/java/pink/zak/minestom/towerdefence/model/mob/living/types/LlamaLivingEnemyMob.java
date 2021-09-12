package pink.zak.minestom.towerdefence.model.mob.living.types;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import pink.zak.minestom.towerdefence.game.MobHandler;
import pink.zak.minestom.towerdefence.game.TowerHandler;
import pink.zak.minestom.towerdefence.model.GameUser;
import pink.zak.minestom.towerdefence.model.map.TowerMap;
import pink.zak.minestom.towerdefence.model.mob.EnemyMob;
import pink.zak.minestom.towerdefence.model.mob.living.LivingEnemyMob;
import pink.zak.minestom.towerdefence.utils.DirectionUtils;

public class LlamaLivingEnemyMob extends LivingEnemyMob {

    public LlamaLivingEnemyMob(TowerHandler towerHandler, MobHandler mobHandler, @NotNull EnemyMob enemyMob, Instance instance, TowerMap map, GameUser gameUser, int level) {
        super(towerHandler, mobHandler, enemyMob, instance, map, gameUser, level);
    }

    @Override
    protected void attackCastle() {
        Pos position = this.getPosition();
        Entity spit = new Entity(EntityType.LLAMA_SPIT);
        Direction direction = this.currentCorner.direction();
        spit.setNoGravity(true);
        spit.setInstance(this.instance, DirectionUtils.add(position.add(0, EntityType.LLAMA.height(), 0), direction, 1));
        spit.setVelocity(DirectionUtils.createVec(direction, 7));
        spit.scheduleRemove(10, TimeUnit.CLIENT_TICK);
    }
}
