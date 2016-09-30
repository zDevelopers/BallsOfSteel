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

package eu.carrade.amaury.BallsOfSteel;

import eu.carrade.amaury.BallsOfSteel.commands.AboutCommand;
import eu.carrade.amaury.BallsOfSteel.commands.ChatGlobalCommand;
import eu.carrade.amaury.BallsOfSteel.commands.ChatTeamCommand;
import eu.carrade.amaury.BallsOfSteel.commands.ChatToggleCommand;
import eu.carrade.amaury.BallsOfSteel.commands.ClearItemsCommand;
import eu.carrade.amaury.BallsOfSteel.commands.RestartCommand;
import eu.carrade.amaury.BallsOfSteel.commands.StartCommand;
import eu.carrade.amaury.BallsOfSteel.commands.TeamsCommand;
import eu.carrade.amaury.BallsOfSteel.game.BoSGameManager;
import eu.carrade.amaury.BallsOfSteel.game.BoSListener;
import eu.carrade.amaury.BallsOfSteel.game.BoSScoreboardManager;
import eu.carrade.amaury.BallsOfSteel.integration.BarAPIWrapper;
import eu.carrade.amaury.BallsOfSteel.teams.BoSTeamChatManager;
import eu.carrade.amaury.BallsOfSteel.teams.BoSTeamManager;
import eu.carrade.amaury.BallsOfSteel.timers.UpdateTimerTask;
import fr.zcraft.zlib.components.commands.Commands;
import fr.zcraft.zlib.components.i18n.I18n;
import fr.zcraft.zlib.core.ZLib;
import fr.zcraft.zlib.core.ZPlugin;


public final class BallsOfSteel extends ZPlugin
{
    private static BallsOfSteel instance;

    private BoSTeamManager teamManager = null;
    private BoSScoreboardManager scoreboardManager = null;
    private BoSTeamChatManager teamChatManager = null;
    private BoSGameManager gameManager = null;

    private BarAPIWrapper barAPIWrapper = null;

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveDefaultConfig();

        loadComponents(I18n.class, Commands.class, Config.class);

        I18n.setPrimaryLocale(Config.LANG.get());

        teamManager = new BoSTeamManager(this);
        teamChatManager = new BoSTeamChatManager(this);
        scoreboardManager = new BoSScoreboardManager(this);
        gameManager = new BoSGameManager(this);

        Commands.register("bos",
                AboutCommand.class, ClearItemsCommand.class,
                ChatTeamCommand.class, ChatGlobalCommand.class, ChatToggleCommand.class,
                StartCommand.class, RestartCommand.class, TeamsCommand.class
        );

        Commands.registerShortcut("bos", ChatTeamCommand.class, "t");
        Commands.registerShortcut("bos", ChatGlobalCommand.class, "g");
        Commands.registerShortcut("bos", ChatToggleCommand.class, "togglechat");

        ZLib.registerEvents(new BoSListener(this));


        barAPIWrapper = new BarAPIWrapper(this);


        // Imports teams from the config.
        this.teamManager.importTeamsFromConfig();

        // Starts the task that updates the timers.
        // Started here, so a timer can be displayed before the start of the game
        // (example: countdown before the start).
        new UpdateTimerTask(this).runTaskTimer(this, 20l, 20l);
    }

    public static BallsOfSteel get()
    {
        return instance;
    }

    /**
     * Returns the game manager.
     */
    public BoSGameManager getGameManager()
    {
        return gameManager;
    }

    /**
     * Returns the team manager.
     */
    public BoSTeamManager getTeamManager()
    {
        return teamManager;
    }

    /**
     * Returns the scoreboard manager.
     */
    public BoSScoreboardManager getScoreboardManager()
    {
        return scoreboardManager;
    }

    /**
     * Returns the team-chat manager.
     */
    public BoSTeamChatManager getTeamChatManager()
    {
        return teamChatManager;
    }


    public BarAPIWrapper getBarAPIWrapper()
    {
        return barAPIWrapper;
    }
}
