/*
 * Copyright or © or Copr. AmauryCarrade (2015)
 * 
 * http://amaury.carrade.eu
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package eu.carrade.amaury.BallsOfSteel.generation;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.carrade.amaury.BallsOfSteel.BallsOfSteel;
import eu.carrade.amaury.BallsOfSteel.MapConfig;
import eu.carrade.amaury.BallsOfSteel.generation.generation.BallsOfSteelGenerator;
import eu.carrade.amaury.BallsOfSteel.generation.utils.WorldLoader;
import fr.zcraft.zlib.core.ZLibComponent;
import fr.zcraft.zlib.tools.PluginLogger;
import fr.zcraft.zlib.tools.reflection.Reflection;
import fr.zcraft.zlib.tools.runners.RunAsyncTask;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class GenerationManager extends ZLibComponent implements Listener
{
    private final static String MANAGED_WORLDS_FILENAME = "managed_worlds.dat";

    private final Set<GenerationProcess> generationProcesses = new HashSet<>();
    private final Queue<GenerationProcess> generationProcessesQueue = new ArrayDeque<>();

    private final Set<StaticBuilding> buildings = new HashSet<>();
    private final Set<CuboidRegion> buildingsRegions = new HashSet<>();

    private boolean logs;

    private Location lowestCorner;
    private Location highestCorner;

    private File managedWorldsListFile = null;
    private final Set<World> managedWorlds = new HashSet<>();
    private final Set<String> managedWorldsNames = new HashSet<>();


    @Override
    protected void onEnable()
    {
        if (!MapConfig.GENERATION.ENABLED.get())
        {
            setEnabled(false);
            return;
        }

        if (!BallsOfSteel.get().getWorldEditDependency().isEnabled())
        {
            PluginLogger.error("Cannot use the generator without WorldEdit installed.");
            setEnabled(false);
            return;
        }

        logs = MapConfig.GENERATION.LOGS.get();


        // Loading generation processes & static buildings
        final List<GenerationProcess> spheres = MapConfig.GENERATION.SPHERES.get();
        final List<StaticBuilding> staticBuildings = MapConfig.GENERATION.STATIC_BUILDINGS.get();

        if (spheres.size() == 0)
            PluginLogger.warning("No sphere loaded from config, you may have an error somewhere.");
        if (staticBuildings.size() == 0)
            PluginLogger.warning("No static building loaded from config, you may have an error somewhere.");

        generationProcesses.addAll(spheres);
        buildings.addAll(staticBuildings);

        recalculatePrivateBuildingRegions();


        // Loading map boundaries
        World world = BallsOfSteel.get().getGameManager().getGameWorld();

        final Location corner1 = BukkitUtil.toLocation(world, MapConfig.GENERATION.MAP.BOUNDARIES.CORNER_1.get());
        final Location corner2 = BukkitUtil.toLocation(world, MapConfig.GENERATION.MAP.BOUNDARIES.CORNER_2.get());

        lowestCorner = new Location(
                corner1.getWorld(),
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ())
        );

        highestCorner = new Location(
                corner1.getWorld(),
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ())
        );


        // Loading managed worlds
        managedWorldsNames.add(MapConfig.WORLD.get());
        loadManagedWorlds();
        saveManagedWorlds();
    }



    /* ========== Generation processes and buildings ========== */


    /**
     * @return All registered generation processes.
     */
    public Set<GenerationProcess> getGenerationProcesses()
    {
        return Collections.unmodifiableSet(generationProcesses);
    }

    /**
     * @param random A source of randomness.
     *
     * @return A random generation process.
     */
    public GenerationProcess getRandomGenerationProcess(final Random random)
    {
        if (generationProcessesQueue.isEmpty())
        {
            final List<GenerationProcess> generationProcessesList = new ArrayList<>();
            for (final GenerationProcess generationProcess : generationProcesses)
                if (generationProcess.isEnabled())
                    generationProcessesList.add(generationProcess);

            Collections.shuffle(generationProcessesList, random);

            generationProcessesQueue.addAll(generationProcessesList);
        }

        return generationProcessesQueue.poll();
    }

    /**
     * @return All registered static buildings.
     */
    public Set<StaticBuilding> getBuildings()
    {
        return buildings;
    }

    /**
     * @return A region where nothing should be built—except static buildings.
     */
    public Set<CuboidRegion> getBuildingsPrivateRegions()
    {
        return buildingsRegions;
    }

    /**
     * Recalculates the private regions of the static buildings, where nothing should be
     * generated.
     */
    private void recalculatePrivateBuildingRegions()
    {
        buildingsRegions.clear();

        for (final StaticBuilding building : buildings)
            buildingsRegions.add(CuboidRegion.makeCuboid(building.getPrivateRegion()));
    }

    /**
     * @return The corner of the world with the lowest coordinates
     */
    public Location getLowestCorner()
    {
        return lowestCorner;
    }

    /**
     * @return The corner of the world with the highest coordinates
     */
    public Location getHighestCorner()
    {
        return highestCorner;
    }



    /* ========== Misc ========== */


    /**
     * Checks if the given location is inside the defined boundaries of the map.
     * The world is not checked.
     *
     * @param location The location.
     *
     * @return {@code true} if inside the boundaries.
     */
    public boolean isInsideBoundaries(final Location location)
    {
        return location.getX() >= lowestCorner.getX()
                && location.getY() >= lowestCorner.getY()
                && location.getZ() >= lowestCorner.getZ()
                && location.getX() <= highestCorner.getX()
                && location.getY() <= highestCorner.getY()
                && location.getZ() <= highestCorner.getZ();
    }

    /**
     * @return {@code true} if the generation should be logged.
     */
    public boolean isLogged()
    {
        return logs;
    }



    /* ========== World creation ========== */


    /**
     * Creates a world using the Balls of Steel generator and the name defined
     * in the map.yml file.
     *
     * If the world already exists, it's generator and some options will be
     * updated.
     *
     * @return A new world, or a reference to an existing world if it already
     * exists.
     */
    public World createWorld()
    {
        return createWorld(MapConfig.WORLD.get());
    }

    /**
     * Create a world using the Balls of Steel generator.
     *
     * If the world already exists, it's generator and some options will be
     * updated.
     *
     * @param name A world name.
     *
     * @return A new world, or a reference to an existing world if it already
     * exists.
     */
    public World createWorld(final String name)
    {
        final World world = new WorldCreator(name)
                .environment(MapConfig.GENERATION.MAP.ENVIRONMENT.get())
                .generator(new BallsOfSteelGenerator())
                .generateStructures(true)
                .createWorld();

        world.setSpawnFlags(false, false);
        world.setGameRuleValue("doMobSpawning", "false");

        if (world.getEnvironment() == World.Environment.THE_END)
        {
            patchWorldAgainstDragon(world);
        }

        manageWorld(world);

        return world;
    }

    /**
     * Create a world using the Balls of Steel generator, and returns a
     * configured {@link WorldLoader} ready to be used.
     *
     * If the world already exists, it's generator and some options will be
     * updated.
     *
     * @param name         A world name.
     * @param logsReceiver A receiver for the generation logs (progress...). Can
     *                     be {@code null}.
     *
     * @return A {@link WorldLoader} containing a reference to either a new
     * world or the world with the given name.
     */
    public WorldLoader createWorldAndGetLoader(final String name, final CommandSender logsReceiver)
    {
        final Vector2D corner1 = MapConfig.GENERATION.MAP.BOUNDARIES.CORNER_1.get().toVector2D();
        final Vector2D corner2 = MapConfig.GENERATION.MAP.BOUNDARIES.CORNER_2.get().toVector2D();

        // We also load the chunks near the border to avoid load when players are close to it.
        // If the player logged out we switch to the console to display the logs.
        return new WorldLoader(
                createWorld(name),
                logsReceiver instanceof Player && !((Player) logsReceiver).isOnline() ? Bukkit.getConsoleSender() : logsReceiver,
                Vector2D.getMinimum(corner1, corner2).subtract(32, 32),
                Vector2D.getMaximum(corner1, corner2).add(32, 32)
        );
    }

    /**
     * Create a world using the Balls of Steel generator with the name defined
     * in map.yml, and returns a configured {@link WorldLoader} ready to be
     * used.
     *
     * If the world already exists, it's generator and some options will be
     * updated.
     *
     * @param logsReceiver A receiver for the generation logs (progress...). Can
     *                     be {@code null}.
     *
     * @return A {@link WorldLoader} containing a reference to either a new
     * world or the world with the given name.
     */
    public WorldLoader createWorldAndGetLoader(final CommandSender logsReceiver)
    {
        return createWorldAndGetLoader(MapConfig.WORLD.get(), logsReceiver);
    }

    /**
     * Create a world using the Balls of Steel generator with the name defined
     * in map.yml, and returns a configured {@link WorldLoader} ready to be
     * used, without log receiver.
     *
     * If the world already exists, it's generator and some options will be
     * updated.
     *
     * @return A {@link WorldLoader} containing a reference to either a new
     * world or the world with the given name.
     */
    public WorldLoader createWorldAndGetLoader()
    {
        return createWorldAndGetLoader(MapConfig.WORLD.get(), null);
    }



    /* ========== World management and patching ========== */


    /**
     * Configures a world to be managed by BoS.
     *
     * @param world The world.
     */
    private void manageWorld(World world)
    {
        managedWorlds.add(world);
        managedWorldsNames.add(world.getName());

        saveManagedWorlds();
    }

    /**
     * Loads the managed worlds from the file in the plugin directory.
     */
    private void loadManagedWorlds()
    {
        managedWorldsListFile = new File(BallsOfSteel.get().getDataFolder(), MANAGED_WORLDS_FILENAME);
        if (!managedWorldsListFile.exists())
        {
            try
            {
                if (!managedWorldsListFile.getParentFile().mkdirs())
                    PluginLogger.warning("Cannot create the {0} file directory to remember BoS worlds, you may have problems with End ones.", managedWorldsListFile.getParent());

                if (!managedWorldsListFile.createNewFile())
                    PluginLogger.warning("Cannot create the {0} file to remember BoS worlds, you may have problems with End ones.", managedWorldsListFile.getAbsolutePath());
            }
            catch (IOException e)
            {
                PluginLogger.error("Error while creating {0} file to remember BoS worlds, you may have problems with End ones.", e, managedWorldsListFile.getAbsolutePath());
            }
        }

        try
        {
            final StringBuilder builder = new StringBuilder();
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(managedWorldsListFile))))
            {
                String line;
                while ((line = reader.readLine()) != null)
                    builder.append(line);
            }

            for (final String name : builder.toString().split(","))
            {
                final String cleanName = name.trim();
                if (!cleanName.isEmpty()) managedWorldsNames.add(cleanName);
            }
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Saves the managed worlds to the file in the plugin directory.
     */
    private void saveManagedWorlds()
    {
        final String rawManagedWorlds = StringUtils.join(managedWorldsNames, ",");
        RunAsyncTask.nextTick(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(managedWorldsListFile)))
                    {
                        writer.write(rawManagedWorlds);
                    }
                }
                catch (IOException e)
                {
                    PluginLogger.error("Cannot save BallsOfSteel managed worlds", e);
                }
            }
        });
    }


    /**
     * Patches a world so the Ender Dragon will not spawn inside.
     *
     * This will update the world's NBT data to save a fake old dragon fight,
     * and patch the {@code EnderDragonBattle} object so the presence of a
     * portal is not checked.
     *
     * Nothing is done if the world environment is not {@link
     * org.bukkit.World.Environment#THE_END THE_END}.
     *
     * @param world A world.
     */
    private void patchWorldAgainstDragon(World world)
    {
        if (world.getEnvironment() != World.Environment.THE_END)
            return;

        try
        {
            final Object worldServer = Reflection.call(Reflection.getBukkitClassByName("CraftWorld").cast(world), "getHandle");

            final Object worldData = Reflection.call(worldServer, "getWorldData");
            final Object dimensionManager = Reflection.call(Reflection.getMinecraftClassByName("DimensionManager"), "valueOf", new Object[] {"THE_END"});

            final Object nbtCompound = Reflection.call(worldData, "a", dimensionManager);

            final Object dragonFightCompound = Reflection.call(nbtCompound, "getCompound", "DragonFight");

            final Method nbtSetBoolean = dragonFightCompound.getClass().getMethod("setBoolean", String.class, boolean.class);
            final Method nbtSetLong = dragonFightCompound.getClass().getMethod("setLong", String.class, long.class);
            final Method nbtSetBase = nbtCompound.getClass().getMethod("set", String.class, Reflection.getMinecraftClassByName("NBTBase"));


            // Saves the fact the dragon was killed (sort of)
            final UUID dragonUUID = UUID.randomUUID();

            nbtSetBoolean.invoke(dragonFightCompound, "DragonKilled", true);
            nbtSetBoolean.invoke(dragonFightCompound, "PreviouslyKilled", true);

            nbtSetLong.invoke(dragonFightCompound, "DragonUUIDMost", dragonUUID.getMostSignificantBits());
            nbtSetLong.invoke(dragonFightCompound, "DragonUUIDLeast", dragonUUID.getLeastSignificantBits());

            nbtSetBase.invoke(nbtCompound, "DragonFight", dragonFightCompound);
            Reflection.call(worldData, "a", dimensionManager, nbtCompound);


            // Patches the EnderDragonBattle object to disable legacy dragon check
            final Object worldProvider = Reflection.getFieldValue(Reflection.getMinecraftClassByName("World"), worldServer, "worldProvider");
            if (worldProvider.getClass().isAssignableFrom(Reflection.getMinecraftClassByName("WorldProviderTheEnd")))
            {
                final Object enderDragonBattle = Reflection.call(worldProvider, "s");
                if (enderDragonBattle != null)
                    Reflection.setFieldValue(enderDragonBattle, "n", false);
            }
            else
            {
                PluginLogger.error("Cannot patch world {0} against dragon: wrong world provider for world: expecting WorldProviderTheEnd, got {1}", world.getName(), worldProvider.getClass());
            }
        }
        catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException | NoSuchFieldException e)
        {
            PluginLogger.error("Error while removing dragon from world {0}", e, world.getName());
        }
    }


    /**
     * Used to patch the worlds when loaded, and to keep track of managed
     * worlds.
     */
    @EventHandler
    public void onWorldLoad(final WorldLoadEvent ev)
    {
        if (managedWorldsNames.contains(ev.getWorld().getName()))
            manageWorld(ev.getWorld());

        if (managedWorlds.contains(ev.getWorld()) && ev.getWorld().getEnvironment() == World.Environment.THE_END)
            patchWorldAgainstDragon(ev.getWorld());
    }

    /**
     * Used to patch the worlds when a player enter inside, as sometimes the
     * EnderDragonBattle object cannot be patched when the world was just
     * created.
     */
    @EventHandler
    public void onPlayerChangeWorld(final PlayerChangedWorldEvent ev)
    {
        if (managedWorlds.contains(ev.getPlayer().getWorld()))
            patchWorldAgainstDragon(ev.getPlayer().getWorld());
    }
}
