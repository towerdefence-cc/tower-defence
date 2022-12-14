package pink.zak.minestom.towerdefence.model.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import pink.zak.minestom.towerdefence.enums.Team;

import java.util.ArrayList;
import java.util.List;

public class TowerMap {
    private Pos spectatorSpawn;
    private Pos redSpawn;
    private Pos blueSpawn;
    private Area redArea;
    private Area blueArea;

    private Pos redMobSpawn;
    private Pos blueMobSpawn;

    private Pos redTowerHologram;
    private Pos blueTowerHologram;

    private int pathWidth;

    private List<PathCorner> redCorners;
    private List<PathCorner> blueCorners;

    private Material towerBaseMaterial;

    public TowerMap() {
    }

    public TowerMap(Pos spectatorSpawn, Pos redSpawn, Pos blueSpawn,
                    Pos redTowerHologram, Pos blueTowerHologram,
                    Area redArea, Area blueArea,
                    Pos redMobSpawn, Pos blueMobSpawn,
                    int pathWidth, List<PathCorner> redCorners, List<PathCorner> blueCorners,
                    Material towerBaseMaterial) {
        this.spectatorSpawn = spectatorSpawn;
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.redTowerHologram = redTowerHologram;
        this.blueTowerHologram = blueTowerHologram;
        this.redArea = redArea;
        this.blueArea = blueArea;
        this.redMobSpawn = redMobSpawn;
        this.blueMobSpawn = blueMobSpawn;
        this.pathWidth = pathWidth;
        this.redCorners = redCorners;
        this.blueCorners = blueCorners;
        this.towerBaseMaterial = towerBaseMaterial;
    }

    public static TowerMap fromJson(JsonObject jsonObject) {
        Pos spectatorSpawn = null;
        Pos redSpawn = null;
        Pos blueSpawn = null;
        Area redArea = null;
        Area blueArea = null;

        int pathWidth;

        Pos redMobSpawn = null;
        Pos blueMobSpawn = null;

        Pos redTowerHologram = null;
        Pos blueTowerHologram = null;

        List<PathCorner> redCorners = null;
        List<PathCorner> blueCorners = null;

        Material towerBaseMaterial = null;

        pathWidth = jsonObject.get("pathWidth").getAsInt();
        if (jsonObject.has("spectatorSpawn"))
            spectatorSpawn = jsonToPosition(jsonObject.getAsJsonObject("spectatorSpawn"));
        if (jsonObject.has("redSpawn"))
            redSpawn = jsonToPosition(jsonObject.getAsJsonObject("redSpawn"));
        if (jsonObject.has("blueSpawn"))
            blueSpawn = jsonToPosition(jsonObject.getAsJsonObject("blueSpawn"));
        if (jsonObject.has("redTowerHologram"))
            redTowerHologram = jsonToPosition(jsonObject.getAsJsonObject("redTowerHologram"));
        if (jsonObject.has("blueTowerHologram"))
            blueTowerHologram = jsonToPosition(jsonObject.getAsJsonObject("blueTowerHologram"));
        if (jsonObject.has("redArea"))
            redArea = Area.fromJson(jsonObject.getAsJsonObject("redArea"));
        if (jsonObject.has("blueArea"))
            blueArea = Area.fromJson(jsonObject.getAsJsonObject("blueArea"));
        if (jsonObject.has("redMobSpawn"))
            redMobSpawn = jsonToPosition(jsonObject.getAsJsonObject("redMobSpawn"));
        if (jsonObject.has("blueMobSpawn"))
            blueMobSpawn = jsonToPosition(jsonObject.getAsJsonObject("blueMobSpawn"));
        if (jsonObject.has("redCorners"))
            redCorners = jsonToPathCorners(jsonObject.getAsJsonArray("redCorners"));
        if (jsonObject.has("blueCorners"))
            blueCorners = jsonToPathCorners(jsonObject.getAsJsonArray("blueCorners"));
        if (jsonObject.has("towerBaseMaterial"))
            towerBaseMaterial = Material.fromNamespaceId(jsonObject.get("towerBaseMaterial").getAsString());

        return new TowerMap(spectatorSpawn, redSpawn, blueSpawn,
                redTowerHologram, blueTowerHologram,
                redArea, blueArea,
                redMobSpawn, blueMobSpawn,
                pathWidth, redCorners, blueCorners,
                towerBaseMaterial);
    }

    private static List<PathCorner> jsonToPathCorners(JsonArray jsonElements) {
        List<PathCorner> corners = new ArrayList<>();
        for (JsonElement jsonElement : jsonElements) {
            corners.add(PathCorner.fromJson(jsonElement.getAsJsonObject()));
        }
        return corners;
    }

    private static Pos jsonToPosition(JsonObject jsonObject) {
        double x = jsonObject.get("x").getAsDouble();
        double y = jsonObject.get("y").getAsDouble();
        double z = jsonObject.get("z").getAsDouble();
        float yaw = jsonObject.get("yaw").getAsFloat();
        float pitch = jsonObject.get("pitch").getAsFloat();

        return new Pos(x, y, z, yaw, pitch);
    }

    public Pos getSpectatorSpawn() {
        return this.spectatorSpawn;
    }

    public void setSpectatorSpawn(Pos spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public Pos getSpawn(Team team) {
        return team == Team.RED ? this.redSpawn : this.blueSpawn;
    }

    public Pos getRedSpawn() {
        return this.redSpawn;
    }

    public void setRedSpawn(Pos redSpawn) {
        this.redSpawn = redSpawn;
    }

    public Pos getBlueSpawn() {
        return this.blueSpawn;
    }

    public void setBlueSpawn(Pos blueSpawn) {
        this.blueSpawn = blueSpawn;
    }

    public Area getArea(Team team) {
        return team == Team.RED ? this.redArea : this.blueArea;
    }

    public Area getRedArea() {
        return this.redArea;
    }

    public void setRedArea(Area redArea) {
        this.redArea = redArea;
    }

    public Area getBlueArea() {
        return this.blueArea;
    }

    public void setBlueArea(Area blueArea) {
        this.blueArea = blueArea;
    }

    public int getRandomValue() {
        if (this.pathWidth % 2 == 0)
            return this.pathWidth / 2 - 2;
        return ((this.pathWidth - 1) / 2) - 1;
    }

    public int getPathWidth() {
        return this.pathWidth;
    }

    public Pos getMobSpawn(Team team) {
        return team == Team.RED ? this.redMobSpawn : this.blueMobSpawn;
    }

    public Pos getRedMobSpawn() {
        return this.redMobSpawn;
    }

    public void setRedMobSpawn(Pos redMobSpawn) {
        this.redMobSpawn = redMobSpawn;
    }

    public Pos getBlueMobSpawn() {
        return this.blueMobSpawn;
    }

    public void setBlueMobSpawn(Pos blueMobSpawn) {
        this.blueMobSpawn = blueMobSpawn;
    }

    public Pos getRedTowerHologram() {
        return this.redTowerHologram;
    }

    public void setRedTowerHologram(Pos redTowerHologram) {
        this.redTowerHologram = redTowerHologram;
    }

    public Pos getBlueTowerHologram() {
        return this.blueTowerHologram;
    }

    public void setBlueTowerHologram(Pos blueTowerHologram) {
        this.blueTowerHologram = blueTowerHologram;
    }

    public List<PathCorner> getCorners(Team team) {
        return team == Team.RED ? this.redCorners : this.blueCorners;
    }

    public List<PathCorner> getRedCorners() {
        return this.redCorners;
    }

    public List<PathCorner> getBlueCorners() {
        return this.blueCorners;
    }

    public Material getTowerBaseMaterial() {
        return this.towerBaseMaterial;
    }

    public void setTowerBaseMaterial(Material towerBaseMaterial) {
        this.towerBaseMaterial = towerBaseMaterial;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("pathWidth", this.pathWidth);
        if (this.spectatorSpawn != null)
            jsonObject.add("spectatorSpawn", this.posToJson(this.spectatorSpawn));
        if (this.redSpawn != null)
            jsonObject.add("redSpawn", this.posToJson(this.redSpawn));
        if (this.blueSpawn != null)
            jsonObject.add("blueSpawn", this.posToJson(this.blueSpawn));
        if (this.redTowerHologram != null)
            jsonObject.add("redTowerHologram", this.posToJson(this.redTowerHologram));
        if (this.blueTowerHologram != null)
            jsonObject.add("blueTowerHologram", this.posToJson(this.blueTowerHologram));
        if (this.redArea != null)
            jsonObject.add("redArea", this.redArea.toJsonObject());
        if (this.blueArea != null)
            jsonObject.add("blueArea", this.blueArea.toJsonObject());
        if (this.redMobSpawn != null)
            jsonObject.add("redMobSpawn", this.posToJson(this.redMobSpawn));
        if (this.blueMobSpawn != null)
            jsonObject.add("blueMobSpawn", this.posToJson(this.blueMobSpawn));
        if (this.redCorners != null)
            jsonObject.add("redCorners", this.cornersToJsonArray(this.redCorners));
        if (this.blueCorners != null)
            jsonObject.add("blueCorners", this.cornersToJsonArray(this.blueCorners));
        if (this.towerBaseMaterial != null)
            jsonObject.addProperty("towerBaseMaterial", this.towerBaseMaterial.name());

        return jsonObject;
    }

    private JsonArray cornersToJsonArray(List<PathCorner> corners) {
        JsonArray jsonElements = new JsonArray();
        for (PathCorner corner : corners) {
            jsonElements.add(corner.toJson());
        }
        return jsonElements;
    }

    private JsonObject posToJson(Pos position) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("x", position.x());
        jsonObject.addProperty("y", position.y());
        jsonObject.addProperty("z", position.z());
        jsonObject.addProperty("yaw", position.yaw());
        jsonObject.addProperty("pitch", position.pitch());

        return jsonObject;
    }
}
