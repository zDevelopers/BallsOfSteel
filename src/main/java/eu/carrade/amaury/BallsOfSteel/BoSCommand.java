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

import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.components.i18n.I18n;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BoSCommand implements CommandExecutor
{
    private BallsOfSteel p = null;

    private ArrayList<String> commands = new ArrayList<>();
    private ArrayList<String> teamCommands = new ArrayList<>();


    public BoSCommand(BallsOfSteel p)
    {
        this.p = p;

        commands.add("about");
        commands.add("start");
        commands.add("restart");
        commands.add("team");
        commands.add("finish");
        commands.add("clearitems");

        teamCommands.add("add");
        teamCommands.add("remove");
        teamCommands.add("spawn");
        teamCommands.add("chest");
        teamCommands.add("join");
        teamCommands.add("leave");
        teamCommands.add("list");
        teamCommands.add("reset");
    }


    /**
     * Handles a command.
     *
     * @param sender The sender
     * @param command The executed command
     * @param label The alias used for this command
     * @param args The arguments given to the command
     *
     * @author Amaury Carrade
     */
    @SuppressWarnings ("rawtypes")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {

        boolean ourCommand = false;
        for (String commandName : p.getDescription().getCommands().keySet())
        {
            if (commandName.equalsIgnoreCase(command.getName()))
            {
                ourCommand = true;
                break;
            }
        }

        if (!ourCommand)
        {
            return false;
        }

        /** Team chat commands **/

        if (command.getName().equalsIgnoreCase("t"))
        {
            doTeamMessage(sender, command, label, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("g"))
        {
            doGlobalMessage(sender, command, label, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("togglechat"))
        {
            doToggleTeamChat(sender, command, label, args);
            return true;
        }

        if (args.length == 0)
        {
            help(sender, args, false);
            return true;
        }

        String subcommandName = args[0].toLowerCase();

        // First: subcommand existence.
        if (!this.commands.contains(subcommandName))
        {
            try
            {
                Integer.valueOf(subcommandName);
                help(sender, args, false);
            }
            catch (NumberFormatException e)
            { // If the subcommand isn't a number, it's an error.
                help(sender, args, true);
            }
            return true;
        }

        // Second: is the sender allowed?
        if (!isAllowed(sender, subcommandName))
        {
            unauthorized(sender, command);
            return true;
        }

        // Third: instantiation
        try
        {
            Class<? extends BoSCommand> cl = this.getClass();
            Class[] parametersTypes = new Class[] {CommandSender.class, Command.class, String.class, String[].class};

            Method doMethod = cl.getDeclaredMethod("do" + WordUtils.capitalize(subcommandName), parametersTypes);

            doMethod.invoke(this, new Object[] {sender, command, label, args});

            return true;

        }
        catch (NoSuchMethodException e)
        {
            // Unknown method => unknown subcommand.
            help(sender, args, true);
            return true;

        }
        catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            sender.sendMessage(I.t("{ce}An error occurred, see console for details. This is probably a bug."));
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Prints the help.
     *
     * @param sender
     * @param args The arguments of the command.
     * @param error True if the help is printed because the user typed an unknown command.
     */
    private void help(CommandSender sender, String[] args, boolean error)
    {
        if (error)
        {
            sender.sendMessage(I.t("{ce}This subcommand does not exists. See /bos for the available commands."));
            return;
        }

        if (sender instanceof Player) sender.sendMessage("");
        sender.sendMessage(I.t("{yellow}{0} - version {1}", p.getDescription().getDescription(), p.getDescription().getVersion()));

        sender.sendMessage(I.t("{ci}Legend: {cc}/bos command <required> [optional=default] <spaces allowed ...>{ci}."));

        sender.sendMessage(I.t("{cc}/bos start {ci}: launches the game."));
        sender.sendMessage(I.t("{cc}/bos restart {ci}: restarts the game, during a party. This will not empty the diamond chest."));
        sender.sendMessage(I.t("{cc}/bos team {ci}: manages the teams. See /bos teams for details."));
        sender.sendMessage(I.t("{cc}/bos finish {ci}: displays the name of the winners and launches some fireworks."));
        sender.sendMessage(I.t("{cc}/bos clearitems {ci}: deletes all dropped items from the world, excepted diamond-based items."));
        sender.sendMessage(I.t("{cc}/bos about {ci}: informations about the plugin and the translation."));
    }


    /**
     * This method checks if an user is allowed to send a command.
     *
     * @param sender
     * @param subcommand
     * @return boolean The allowance status.
     */
    private boolean isAllowed(CommandSender sender, String subcommand)
    {
        if (sender instanceof Player)
        {
            if (sender.isOp())
            {
                return true;
            }
            else if (sender.hasPermission("bos." + subcommand))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    /**
     * This method sends a message to a player who try to use a command without the permission.
     *
     * @param sender
     * @param command
     */
    private void unauthorized(CommandSender sender, Command command)
    {
        sender.sendMessage(I.t("{ce}Hahahahahaha no."));
    }


    /**
     * This command prints some informations about the plugin and the translation.
     *
     * Usage: /bos about
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    @SuppressWarnings ("unused")
    private void doAbout(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player) sender.sendMessage("");
        sender.sendMessage(I.t("{yellow}{0} - version {1}", p.getDescription().getDescription(), p.getDescription().getVersion()));

        String authors = "";
        List<String> listAuthors = p.getDescription().getAuthors();
        for (String author : listAuthors)
        {
            if (author == listAuthors.get(0))
            {
                // Nothing
            }
            else if (author == listAuthors.get(listAuthors.size() - 1))
            {
                authors += " " + I.t("and") + " ";
            }
            else
            {
                authors += ", ";
            }
            authors += author;
        }
        sender.sendMessage(I.t("Plugin made with love by {0}.", authors));

        sender.sendMessage(I.t("{aqua}------ Translations ------"));
        sender.sendMessage(I.t("Current language: {0} (translated by {1}).", I18n.getPrimaryLocale().getDisplayName(), I18n.getLastTranslator(I18n.getPrimaryLocale())));
        sender.sendMessage(I.t("Fallback language: {0} (translated by {1}).", I18n.getFallbackLocale().getDisplayName(), I18n.getLastTranslator(I18n.getFallbackLocale())));
        sender.sendMessage(I.t("{aqua}------ License ------"));
        sender.sendMessage(I.t("Published under the GNU General Public License (version 3)."));
    }

    /**
     * This command starts the game.
     *
     * Usage: /bos start
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    @SuppressWarnings ("unused")
    private void doStart(CommandSender sender, Command command, String label, String[] args)
    {
        try
        {
            p.getGameManager().start(sender);
        }
        catch (IllegalStateException e)
        {
            sender.sendMessage(I.t("{ce}The game is already started! Use {cc}/bos restart{ci} to restart it."));
        }
    }

    /**
     * This command restarts the game.
     * <p>
     * The chests are not emptied.
     *
     * Usage: /bos restart
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    @SuppressWarnings ("unused")
    private void doRestart(CommandSender sender, Command command, String label, String[] args)
    {
        p.getGameManager().restart(sender);
    }


    /**
     * This command is used to manage the teams.
     *
     * Usage: /bos team (for the doc).
     * Usage: /bos team <add|remove|spawn|join|leave|list|reset> (see doc for details).
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    @SuppressWarnings ("unused")
    private void doTeam(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 1)
        { // No action provided: doc
            if (sender instanceof Player) sender.sendMessage("");
            sender.sendMessage(I.t("{yellow}{0} - version {1}", p.getDescription().getDescription(), p.getDescription().getVersion()));
            sender.sendMessage(I.t("{ci}Legend: {cc}/bos command <required> [optional=default] <spaces allowed ...>{ci}."));

            sender.sendMessage(I.t("{aqua}------ Team commands ------"));
            sender.sendMessage(I.t("{cc}/bos team add <color> {ci}: adds a team with the provided color."));
            sender.sendMessage(I.t("{cc}/bos team add <color> <name ...> {ci}: adds a named team with the provided name and color."));
            sender.sendMessage(I.t("{cc}/bos team remove <name ...> {ci}: removes a team"));
            sender.sendMessage(I.t("{cc}/bos team spawn [x,y,z | x,z] <name ...> {ci}: sets the spawn point of the team (location of the sender or coordinates)."));
            sender.sendMessage(I.t("{cc}/bos team chest [x,y,z] <name ...> {ci}: sets the chest of this team (where the diamonds will be stored), using the given coordinates or the block the sender is looking at."));
            sender.sendMessage(I.t("{cc}/bos team join <player> <teamName ...>{ci}: adds a player inside the given team. The name of the team is it color, or the explicit name given."));
            sender.sendMessage(I.t("{cc}/bos team leave <player> {ci}: removes a player from his team."));
            sender.sendMessage(I.t("{cc}/bos team list {ci}: lists the teams and their players."));
            sender.sendMessage(I.t("{cc}/bos team reset {ci}: removes all teams."));
        }
        else
        {
            BoSTeamManager tm = p.getTeamManager();
            String subcommand = args[1];

            if (subcommand.equalsIgnoreCase("add"))
            {
                if (args.length == 3)
                { // /bos team add <color>

                    ChatColor color = p.getTeamManager().getChatColorByName(args[2]);

                    if (color == null)
                    {
                        sender.sendMessage(I.t("{ce}Unable to add the team, check the color name. Tip: use Tab to autocomplete."));
                    }
                    else
                    {
                        try
                        {
                            tm.addTeam(color, args[2].toLowerCase());
                        }
                        catch (IllegalArgumentException e)
                        {
                            sender.sendMessage(I.t("{ce}This team already exists."));
                        }
                        sender.sendMessage(I.t("{cs}Team {0}{cs} added.", color.toString() + args[2]));
                    }

                }
                else if (args.length >= 4)
                { // /bos team add <color> <name ...>

                    ChatColor color = p.getTeamManager().getChatColorByName(args[2]);

                    if (color == null)
                    {
                        sender.sendMessage(I.t("{ce}Unable to add the team, check the color name. Tip: use Tab to autocomplete."));
                    }
                    else
                    {
                        String name = BoSUtils.getStringFromCommandArguments(args, 3);

                        try
                        {
                            tm.addTeam(color, name);
                        }
                        catch (IllegalArgumentException e)
                        {
                            sender.sendMessage(I.t("{ce}This team already exists."));
                            return;
                        }
                        sender.sendMessage(I.t("{cs}Team {0}{cs} added.", color.toString() + name));
                    }

                }
                else
                {
                    sender.sendMessage(I.t("{ce}Syntax error, see /uh team."));
                }
            }


            else if (subcommand.equalsIgnoreCase("remove"))
            {
                if (args.length >= 3)
                { // /bos team remove <teamName>
                    String name = BoSUtils.getStringFromCommandArguments(args, 2);
                    if (!tm.removeTeam(name))
                    {
                        sender.sendMessage(I.t("{ce}This team does not exists."));
                    }
                    else
                    {
                        sender.sendMessage(I.t("{cs}Team {0} deleted.", name));
                    }
                }
                else
                {
                    sender.sendMessage(I.t("{ce}Syntax error, see /uh team."));
                }
            }


            else if (subcommand.equalsIgnoreCase("spawn"))
            {
                Location spawnPoint = null;

                World world;
                if (sender instanceof Player)
                {
                    world = ((Player) sender).getWorld();
                }
                else if (sender instanceof BlockCommandSender)
                {
                    world = ((BlockCommandSender) sender).getBlock().getWorld();
                }
                else
                {
                    world = p.getServer().getWorlds().get(0);
                }

                String nameTeamWithoutCoords = null, nameTeamWithCoords = null, teamName = null;
                if (args.length >= 3)
                {
                    nameTeamWithCoords = BoSUtils.getStringFromCommandArguments(args, 3);
                }
                if (args.length >= 2)
                {
                    nameTeamWithoutCoords = BoSUtils.getStringFromCommandArguments(args, 2);
                }

                if (p.getTeamManager().getTeam(nameTeamWithoutCoords) != null)
                { // /bos spawn <team ...>
                    if (!(sender instanceof Player))
                    {
                        sender.sendMessage(I.t("{ce}You must specify the coordinates from the console."));
                        return;
                    }

                    spawnPoint = ((Player) sender).getLocation();
                    teamName = nameTeamWithoutCoords;
                }
                else if (p.getTeamManager().getTeam(nameTeamWithCoords) != null)
                { // /bos spawn <x,y,z> <team ...>
                    teamName = nameTeamWithCoords;

                    String[] coords = args[2].split(",");

                    if (coords.length == 2)
                    {
                        try
                        {
                            double x = Double.valueOf(coords[0]);
                            double z = Double.valueOf(coords[1]);

                            spawnPoint = new Location(world, x, world.getHighestBlockYAt(Location.locToBlock(x), Location.locToBlock(z)), z);
                        }
                        catch (NumberFormatException e)
                        {
                            sender.sendMessage(I.t("{ce}The coordinates need to be numbers."));
                            return;
                        }
                    }
                    else if (coords.length >= 3)
                    {
                        try
                        {
                            double x = Double.valueOf(coords[0]);
                            double y = Double.valueOf(coords[1]);
                            double z = Double.valueOf(coords[2]);

                            spawnPoint = new Location(world, x, y, z);
                        }
                        catch (NumberFormatException e)
                        {
                            sender.sendMessage(I.t("{ce}The coordinates need to be numbers."));
                            return;
                        }
                    }
                    else
                    {
                        sender.sendMessage(I.t("{ce}Syntax error, see /uh team."));
                        return;
                    }
                }

                if (teamName == null)
                { // Unknown team
                    sender.sendMessage(I.t("{ce}This team does not exists."));
                    return;
                }

                BoSTeam team = p.getTeamManager().getTeam(teamName); // This cannot be null, here.

                team.setSpawnPoint(spawnPoint);

                sender.sendMessage(I.t("{cs}The spawn point of the team {0}{cs} is now {1};{2};{3}.", team.getDisplayName(), String.valueOf(spawnPoint.getBlockX()), String.valueOf(spawnPoint.getBlockY()), String.valueOf(spawnPoint.getBlockZ())));
            }


            else if (subcommand.equalsIgnoreCase("chest"))
            {
                Location chestLocation = null;

                World world;
                if (sender instanceof Player)
                {
                    world = ((Player) sender).getWorld();
                }
                else if (sender instanceof BlockCommandSender)
                {
                    world = ((BlockCommandSender) sender).getBlock().getWorld();
                }
                else
                {
                    world = p.getServer().getWorlds().get(0);
                }

                String nameTeamWithoutCoords = null, nameTeamWithCoords = null, teamName = null;
                if (args.length >= 3)
                {
                    nameTeamWithCoords = BoSUtils.getStringFromCommandArguments(args, 3);
                }
                if (args.length >= 2)
                {
                    nameTeamWithoutCoords = BoSUtils.getStringFromCommandArguments(args, 2);
                }

                if (p.getTeamManager().getTeam(nameTeamWithoutCoords) != null)
                { // /bos chest <team ...>
                    if (!(sender instanceof Player))
                    {
                        sender.sendMessage(I.t("{ce}You must specify the coordinates from the console."));
                        return;
                    }

                    teamName = nameTeamWithoutCoords;

                    Block chest = ((Player) sender).getTargetBlock(Collections.<Material>emptySet(), 10);
                    if (chest != null)
                    {
                        if (chest.getType() == Material.CHEST || chest.getType() == Material.TRAPPED_CHEST)
                        {
                            chestLocation = chest.getLocation();
                        }
                        else
                        {
                            sender.sendMessage(I.t("{ce}You are not looking at a chest usable by more than one player!"));
                            return;
                        }
                    }
                    else
                    {
                        sender.sendMessage(I.t("{ce}You are not looking at anything..."));
                        return;
                    }
                }
                else if (p.getTeamManager().getTeam(nameTeamWithCoords) != null)
                { // /bos spawn <x,y,z> <team ...>
                    teamName = nameTeamWithCoords;

                    String[] coords = args[2].split(",");

                    if (coords.length >= 3)
                    {
                        try
                        {
                            double x = Double.valueOf(coords[0]);
                            double y = Double.valueOf(coords[1]);
                            double z = Double.valueOf(coords[2]);

                            chestLocation = new Location(world, x, y, z);
                        }
                        catch (NumberFormatException e)
                        {
                            sender.sendMessage(I.t("{ce}The coordinates need to be numbers."));
                            return;
                        }
                    }
                    else
                    {
                        sender.sendMessage(I.t("{ce}Syntax error, see /uh team."));
                        return;
                    }
                }

                if (teamName == null)
                { // Unknown team
                    sender.sendMessage(I.t("{ce}This team does not exists."));
                    return;
                }

                BoSTeam team = p.getTeamManager().getTeam(teamName); // This cannot be null, here.

                try
                {
                    team.setChest(chestLocation);
                }
                catch (IllegalArgumentException e)
                {
                    sender.sendMessage(I.t("{ce}There isn't any chest usable by more than one player here."));
                    return;
                }

                sender.sendMessage(I.t("{cs}This chest (at {1};{2};{3}) is now the private chest of the team {0}{cs}.", team.getDisplayName(), String.valueOf(chestLocation.getBlockX()), String.valueOf(chestLocation.getBlockY()), String.valueOf(chestLocation.getBlockZ())));
            }


            else if (subcommand.equalsIgnoreCase("join"))
            {
                if (args.length >= 4)
                { // /bos team join <player> <teamName>

                    Player player = p.getServer().getPlayer(args[2]);
                    String teamName = BoSUtils.getStringFromCommandArguments(args, 3);

                    if (player == null || !player.isOnline())
                    {
                        sender.sendMessage(I.t("{ce}Unable to add the player {0} to the team {1}. The player must be connected.", args[2], teamName));
                    }
                    else
                    {
                        try
                        {
                            tm.addPlayerToTeam(teamName, player);
                        }
                        catch (IllegalArgumentException e)
                        {
                            sender.sendMessage(I.t("{ce}This team does not exists."));
                            return;
                        }
                        catch (RuntimeException e)
                        {
                            sender.sendMessage(I.t("{ce}The team {0}{ce} is full!", teamName));
                            return;
                        }
                        BoSTeam team = p.getTeamManager().getTeam(teamName);
                        sender.sendMessage(I.t("{cs}The player {0} was successfully added to the team {1}", args[2], team.getDisplayName()));
                    }
                }
                else
                {
                    sender.sendMessage(I.t("{ce}Syntax error, see /uh team."));
                }
            }


            else if (subcommand.equalsIgnoreCase("leave"))
            {
                if (args.length == 3)
                { // /bos team leave <player>

                    Player player = p.getServer().getPlayer(args[2]);

                    if (player == null || !player.isOnline())
                    {
                        sender.sendMessage(I.t("{ce}The player {0} is disconnected!", args[2]));
                    }
                    else
                    {
                        tm.removePlayerFromTeam(player);
                        sender.sendMessage(I.t("{cs}The player {0} was successfully removed from his team.", args[2]));
                    }
                }
                else
                {
                    sender.sendMessage(I.t("{ce}Syntax error, see /uh team."));
                }
            }


            else if (subcommand.equalsIgnoreCase("list"))
            {
                if (tm.getTeams().size() == 0)
                {
                    sender.sendMessage(I.t("{ce}There isn't any team to show."));
                    return;
                }

                for (final BoSTeam team : tm.getTeams())
                {
                    sender.sendMessage(I.t("{0} ({1} players)", team.getDisplayName(), ((Integer) team.getPlayers().size()).toString()));
                    for (final OfflinePlayer player : team.getPlayers())
                    {
                        String bullet = null;
                        if (player.isOnline())
                        {
                            bullet = I.t("{green} • ");
                        }
                        else
                        {
                            bullet = I.t("{red} • ");
                        }

                        sender.sendMessage(bullet + I.t("{0}", player.getName()));
                    }
                }
            }

            else if (subcommand.equalsIgnoreCase("reset"))
            {
                tm.reset();
                sender.sendMessage(I.t("{cs}All teams where removed."));
            }

            else
            {
                sender.sendMessage(I.t("{ce}Unknown command. See /uh team for available commands."));
            }
        }
    }

    /**
     * This command clears all floating items in the sender's world, except diamonds.
     * <p>
     * If the sender is the console, this uses the game's world.<br>
     * If the game's world is null, nothing is removed.
     *
     * Usage: /bos clearitems
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    @SuppressWarnings ("unused")
    private void doClearitems(CommandSender sender, Command command, String label, String[] args)
    {
        World world;
        if (sender instanceof Player)
        {
            world = ((Player) sender).getWorld();
        }
        else if (sender instanceof BlockCommandSender)
        {
            world = ((BlockCommandSender) sender).getBlock().getWorld();
        }
        else
        {
            world = p.getGameManager().getGameWorld();
        }

        if (world != null)
        {
            for (Entity entity : world.getEntities())
            {
                if (entity.getType() == EntityType.DROPPED_ITEM)
                {
                    switch (((Item) entity).getItemStack().getType())
                    {
                        case DIAMOND:
                        case DIAMOND_AXE:
                        case DIAMOND_BARDING:
                        case DIAMOND_BLOCK:
                        case DIAMOND_BOOTS:
                        case DIAMOND_CHESTPLATE:
                        case DIAMOND_HELMET:
                        case DIAMOND_HOE:
                        case DIAMOND_LEGGINGS:
                        case DIAMOND_ORE:
                        case DIAMOND_PICKAXE:
                        case DIAMOND_SPADE:
                        case DIAMOND_SWORD:
                            continue;
                        default:
                            entity.remove();
                    }
                }
            }
            sender.sendMessage(I.t("{cs}All items, diamonds-based items excepted, were destroyed in the world {0}.", world.getName()));
        }
        else
        {
            sender.sendMessage(I.t("{ce}Cannot clear the items from the console if the game's world is not set (i.e. world not set in the config and game not started)."));
        }
    }

    /**
     * This commands broadcast the winner(s) of the game and sends some fireworks at these players.
     * It fails if there is more than one team alive.
     *
     * Usage: /bos finish
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    @SuppressWarnings ("unused")
    private void doFinish(CommandSender sender, Command command, String label, String[] args)
    {

//		try {
//			p.getGameManager().finishGame();
//			
//		} catch(IllegalStateException e) {
//			
//			if(e.getMessage().equals(UHGameManager.FINISH_ERROR_NOT_STARTED)) {
//				sender.sendMessage(I.t("{ce}The game is not started!"));
//			}
//			else if(e.getMessage().equals(UHGameManager.FINISH_ERROR_NOT_FINISHED)) {
//				sender.sendMessage(I.t("{ce}The game is not finished!"));
//			}
//			else {
//				throw e;
//			}
//		}

    }

    /**
     * This command, /t <message>, is used to send a team-message.
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    private void doTeamMessage(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(I.t("{ce}You can't send a team-message from the console."));
            return;
        }

        if (args.length == 0)
        { // /t
            sender.sendMessage(I.t("{ce}Usage: /{0} <message>", "t"));
            return;
        }

        String message = "";
        for (Integer i = 0; i < args.length; i++)
        {
            message += args[i] + " ";
        }

        p.getTeamChatManager().sendTeamMessage((Player) sender, message);
    }

    /**
     * This command, /g <message>, is used to send a global message.
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    private void doGlobalMessage(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(I.t("{ce}You can't send a team-message from the console."));
            return;
        }

        if (args.length == 0)
        { // /g
            sender.sendMessage(I.t("{ce}Usage: /{0} <message>", "g"));
            return;
        }

        String message = "";
        for (Integer i = 0; i < args.length; i++)
        {
            message += args[i] + " ";
        }

        p.getTeamChatManager().sendGlobalMessage((Player) sender, message);
    }

    /**
     * This command, /togglechat, is used to toggle the chat between the global chat and the team chat.
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     */
    private void doToggleTeamChat(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(I.t("{ce}You can't send a team-message from the console."));
            return;
        }

        if (args.length == 0)
        { // /togglechat
            if (p.getTeamChatManager().toggleChatForPlayer((Player) sender))
            {
                sender.sendMessage(I.t("{cs}You are now chatting with your team only."));
            }
            else
            {
                sender.sendMessage(I.t("{cs}You are now chatting with everyone."));
            }
        }
        else
        { // /togglechat <another team>
            String teamName = BoSUtils.getStringFromCommandArguments(args, 0);
            BoSTeam team = p.getTeamManager().getTeam(teamName);

            if (team != null)
            {
                if (p.getTeamChatManager().toggleChatForPlayer((Player) sender, team))
                {
                    sender.sendMessage(I.t("{cs}You are now chatting with the team {0}{cs}.", team.getDisplayName()));
                }
            }
            else
            {
                sender.sendMessage(I.t("{ce}This team does not exists."));
            }
        }
    }


    public ArrayList<String> getCommands()
    {
        return commands;
    }

    public ArrayList<String> getTeamCommands()
    {
        return teamCommands;
    }
}
