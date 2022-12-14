package pink.zak.minestom.towerdefence.cache;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Vec;
import pink.zak.minestom.towerdefence.TowerDefenceModule;
import pink.zak.minestom.towerdefence.utils.FileUtils;

import java.util.List;
import java.util.stream.StreamSupport;

public class DamageIndicatorCache {
    private final List<Vec> preCalculatedVelocity;

    public DamageIndicatorCache(TowerDefenceModule plugin) {
        JsonObject jsonObject = FileUtils.getResourceJson("precalculated/damageIndicators.json").getAsJsonObject();
        this.preCalculatedVelocity = this.parseVelocity(jsonObject);
    }

    public Vec[] getPreCalculatedVelocity() {
        return this.preCalculatedVelocity.toArray(new Vec[0]);
    }

    private List<Vec> parseVelocity(JsonObject jsonObject) {
        return StreamSupport.stream(jsonObject.get("velocity").getAsJsonArray()
                        .spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(json -> new Vec(
                        json.get("x").getAsDouble(),
                        json.get("y").getAsDouble(),
                        json.get("z").getAsDouble()
                )).toList();
    }
}
