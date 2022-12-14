package pink.zak.minestom.towerdefence.game.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import pink.zak.minestom.towerdefence.TowerDefenceModule;
import pink.zak.minestom.towerdefence.enums.GameState;
import pink.zak.minestom.towerdefence.game.GameHandler;
import pink.zak.minestom.towerdefence.game.TowerHandler;
import pink.zak.minestom.towerdefence.model.map.TowerMap;
import pink.zak.minestom.towerdefence.model.tower.config.Tower;
import pink.zak.minestom.towerdefence.model.tower.config.TowerLevel;
import pink.zak.minestom.towerdefence.model.user.GameUser;
import pink.zak.minestom.towerdefence.storage.TowerStorage;

public class TowerPlaceHandler {
    private final @NotNull TowerDefenceModule module;
    private final @NotNull GameHandler gameHandler;
    private final @NotNull TowerHandler towerHandler;
    private final @NotNull TowerStorage towerStorage;

    private final @NotNull Inventory towerPlaceGui;
    private final @NotNull TowerMap towerMap;

    public TowerPlaceHandler(@NotNull TowerDefenceModule module, @NotNull GameHandler gameHandler) {
        this.module = module;
        this.gameHandler = gameHandler;
        this.towerHandler = gameHandler.getTowerHandler();
        this.towerStorage = module.getTowerStorage();

        this.towerPlaceGui = this.createTowerPlaceGui();
        this.towerMap = module.getInstance().getTowerMap();

        module.getEventNode()
                .addListener(PlayerBlockInteractEvent.class, event -> {
                    Player player = event.getPlayer();
                    if (event.getHand() != Player.Hand.MAIN || module.getGameState() != GameState.GAME)
                        return;
                    GameUser gameUser = this.gameHandler.getGameUser(player);
                    if (gameUser == null || event.getBlock().registry().material() != this.towerMap.getTowerBaseMaterial())
                        return;
                    gameUser.setLastClickedTowerBlock(event.getBlockPosition().add(0.5, 0.5, 0.5));
                    if (!this.towerMap.getArea(gameUser.getTeam()).isWithin(gameUser.getLastClickedTowerBlock())) {
                        player.sendMessage(Component.text("You can only place towers on your side of the map (" + gameUser.getTeam().name().toLowerCase() + ").", NamedTextColor.RED));
                        return;
                    }
                    player.openInventory(this.towerPlaceGui);
                });
    }

    private @NotNull Inventory createTowerPlaceGui() {
        TextComponent title = Component.text("Place a Tower", NamedTextColor.DARK_GRAY);
        Inventory inventory = new Inventory(InventoryType.CHEST_3_ROW, title);

        for (Tower tower : this.towerStorage.getTowers().values()) {
            inventory.setItemStack(tower.getGuiSlot(), tower.getBaseMenuItem());
        }

        this.module.getEventNode().addListener(InventoryPreClickEvent.class, event -> {
            Inventory checkInventory = event.getInventory();
            if (event.getClickType() == ClickType.START_DOUBLE_CLICK || checkInventory == null || checkInventory.getTitle() != title)
                return;
            event.setCancelled(true);
            Tower clickedTower = this.towerStorage.getTower(event.getSlot());
            if (clickedTower == null) return;

            Player player = event.getPlayer();
            player.closeInventory();
            this.requestTowerBuy(player, clickedTower);
        });

        return inventory;
    }

    private void requestTowerBuy(@NotNull Player player, @NotNull Tower tower) {
        GameUser gameUser = this.gameHandler.getGameUser(player);
        TowerLevel level = tower.getLevel(1);

        int coins = gameUser.getCoins();
        if (level.getCost() > coins) {
            player.sendMessage(Component.text("You do not have enough money to buy this tower", NamedTextColor.RED));
            return;
        }
        Point basePoint = gameUser.getLastClickedTowerBlock();
        Material placeMaterial = this.towerMap.getTowerBaseMaterial();
        if (!tower.isSpaceClear(player.getInstance(), basePoint, placeMaterial)) {
            player.sendMessage(Component.text("Cannot place a tower as the area is not clear", NamedTextColor.RED));
            return;
        }
        gameUser.updateAndGetCoins(current -> current - level.getCost());
        this.towerHandler.createTower(tower, gameUser);
    }
}
