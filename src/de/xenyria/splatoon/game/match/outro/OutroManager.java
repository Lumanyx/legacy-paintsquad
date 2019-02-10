package de.xenyria.splatoon.game.match.outro;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.mojang.authlib.GameProfile;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import de.xenyria.splatoon.game.equipment.gear.boots.FootGear;
import de.xenyria.splatoon.game.equipment.gear.chest.BodyGear;
import de.xenyria.splatoon.game.equipment.gear.head.HeadGear;
import de.xenyria.splatoon.game.equipment.gear.level.GearLevel;
import de.xenyria.splatoon.game.equipment.weapon.util.ProgressBarUtil;
import de.xenyria.splatoon.game.map.Map;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.userdata.level.Level;
import de.xenyria.splatoon.game.player.userdata.level.LevelTree;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.ArmorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class OutroManager {

    private Location centeredOverviewPosition;

    private ArrayList<Block> bar = new ArrayList<>();

    private Team goodGuys,badGuys;
    private float percentageTeam1,percentageTeam2;

    private boolean preDeterminePhase = true;

    private Location cameraLocation;
    public Location getCameraLocation() { return cameraLocation; }

    public static final float DETERMINE_VALUE = 25f;
    private int idleTicks = 23;
    private int determineTicks = 5;
    private int resultLookTicker = 20;
    private int ticksToRewards = 75;

    public void setBlocks() {

        int totalSize = 24;
        float ratio1 = (float)Math.floor(percentageTeam1) / 100f;
        float ratio2 = (float)Math.floor(percentageTeam2) / 100f;
        int blocksForTeam1 = (int) ((float)totalSize * ratio1);
        int blocksForTeam2 = (int) ((float)totalSize * ratio2);

        boolean full = (Math.ceil(percentageTeam1) + Math.ceil(percentageTeam2) >= 99D);
        if(true) {

            for(int x = 0; x < (blocksForTeam1 * 2); x++) {

                Block block = bar.get(x);
                block.setType(goodGuys.getColor().getWool());

            }

            int minIndex = bar.size() - (blocksForTeam2*2);

            for(int x = minIndex; x < (bar.size()); x++) {

                Block block = bar.get(x);
                block.setType(badGuys.getColor().getWool());

            }

        }

    }

    private int soundTicker;
    private float pitch = 0.1f;
    private float rewardYaw() { return yaw+90f; }

    private Match match;
    private boolean resultState = true;
    private int absoluteTicker = 0;
    private Location rewardLocation;
    private Vector rewardStraight;
    private Vector rewardNormal;

    public class ResultScreen {

        public EntityArmorStand xpBar, remainingXP, totalCoinCounter,
                helmetProgress, helmetEffects,
                chestplateProgress, chestplateEffects,
                bootsProgress, bootsEffects;
        public int headGearIncr, bodyGearIncr, footGearIncr;
        public boolean headFinishFlag, bodyFinishFlag, footFinishFlag;
        private SplatoonHumanPlayer player;
        private ArrayList<EntityPlayer> npcs = new ArrayList<>();
        private ArrayList<EntityArmorStand> stands = new ArrayList<>();
        private int levelID = 0;
        private boolean insertedToTable = false;
        private boolean setSubAbilities = false;
        private int gearRandomEffectTicker = 0;
        private int randomIterationTicker = 0;
        private int ticksToEffectTicker = 20;
        private HashMap<GearType, ArrayList<Integer>> randomIndexes = new HashMap<>();

        public String abilities(Gear gear) {

            String str = gear.getGearData().getPrimaryEffect().getShortName() + " §8- ";
            for(int x = 0; x < gear.getMaxSubAbilities(); x++) {

                boolean isRandom = false;
                GearType type = gear.getType();

                System.out.println("Check type: " + type + " | RandomIndexes: " + randomIndexes.size());
                if(randomIndexes.containsKey(type)) {

                    for(int yx : randomIndexes.get(type)) {

                        if(yx == x) {

                            isRandom = true;

                        }

                    }

                }

                if(!isRandom) {

                    if (x <= (gear.getGearData().getSubAbilities().size() - 1) && !gear.getGearData().getSubAbilities().isEmpty()) {

                        SpecialEffect effect = gear.getGearData().getSubAbilities().get(x);
                        str += effect.getShortName() + " ";

                    } else {

                        str += "§f§l?§r ";

                    }

                } else {

                    str += "§" + (new Random().nextInt(9)) + "§l?§r ";

                }

            }

            return str.substring(0, str.length()-1);

        }

        public String progress(Gear gear) {

            if(!gear.getGearData().isFullyLevelled()) {

                int additionalXP = 0;
                if(rewardMap.containsKey(player)) {

                    additionalXP = rewardMap.get(player).gearExperience;

                }

                String progress = ProgressBarUtil.generateProgressBar(gear.getGearData().levelPercentage(), 8, player.getTeam().getColor().prefix(), "§8");
                progress+=" §a(+ " + additionalXP + " EP) Stufe " + gear.getGearData().currentLevel() + "/" + gear.getMaxSubAbilities();
                return progress;

            } else {

                return "§a" + Characters.OKAY + " Max.-Stufe erreicht!";

            }

        }

        public void updateGear() {

            helmetEffects.getBukkitEntity().setCustomName(abilities(player.getEquipment().getHeadGear()));
            chestplateEffects.getBukkitEntity().setCustomName(abilities(player.getEquipment().getBodyGear()));
            bootsEffects.getBukkitEntity().setCustomName(abilities(player.getEquipment().getFootGear()));
            helmetProgress.getBukkitEntity().setCustomName(progress(player.getEquipment().getHeadGear()));
            chestplateProgress.getBukkitEntity().setCustomName(progress(player.getEquipment().getBodyGear()));
            bootsProgress.getBukkitEntity().setCustomName(progress(player.getEquipment().getFootGear()));

            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(helmetEffects.getId(), helmetEffects.getDataWatcher(), false));
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(chestplateEffects.getId(), chestplateEffects.getDataWatcher(), false));
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(bootsEffects.getId(), bootsEffects.getDataWatcher(), false));

            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(helmetProgress.getId(), helmetProgress.getDataWatcher(), false));
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(chestplateProgress.getId(), chestplateProgress.getDataWatcher(), false));
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(bootsProgress.getId(), bootsProgress.getDataWatcher(), false));

        }

        private int headGearLevelID,bodyGearLevelID,footGearLevelID;
        private boolean headFinished,bodyFinished,footFinished;
        public ResultScreen(SplatoonHumanPlayer player) {

            this.player = player;
            levelID = player.getUserData().currentLevel();
            headGearLevelID = player.getEquipment().getHeadGear().getGearData().currentLevel();
            bodyGearLevelID = player.getEquipment().getBodyGear().getGearData().currentLevel();
            footGearLevelID = player.getEquipment().getFootGear().getGearData().currentLevel();
            headFinished = player.getEquipment().getHeadGear().getGearData().isFullyLevelled();
            bodyFinished = player.getEquipment().getBodyGear().getGearData().isFullyLevelled();
            footFinished = player.getEquipment().getFootGear().getGearData().isFullyLevelled();

        }

        public EntityArmorStand addArmorStand(Vector position, String name) {

            return addArmorStand(position, name, 0f);

        }

        public EntityArmorStand addArmorStand(Vector position, String name, float yaw) {

            EntityArmorStand stand = new EntityArmorStand(match.nmsWorld(), position.getX(), position.getY(), position.getZ());
            stand.getBukkitEntity().setCustomName(name);
            stand.setCustomNameVisible(true);
            stand.setPositionRotation(position.getX(), position.getY(), position.getZ(), yaw, 0f);
            stand.setNoGravity(true);
            stand.setInvisible(true);
            stands.add(stand);

            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(stand));
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), false));
            return stand;

        }

        public void updateLevelDisplay() {

            boolean atEnd = player.getUserData().getExperience() >= XenyriaSplatoon.getLevelTree().maxExperience();

            Level target = player.getUserData().targetLevel();
            int current = player.getUserData().currentLevel();

            if(current != levelID) {

                player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§eStufe " + levelID + " §7erreicht!");
                player.getPlayer().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1f, 2f);
                levelID = current;

            }

            String text = "";
            if(!atEnd) {

                boolean finalLevel = target.getID() == player.getUserData().currentLevel();
                //if(target.getID() != player.getUserData().currentLevel()) {

                    String bar = player.getTeam().getColor().prefix();
                    String empty = "§7";
                    for (int x = 0; x < 14; x++) {

                        double currentPercentage = ((float) x / 14f) * 100f;
                        double realPercentage = player.getUserData().currentLevelPercentage();
                        if(currentPercentage <= realPercentage) {

                            bar+="█";

                        } else {

                            empty+="█";

                        }

                    }

                    if(finalLevel) {

                        text = "§eLv. " + current + " " + bar + empty + "";

                    } else {

                        text = "§eLv. " + current + " " + bar + empty + " §eLv. " + target.getID() + "";

                    }

                //}

            } else {

                text = "§eMax. Stufe (Lv. " + XenyriaSplatoon.getLevelTree().lastLevel().getID() + ") erreicht!";

            }
            xpBar.getBukkitEntity().setCustomName(text);

            int remainingXP1 = 0;
            if(rewardMap.containsKey(player)) {

                remainingXP1 = rewardMap.get(player).experience;

            }

            remainingXP.getBukkitEntity().setCustomName("§a§l+ " + remainingXP1 + " EP");

            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(xpBar.getId(), xpBar.getDataWatcher(), false));
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(remainingXP.getId(), remainingXP.getDataWatcher(), false));

        }

        public void updateCoinDisplay() {

            Reward reward = rewardMap.getOrDefault(player, null);
            if(reward != null) {

                totalCoinCounter.getBukkitEntity().setCustomName("§7" + player.getUserData().getCoins() + " §a(+ " + reward.coins + ")");

            } else {

                totalCoinCounter.getBukkitEntity().setCustomName("§7" + player.getUserData().getCoins());

            }
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(totalCoinCounter.getId(), totalCoinCounter.getDataWatcher(), false));

        }

    }

    private static ArrayList<Map.TeamSpawn.PositionWithMaterial> resultStand = new ArrayList<>();
    static {

        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(0,0,0), Material.SEA_LANTERN));
        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(1,0,0), Material.SMOOTH_STONE));
        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(-1,0,0), Material.SMOOTH_STONE));
        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(0,0,1), Material.SMOOTH_STONE));
        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(0,0,-1), Material.SMOOTH_STONE));
        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(0,-1,0), Material.COBBLESTONE_WALL));
        resultStand.add(new Map.TeamSpawn.PositionWithMaterial(new Vector(0,-2,0), Material.OAK_FENCE));

    }

    private ArrayList<Block> placedBlocks = new ArrayList<>();
    public void createRewardLocation() {

        Location location = cameraLocation.clone();
        location.setPitch(0f);
        location.setYaw(rewardYaw());

        Vector vector = location.getDirection();
        Location target = location.toVector().add(vector.clone().multiply(9)).toLocation(world);

        Location locOther = location.clone();
        locOther.setYaw(locOther.getYaw() + 90f);
        rewardLocation = target.clone();
        rewardStraight = vector;
        rewardNormal = locOther.getDirection();
        target = target.add(rewardNormal.clone().multiply(-2));
        rewardLocation = target;

    }
    public void initRewards() {

        for(Block block : placedBlocks) {

            block.setType(Material.AIR);

        }
        placedBlocks.clear();


        Vector vector1 = rewardLocation.toVector();
        for(Map.TeamSpawn.PositionWithMaterial material : resultStand) {

            Block block = world.getBlockAt((int)vector1.getX() + (int)material.relativePosition.getX(),
                    (int)vector1.getY() + (int)material.relativePosition.getY(),
                    (int)vector1.getZ() + (int)material.relativePosition.getZ());
            block.setType(material.material);

        }

    }

    public void destroyResults() {

        for(java.util.Map.Entry<SplatoonHumanPlayer, ResultScreen> entry : resultScreens.entrySet()) {

            for(EntityPlayer player : entry.getValue().npcs) {

                entry.getKey().getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getId()));
                entry.getKey().getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player));

            }
            for(EntityArmorStand stand : entry.getValue().stands) {

                entry.getKey().getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(stand.getId()));

            }

        }

    }

    private Location fireworkOrientationLocation = null;
    private Vector fireworkNormal = null;
    private boolean firedFireworks = false;

    private HashMap<SplatoonHumanPlayer, ResultScreen> resultScreens = new HashMap<>();
    public void initResults() {

        Location location = cameraLocation.clone();
        location.setPitch(0f);

        Vector vector = location.getDirection();
        Location target = location.toVector().add(vector.clone().multiply(9)).toLocation(world);

        Location offset = location.clone();
        offset.setYaw(offset.getYaw() - 90f);
        Vector offsetDir = offset.getDirection();

        Vector[] spawnPositions = new Vector[4];

        spawnPositions[0] = target.toVector().add(offsetDir.clone().multiply(-7));
        spawnPositions[1] = target.toVector().add(offsetDir.clone().multiply(-3));
        spawnPositions[2] = target.toVector().add(offsetDir.clone().multiply(2));
        spawnPositions[3] = target.toVector().add(offsetDir.clone().multiply(6));
        fireworkNormal = offsetDir.clone();
        fireworkOrientationLocation = target.clone().add(vector.clone().multiply(4));

        for(Vector vector1 : spawnPositions) {

            for(Map.TeamSpawn.PositionWithMaterial material : resultStand) {

                Block block = world.getBlockAt((int)vector1.getX() + (int)material.relativePosition.getX(),
                        (int)vector1.getY() + (int)material.relativePosition.getY(),
                        (int)vector1.getZ() + (int)material.relativePosition.getZ());
                block.setType(material.material);
                placedBlocks.add(block);

            }

        }

        createRewardLocation();
        for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

            /*Location depth = rewardLocation.clone().add(rewardStraight.clone().multiply(38));
            Vector endDepth = depth.toVector().add(rewardNormal.clone().multiply(50));
            Vector startDepth = depth.toVector().add(rewardNormal.clone().multiply(-50));

            int minX = Math.min((int)startDepth.getX(), (int)endDepth.getX());
            int minZ = Math.min((int)startDepth.getZ(), (int)endDepth.getZ());
            int maxX = Math.max((int)startDepth.getX(), (int)endDepth.getX());
            int maxZ = Math.max((int)startDepth.getZ(), (int)endDepth.getZ());

            HashMap<ChunkCoordIntPair, ArrayList<MultiBlockChangeInfo>> coords = new HashMap<>();
            for(int x = minX; x <= maxX; x++) {

                for(int z = minZ; z <= maxZ; z++) {

                    for(int y = -7; y < 35; y++) {

                        ChunkCoordIntPair pair = new ChunkCoordIntPair(x>>4,z>>4);

                        if(!coords.containsKey(pair)) {

                            ArrayList<MultiBlockChangeInfo> infos = new ArrayList<>();
                            infos.add(new MultiBlockChangeInfo(new Location(world, x,depth.getY() + y,z),
                                    WrappedBlockData.createData(player.getTeam().getColor().getClay())));
                            coords.put(pair, infos);

                        } else {

                            coords.get(pair).add(new MultiBlockChangeInfo(new Location(world, x,depth.getY() + y,z),
                                    WrappedBlockData.createData(player.getTeam().getColor().getClay())));

                        }

                    }

                }

            }
            try {

                for(java.util.Map.Entry<ChunkCoordIntPair, ArrayList<MultiBlockChangeInfo>> entry : coords.entrySet()) {

                    int x = entry.getKey().x;
                    int z = entry.getKey().z;
                    PacketContainer container = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
                    container.getChunkCoordIntPairs().write(0, new com.comphenix.protocol.wrappers.ChunkCoordIntPair(x,z));
                    container.getMultiBlockChangeInfoArrays().write(0, entry.getValue().toArray(new MultiBlockChangeInfo[]{}));
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), container);

                }

            } catch (Exception e) {

                e.printStackTrace();

            }*/

            ArrayList<SplatoonPlayer> members = match.getPlayers(player.getTeam());
            ResultScreen screen = new ResultScreen(player);
            resultScreens.put(player, screen);

            int x = 0;
            for(Vector vector1 : spawnPositions) {

                SplatoonPlayer player1 = null;
                if(x <= (members.size() - 1)) {

                    player1 = members.get(x);
                    EntityPlayer npc = new EntityPlayer(match.nmsWorld().getMinecraftServer(), (WorldServer)match.nmsWorld(), player1.getGameProfile(), new PlayerInteractManager(match.nmsWorld()));
                    npc.yaw = yaw-180;
                    npc.setHeadRotation(yaw);
                    npc.locX = vector1.getX() + .5;
                    npc.locY = vector1.getY();
                    npc.locZ = vector1.getZ() + .5;
                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc));
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte)(npc.yaw * .703)));
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(npc.getId(), EnumItemSlot.MAINHAND,
                                CraftItemStack.asNMSCopy(player.getEquipment().getPrimaryWeapon().asItemStack())));

                    }, 2l);

                    screen.npcs.add(npc);
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 4, .5)), "§f§lWaffe");
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 3.75, .5)), "§e" + player1.getEquipment().getPrimaryWeapon().getName());
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 3.25, .5)), "§f§lKampfstatistik");
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 3, .5)), "§a§l" + player1.getStatistic().getSplats() + " Kill(s)");
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 2.75, .5)), "§e§l" + player1.getStatistic().getAssists() + " Assist(s)");
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 2.5, .5)), "§c§l" + player1.getStatistic().getDeaths() + " Tod(e)");
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 2.25, .5)), "§b§l0x Spezialwaffe eingesetzt");
                    screen.addArmorStand(vector1.clone().add(new Vector(.5, 2, .5)), "§b§l0 Punkte");

                }
                x++;

            }

        }

    }

    private float resultOffset = 90f;
    private boolean despawn = false;
    private boolean lookThreadCreated = false;
    private boolean lookDone = false;
    private boolean resultsInit = false;
    private boolean rewardsInit = false;
    private boolean rewardsState = false;

    private boolean zoomActive = false;
    public boolean isZoomActive() { return zoomActive; }

    public class Reward {

        private int experience;
        private int coins;
        private int gearExperience;

        public Reward(int experience, int coins, int gearExperience) {

            this.experience = experience;
            this.coins = coins;
            this.gearExperience = gearExperience;

        }


    }
    public Reward defaultReward() {

        return new Reward(XenyriaSplatoon.getLevelTree().maxExperience(),1720,1090);

    }

    private HashMap<SplatoonHumanPlayer, Reward> rewardMap = new HashMap<>();
    private int rewardGiveTicker = 20;
    private Team winningTeam = null;

    public void tick() {

        if(!despawn) {

            for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                for(SplatoonPlayer player1 : match.getAllPlayers()) {

                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player1.getEntityID()));

                }

            }

            despawn = true;

        }

        if(!rewardsState) {

            if (resultState) {

                if (determineTicks > 0) {

                    determineTicks--;
                    return;

                }

                if (preDeterminePhase) {

                    if (percentageTeam1 <= DETERMINE_VALUE) {

                        soundTicker++;
                        if (soundTicker > 4) {

                            soundTicker = 0;
                            pitch += 0.125f;

                            for (SplatoonHumanPlayer humanPlayer : match.getHumanPlayers()) {

                                humanPlayer.getPlayer().playSound(humanPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, pitch);

                            }

                        }

                        percentageTeam1 += 0.64f;
                        percentageTeam2 += 0.64f;

                        goodGuysPercentage.setCustomName("§f§l" + percentageTeam1 + " %");
                        badGuysPercentage.setCustomName("§f§l" + percentageTeam2 + " %");

                    } else {

                        preDeterminePhase = false;

                    }
                    setBlocks();

                } else {

                    if (idleTicks > 0) {

                        idleTicks--;

                        if (!resultsInit) {

                            initResults();
                            resultsInit = true;

                        }

                    } else {


                        for (SplatoonHumanPlayer player : match.getHumanPlayers()) {

                            player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                        }

                        percentageTeam1 = 65f;
                        percentageTeam2 = 35f;
                        goodGuysPercentage.setCustomName("§f§l" + percentageTeam1 + " %");
                        badGuysPercentage.setCustomName("§f§l" + percentageTeam2 + " %");

                        if(percentageTeam1 >= percentageTeam2) {

                            winningTeam = goodGuys;

                        } else if(percentageTeam2 >= percentageTeam1) {

                            winningTeam = badGuys;

                        } else {

                            if(new Random().nextBoolean()) {

                                winningTeam = goodGuys;

                            } else {

                                winningTeam = badGuys;

                            }

                        }
                        match.broadcast(winningTeam.getColor().prefix() + "Team " + winningTeam.getColor().getName() + " §7gewinnt!");

                        setBlocks();

                        for (Block block : bar) {

                            for (SplatoonHumanPlayer player : match.getHumanPlayers()) {

                                Player player1 = player.getPlayer();
                                player1.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(.5, 1.5, .5), 0, CraftBlockData.newData(block.getType(), ""));

                            }

                        }

                        resultState = false;

                    }

                }

            } else {

                if (resultLookTicker > 0) {

                    resultLookTicker--;
                    return;

                }

                if (!lookThreadCreated) {

                    lookThreadCreated = true;

                    ArrayList<SplatoonHumanPlayer> players = new ArrayList<>();
                    players.addAll(match.getHumanPlayers());

                    HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags = new HashSet<>();
                    for(PacketPlayOutPosition.EnumPlayerTeleportFlags flags1 : PacketPlayOutPosition.EnumPlayerTeleportFlags.values()) { flags.add(flags1); }

                    new Thread(() -> {

                        while (!lookDone) {

                            try { Thread.sleep(1000 / 60); } catch (Exception e) {}
                            if (resultOffset > 0f) {

                                float increment = -2.5f;
                                resultOffset += increment;
                                if (resultOffset <= 0f) {
                                    resultOffset = 0f;
                                    absoluteTicker = 6;
                                    lookDone = true;

                                    for (SplatoonHumanPlayer player : players) {

                                        try {

                                            Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                                                player.getPlayer().setGameMode(GameMode.ADVENTURE);
                                                player.getPlayer().setAllowFlight(true);
                                                player.getPlayer().setFlying(true);
                                                player.getPlayer().setFlySpeed(0f);

                                            });

                                        } catch (Exception e) {
                                        }

                                    }

                                }

                                absoluteTicker++;
                                if (absoluteTicker > 5) {

                                    absoluteTicker = 0;
                                    for (SplatoonHumanPlayer player : players) {

                                        try {
                                            player.getNMSPlayer().playerConnection.sendPacket(
                                                    new PacketPlayOutPosition(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), yaw, resultOffset, new HashSet<>(), 0)
                                            );
                                        } catch (Exception e) {
                                        }

                                    }

                                } else {

                                    for (SplatoonHumanPlayer player : match.getHumanPlayers()) {

                                        try {
                                            player.getNMSPlayer().playerConnection.sendPacket(
                                                    new PacketPlayOutPosition(0, 0, 0, 0, increment, flags, 0)
                                            );
                                        } catch (Exception e) {
                                        }

                                    }

                                }

                            }

                        }

                    }).start();

                }

                if (lookDone) {

                    if(!firedFireworks) {

                        ItemStack stack = new ItemStack(Material.FIREWORK_ROCKET);
                        FireworkMeta meta = (FireworkMeta) stack.getItemMeta();
                        meta.addEffect(FireworkEffect.builder().withColor(winningTeam.getColor().getBukkitColor()).flicker(true).trail(true).build());
                        stack.setItemMeta(meta);

                        Location target = fireworkOrientationLocation.clone().add(fireworkNormal.clone().multiply(new Random().nextInt(14) - 7));

                        EntityFireworks fireworks = new EntityFireworks(match.nmsWorld(), target.getX(), target.getY() - 12, target.getZ(), CraftItemStack.asNMSCopy(stack));
                        fireworks.locX = target.getX();
                        fireworks.locY = target.getY() - 12;
                        fireworks.locZ = target.getZ();
                        //fireworks.expectedLifespan = ticksToDetonation - 6;
                        this.fireworks.add(fireworks);

                        for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                            if(player.getTeam().equals(winningTeam)) {

                                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutSpawnEntity(fireworks, 76, 22));
                                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(fireworks.getId(), fireworks.getDataWatcher(), false));

                            }

                        }
                        if(this.fireworks.size() >= 10) {

                            firedFireworks = true;

                        }

                    } else {

                        if(ticksToDetonation > 0) {

                            ticksToDetonation--;

                        } else {

                            if(!fireworksRemoved) {

                                fireworksRemoved = true;
                                for (SplatoonHumanPlayer player : match.getHumanPlayers()) {

                                    if (player.getTeam().equals(winningTeam)) {

                                        for (EntityFireworks fireworks : fireworks) {

                                            //fireworks.dead = true;
                                            //player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(fireworks.getId(), fireworks.getDataWatcher(), false));

                                            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityStatus(fireworks, EntityEffect.FIREWORK_EXPLODE.getData()));
                                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(fireworks.getId()));

                                            }, 2l);

                                        }

                                    }

                                }

                            }

                        }

                    }

                    ticksToRewards--;
                    if (ticksToRewards < 1) {

                        rewardsState = true;

                    }

                }

            }

        } else {

            if(!rewardsInit) {

                initRewards();
                rewardsInit = true;

            }

            ArrayList<SplatoonHumanPlayer> players = new ArrayList<>();
            players.addAll(match.getHumanPlayers());
            HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags = new HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags>();
            for (PacketPlayOutPosition.EnumPlayerTeleportFlags flags1 : PacketPlayOutPosition.EnumPlayerTeleportFlags.values()) {

                flags.add(flags1);

            }

            if(rewardsHalfWaysDone) {

                if(!resultsRemoved) {

                    resultsRemoved = true;
                    zoomActive = true;
                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutAbilities(
                                player.getNMSPlayer().abilities
                        ));

                    }

                    destroyResults();

                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                        player.getPlayer().setGameMode(GameMode.ADVENTURE);
                        ResultScreen screen = resultScreens.get(player);
                        EntityPlayer player1 = new EntityPlayer(match.nmsWorld().getMinecraftServer(),
                                (WorldServer)match.nmsWorld(), player.getGameProfile(), new PlayerInteractManager(
                                match.nmsWorld()
                        ));
                        player1.locX = Math.floor(rewardLocation.getX()) + .5;
                        player1.locY = Math.ceil(rewardLocation.getY());
                        player1.locZ = Math.floor(rewardLocation.getZ()) + .5;
                        player1.yaw = rewardLocation.getYaw() - 90f;
                        player1.pitch = rewardLocation.getPitch();
                        screen.npcs.add(player1);
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, player1));
                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(player1));
                            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(player1, (byte)((rewardLocation.getYaw()-90f) * 0.711)));
                            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutAnimation(player1, 0));

                        }, 2l);

                        screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 2.5, 0)).add(rewardNormal.clone().multiply(3)), "§a§lStufe");
                        screen.xpBar = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 2, 0)).add(rewardNormal.clone().multiply(3)), "");
                        screen.remainingXP = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 2.25, 0)).add(rewardNormal.clone().multiply(3)), "");
                        screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 1.5, 0)).add(rewardNormal.clone().multiply(3)), "§e§lMünzen");
                        screen.totalCoinCounter = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 1.25, 0)).add(rewardNormal.clone().multiply(3)), "");
                        screen.updateCoinDisplay();
                        screen.updateLevelDisplay();

                        screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 0.75, 0)).add(rewardNormal.clone().multiply(3)), "§e§lAusrüstung");

                        Location rewardLocAlt = rewardLocation.clone();
                        rewardLocAlt.setYaw(rewardLocation.getYaw() - 90f);

                        EntityArmorStand helmet = screen.addArmorStand(rewardLocAlt.toVector().add(new Vector(0, 0.75, 0)).add(rewardNormal.clone().multiply(2)), "§0", rewardLocAlt.getYaw());
                        helmet.yaw = rewardLocAlt.getYaw();
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(helmet.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(player.getEquipment().getHeadGear().asItemStack(player.getColor()))));

                        EntityArmorStand chest = screen.addArmorStand(rewardLocAlt.toVector().add(new Vector(0, 0.325, 0)).add(rewardNormal.clone().multiply(2)), "§0", rewardLocAlt.getYaw());
                        chest.yaw = rewardLocAlt.getYaw();
                        chest.aS = rewardLocAlt.getYaw();
                        chest.bodyPose = new Vector3f(0, rewardLocAlt.getYaw(), 0);
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(chest.getId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(player.getEquipment().getBodyGear().asItemStack(player.getColor()))));
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(chest.getId(), chest.getDataWatcher(), false));
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(chest));

                        EntityArmorStand boots = screen.addArmorStand(rewardLocAlt.toVector().add(new Vector(0, 0.25, 0)).add(rewardNormal.clone().multiply(2)), "§0", rewardLocAlt.getYaw());
                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(boots.getId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(player.getEquipment().getFootGear().asItemStack(player.getColor()))));

                        screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 0.25, 0)).add(rewardNormal.clone().multiply(4.5)), "§e" + player.getEquipment().getHeadGear().getBrand().getIcon() + " §7" + player.getEquipment().getHeadGear().getName());

                        screen.helmetEffects = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, 0, 0)).add(rewardNormal.clone().multiply(4.5)), "");
                        screen.helmetProgress = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -0.25, 0)).add(rewardNormal.clone().multiply(4.5)), "");
                        screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -0.625, 0)).add(rewardNormal.clone().multiply(4.5)), "§e" + player.getEquipment().getBodyGear().getBrand().getIcon() + " §7" + player.getEquipment().getBodyGear().getName());
                        screen.chestplateEffects = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -0.875, 0)).add(rewardNormal.clone().multiply(4.5)), "");
                        screen.chestplateProgress = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -1.125, 0)).add(rewardNormal.clone().multiply(4.5)), "");
                        screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -1.5, 0)).add(rewardNormal.clone().multiply(4.5)), "§e" + player.getEquipment().getFootGear().getBrand().getIcon() + " §7" + player.getEquipment().getFootGear().getName());
                        screen.bootsEffects = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -1.75, 0)).add(rewardNormal.clone().multiply(4.5)), "");
                        screen.bootsProgress = screen.addArmorStand(rewardLocation.toVector().add(new Vector(0, -2, 0)).add(rewardNormal.clone().multiply(4.5)), "");
                        screen.updateGear();

                    }

                }

            }

            if(!createdTurnThread) {

                createdTurnThread = true;

                new Thread(() -> {

                    while (!rewardsLookComplete) {

                        try {
                            Thread.sleep(1000 / 60);
                        } catch (Exception e) {
                        }
                        float increment = 1.75f;
                        rewardYaw = rewardYaw + increment;
                        boolean forceAbsolute = false;
                        if (absoluteTicker > 5) {

                            forceAbsolute = true;
                            absoluteTicker = 0;

                        }

                        if (rewardYaw >= 45f) {

                            rewardsHalfWaysDone = true;

                        }

                        if (rewardYaw >= rewardYaw()) {

                            rewardYaw = rewardYaw();
                            forceAbsolute = true;

                        }

                        for (SplatoonHumanPlayer player : players) {

                            try {

                                /*
                                HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags1 = new HashSet<>();
                                flags1.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X);
                                flags1.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y);
                                flags1.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z);
                                flags1.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT);
                                flags1.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT);*/

                                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPosition(
                                        cameraLocation.getX(), cameraLocation.getY(), cameraLocation.getZ(), rewardYaw, 0f, new HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags>(), 0));


                            } catch (Exception e) {}

                        }

                    }

                }).start();

            }

            if(rewardGiveTicker > 0) {

                rewardGiveTicker--;

            } else {

                for(SplatoonHumanPlayer player : players) {

                    Reward reward = rewardMap.getOrDefault(player, defaultReward());
                    if(!rewardMap.containsKey(player)) {

                        rewardMap.put(player, reward);

                    }

                    ResultScreen screen = resultScreens.getOrDefault(player, null);
                    int decrement = 32;
                    if(screen != null) {

                        if(reward.coins >= decrement) {

                            reward.coins -= decrement;
                            player.getUserData().updateCoins(player.getUserData().getCoins() + decrement);
                            screen.updateCoinDisplay();

                        } else {

                            player.getUserData().updateCoins(player.getUserData().getCoins() + reward.coins);
                            reward.coins = 0;
                            screen.updateCoinDisplay();

                        }

                        if(reward.experience >= decrement) {

                            reward.experience -= decrement;
                            player.getUserData().updateExperience(player.getUserData().getExperience() + decrement);
                            screen.updateLevelDisplay();

                        } else {

                            player.getUserData().updateExperience(player.getUserData().getExperience() + reward.experience);
                            reward.experience = 0;
                            screen.updateLevelDisplay();

                        }

                        int realIncrement = decrement;
                        if(reward.gearExperience < decrement) {

                            realIncrement = reward.gearExperience;

                        }

                        System.out.println("Reward: " + reward.gearExperience);
                        if(reward.gearExperience != 0) {

                            reward.gearExperience -= realIncrement;
                            HeadGear headGear = player.getEquipment().getHeadGear();
                            BodyGear bodyGear = player.getEquipment().getBodyGear();
                            FootGear footGear = player.getEquipment().getFootGear();
                            int headLevel = headGear.getGearData().currentLevel();
                            int bodyLevel = bodyGear.getGearData().currentLevel();
                            int footLevel = footGear.getGearData().currentLevel();

                            boolean finishedHeadBefore = headGear.getGearData().isFullyLevelled();
                            boolean finishedBodyBefore = bodyGear.getGearData().isFullyLevelled();
                            boolean finishedFootBefore = footGear.getGearData().isFullyLevelled();
                            headGear.getGearData().setExperience(headGear.getGearData().getExperience() + realIncrement);
                            bodyGear.getGearData().setExperience(bodyGear.getGearData().getExperience() + realIncrement);
                            footGear.getGearData().setExperience(footGear.getGearData().getExperience() + realIncrement);
                            boolean finishedHeadAfter = headGear.getGearData().isFullyLevelled();
                            boolean finishedBodyAfter = bodyGear.getGearData().isFullyLevelled();
                            boolean finishedFootAfter = footGear.getGearData().isFullyLevelled();

                            if (headLevel != screen.headGearLevelID
                                    || (!finishedHeadBefore && finishedHeadAfter)) {

                                screen.headGearIncr++;
                                screen.headGearLevelID = headLevel;

                            }

                            if (bodyLevel != screen.bodyGearLevelID
                                    || (!finishedBodyBefore && finishedBodyAfter)) {

                                screen.bodyGearIncr++;
                                screen.bodyGearLevelID = bodyLevel;

                            }

                            if (footLevel != screen.footGearLevelID
                                    || (!finishedFootBefore && finishedFootAfter)) {

                                screen.footGearIncr++;
                                screen.footGearLevelID = footLevel;


                            }

                            screen.updateGear();

                        } else {

                            if(screen.footGearIncr != 0 || screen.bodyGearIncr != 0 || screen.headGearIncr != 0) {

                                if(!screen.insertedToTable) {

                                    screen.insertedToTable = true;
                                    HashMap<GearType, ArrayList<Integer>> ints = new HashMap<>();
                                    if(screen.headGearIncr != 0) {

                                        ArrayList<Integer> i = new ArrayList<>();
                                        for(int y = 0; y < screen.headGearIncr; y++) {

                                            i.add((player.getEquipment().getHeadGear().getGearData().firstSubIndex()) + y);

                                        }
                                        System.out.println("helmet -> " + i.size());
                                        ints.put(GearType.HELMET, i);

                                    }
                                    if(screen.bodyGearIncr != 0) {

                                        ArrayList<Integer> i = new ArrayList<>();
                                        for(int y = 0; y < screen.bodyGearIncr; y++) {

                                            i.add((player.getEquipment().getBodyGear().getGearData().firstSubIndex()) + y);

                                        }
                                        System.out.println("chest -> " + i.size());
                                        ints.put(GearType.CHESTPLATE, i);

                                    }
                                    if(screen.footGearIncr != 0) {

                                        ArrayList<Integer> i = new ArrayList<>();
                                        for(int y = 0; y < screen.footGearIncr; y++) {

                                            i.add((player.getEquipment().getFootGear().getGearData().firstSubIndex()) + y);

                                        }
                                        System.out.println("boots -> " + i.size());
                                        ints.put(GearType.BOOTS, i);

                                    }
                                    screen.randomIndexes = ints;

                                }

                                if(screen.ticksToEffectTicker > 0) {

                                    screen.ticksToEffectTicker--;

                                } else {

                                    if(screen.randomIterationTicker < 10) {

                                        screen.gearRandomEffectTicker++;

                                        if (screen.gearRandomEffectTicker > 3) {

                                            screen.gearRandomEffectTicker = 0;
                                            screen.randomIterationTicker++;
                                            player.getPlayer().playSound(player.getLocation(),
                                                    Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
                                            screen.updateGear();

                                        }

                                    } else {

                                        if(!screen.setSubAbilities) {

                                            screen.setSubAbilities = true;
                                            for(int y = 0; y < screen.headGearIncr; y++) {

                                                player.getPlayer().playSound(player.getLocation(),
                                                        Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                                                player.getEquipment().getHeadGear().getGearData().addSubEffect(randomEffect());

                                            }
                                            for(int y = 0; y < screen.bodyGearIncr; y++) {

                                                player.getPlayer().playSound(player.getLocation(),
                                                        Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                                                player.getEquipment().getBodyGear().getGearData().addSubEffect(randomEffect());

                                            }
                                            for(int y = 0; y < screen.footGearIncr; y++) {

                                                player.getPlayer().playSound(player.getLocation(),
                                                        Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                                                player.getEquipment().getFootGear().getGearData().addSubEffect(randomEffect());

                                            }

                                            screen.footGearIncr = 0;
                                            screen.bodyGearIncr = 0;
                                            screen.headGearIncr = 0;
                                            screen.randomIndexes.clear();
                                            screen.updateGear();

                                        }

                                    }

                                }

                            }

                        }

                        /*} else {



                        }*/

                    }

                }

            }

        }

    }

    private boolean fireworksRemoved = false;
    private ArrayList<EntityFireworks> fireworks = new ArrayList<>();
    private int ticksToDetonation = 20;

    public SpecialEffect randomEffect() {

        SpecialEffect effect = null;
        while (effect == null) {

            effect = SpecialEffect.values()[new Random().nextInt(SpecialEffect.values().length - 1)];
            if(!effect.isSub()) {

                effect = null;

            }

        }
        return effect;

    }

    private HashMap<GearType, ArrayList<Integer>> newAbilities = new HashMap<>();


    private boolean rewardsHalfWaysDone = false;
    private boolean createdTurnThread = false;
    private boolean resultsRemoved = false;
    private boolean rewardsLookComplete = false;
    private World world;
    private float yaw = 0f;
    private float rewardYaw = 0f;

    public OutroManager(Match match) {

        this.match = match;
        this.world = match.getWorld();
        Vector spawnCenter = new Vector();
        int spawnCount = 0;
        goodGuys = match.getRegisteredTeams().get(0);
        badGuys = match.getRegisteredTeams().get(1);

        for(Map.TeamSpawn spawn : match.getMap().getSpawns()) {

            spawnCenter = spawnCenter.add(spawn.getPosition().toVector());
            spawnCount++;

        }
        spawnCenter.setY(0);
        spawnCenter = spawnCenter.divide(new Vector(spawnCount, 0, spawnCount));

        Map.TeamSpawn pairSpawn1 = null,pairSpawn2 = null;
        double highestDistance = 0d;

        for(Map.TeamSpawn spawn : match.getMap().getSpawns()) {

            for (Map.TeamSpawn otherSpawn : match.getMap().getSpawns()) {

                if(otherSpawn != spawn) {

                    Vector spawnLoc1 = spawn.getPosition().toVector();
                    Vector spawnLoc2 = otherSpawn.getPosition().toVector();
                    spawnLoc1.setY(0);
                    spawnLoc2.setY(0);

                    double distance = spawnLoc1.distance(spawnLoc2);
                    if (pairSpawn1 == null || distance > highestDistance) {

                        pairSpawn1 = spawn;
                        pairSpawn2 = otherSpawn;
                        highestDistance = distance;

                    }

                }

            }

        }

        double cameraY = highestDistance * 1.3;
        if(cameraY > 256) {

            cameraY = 256;

        }

        cameraLocation = new Location(match.getWorld(), spawnCenter.getX(), cameraY, spawnCenter.getZ(), yaw, 90f);

        for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

            //player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, camera));

            Player player1 = player.getPlayer();
            player1.setGameMode(GameMode.ADVENTURE);
            player1.setAllowFlight(true);
            player1.setFlying(true);
            player1.setFlySpeed(0f);
            player1.getInventory().clear();
            player1.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 2, false, false, false));
            player1.teleport(cameraLocation);

        }

        Vector direction = cameraLocation.getDirection();
        Vector bottom = cameraLocation.toVector().add(direction.clone().multiply(10));

        Location dir1 = cameraLocation.clone();
        dir1.setPitch(0f);
        dir1.setYaw(dir1.getYaw() - 90f);
        Vector normal = dir1.getDirection();

        Location dir2 = cameraLocation.clone();
        dir2.setPitch(0f);
        dir2.setYaw(dir2.getYaw() - 180f);
        Vector newDir = dir2.getDirection();
        bottom = bottom.add(newDir.clone().multiply(4));

        int minX = Math.min((int)bottom.getX() - (int)(normal.getX() * 12), (int)bottom.getX() + (int)(normal.getX() * 12));
        int minZ = Math.min((int)bottom.getZ() - (int)(normal.getZ() * 12), (int)bottom.getZ() + (int)(normal.getZ() * 12));
        int maxX = Math.max((int)bottom.getX() - (int)(normal.getX() * 12), (int)bottom.getX() + (int)(normal.getX() * 12));
        int maxZ = Math.max((int)bottom.getZ() - (int)(normal.getZ() * 12), (int)bottom.getZ() + (int)(normal.getZ() * 12));

        World world = match.getWorld();
        for(int x = minX; x <= maxX; x++) {

            for(int z = minZ; z <= maxZ; z++) {

                for(int depth = 0; depth < 4; depth++) {

                    int offsetX = (int)(newDir.getX() * depth);
                    int offsetZ = (int)(newDir.getZ() * depth);

                    boolean inBar = ((x+offsetX) != minX && (x+offsetX) != maxX && (z+offsetZ) != minZ && (z+offsetZ) != maxZ && depth >= 1 && depth <= 2);
                    if(!inBar) {

                        world.getBlockAt(x + offsetX, bottom.getBlockY(), z + offsetZ).setType(Material.BLACK_TERRACOTTA);
                        world.getBlockAt(x + offsetX, bottom.getBlockY() + 1, z + offsetZ).setType(Material.BLACK_TERRACOTTA);

                    } else {

                        world.getBlockAt(x + offsetX, bottom.getBlockY(), z + offsetZ).setType(Material.BLACK_TERRACOTTA);
                        world.getBlockAt(x + offsetX, bottom.getBlockY() + 1, z + offsetZ).setType(Material.GRAY_TERRACOTTA);
                        bar.add(world.getBlockAt(x + offsetX, bottom.getBlockY() + 1, z + offsetZ));

                    }

                }


            }

        }

        Location goodGuysLocation = bar.get(0).getLocation().add(normal.clone().multiply(3)).add(newDir.clone().multiply(-2.25));
        Location badGuysLocation = bar.get(bar.size() - 2).getLocation().add(normal.clone().multiply(-2)).add(newDir.clone().multiply(-2.25));

        goodGuysPercentage = (ArmorStand) goodGuysLocation.getWorld().spawnEntity(goodGuysLocation.clone().add(newDir.clone().multiply(.25)), EntityType.ARMOR_STAND);
        goodGuysPercentage.setVisible(false); goodGuysPercentage.setCustomNameVisible(true); goodGuysPercentage.setCanMove(false); goodGuysPercentage.setCanTick(false); goodGuysPercentage.setCustomName("§f§l0%");
        badGuysPercentage = (ArmorStand) goodGuysLocation.getWorld().spawnEntity(badGuysLocation.clone().add(newDir.clone().multiply(.25)), EntityType.ARMOR_STAND);
        badGuysPercentage.setVisible(false); badGuysPercentage.setCustomNameVisible(true); badGuysPercentage.setCanMove(false); badGuysPercentage.setCanTick(false); badGuysPercentage.setCustomName("§f§l0%");

        goodGuysTitle = (ArmorStand) goodGuysLocation.getWorld().spawnEntity(goodGuysLocation, EntityType.ARMOR_STAND);
        goodGuysTitle.setCanTick(false);
        goodGuysTitle.setVisible(false);
        goodGuysTitle.setCanMove(false);
        goodGuysTitle.setCustomNameVisible(true);
        goodGuysTitle.setCustomName(goodGuys.getColor().prefix() + "§lTeam " + goodGuys.getColor().getName());

        badGuysTitle = (ArmorStand) badGuysLocation.getWorld().spawnEntity(badGuysLocation, EntityType.ARMOR_STAND);
        badGuysTitle.setCanTick(false);
        badGuysTitle.setVisible(false);
        badGuysTitle.setCustomNameVisible(true);
        badGuysTitle.setCanMove(false);
        badGuysTitle.setCustomName(badGuys.getColor().prefix() + "§lTeam " + badGuys.getColor().getName());

    }

    private ArmorStand goodGuysTitle, badGuysTitle, goodGuysPercentage, badGuysPercentage;

}
