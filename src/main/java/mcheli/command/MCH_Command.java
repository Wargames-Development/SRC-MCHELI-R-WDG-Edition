package mcheli.command;

import com.google.gson.JsonParseException;
import mcheli.MCH_Config;
import mcheli.MCH_FreeLookDebug;
import mcheli.MCH_Lib;
import mcheli.MCH_WaypointNavDebug;
import mcheli.MCH_MOD;
import mcheli.MCH_PacketNotifyServerSettings;
import mcheli.MCH_RadarDebug;
import mcheli.MCH_ServerSettings;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.block.MCH_BlockInfoManager;
import mcheli.block.MCH_ConfigSpawnerBlock;
import mcheli.block.MCH_ConfigSpawnerTileEntity;
import mcheli.render.MCH_RenderRWR;
import mcheli.structure.MCH_StructureBlob;
import mcheli.structure.MCH_StructureDebugLogger;
import mcheli.structure.MCH_StructureRule;
import mcheli.structure.MCH_StructureRuleManager;
import mcheli.structure.MCH_StructureIO;
import mcheli.structure.MCH_StructureMeta;
import mcheli.structure.MCH_SchemImporter;
import mcheli.multiplay.MCH_MultiplayPacketHandler;
import mcheli.multiplay.MCH_PacketIndClient;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.block.Block;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.IChatComponent.Serializer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.CommandEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class MCH_Command extends CommandBase {

    public static final String CMD_GET_SS = "sendss";
    public static final String CMD_MOD_LIST = "modlist";
    public static final String CMD_RECONFIG = "reconfig";
    public static final String CMD_TITLE = "title";
    public static final String CMD_FILL = "fill";
    public static final String CMD_STATUS = "status";
    public static final String CMD_KILL_ENTITY = "killentity";
    public static final String CMD_REMOVE_ENTITY = "removeentity";
    public static final String CMD_ATTACK_ENTITY = "attackentity";
    public static final String CMD_SHOW_BB = "showboundingbox";
    public static final String CMD_DEBUG = "debug";
    public static final String CMD_SPAWNER_FREEZE = "spawnerfreeze";
    public static final String CMD_SPAWNER_DEBUG = "spawnerdebug";
    public static final String CMD_STRUCT_DEBUG = "structdebug";
    public static final String CMD_RADAR_DEBUG = "radardebug";
    public static final String CMD_STRUCT = "struct";
    public static final String CMD_LIST = "list";
    public static String[] ALL_COMMAND = new String[]{"sendss", "modlist", "reconfig", "title", "fill", "status", "killentity", "removeentity", "attackentity", "showboundingbox", "debug", "spawnerfreeze", "spawnerdebug", "structdebug", "radardebug", "struct", "list"};
    public static MCH_Command instance = new MCH_Command();


    public static boolean canUseCommand(Entity player) {
        return player instanceof EntityPlayer ? instance.canCommandSenderUseCommand((EntityPlayer) player) : false;
    }

    public static boolean checkCommandPermission(ICommandSender sender, String cmd) {
        if ((new CommandGameMode()).canCommandSenderUseCommand(sender)) {
            return true;
        } else {
            if (sender instanceof EntityPlayer && cmd.length() > 0) {
                String playerName = ((EntityPlayer) sender).getGameProfile().getName();
                MCH_Config var10000 = MCH_MOD.config;
                Iterator i$ = MCH_Config.CommandPermissionList.iterator();

                while (i$.hasNext()) {
                    MCH_Config.CommandPermission c = (MCH_Config.CommandPermission) i$.next();
                    if (c.name.equals(cmd)) {
                        String[] arr$ = c.players;
                        int len$ = arr$.length;

                        for (int i$1 = 0; i$1 < len$; ++i$1) {
                            String s = arr$[i$1];
                            if (s.equalsIgnoreCase(playerName)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    public static void onCommandEvent(CommandEvent event) {
        if (event.command instanceof MCH_Command) {
            if (event.parameters.length > 0 && event.parameters[0].length() > 0) {
                if (!checkCommandPermission(event.sender, event.parameters[0])) {
                    event.setCanceled(true);
                    ChatComponentTranslation c = new ChatComponentTranslation("commands.generic.permission", new Object[0]);
                    c.getChatStyle().setColor(EnumChatFormatting.RED);
                    event.sender.addChatMessage(c);
                }

            } else {
                event.setCanceled(true);
            }
        }
    }

    public String getCommandName() {
        return "mcheli";
    }

    public boolean canCommandSenderUseCommand(ICommandSender player) {
        return true;
    }

    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "commands.mcheli.usage";
    }

    public void processCommand(ICommandSender sender, String[] prm) {
        MCH_Config var10000 = MCH_MOD.config;
        if (MCH_Config.EnableCommand.prmBool) {
            if (!checkCommandPermission(sender, prm[0])) {
                ChatComponentTranslation var11 = new ChatComponentTranslation("commands.generic.permission", new Object[0]);
                var11.getChatStyle().setColor(EnumChatFormatting.RED);
                sender.addChatMessage(var11);
            } else {
                EntityPlayerMP msg;
                if (prm[0].equalsIgnoreCase("sendss")) {
                    if (prm.length != 2) {
                        throw new CommandException("Parameter error! : /mcheli sendss playerName", new Object[0]);
                    }

                    msg = getPlayer(sender, prm[1]);
                    if (msg != null) {
                        MCH_PacketIndClient.send(msg, 1, prm[1]);
                    }
                } else if (prm[0].equalsIgnoreCase("modlist")) {
                    if (prm.length != 2) {
                        throw new CommandException("Parameter error! : /mcheli modlist playerName", new Object[0]);
                    }

                    msg = sender instanceof EntityPlayerMP ? (EntityPlayerMP) sender : null;
                    EntityPlayerMP arr$ = getPlayer(sender, prm[1]);
                    if (arr$ != null) {
                        MCH_PacketIndClient.send(arr$, 2, "" + MCH_MultiplayPacketHandler.getPlayerInfoId(msg));
                    }
                } else if (prm[0].equalsIgnoreCase("reconfig")) {
                    if (prm.length != 1) {
                        throw new CommandException("Parameter error! : /mcheli reconfig", new Object[0]);
                    }

                    MCH_MOD.proxy.reconfig();
                    if (sender.getEntityWorld() != null && !sender.getEntityWorld().isRemote) {
                        MCH_PacketNotifyServerSettings.sendAll();
                    }

                    if (MCH_MOD.proxy.isSinglePlayer()) {
                        sender.addChatMessage(new ChatComponentText("Reload mcheli.cfg"));
                    } else {
                        sender.addChatMessage(new ChatComponentText("Reload server side mcheli.cfg"));
                    }
                } else {
                    int len$;
                    String var9;
                    if (prm[0].equalsIgnoreCase("title")) {
                        if (prm.length < 4) {
                            throw new WrongUsageException("Parameter error! : /mcheli title time[1~180] position[0~4] messege[JSON format]", new Object[0]);
                        }

                        var9 = func_82360_a(sender, prm, 3);
                        int var10 = Integer.valueOf(prm[1]).intValue();
                        if (var10 < 1) {
                            var10 = 1;
                        }

                        if (var10 > 180) {
                            var10 = 180;
                        }

                        len$ = Integer.valueOf(prm[2]).intValue();
                        if (len$ < 0) {
                            len$ = 0;
                        }

                        if (len$ > 5) {
                            len$ = 5;
                        }

                        try {
                            IChatComponent i$ = Serializer.func_150699_a(var9);
                            MCH_PacketTitle.send(i$, 20 * var10, len$);
                        } catch (JsonParseException var8) {
                            Throwable s = ExceptionUtils.getRootCause(var8);
                            throw new SyntaxErrorException("mcheli.title.jsonException", new Object[]{s == null ? "" : s.getMessage()});
                        }
                    } else if (prm[0].equalsIgnoreCase("fill")) {
                        this.executeFill(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("status")) {
                        this.executeStatus(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("killentity")) {
                        this.executeKillEntity(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("removeentity")) {
                        this.executeRemoveEntity(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("attackentity")) {
                        this.executeAttackEntity(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("showboundingbox")) {
                        if (prm.length != 2) {
                            throw new CommandException("Parameter error! : /mcheli showboundingbox true or false", new Object[0]);
                        }

                        if (!parseBoolean(sender, prm[1])) {
                            var10000 = MCH_MOD.config;
                            MCH_Config.EnableDebugBoundingBox.prmBool = false;
                            MCH_PacketNotifyServerSettings.sendAll();
                            sender.addChatMessage(new ChatComponentText("Disabled bounding box"));
                        } else {
                            var10000 = MCH_MOD.config;
                            MCH_Config.EnableDebugBoundingBox.prmBool = true;
                            MCH_PacketNotifyServerSettings.sendAll();
                            sender.addChatMessage(new ChatComponentText("Enabled bounding box [F3 + b]"));
                        }
                    } else if (prm[0].equalsIgnoreCase("debug")) {
                        if (prm.length != 3 || (!prm[1].equalsIgnoreCase("gunner") && !prm[1].equalsIgnoreCase("freelook") && !prm[1].equalsIgnoreCase("waypoint") && !prm[1].equalsIgnoreCase("waypointnav"))) {
                            throw new CommandException("Parameter error! : /mcheli debug <gunner|freelook|waypoint|waypointnav> true or false", new Object[0]);
                        }
                        boolean enabled = parseBoolean(sender, prm[2]);
                        if (prm[1].equalsIgnoreCase("gunner")) {
                            MCH_ServerSettings.enableDebugGunnerTeam = enabled;
                            MCH_PacketNotifyServerSettings.sendAll();
                            sender.addChatMessage(new ChatComponentText("Debug gunner team label: " + (MCH_ServerSettings.enableDebugGunnerTeam ? "ON" : "OFF")));
                        } else if (prm[1].equalsIgnoreCase("freelook")) {
                            MCH_ServerSettings.enableDebugFreeLook = enabled;
                            if (MCH_ServerSettings.enableDebugFreeLook) {
                                sender.addChatMessage(new ChatComponentText("Debug freelook trace: ON (log: " + MCH_FreeLookDebug.getLogPath() + ")"));
                            } else {
                                sender.addChatMessage(new ChatComponentText("Debug freelook trace: OFF"));
                            }
                        } else if (prm[1].equalsIgnoreCase("waypointnav")) {
                            MCH_ServerSettings.enableDebugWaypointNav = enabled;
                            if (MCH_ServerSettings.enableDebugWaypointNav) {
                                sender.addChatMessage(new ChatComponentText("Debug waypoint nav trace: ON (log: " + MCH_WaypointNavDebug.getLogPath() + ")"));
                            } else {
                                sender.addChatMessage(new ChatComponentText("Debug waypoint nav trace: OFF"));
                            }
                        } else {
                            MCH_ServerSettings.enableDebugWaypointLabel = enabled;
                            MCH_PacketNotifyServerSettings.sendAll();
                            sender.addChatMessage(new ChatComponentText("Debug waypoint label: " + (MCH_ServerSettings.enableDebugWaypointLabel ? "ON" : "OFF")));
                        }
                    } else if (prm[0].equalsIgnoreCase("spawnerfreeze")) {
                        if (prm.length == 1 || (prm.length >= 2 && prm[1].equalsIgnoreCase("toggle"))) {
                            MCH_ServerSettings.freezeConfigSpawner = !MCH_ServerSettings.freezeConfigSpawner;
                        } else if (prm.length >= 2 && prm[1].equalsIgnoreCase("status")) {
                            sender.addChatMessage(new ChatComponentText("Config spawner freeze: " + (MCH_ServerSettings.freezeConfigSpawner ? "ON" : "OFF")));
                            return;
                        } else if (prm.length == 2) {
                            MCH_ServerSettings.freezeConfigSpawner = parseBoolean(sender, prm[1]);
                        } else {
                            throw new CommandException("Parameter error! : /mcheli spawnerfreeze [true|false|toggle|status]", new Object[0]);
                        }
                        sender.addChatMessage(new ChatComponentText("Config spawner freeze: " + (MCH_ServerSettings.freezeConfigSpawner ? "ON" : "OFF")));
                    } else if (prm[0].equalsIgnoreCase("spawnerdebug")) {
                        this.executeSpawnerDebug(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("structdebug")) {
                        this.executeStructureDebug(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("radardebug")) {
                        this.executeRadarDebug(sender, prm);
                    } else if (prm[0].equalsIgnoreCase("struct")) {
                        this.executeStructureCommand(sender, prm);
                    } else {
                        if (!prm[0].equalsIgnoreCase("list")) {
                            throw new CommandException("Unknown mcheli command. please type /mcheli list", new Object[0]);
                        }

                        var9 = "";
                        String[] var12 = ALL_COMMAND;
                        len$ = var12.length;

                        for (int var13 = 0; var13 < len$; ++var13) {
                            String var14 = var12[var13];
                            var9 = var9 + var14 + ", ";
                        }

                        sender.addChatMessage(new ChatComponentText("/mcheli command list : " + var9));
                    }
                }

            }
        }
    }

    private void executeAttackEntity(ICommandSender sender, String[] args) {
        if (args.length < 3) {
            throw new WrongUsageException("/mcheli attackentity <entity class name : example1 EntityBat , example2 minecraft.entity.passive> <damage> [damage source]", new Object[0]);
        } else {
            String className = args[1].toLowerCase();
            float damage = Float.valueOf(args[2]).floatValue();
            String damageName = args.length >= 4 ? args[3].toLowerCase() : "";
            DamageSource ds = DamageSource.generic;
            if (!damageName.isEmpty()) {
                if (damageName.equals("player")) {
                    if (sender instanceof EntityPlayer) {
                        ds = DamageSource.causePlayerDamage((EntityPlayer) sender);
                    }
                } else if (damageName.equals("anvil")) {
                    ds = DamageSource.anvil;
                } else if (damageName.equals("cactus")) {
                    ds = DamageSource.cactus;
                } else if (damageName.equals("drown")) {
                    ds = DamageSource.drown;
                } else if (damageName.equals("fall")) {
                    ds = DamageSource.fall;
                } else if (damageName.equals("fallingblock")) {
                    ds = DamageSource.fallingBlock;
                } else if (damageName.equals("generic")) {
                    ds = DamageSource.generic;
                } else if (damageName.equals("infire")) {
                    ds = DamageSource.inFire;
                } else if (damageName.equals("inwall")) {
                    ds = DamageSource.inWall;
                } else if (damageName.equals("lava")) {
                    ds = DamageSource.lava;
                } else if (damageName.equals("magic")) {
                    ds = DamageSource.magic;
                } else if (damageName.equals("onfire")) {
                    ds = DamageSource.onFire;
                } else if (damageName.equals("starve")) {
                    ds = DamageSource.starve;
                } else if (damageName.equals("wither")) {
                    ds = DamageSource.wither;
                }
            }

            int attacked = 0;
            List list = sender.getEntityWorld().loadedEntityList;

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i) != null && !(list.get(i) instanceof EntityPlayer) && list.get(i).getClass().getName().toLowerCase().contains(className)) {
                    ((Entity) list.get(i)).attackEntityFrom(ds, damage);
                    ++attacked;
                }
            }

            sender.addChatMessage(new ChatComponentText(attacked + " entity attacked(" + args[1] + ", damage=" + damage + ")."));
        }
    }

    private void executeKillEntity(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException("/mcheli killentity <entity class name : example1 EntityBat , example2 minecraft.entity.passive>", new Object[0]);
        } else {
            String className = args[1].toLowerCase();
            int killed = 0;
            List list = sender.getEntityWorld().loadedEntityList;

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i) != null && !(list.get(i) instanceof EntityPlayer) && list.get(i).getClass().getName().toLowerCase().contains(className)) {
                    ((Entity) list.get(i)).setDead();
                    ++killed;
                }
            }

            sender.addChatMessage(new ChatComponentText(killed + " entity killed(" + args[1] + ")."));
        }
    }

    private void executeRemoveEntity(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException("/mcheli removeentity <entity class name : example1 EntityBat , example2 minecraft.entity.passive>", new Object[0]);
        } else {
            String className = args[1].toLowerCase();
            List list = sender.getEntityWorld().loadedEntityList;
            int removed = 0;

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i) != null && !(list.get(i) instanceof EntityPlayer) && list.get(i).getClass().getName().toLowerCase().indexOf(className) >= 0) {
                    ((Entity) list.get(i)).isDead = true;
                    ++removed;
                }
            }

            sender.addChatMessage(new ChatComponentText(removed + " entity removed(" + args[1] + ")."));
        }
    }

    private void executeStatus(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException("/mcheli status <entity or tile> [min num]", new Object[0]);
        } else {
            if (args[1].equalsIgnoreCase("entity")) {
                this.executeStatusSub(sender, args, "Server loaded Entity List", sender.getEntityWorld().loadedEntityList);
            } else if (args[1].equalsIgnoreCase("tile")) {
                this.executeStatusSub(sender, args, "Server loaded Tile Entity List", sender.getEntityWorld().loadedTileEntityList);
            }

        }
    }

    private void executeStatusSub(ICommandSender sender, String[] args, String title, List list) {
        int minNum = args.length >= 3 ? Integer.valueOf(args[2]).intValue() : 0;
        HashMap map = new HashMap();

        for (int entries = 0; entries < list.size(); ++entries) {
            String send = list.get(entries).getClass().getName();
            if (map.containsKey(send)) {
                map.put(send, Integer.valueOf(((Integer) map.get(send)).intValue() + 1));
            } else {
                map.put(send, Integer.valueOf(1));
            }
        }

        ArrayList var12 = new ArrayList(map.entrySet());
        Collections.sort(var12, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Entry entry1, Entry entry2) {
                return ((String) entry1.getKey()).compareTo((String) entry2.getKey());
            }
        });
        boolean var13 = false;
        sender.addChatMessage(new ChatComponentText("--- " + title + " ---"));
        Iterator i$ = var12.iterator();

        while (i$.hasNext()) {
            Entry s = (Entry) i$.next();
            if (((Integer) s.getValue()).intValue() >= minNum) {
                String msg = " " + (String) s.getKey() + " : " + s.getValue();
                System.out.println(msg);
                sender.addChatMessage(new ChatComponentText(msg));
                var13 = true;
            }
        }

        if (!var13) {
            System.out.println("none");
            sender.addChatMessage(new ChatComponentText("none"));
        }

    }

    public void executeFill(ICommandSender sender, String[] args) {
        if (args.length < 8) {
            throw new WrongUsageException("/mcheli fill <x1> <y1> <z1> <x2> <y2> <z2> <block name> [meta data] [oldBlockHandling] [data tag]", new Object[0]);
        } else {
            int x1 = sender.getPlayerCoordinates().posX;
            int y1 = sender.getPlayerCoordinates().posY;
            int z1 = sender.getPlayerCoordinates().posZ;
            int x2 = sender.getPlayerCoordinates().posX;
            int y2 = sender.getPlayerCoordinates().posY;
            int z2 = sender.getPlayerCoordinates().posZ;
            x1 = MathHelper.floor_double(func_110666_a(sender, (double) x1, args[1]));
            y1 = MathHelper.floor_double(func_110666_a(sender, (double) y1, args[2]));
            z1 = MathHelper.floor_double(func_110666_a(sender, (double) z1, args[3]));
            x2 = MathHelper.floor_double(func_110666_a(sender, (double) x2, args[4]));
            y2 = MathHelper.floor_double(func_110666_a(sender, (double) y2, args[5]));
            z2 = MathHelper.floor_double(func_110666_a(sender, (double) z2, args[6]));
            Block block = CommandBase.getBlockByText(sender, args[7]);
            int metadata = 0;
            if (args.length >= 9) {
                metadata = parseIntBounded(sender, args[8], 0, 15);
            }

            World world = sender.getEntityWorld();
            int t;
            if (x1 > x2) {
                t = x1;
                x1 = x2;
                x2 = t;
            }

            if (y1 > y2) {
                t = y1;
                y1 = y2;
                y2 = t;
            }

            if (z1 > z2) {
                t = z1;
                z1 = z2;
                z2 = t;
            }

            if (y1 >= 0 && y2 < 256) {
                int blockNum = (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
                if (blockNum > 3000000) {
                    throw new CommandException("commands.setblock.tooManyBlocks " + blockNum + " limit=327680", new Object[]{Integer.valueOf(blockNum), Integer.valueOf(3276800)});
                } else {
                    boolean result = false;
                    boolean keep = args.length >= 10 && args[9].equals("keep");
                    boolean destroy = args.length >= 10 && args[9].equals("destroy");
                    boolean override = args.length >= 10 && args[9].equals("override");
                    NBTTagCompound nbttagcompound = new NBTTagCompound();
                    boolean flag = false;
                    if (args.length >= 11 && block.hasTileEntity()) {
                        String x = func_147178_a(sender, args, 10).getUnformattedText();

                        try {
                            NBTBase y = JsonToNBT.func_150315_a(x);
                            if (!(y instanceof NBTTagCompound)) {
                                throw new CommandException("commands.setblock.tagError", new Object[]{"Not a valid tag"});
                            }

                            nbttagcompound = (NBTTagCompound) y;
                            flag = true;
                        } catch (NBTException var27) {
                            throw new CommandException("commands.setblock.tagError", new Object[]{var27.getMessage()});
                        }
                    }

                    for (int var28 = x1; var28 <= x2; ++var28) {
                        for (int var29 = y1; var29 <= y2; ++var29) {
                            for (int z = z1; z <= z2; ++z) {
                                if (world.blockExists(var28, var29, z)) {
                                    if (world.isAirBlock(var28, var29, z)) {
                                        if (override) {
                                            continue;
                                        }
                                    } else if (keep) {
                                        continue;
                                    }

                                    if (destroy) {
                                        world.func_147480_a(var28, var29, z, false);
                                    }

                                    TileEntity block2 = world.getTileEntity(var28, var29, z);
                                    if (block2 instanceof IInventory) {
                                        IInventory tileentity = (IInventory) block2;

                                        for (int i = 0; i < tileentity.getSizeInventory(); ++i) {
                                            ItemStack is = tileentity.getStackInSlotOnClosing(i);
                                            if (is != null) {
                                                is.stackSize = 0;
                                            }
                                        }
                                    }

                                    if (world.setBlock(var28, var29, z, block, metadata, 3)) {
                                        if (flag) {
                                            TileEntity var30 = world.getTileEntity(var28, var29, z);
                                            if (var30 != null) {
                                                nbttagcompound.setInteger("x", var28);
                                                nbttagcompound.setInteger("y", var29);
                                                nbttagcompound.setInteger("z", z);
                                                var30.readFromNBT(nbttagcompound);
                                            }
                                        }

                                        result = true;
                                    }
                                }
                            }
                        }
                    }

                    if (result) {
                        func_152373_a(sender, this, "commands.setblock.success", new Object[0]);
                    } else {
                        throw new CommandException("commands.setblock.noChange", new Object[0]);
                    }
                }
            } else {
                throw new CommandException("commands.setblock.outOfWorld", new Object[0]);
            }
        }
    }

    public List addTabCompletionOptions(ICommandSender sender, String[] prm) {
        MCH_Config var10000 = MCH_MOD.config;
        if (!MCH_Config.EnableCommand.prmBool) {
            return null;
        } else if (prm.length <= 1) {
            return getListOfStringsMatchingLastWord(prm, ALL_COMMAND);
        } else {
            if (prm[0].equalsIgnoreCase("sendss")) {
                if (prm.length == 2) {
                    return getListOfStringsMatchingLastWord(prm, MinecraftServer.getServer().getAllUsernames());
                }
            } else if (prm[0].equalsIgnoreCase("modlist")) {
                if (prm.length == 3) {
                    return getListOfStringsMatchingLastWord(prm, MinecraftServer.getServer().getAllUsernames());
                }
            } else {
                if (prm[0].equalsIgnoreCase("fill")) {
                    if ((prm.length == 2 || prm.length == 5) && sender instanceof Entity) {
                        Entity entity = (Entity) sender;
                        ArrayList a = new ArrayList();
                        int x = entity.posX < 0.0D ? (int) (entity.posX - 1.0D) : (int) entity.posX;
                        int z = entity.posZ < 0.0D ? (int) (entity.posZ - 1.0D) : (int) entity.posZ;
                        a.add("" + x + " " + (int) (entity.posY + 0.5D) + " " + z);
                        return a;
                    }

                    return prm.length == 8 ? getListOfStringsFromIterableMatchingLastWord(prm, Block.blockRegistry.getKeys()) : (prm.length == 10 ? getListOfStringsMatchingLastWord(prm, new String[]{"replace", "destroy", "keep", "override"}) : null);
                }

                if (prm[0].equalsIgnoreCase("status")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"entity", "tile"});
                    }
                } else if (prm[0].equalsIgnoreCase("attackentity")) {
                    if (prm.length == 4) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"player", "inFire", "onFire", "lava", "inWall", "drown", "starve", "cactus", "fall", "outOfWorld", "generic", "magic", "wither", "anvil", "fallingBlock"});
                    }
                } else if (prm[0].equalsIgnoreCase("showboundingbox") && prm.length == 2) {
                    return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false"});
                } else if (prm[0].equalsIgnoreCase("debug")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"gunner", "freelook", "waypoint", "waypointnav"});
                    }
                    if (prm.length == 3 && (prm[1].equalsIgnoreCase("gunner") || prm[1].equalsIgnoreCase("freelook") || prm[1].equalsIgnoreCase("waypoint") || prm[1].equalsIgnoreCase("waypointnav"))) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false"});
                    }
                } else if (prm[0].equalsIgnoreCase("spawnerfreeze")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false", "toggle", "status"});
                    }
                } else if (prm[0].equalsIgnoreCase("spawnerdebug")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"here"});
                    }
                    if ((prm.length == 2 || prm.length == 3 || prm.length == 4) && sender instanceof Entity) {
                        Entity entity = (Entity) sender;
                        ArrayList<String> a = new ArrayList<String>();
                        int x = entity.posX < 0.0D ? (int)(entity.posX - 1.0D) : (int)entity.posX;
                        int y = entity.posY < 0.0D ? (int)(entity.posY - 1.0D) : (int)entity.posY;
                        int z = entity.posZ < 0.0D ? (int)(entity.posZ - 1.0D) : (int)entity.posZ;
                        if (prm.length == 2) a.add(String.valueOf(x));
                        if (prm.length == 3) a.add(String.valueOf(y));
                        if (prm.length == 4) a.add(String.valueOf(z));
                        return a;
                    }
                } else if (prm[0].equalsIgnoreCase("structdebug")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"here", "true", "false", "status"});
                    }
                    if ((prm.length == 2 || prm.length == 3) && sender instanceof Entity) {
                        Entity entity = (Entity) sender;
                        ArrayList<String> a = new ArrayList<String>();
                        int x = entity.posX < 0.0D ? (int)(entity.posX - 1.0D) : (int)entity.posX;
                        int z = entity.posZ < 0.0D ? (int)(entity.posZ - 1.0D) : (int)entity.posZ;
                        if (prm.length == 2) a.add(String.valueOf(x));
                        if (prm.length == 3) a.add(String.valueOf(z));
                        return a;
                    }
                } else if (prm[0].equalsIgnoreCase("radardebug")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false", "toggle", "status", "verbose", "dl", "dlwatch"});
                    }
                    if (prm.length == 3 && prm[1].equalsIgnoreCase("verbose")) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false", "toggle", "status"});
                    }
                    if (prm.length == 3 && prm[1].equalsIgnoreCase("dlwatch")) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false", "toggle", "status"});
                    }
                } else if (prm[0].equalsIgnoreCase("struct")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"capture", "place", "list", "validate", "verify", "importschem"});
                    }
                    if (prm.length == 10 && prm[1].equalsIgnoreCase("capture")) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"true", "false"});
                    }
                    if (prm.length == 3 && (prm[1].equalsIgnoreCase("place") || prm[1].equalsIgnoreCase("validate") || prm[1].equalsIgnoreCase("verify"))) {
                        return getListOfStringsFromIterableMatchingLastWord(prm, this.getStructureNames());
                    }
                    if (prm.length == 4 && prm[1].equalsIgnoreCase("importschem")) {
                        return getListOfStringsFromIterableMatchingLastWord(prm, this.getStructureNames());
                    }
                    if (prm.length == 5 && prm[1].equalsIgnoreCase("importschem")) {
                        return getListOfStringsFromIterableMatchingLastWord(prm, this.getSchemImportCandidates());
                    }
                    if (prm.length == 7 && prm[1].equalsIgnoreCase("place")) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"0", "90", "180", "270"});
                    }
                    if (prm.length == 7 && prm[1].equalsIgnoreCase("verify")) {
                        return getListOfStringsMatchingLastWord(prm, new String[]{"0", "90", "180", "270"});
                    }
                }
            }

            return null;
        }
    }

    private void executeSpawnerDebug(ICommandSender sender, String[] args) {
        if (args.length != 2 && args.length != 4) {
            throw new WrongUsageException("/mcheli spawnerdebug <here|x y z>", new Object[0]);
        }
        World world = sender.getEntityWorld();
        int x;
        int y;
        int z;
        if (args.length == 2) {
            if (!args[1].equalsIgnoreCase("here")) {
                throw new WrongUsageException("/mcheli spawnerdebug <here|x y z>", new Object[0]);
            }
            ChunkCoordinates pc = sender.getPlayerCoordinates();
            x = pc.posX;
            y = pc.posY;
            z = pc.posZ;
            if (!(world.getTileEntity(x, y, z) instanceof MCH_ConfigSpawnerTileEntity) && y > 0 && world.getTileEntity(x, y - 1, z) instanceof MCH_ConfigSpawnerTileEntity) {
                y -= 1;
            }
        } else {
            ChunkCoordinates pc = sender.getPlayerCoordinates();
            x = MathHelper.floor_double(func_110666_a(sender, (double)pc.posX, args[1]));
            y = MathHelper.floor_double(func_110666_a(sender, (double)pc.posY, args[2]));
            z = MathHelper.floor_double(func_110666_a(sender, (double)pc.posZ, args[3]));
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof MCH_ConfigSpawnerTileEntity)) {
            sender.addChatMessage(new ChatComponentText("No config spawner tile at " + x + "," + y + "," + z));
            return;
        }
        MCH_ConfigSpawnerTileEntity tile = (MCH_ConfigSpawnerTileEntity)te;
        sender.addChatMessage(new ChatComponentText("SpawnerDebug@" + x + "," + y + "," + z));
        sender.addChatMessage(new ChatComponentText(tile.getDebugStatusLine()));
    }

    private void executeStructureDebug(ICommandSender sender, String[] args) {
        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("status")) {
                sender.addChatMessage(new ChatComponentText("Structure debug ticker: " + (MCH_StructureDebugLogger.isEnabled() ? "ON" : "OFF")));
                sender.addChatMessage(new ChatComponentText("Log file: logs/mcheli_structure_debug.log"));
                return;
            }
            if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("ture")) {
                boolean enabled = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("ture");
                MCH_StructureDebugLogger.setEnabled(enabled);
                sender.addChatMessage(new ChatComponentText("Structure debug ticker: " + (enabled ? "ON" : "OFF")));
                sender.addChatMessage(new ChatComponentText("Tick interval: 150, log file: logs/mcheli_structure_debug.log"));
                return;
            }
        }
        if (args.length != 2 && args.length != 3) {
            throw new WrongUsageException("/mcheli structdebug <true|false|status|here|x z>", new Object[0]);
        }
        World world = sender.getEntityWorld();
        int x;
        int z;
        if (args.length == 2) {
            if (!args[1].equalsIgnoreCase("here")) {
                throw new WrongUsageException("/mcheli structdebug <here|x z>", new Object[0]);
            }
            ChunkCoordinates pc = sender.getPlayerCoordinates();
            x = pc.posX;
            z = pc.posZ;
        } else {
            ChunkCoordinates pc = sender.getPlayerCoordinates();
            x = MathHelper.floor_double(func_110666_a(sender, (double)pc.posX, args[1]));
            z = MathHelper.floor_double(func_110666_a(sender, (double)pc.posZ, args[2]));
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        int y = world.getTopSolidOrLiquidBlock(centerX, centerZ);
        int dim = world.provider != null ? world.provider.dimensionId : 0;
        String worldName = world.getWorldInfo() != null ? world.getWorldInfo().getWorldName() : "<unknown>";
        BiomeGenBase biome = world.getBiomeGenForCoords(centerX, centerZ);
        String biomeName = biome != null && biome.biomeName != null ? biome.biomeName.trim().toLowerCase() : "";

        List<MCH_StructureRule> rules = MCH_StructureRuleManager.getRules();
        File loadedDir = MCH_StructureRuleManager.getLoadedDir();
        sender.addChatMessage(new ChatComponentText("StructDebug world=" + worldName + " dim=" + dim + " pos=" + centerX + "," + y + "," + centerZ + " chunk=" + chunkX + "," + chunkZ + " biome=" + biomeName));
        sender.addChatMessage(new ChatComponentText("StructDebug rules=" + rules.size() + " dir=" + (loadedDir != null ? loadedDir.getPath() : "<null>")));
        if (rules.isEmpty()) {
            sender.addChatMessage(new ChatComponentText(" - no rules loaded"));
            return;
        }

        for (MCH_StructureRule r : rules) {
            if (r == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(" - ").append(r.id).append(" [").append(r.structure).append("] ");
            if (!r.enable) {
                sb.append("DISABLED");
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            if (r.gridSpacingChunk <= 0) {
                sb.append("FAIL gridSpacing<=0");
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            int modX = floorMod(chunkX, r.gridSpacingChunk);
            int modZ = floorMod(chunkZ, r.gridSpacingChunk);
            if (modX != 0 || modZ != 0) {
                sb.append("FAIL grid chunkMod=").append(modX).append(",").append(modZ).append(" spacing=").append(r.gridSpacingChunk);
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            if (!r.matchesWorld(world, centerX, centerZ)) {
                sb.append("FAIL world/dim/biome filter");
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            if (y < r.heightMin || y > r.heightMax) {
                sb.append("FAIL height y=").append(y).append(" range=").append(r.heightMin).append("-").append(r.heightMax);
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            if (!checkSlope(world, centerX, centerZ, r.slopeMax)) {
                sb.append("FAIL slope>").append(r.slopeMax);
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            File root = this.getStructureRootDir();
            File meta = MCH_StructureIO.getMetaFile(root, r.structure);
            File blob = MCH_StructureIO.getBlobFile(root, r.structure);
            if (!meta.exists() || !blob.exists()) {
                sb.append("FAIL asset missing meta=").append(meta.exists()).append(" blob=").append(blob.exists());
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                continue;
            }
            sb.append("PASS static checks; chance=").append(r.chance).append(" (random gate)");
            sender.addChatMessage(new ChatComponentText(sb.toString()));
        }
    }

    private void executeRadarDebug(ICommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("dlwatch")) {
            if (args.length == 2 || (args.length >= 3 && args[2].equalsIgnoreCase("toggle"))) {
                MCH_RadarDebug.setDataLinkWatchEnabled(!MCH_RadarDebug.isDataLinkWatchEnabled());
            } else if (args.length >= 3 && args[2].equalsIgnoreCase("status")) {
                sender.addChatMessage(new ChatComponentText("DL watch: " + (MCH_RadarDebug.isDataLinkWatchEnabled() ? "ON" : "OFF")
                    + ", intervalTick=" + MCH_RadarDebug.getDataLinkWatchIntervalTick()));
                sender.addChatMessage(new ChatComponentText("Log file: " + MCH_RadarDebug.getLogPath()));
                return;
            } else if (args.length >= 3 && (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false"))) {
                MCH_RadarDebug.setDataLinkWatchEnabled(parseBoolean(sender, args[2]));
            } else {
                throw new WrongUsageException("/mcheli radardebug dlwatch [true|false|toggle|status] [intervalTick]", new Object[0]);
            }
            if (args.length >= 4) {
                try {
                    MCH_RadarDebug.setDataLinkWatchIntervalTick(Integer.parseInt(args[3]));
                } catch (Exception ex) {
                    sender.addChatMessage(new ChatComponentText("Invalid intervalTick: " + args[3]));
                }
            }
            sender.addChatMessage(new ChatComponentText("DL watch: " + (MCH_RadarDebug.isDataLinkWatchEnabled() ? "ON" : "OFF")
                + ", intervalTick=" + MCH_RadarDebug.getDataLinkWatchIntervalTick()));
            sender.addChatMessage(new ChatComponentText("Log file: " + MCH_RadarDebug.getLogPath()));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("dl")) {
            if (!(sender instanceof EntityPlayer)) {
                sender.addChatMessage(new ChatComponentText("DataLink debug: player only."));
                return;
            }
            EntityPlayer player = (EntityPlayer)sender;
            MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(player);
            if (ac == null) {
                sender.addChatMessage(new ChatComponentText("DataLink debug: not in aircraft."));
                return;
            }
            MCH_WeaponSet ws = ac.getCurrentWeapon(player);
            MCH_WeaponInfo wi = ws != null ? ws.getInfo() : null;
            String weaponName = ws != null ? ws.getName() : "(none)";
            boolean dlEnabled = wi != null && wi.enableDataLink;
            boolean dlOnly = wi != null && wi.onlyDataLink;
            boolean dlMode = ws != null && (dlOnly || ws.isDataLinkMode());
            boolean radarClass = wi != null && (wi.activeRadar || wi.passiveRadar || wi.semiActiveRadar) && !wi.antiRadiationMissile;
            int selected = MCH_RenderRWR.getRadarSelectedTargetId(ac);
            int tracking = MCH_RenderRWR.getRadarTrackingTargetId(ac);
            String search = MCH_RenderRWR.getRadarSearchType(ac);
            String line1 = String.format(Locale.ROOT,
                "[DL] weapon=%s radarClass=%s dlEnabled=%s dlOnly=%s dlMode=%s search=%s sel=%d trk=%d",
                weaponName, radarClass, dlEnabled, dlOnly, dlMode, search, selected, tracking);
            sender.addChatMessage(new ChatComponentText(line1));
            int missileCount = 0;
            int relayCount = 0;
            int capturedCount = 0;
            if (ac.worldObj != null) {
                for (Object o : ac.worldObj.loadedEntityList) {
                    if (!(o instanceof MCH_EntityBaseBullet)) {
                        continue;
                    }
                    MCH_EntityBaseBullet m = (MCH_EntityBaseBullet)o;
                    if (m.shootingAircraft == ac || m.shootingEntity == ac || m.shootingEntity == player) {
                        missileCount++;
                        if (m.isDataLinkRelayMode()) {
                            relayCount++;
                        }
                        if (m.isActiveRadarCaptured()) {
                            capturedCount++;
                        }
                    }
                }
            }
            String line2 = String.format(Locale.ROOT,
                "[DL] missiles self=%d relay=%d captured=%d", missileCount, relayCount, capturedCount);
            sender.addChatMessage(new ChatComponentText(line2));
            MCH_RadarDebug.appendManual("DLDBG player=%s acId=%d %s", player.getCommandSenderName(), ac.getEntityId(), line1);
            MCH_RadarDebug.appendManual("DLDBG player=%s acId=%d %s", player.getCommandSenderName(), ac.getEntityId(), line2);
            sender.addChatMessage(new ChatComponentText("DL debug written: " + MCH_RadarDebug.getLogPath()));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("verbose")) {
            if (args.length == 2 || (args.length >= 3 && args[2].equalsIgnoreCase("toggle"))) {
                MCH_RadarDebug.setVerbose(!MCH_RadarDebug.isVerbose());
            } else if (args.length >= 3 && args[2].equalsIgnoreCase("status")) {
                sender.addChatMessage(new ChatComponentText("Radar debug verbose: " + (MCH_RadarDebug.isVerbose() ? "ON" : "OFF")));
                return;
            } else if (args.length == 3 && (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false"))) {
                MCH_RadarDebug.setVerbose(parseBoolean(sender, args[2]));
            } else {
                throw new WrongUsageException("/mcheli radardebug verbose [true|false|toggle|status]", new Object[0]);
            }
            sender.addChatMessage(new ChatComponentText("Radar debug verbose: " + (MCH_RadarDebug.isVerbose() ? "ON" : "OFF")));
            return;
        }
        if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("toggle"))) {
            MCH_RadarDebug.setEnabled(!MCH_RadarDebug.isEnabled());
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("status")) {
            sender.addChatMessage(new ChatComponentText("Radar debug monitor: " + (MCH_RadarDebug.isEnabled() ? "ON" : "OFF")));
            sender.addChatMessage(new ChatComponentText("Radar debug verbose: " + (MCH_RadarDebug.isVerbose() ? "ON" : "OFF")));
            sender.addChatMessage(new ChatComponentText("Log file: " + MCH_RadarDebug.getLogPath()));
            return;
        } else if (args.length == 2 && (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false"))) {
            MCH_RadarDebug.setEnabled(parseBoolean(sender, args[1]));
        } else {
            throw new WrongUsageException("/mcheli radardebug [true|false|toggle|status|verbose ...|dl|dlwatch ...]", new Object[0]);
        }
        sender.addChatMessage(new ChatComponentText("Radar debug monitor: " + (MCH_RadarDebug.isEnabled() ? "ON" : "OFF")));
        sender.addChatMessage(new ChatComponentText("Radar debug verbose: " + (MCH_RadarDebug.isVerbose() ? "ON" : "OFF")));
        sender.addChatMessage(new ChatComponentText("Log file: " + MCH_RadarDebug.getLogPath()));
    }

    private void executeStructureCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException("/mcheli struct <capture|place|list|validate|verify|importschem> ...", new Object[0]);
        }
        if (args[1].equalsIgnoreCase("capture")) {
            this.executeStructureCapture(sender, args);
        } else if (args[1].equalsIgnoreCase("place")) {
            this.executeStructurePlace(sender, args);
        } else if (args[1].equalsIgnoreCase("list")) {
            this.executeStructureList(sender, args);
        } else if (args[1].equalsIgnoreCase("validate")) {
            this.executeStructureValidate(sender, args);
        } else if (args[1].equalsIgnoreCase("verify")) {
            this.executeStructureVerify(sender, args);
        } else if (args[1].equalsIgnoreCase("importschem")) {
            this.executeStructureImportSchem(sender, args);
        } else {
            throw new WrongUsageException("/mcheli struct <capture|place|list|validate|verify|importschem> ...", new Object[0]);
        }
    }

    private void executeStructureCapture(ICommandSender sender, String[] args) {
        if (args.length != 9 && args.length != 10) {
            throw new WrongUsageException("/mcheli struct capture <name> <x1> <y1> <z1> <x2> <y2> <z2> [captureAir=true|false]", new Object[0]);
        }
        String name = this.sanitizeStructureName(args[2]);
        boolean captureAir = false;
        if (args.length == 10) {
            captureAir = parseBoolean(sender, args[9]);
        }
        ChunkCoordinates pc = sender.getPlayerCoordinates();
        int x1 = MathHelper.floor_double(func_110666_a(sender, (double) pc.posX, args[3]));
        int y1 = MathHelper.floor_double(func_110666_a(sender, (double) pc.posY, args[4]));
        int z1 = MathHelper.floor_double(func_110666_a(sender, (double) pc.posZ, args[5]));
        int x2 = MathHelper.floor_double(func_110666_a(sender, (double) pc.posX, args[6]));
        int y2 = MathHelper.floor_double(func_110666_a(sender, (double) pc.posY, args[7]));
        int z2 = MathHelper.floor_double(func_110666_a(sender, (double) pc.posZ, args[8]));
        if (x1 > x2) {
            int t = x1; x1 = x2; x2 = t;
        }
        if (y1 > y2) {
            int t = y1; y1 = y2; y2 = t;
        }
        if (z1 > z2) {
            int t = z1; z1 = z2; z2 = t;
        }
        if (y1 < 0 || y2 >= 256) {
            throw new CommandException("commands.setblock.outOfWorld", new Object[0]);
        }
        int sx = x2 - x1 + 1;
        int sy = y2 - y1 + 1;
        int sz = z2 - z1 + 1;
        int volume = sx * sy * sz;
        if (volume > 3000000) {
            throw new CommandException("Too many blocks for capture: " + volume, new Object[0]);
        }

        World world = sender.getEntityWorld();
        MCH_StructureMeta meta = new MCH_StructureMeta();
        meta.name = name;
        meta.sizeX = sx;
        meta.sizeY = sy;
        meta.sizeZ = sz;
        meta.anchorX = 0;
        meta.anchorY = 0;
        meta.anchorZ = 0;
        meta.author = sender.getCommandSenderName();
        meta.createdAt = String.valueOf(System.currentTimeMillis());
        meta.description = "Captured by /mcheli struct capture";

        MCH_StructureBlob blob = new MCH_StructureBlob();
        blob.sizeX = sx;
        blob.sizeY = sy;
        blob.sizeZ = sz;

        int captured = 0;
        for (int x = x1; x <= x2; ++x) {
            for (int y = y1; y <= y2; ++y) {
                for (int z = z1; z <= z2; ++z) {
                    Block block = world.getBlock(x, y, z);
                    if (block == null) {
                        continue;
                    }
                    if (!captureAir && block == Blocks.air) {
                        continue;
                    }
                    Object nameObj = Block.blockRegistry.getNameForObject(block);
                    if (nameObj == null) {
                        continue;
                    }
                    MCH_StructureBlob.BlockEntry e = new MCH_StructureBlob.BlockEntry();
                    e.x = x - x1;
                    e.y = y - y1;
                    e.z = z - z1;
                    e.blockName = nameObj.toString();
                    e.meta = world.getBlockMetadata(x, y, z) & 15;
                    TileEntity te = world.getTileEntity(x, y, z);
                    if (te != null) {
                        NBTTagCompound teTag = new NBTTagCompound();
                        te.writeToNBT(teTag);
                        e.tileEntity = teTag;
                    }
                    blob.blocks.add(e);
                    captured++;
                }
            }
        }

        try {
            MCH_StructureIO.saveAsset(this.getStructureRootDir(), name, meta, blob);
            sender.addChatMessage(new ChatComponentText("Structure captured: " + name + " size=" + sx + "x" + sy + "x" + sz + " blocks=" + captured + " captureAir=" + captureAir));
        } catch (IOException e) {
            throw new CommandException("Failed to save structure: " + e.getMessage(), new Object[0]);
        }
    }

    private void executeStructurePlace(ICommandSender sender, String[] args) {
        if (args.length < 6 || args.length > 7) {
            throw new WrongUsageException("/mcheli struct place <name> <x> <y> <z> [rot=0|90|180|270]", new Object[0]);
        }
        String name = this.sanitizeStructureName(args[2]);
        ChunkCoordinates pc = sender.getPlayerCoordinates();
        int baseX = MathHelper.floor_double(func_110666_a(sender, (double) pc.posX, args[3]));
        int baseY = MathHelper.floor_double(func_110666_a(sender, (double) pc.posY, args[4]));
        int baseZ = MathHelper.floor_double(func_110666_a(sender, (double) pc.posZ, args[5]));
        int rot = 0;
        if (args.length >= 7) {
            rot = this.normalizeRotation(parseIntBounded(sender, args[6], -360, 360));
            if (rot < 0) {
                throw new WrongUsageException("Rotation must be one of 0/90/180/270", new Object[0]);
            }
        }

        MCH_StructureMeta meta;
        MCH_StructureBlob blob;
        try {
            File root = this.getStructureRootDir();
            meta = MCH_StructureIO.loadMeta(root, name);
            blob = MCH_StructureIO.loadBlob(root, name);
        } catch (IOException e) {
            throw new CommandException("Failed to load structure: " + e.getMessage(), new Object[0]);
        }

        int[] anchorR = this.rotateXZ(meta.anchorX, meta.anchorZ, blob.sizeX, blob.sizeZ, rot);
        World world = sender.getEntityWorld();
        int placed = 0;
        int skipped = 0;
        int teLoaded = 0;
        HashMap<Long, String> expectedBlockInfoByPos = new HashMap<Long, String>();

        for (MCH_StructureBlob.BlockEntry e : blob.blocks) {
            int[] rz = this.rotateXZ(e.x, e.z, blob.sizeX, blob.sizeZ, rot);
            int wx = baseX + (rz[0] - anchorR[0]);
            int wy = baseY + (e.y - meta.anchorY);
            int wz = baseZ + (rz[1] - anchorR[1]);
            if (wy < 0 || wy >= 256 || !world.blockExists(wx, wy, wz)) {
                skipped++;
                continue;
            }
            Block block = Block.getBlockFromName(e.blockName);
            if (block == null) {
                skipped++;
                continue;
            }
            if (block == Blocks.air && world.isAirBlock(wx, wy, wz)) {
                skipped++;
                continue;
            }
            if (!world.setBlock(wx, wy, wz, block, e.meta & 15, 3)) {
                skipped++;
                continue;
            }
            String expectedBlockInfo = this.extractExpectedBlockInfoName(e, block);
            if (expectedBlockInfo != null && !expectedBlockInfo.isEmpty()) {
                expectedBlockInfoByPos.put(this.packBlockPos(wx, wy, wz), expectedBlockInfo);
            }
            if (e.tileEntity != null) {
                TileEntity te = world.getTileEntity(wx, wy, wz);
                if (te != null) {
                    NBTTagCompound teTag = (NBTTagCompound) e.tileEntity.copy();
                    teTag.setInteger("x", wx);
                    teTag.setInteger("y", wy);
                    teTag.setInteger("z", wz);
                    if (te instanceof MCH_ConfigSpawnerTileEntity) {
                        this.sanitizeSpawnerRuntimeNBT(teTag);
                    }
                    te.readFromNBT(teTag);
                    te.markDirty();
                    teLoaded++;
                }
            }
            placed++;
        }
        int repaired = this.repairPlacedStructureBlockInfo(world, expectedBlockInfoByPos);
        int forceTry = 0;
        int forceSpawn = 0;
        for (Long key : expectedBlockInfoByPos.keySet()) {
            int x = this.unpackX(key.longValue());
            int y = this.unpackY(key.longValue());
            int z = this.unpackZ(key.longValue());
            TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof MCH_ConfigSpawnerTileEntity)) {
                continue;
            }
            MCH_ConfigSpawnerTileEntity tile = (MCH_ConfigSpawnerTileEntity) te;
            if (tile.getBlockInfo() == null || !tile.getBlockInfo().enableSpawner) {
                continue;
            }
            forceTry++;
            if (tile.forceSpawnOnceNow()) {
                forceSpawn++;
            }
        }

        sender.addChatMessage(new ChatComponentText(
            "Structure placed: " + name
                + " rot=" + rot
                + " placed=" + placed
                + " skipped=" + skipped
                + " te=" + teLoaded
                + " repaired=" + repaired
                + " forceTry=" + forceTry
                + " forceSpawn=" + forceSpawn
        ));
    }

    private void executeStructureList(ICommandSender sender, String[] args) {
        File metaDir = new File(this.getStructureRootDir(), MCH_StructureIO.DIR_META);
        File[] files = metaDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase(Locale.ROOT).endsWith(MCH_StructureIO.EXT_META);
            }
        });
        if (files == null || files.length == 0) {
            sender.addChatMessage(new ChatComponentText("No structures found in " + metaDir.getPath()));
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        sender.addChatMessage(new ChatComponentText("Structure list (" + files.length + "):"));
        for (File f : files) {
            String n = f.getName();
            if (n.endsWith(MCH_StructureIO.EXT_META)) {
                n = n.substring(0, n.length() - MCH_StructureIO.EXT_META.length());
            }
            sender.addChatMessage(new ChatComponentText(" - " + n));
        }
    }

    private void executeStructureValidate(ICommandSender sender, String[] args) {
        if (args.length != 3) {
            throw new WrongUsageException("/mcheli struct validate <name>", new Object[0]);
        }
        String name = this.sanitizeStructureName(args[2]);
        MCH_StructureMeta meta;
        MCH_StructureBlob blob;
        try {
            File root = this.getStructureRootDir();
            meta = MCH_StructureIO.loadMeta(root, name);
            blob = MCH_StructureIO.loadBlob(root, name);
        } catch (IOException e) {
            throw new CommandException("Failed to load structure: " + e.getMessage(), new Object[0]);
        }

        int unknownBlock = 0;
        int outOfBounds = 0;
        int missingBlockInfo = 0;
        int invalidBlockInfo = 0;
        int mismatchMetaSize = 0;
        int samplePrinted = 0;
        final int sampleLimit = 8;

        if (meta.sizeX != blob.sizeX || meta.sizeY != blob.sizeY || meta.sizeZ != blob.sizeZ) {
            mismatchMetaSize = 1;
        }

        for (MCH_StructureBlob.BlockEntry e : blob.blocks) {
            if (e.x < 0 || e.y < 0 || e.z < 0 || e.x >= blob.sizeX || e.y >= blob.sizeY || e.z >= blob.sizeZ) {
                outOfBounds++;
                if (samplePrinted < sampleLimit) {
                    sender.addChatMessage(new ChatComponentText(" - OOB: " + e.blockName + " @ " + e.x + "," + e.y + "," + e.z));
                    samplePrinted++;
                }
                continue;
            }
            Block block = Block.getBlockFromName(e.blockName);
            if (block == null) {
                unknownBlock++;
                if (samplePrinted < sampleLimit) {
                    sender.addChatMessage(new ChatComponentText(" - Unknown block: " + e.blockName + " @ " + e.x + "," + e.y + "," + e.z));
                    samplePrinted++;
                }
                continue;
            }
            if (block instanceof MCH_ConfigSpawnerBlock) {
                String expected = this.extractExpectedBlockInfoName(e, block);
                if (expected == null || expected.trim().isEmpty()) {
                    missingBlockInfo++;
                    if (samplePrinted < sampleLimit) {
                        sender.addChatMessage(new ChatComponentText(" - Missing BlockInfoName @ " + e.x + "," + e.y + "," + e.z));
                        samplePrinted++;
                    }
                } else if (MCH_BlockInfoManager.get(expected) == null) {
                    invalidBlockInfo++;
                    if (samplePrinted < sampleLimit) {
                        sender.addChatMessage(new ChatComponentText(" - Invalid BlockInfoName=" + expected + " @ " + e.x + "," + e.y + "," + e.z));
                        samplePrinted++;
                    }
                }
            }
        }

        int err = unknownBlock + outOfBounds + missingBlockInfo + invalidBlockInfo + mismatchMetaSize;
        sender.addChatMessage(new ChatComponentText(
            "Validate[" + name + "]: blocks=" + blob.blocks.size()
                + " unknown=" + unknownBlock
                + " oob=" + outOfBounds
                + " missingBI=" + missingBlockInfo
                + " invalidBI=" + invalidBlockInfo
                + " sizeMismatch=" + mismatchMetaSize
                + " status=" + (err == 0 ? "OK" : "ERROR")
        ));
        MCH_Lib.Log("[mcheli][struct-validate] name=%s blocks=%d unknown=%d oob=%d missingBI=%d invalidBI=%d sizeMismatch=%d",
            name, blob.blocks.size(), unknownBlock, outOfBounds, missingBlockInfo, invalidBlockInfo, mismatchMetaSize);
    }

    private void executeStructureVerify(ICommandSender sender, String[] args) {
        if (args.length < 6 || args.length > 7) {
            throw new WrongUsageException("/mcheli struct verify <name> <x> <y> <z> [rot=0|90|180|270]", new Object[0]);
        }
        String name = this.sanitizeStructureName(args[2]);
        ChunkCoordinates pc = sender.getPlayerCoordinates();
        int baseX = MathHelper.floor_double(func_110666_a(sender, (double) pc.posX, args[3]));
        int baseY = MathHelper.floor_double(func_110666_a(sender, (double) pc.posY, args[4]));
        int baseZ = MathHelper.floor_double(func_110666_a(sender, (double) pc.posZ, args[5]));
        int rot = 0;
        if (args.length >= 7) {
            rot = this.normalizeRotation(parseIntBounded(sender, args[6], -360, 360));
            if (rot < 0) {
                throw new WrongUsageException("Rotation must be one of 0/90/180/270", new Object[0]);
            }
        }
        MCH_StructureMeta meta;
        MCH_StructureBlob blob;
        try {
            File root = this.getStructureRootDir();
            meta = MCH_StructureIO.loadMeta(root, name);
            blob = MCH_StructureIO.loadBlob(root, name);
        } catch (IOException e) {
            throw new CommandException("Failed to load structure: " + e.getMessage(), new Object[0]);
        }

        World world = sender.getEntityWorld();
        int[] anchorR = this.rotateXZ(meta.anchorX, meta.anchorZ, blob.sizeX, blob.sizeZ, rot);
        int checked = 0;
        int mismatchBlock = 0;
        int mismatchMeta = 0;
        int mismatchBlockInfo = 0;
        int missingChunk = 0;

        for (MCH_StructureBlob.BlockEntry e : blob.blocks) {
            int[] rz = this.rotateXZ(e.x, e.z, blob.sizeX, blob.sizeZ, rot);
            int wx = baseX + (rz[0] - anchorR[0]);
            int wy = baseY + (e.y - meta.anchorY);
            int wz = baseZ + (rz[1] - anchorR[1]);
            if (wy < 0 || wy >= 256 || !world.blockExists(wx, wy, wz)) {
                missingChunk++;
                continue;
            }
            Block expectedBlock = Block.getBlockFromName(e.blockName);
            Block actualBlock = world.getBlock(wx, wy, wz);
            checked++;
            if (expectedBlock == null || actualBlock != expectedBlock) {
                mismatchBlock++;
                continue;
            }
            int actualMeta = world.getBlockMetadata(wx, wy, wz) & 15;
            if (actualMeta != (e.meta & 15)) {
                mismatchMeta++;
            }
            if (actualBlock instanceof MCH_ConfigSpawnerBlock) {
                String expectedBI = this.extractExpectedBlockInfoName(e, actualBlock);
                TileEntity te = world.getTileEntity(wx, wy, wz);
                if (te instanceof MCH_ConfigSpawnerTileEntity) {
                    String actualBI = ((MCH_ConfigSpawnerTileEntity) te).getBlockInfoName();
                    if (expectedBI != null && !expectedBI.isEmpty() && (actualBI == null || !actualBI.equalsIgnoreCase(expectedBI))) {
                        mismatchBlockInfo++;
                    }
                } else {
                    mismatchBlockInfo++;
                }
            }
        }

        sender.addChatMessage(new ChatComponentText(
            "Verify[" + name + "]: checked=" + checked
                + " mismatchBlock=" + mismatchBlock
                + " mismatchMeta=" + mismatchMeta
                + " mismatchBlockInfo=" + mismatchBlockInfo
                + " missingChunk=" + missingChunk
                + " status=" + ((mismatchBlock + mismatchMeta + mismatchBlockInfo) == 0 ? "OK" : "DIFF")
        ));
        MCH_Lib.Log("[mcheli][struct-verify] name=%s checked=%d mismatchBlock=%d mismatchMeta=%d mismatchBI=%d missingChunk=%d",
            name, checked, mismatchBlock, mismatchMeta, mismatchBlockInfo, missingChunk);
    }

    private void executeStructureImportSchem(ICommandSender sender, String[] args) {
        if (args.length != 5) {
            throw new WrongUsageException("/mcheli struct importschem <name> <schemPath>", new Object[0]);
        }
        String assetName = this.sanitizeStructureName(args[3]);
        File schemFile = this.resolveSchemInputFile(args[4]);
        try {
            MCH_SchemImporter.ImportResult result = MCH_SchemImporter.importToAsset(
                schemFile,
                this.getStructureRootDir(),
                assetName,
                sender.getCommandSenderName()
            );
            sender.addChatMessage(new ChatComponentText(
                "Schem imported: " + assetName
                    + " size=" + result.meta.sizeX + "x" + result.meta.sizeY + "x" + result.meta.sizeZ
                    + " nonAir=" + result.nonAirBlocks
                    + " te=" + result.tileEntities
                    + " unknownPalette=" + result.unknownPaletteRefs
            ));
            MCH_Lib.Log("[mcheli][struct-importschem] name=%s file=%s size=%dx%dx%d nonAir=%d te=%d unknownPalette=%d",
                assetName, schemFile.getAbsolutePath(), result.meta.sizeX, result.meta.sizeY, result.meta.sizeZ,
                result.nonAirBlocks, result.tileEntities, result.unknownPaletteRefs);
        } catch (IOException e) {
            throw new CommandException("Failed to import schem: " + e.getMessage(), new Object[0]);
        }
    }

    private File getStructureRootDir() {
        return new File("config/mcheli/structures_runtime");
    }

    private File resolveSchemInputFile(String rawPath) {
        File f = new File(rawPath);
        if (!f.isAbsolute()) {
            File importDir = new File(this.getStructureRootDir(), "import");
            File fromImportDir = new File(importDir, rawPath);
            if (fromImportDir.exists()) {
                f = fromImportDir;
            }
        }
        return f;
    }

    private String sanitizeStructureName(String raw) {
        if (raw == null) {
            throw new WrongUsageException("Invalid structure name", new Object[0]);
        }
        String s = raw.trim();
        if (s.isEmpty() || !s.matches("[A-Za-z0-9_\\-]+")) {
            throw new WrongUsageException("Invalid structure name: use [A-Za-z0-9_-]", new Object[0]);
        }
        return s;
    }

    private int normalizeRotation(int rot) {
        int r = rot % 360;
        if (r < 0) {
            r += 360;
        }
        if (r == 0 || r == 90 || r == 180 || r == 270) {
            return r;
        }
        return -1;
    }

    private int[] rotateXZ(int x, int z, int sizeX, int sizeZ, int rot) {
        if (rot == 90) {
            return new int[]{sizeZ - 1 - z, x};
        }
        if (rot == 180) {
            return new int[]{sizeX - 1 - x, sizeZ - 1 - z};
        }
        if (rot == 270) {
            return new int[]{z, sizeX - 1 - x};
        }
        return new int[]{x, z};
    }

    private String extractExpectedBlockInfoName(MCH_StructureBlob.BlockEntry e, Block block) {
        if (e.tileEntity != null && e.tileEntity.hasKey("BlockInfoName")) {
            String fromTag = e.tileEntity.getString("BlockInfoName");
            if (fromTag != null && !fromTag.trim().isEmpty()) {
                return fromTag.trim();
            }
        }
        if (block instanceof MCH_ConfigSpawnerBlock) {
            String fallback = ((MCH_ConfigSpawnerBlock) block).getDefaultBlockInfoName();
            if (fallback != null && !fallback.trim().isEmpty()) {
                return fallback.trim();
            }
        }
        return "";
    }

    private int repairPlacedStructureBlockInfo(World world, Map<Long, String> expectedMap) {
        if (expectedMap == null || expectedMap.isEmpty()) {
            return 0;
        }
        int repaired = 0;
        for (Map.Entry<Long, String> e : expectedMap.entrySet()) {
            long key = e.getKey();
            String expected = e.getValue();
            int x = this.unpackX(key);
            int y = this.unpackY(key);
            int z = this.unpackZ(key);
            TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof MCH_ConfigSpawnerTileEntity)) {
                continue;
            }
            MCH_ConfigSpawnerTileEntity tile = (MCH_ConfigSpawnerTileEntity) te;
            String cur = tile.getBlockInfoName();
            boolean mismatch = cur == null || cur.trim().isEmpty() || !cur.equalsIgnoreCase(expected);
            if (!mismatch && tile.getBlockInfo() != null) {
                continue;
            }
            tile.setBlockInfoName(expected);
            tile.markDirty();
            repaired++;
        }
        return repaired;
    }

    private void sanitizeSpawnerRuntimeNBT(NBTTagCompound nbt) {
        if (nbt == null) {
            return;
        }
        // Keep config identity (BlockInfoName), reset runtime state to avoid
        // loading stale cooldown/waiting flags from source world.
        nbt.removeTag("NextCheckTick");
        nbt.removeTag("CooldownEndTick");
        nbt.removeTag("SpawnedOnce");
        nbt.removeTag("VisualState");
        nbt.removeTag("WaitingVehicleDestroyed");
        nbt.removeTag("TrackedVehicleEntityId");
        nbt.removeTag("TrackedVehicleUuidMost");
        nbt.removeTag("TrackedVehicleUuidLeast");

        nbt.setLong("NextCheckTick", 0L);
        nbt.setLong("CooldownEndTick", 0L);
        nbt.setBoolean("SpawnedOnce", false);
        nbt.setInteger("VisualState", MCH_ConfigSpawnerTileEntity.STATE_ACTIVE);
        nbt.setBoolean("WaitingVehicleDestroyed", false);
        nbt.setInteger("TrackedVehicleEntityId", -1);
        nbt.setLong("TrackedVehicleUuidMost", 0L);
        nbt.setLong("TrackedVehicleUuidLeast", 0L);
    }

    private long packBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    private int unpackX(long packed) {
        int x = (int) (packed >> 38);
        if (x >= 0x2000000) {
            x -= 0x4000000;
        }
        return x;
    }

    private int unpackY(long packed) {
        return (int) (packed & 0xFFFL);
    }

    private int unpackZ(long packed) {
        int z = (int) ((packed >> 12) & 0x3FFFFFFL);
        if (z >= 0x2000000) {
            z -= 0x4000000;
        }
        return z;
    }

    private List<String> getStructureNames() {
        ArrayList<String> names = new ArrayList<String>();
        File metaDir = new File(this.getStructureRootDir(), MCH_StructureIO.DIR_META);
        File[] files = metaDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase(Locale.ROOT).endsWith(MCH_StructureIO.EXT_META);
            }
        });
        if (files == null) {
            return names;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (File f : files) {
            String n = f.getName();
            if (n.endsWith(MCH_StructureIO.EXT_META)) {
                n = n.substring(0, n.length() - MCH_StructureIO.EXT_META.length());
            }
            names.add(n);
        }
        return names;
    }

    private List<String> getSchemImportCandidates() {
        ArrayList<String> names = new ArrayList<String>();
        File importDir = new File(this.getStructureRootDir(), "import");
        File[] files = importDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name == null) {
                    return false;
                }
                String n = name.toLowerCase(Locale.ROOT);
                return n.endsWith(".schem") || n.endsWith(".schematic");
            }
        });
        if (files == null) {
            return names;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (File f : files) {
            names.add(f.getName());
        }
        return names;
    }

    private static int floorMod(int a, int b) {
        int r = a % b;
        return r < 0 ? r + b : r;
    }

    private static boolean checkSlope(World world, int centerX, int centerZ, int slopeMax) {
        if (slopeMax <= 0 || world == null) {
            return true;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                int y = world.getTopSolidOrLiquidBlock(centerX + dx * 4, centerZ + dz * 4);
                if (y < min) {
                    min = y;
                }
                if (y > max) {
                    max = y;
                }
            }
        }
        return max - min <= slopeMax;
    }

}
