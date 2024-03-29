package pink.zak.minestom.towerdefence.scoreboard.types;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.scoreboard.Sidebar;
import pink.zak.minestom.towerdefence.TowerDefenceModule;
import pink.zak.minestom.towerdefence.api.event.player.PlayerTeamSwitchEvent;
import pink.zak.minestom.towerdefence.enums.Team;
import pink.zak.minestom.towerdefence.lobby.LobbyManager;
import pink.zak.minestom.towerdefence.scoreboard.TowerScoreboard;

public class LobbyScoreboard implements TowerScoreboard {
    private final Sidebar sidebar = new Sidebar(TowerScoreboard.TITLE);
    private final LobbyManager lobbyManager;

    public LobbyScoreboard(TowerDefenceModule plugin, LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;

        this.sidebar.createLine(new Sidebar.ScoreboardLine("empty-6", Component.empty(), 6));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("online-players", this.createOnlinePlayers(), 5));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("empty-4", Component.empty(), 4));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("red-players", this.createRedPlayers(), 3));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("blue-players", this.createBluePlayers(), 2));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("empty-1", Component.empty(), 1));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("website", TowerScoreboard.DOMAIN, 0));

        plugin.getEventNode().addListener(PlayerSpawnEvent.class, event -> {
                    if (!event.isFirstSpawn()) return;

                    this.sidebar.updateLineContent("online-players", this.createOnlinePlayers());
                    this.sidebar.addViewer(event.getPlayer());
                })
                .addListener(PlayerDisconnectEvent.class, event -> this.sidebar.updateLineContent("online-players", this.createOnlinePlayers()))
                .addListener(PlayerTeamSwitchEvent.class, event -> {
                    Team updateTeam;
                    if (event.newTeam() == null)
                        updateTeam = event.oldTeam();
                    else if (event.oldTeam() == null)
                        updateTeam = event.newTeam();
                    else {
                        this.sidebar.updateLineContent("red-players", this.createRedPlayers());
                        this.sidebar.updateLineContent("blue-players", this.createBluePlayers());
                        return;
                    }
                    if (updateTeam == Team.RED)
                        this.sidebar.updateLineContent("red-players", this.createRedPlayers());
                    else
                        this.sidebar.updateLineContent("blue-players", this.createBluePlayers());
                });
    }

    private Component createOnlinePlayers() {
        return Component.text("Online Players: ", NamedTextColor.WHITE)
                .append(Component.text(MinecraftServer.getConnectionManager().getOnlinePlayers().size(), NamedTextColor.YELLOW));
    }

    private Component createRedPlayers() {
        return Component.text("Red Players: ", NamedTextColor.WHITE)
                .append(Component.text(this.lobbyManager.getTeamSize(Team.RED) + "/6", NamedTextColor.RED));
    }

    private Component createBluePlayers() {
        return Component.text("Blue Players: ", NamedTextColor.WHITE)
                .append(Component.text(this.lobbyManager.getTeamSize(Team.BLUE) + "/6", NamedTextColor.BLUE));
    }

    @Override
    public boolean removeViewer(Player player) {
        return this.sidebar.removeViewer(player);
    }

    public void addViewer(Player player) {
        this.sidebar.addViewer(player);
    }
}
