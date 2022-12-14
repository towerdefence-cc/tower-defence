package pink.zak.minestom.towerdefence.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pink.zak.minestom.towerdefence.TowerDefenceModule;
import pink.zak.minestom.towerdefence.enums.TowerType;
import pink.zak.minestom.towerdefence.model.tower.config.Tower;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TowerStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(TowerStorage.class);

    private final Map<TowerType, Tower> towers = new HashMap<>();

    public TowerStorage(TowerDefenceModule plugin) {
        this.load();
    }

    private void load() {
        for (TowerType towerType : TowerType.values()) {
            this.loadTower(towerType.toString().toLowerCase());
        }
    }

    private void loadTower(String towerName) {
        String basePath = "towers/%s".formatted(towerName);
        String towerJsonPath = "%s/%s.json".formatted(basePath, towerName);

        InputStream inputStream = TowerDefenceModule.class.getClassLoader().getResourceAsStream(towerJsonPath);
        if (inputStream == null) {
            LOGGER.error("Could not find tower file: " + towerJsonPath);
            return;
        }
        JsonObject towerJson = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
        TowerType towerType = TowerType.valueOf(towerJson.get("type").getAsString());

        Map<Integer, JsonObject> levelJson = new HashMap<>();
        for (int level = 1; level <= 10; level++) {
            String levelJsonPath = "%s/%s.json".formatted(basePath, level);

            InputStream levelInputStream = TowerDefenceModule.class.getClassLoader().getResourceAsStream(levelJsonPath);
            if (levelInputStream == null) break;

            levelJson.put(level, JsonParser.parseReader(new InputStreamReader(levelInputStream)).getAsJsonObject());
        }

        Tower tower = towerType.getTowerFunction().apply(towerJson, levelJson);

        this.towers.put(towerType, tower);
    }

    public Map<TowerType, Tower> getTowers() {
        return this.towers;
    }

    public Tower getTower(TowerType towerType) {
        return this.towers.get(towerType);
    }

    public @Nullable Tower getTower(int guiSlot) {
        for (Tower tower : this.towers.values()) {
            if (tower.getGuiSlot() == guiSlot) return tower;
        }
        return null;
    }
}
