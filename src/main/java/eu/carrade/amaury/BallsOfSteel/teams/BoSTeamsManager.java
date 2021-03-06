/**
 * Plugin UltraHardcore (UHPlugin) Copyright (C) 2013 azenet Copyright (C) 2014
 * Amaury Carrade
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see [http://www.gnu.org/licenses/].
 */

package eu.carrade.amaury.BallsOfSteel.teams;

import eu.carrade.amaury.BallsOfSteel.BallsOfSteel;
import eu.carrade.amaury.BallsOfSteel.GameConfig;
import eu.carrade.amaury.BallsOfSteel.MapConfig;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.core.ZLibComponent;
import fr.zcraft.zlib.tools.runners.RunTask;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;


public class BoSTeamsManager extends ZLibComponent
{
    private ArrayList<BoSTeam> teams = new ArrayList<>();
    private int maxPlayersPerTeam;


    @Override
    public void onEnable()
    {
        this.maxPlayersPerTeam = GameConfig.TEAMS_OPTIONS.MAX_PLAYERS_PER_TEAM.get();

        // Wait for all components initialization
        RunTask.nextTick(new Runnable() {
            @Override
            public void run()
            {
                importTeamsFromConfig();
            }
        });
    }


    /**
     * Adds a team.
     *
     * @param color The color.
     * @param name The name of the team.
     * @throws IllegalArgumentException if a team with the same name already exists.
     */
    public void addTeam(ChatColor color, String name)
    {
        if (this.getTeam(name) != null)
        {
            throw new IllegalArgumentException("There is already a team named " + name + " registered!");
        }

        teams.add(new BoSTeam(name, color));
    }

    /**
     * Adds a team from an UHTeam object.
     *
     * @param team The team.
     * @throws IllegalArgumentException if a team with the same name already exists.
     */
    public void addTeam(BoSTeam team)
    {
        if (this.getTeam(team.getName()) != null)
        {
            throw new IllegalArgumentException("There is already a team named " + team.getName() + " registered!");
        }

        teams.add(team);
    }

    /**
     * Deletes a team.
     *
     * @param team The team to delete.
     * @param dontNotify If true, the player will not be notified about the leave.
     * @return boolean True if a team was removed.
     */
    public boolean removeTeam(BoSTeam team, boolean dontNotify)
    {
        if (team != null)
        {
            if (dontNotify)
            {
                for (OfflinePlayer player : team.getPlayers())
                {
                    this.removePlayerFromTeam(player, true);
                }
            }

            team.deleteTeam();
        }

        return teams.remove(team);
    }

    /**
     * Deletes a team.
     *
     * @param team The team to delete.
     * @return boolean True if a team was removed.
     */
    public boolean removeTeam(BoSTeam team)
    {
        return removeTeam(team, false);
    }

    /**
     * Deletes a team.
     *
     * @param name The name of the team to delete.
     * @return boolean True if a team was removed.
     */
    public boolean removeTeam(String name)
    {
        return removeTeam(getTeam(name), false);
    }

    /**
     * Deletes a team.
     *
     * @param name The name of the team to delete.
     * @param dontNotify If true, the player will not be notified about the leave.
     * @return boolean True if a team was removed.
     */
    public boolean removeTeam(String name, boolean dontNotify)
    {
        return removeTeam(getTeam(name), dontNotify);
    }

    /**
     * Adds a player to a team.
     *
     * @param teamName The team in which we adds the player.
     * @param player The player to add.
     * @throws IllegalArgumentException if the team does not exists.
     */
    public void addPlayerToTeam(String teamName, Player player)
    {
        BoSTeam team = getTeam(teamName);

        if (team == null)
        {
            throw new IllegalArgumentException("There isn't any team named" + teamName + " registered!");
        }

        if (this.maxPlayersPerTeam != 0 && team.getPlayers().size() >= this.maxPlayersPerTeam)
        {
            throw new RuntimeException("The team " + teamName + " is full");
        }

        removePlayerFromTeam(player, true);
        team.addPlayer(player);
        player.sendMessage(I.t("{aqua}You are now in the {0}{aqua} team.", team.getDisplayName()));
    }

    /**
     * Removes a player from his team.
     *
     * @param player The player to remove.
     * @param dontNotify If true, the player will not be notified about the leave.
     */
    public void removePlayerFromTeam(OfflinePlayer player, boolean dontNotify)
    {
        BoSTeam team = getTeamForPlayer(player);
        if (team != null)
        {
            team.removePlayer(player);
            if (!dontNotify && player.isOnline())
            {
                ((Player) player).sendMessage(I.t("{darkaqua}You are no longer part of the {0}{darkaqua} team.", team.getDisplayName()));
            }
        }
    }

    /**
     * Removes a player from his team.
     *
     * @param player The player to remove.
     */
    public void removePlayerFromTeam(OfflinePlayer player)
    {
        removePlayerFromTeam(player, false);
    }


    /**
     * Removes all teams.
     *
     * @param dontNotify If true, the player will not be notified when they leave the destroyed team.
     */
    public void reset(boolean dontNotify)
    {
        // 1: scoreboard reset
        for (BoSTeam team : teams)
            this.removeTeam(team, dontNotify);

        // 2: internal list reset
        teams.clear();
    }

    /**
     * Removes all teams.
     */
    public void reset()
    {
        reset(false);
    }

    /**
     * Sets the correct display name of a player, according to his team.
     */
    public void colorizePlayer(OfflinePlayer offlinePlayer)
    {
        if (!GameConfig.COLORIZE_CHAT.get() || !offlinePlayer.isOnline())
            return;

        final Player player = (Player) offlinePlayer;
        final BoSTeam team = getTeamForPlayer(player);

        if (team == null)
        {
            player.setDisplayName(player.getName());
        }
        else
        {
            if (team.getColor() != null)
            {
                player.setDisplayName(team.getColor() + player.getName() + ChatColor.RESET);
            }
            else
            {
                player.setDisplayName(player.getName());
            }
        }
    }

    /**
     * Returns all the teams.
     *
     * @return The teams.
     */
    public ArrayList<BoSTeam> getTeams()
    {
        return this.teams;
    }

    /**
     * Returns the UHTeam object of the team with the given name.
     *
     * @param name The name of the team.
     * @return The team, or null if the team does not exists.
     */
    public BoSTeam getTeam(String name)
    {
        for (BoSTeam t : teams)
        {
            if (t.getName().equalsIgnoreCase(name))
            {
                return t;
            }
        }
        return null;
    }

    /**
     * Gets a player's team.
     *
     * @param player The player.
     * @return The team of this player.
     */
    public BoSTeam getTeamForPlayer(OfflinePlayer player)
    {
        for (BoSTeam t : teams)
        {
            if (t.containsPlayer(player.getUniqueId())) return t;
        }
        return null;
    }

    /**
     * Checks if two players are in the same team.
     *
     * @param player1 The first player.
     * @param player2 The second player
     * @return True if the players are in the same team, false else.
     */
    public boolean inSameTeam(Player player1, Player player2)
    {
        return (getTeamForPlayer(player1).equals(getTeamForPlayer(player2)));
    }

    /**
     * Imports the teams from the configuration.
     *
     * @return The number of teams imported.
     */
    public int importTeamsFromConfig()
    {
        int count = 0;
        for (BoSTeam team : MapConfig.TEAMS)
        {
            addTeam(team);
            count++;
        }

        BallsOfSteel.get().getGameManager().updateTrackedChests();

        return count;
    }
}
