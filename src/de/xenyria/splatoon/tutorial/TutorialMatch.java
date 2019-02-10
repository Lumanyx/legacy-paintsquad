package de.xenyria.splatoon.tutorial;

import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchControlInterface;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.objects.*;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.objects.tutorial.Checkpoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TutorialMatch extends Match {

    public class TutorialAIEntity {

        private Location spawnPoint;
        private EntityNPC npc;
        public TutorialAIEntity(EntityNPC npc, Location location) {

            this.spawnPoint = location;
            this.npc = npc;

        }

    }

    private ArrayList<TutorialAIEntity> aiEntities = new ArrayList<>();
    public void endTutorial() {

        if(!getHumanPlayers().isEmpty()) {

            SplatoonHumanPlayer player = getHumanPlayers().get(0);
            player.getPlayer().sendMessage("Tutorial finish triggered.");
            player.getEquipment().resetPrimaryWeapon();
            player.getEquipment().resetSecondaryWeapon();
            player.getEquipment().resetSpecialWeapon();

        }

        rollback();

        ArrayList<TutorialAIEntity> newEntities = new ArrayList<>();
        for(TutorialAIEntity aiEntity : aiEntities) {

            if(!aiEntity.npc.isRemoved()) {

                aiEntity.npc.remove();

            }
            TutorialAIEntity entity = new TutorialAIEntity(new EntityNPC(aiEntity.spawnPoint, getRegisteredTeams().get(1), this, TUTORIAL_PROPERTIES), aiEntity.spawnPoint);
            entity.npc.getEquipment().setPrimaryWeapon(23);
            entity.npc.setSpawnPoint(aiEntity.spawnPoint);
            newEntities.add(entity);

            addPlayer(entity.npc);

        }
        aiEntities.clear();
        aiEntities.addAll(newEntities);

        for(SplatoonProjectile projectile : getProjectiles()) {

            projectile.onRemove();

        }
        clearQueues();

        for(GameObject object : getGameObjects()) {

            if(object instanceof Sponge) {

                ((Sponge)object).reset();

            } else if(object instanceof WeaponDrop) {

                ((WeaponDrop)object).restore();

            } else if(object instanceof RideRail || object instanceof InkRail) {

                object.reset();

            } else if(object instanceof Gusher) {

                object.reset();

            }

        }

    }


    private AIProperties TUTORIAL_PROPERTIES = new AIProperties(20d, 20d, 25d);

    public TutorialMatch(World world) {

        super(world);
        setMatchController(new MatchControlInterface() {
            @Override
            public ArrayList<JumpPoint> getJumpPoints(Team team) {
                return new ArrayList<>();
            }

            @Override
            public void playerAdded(SplatoonPlayer player) {

                if (player instanceof SplatoonHumanPlayer) {

                    player.teleport(spawnPoint);
                    player.setTeam(getRegisteredTeams().get(0));
                    player.setSpawnPoint(spawnPoint);

                    ((SplatoonHumanPlayer) player).getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Willkommen im Splatoon-Tutorial!");

                } else {

                    player.setTeam(getRegisteredTeams().get(1));

                }

            }

            @Override
            public void playerRemoved(SplatoonPlayer player) {

            }

            @Override
            public void objectAdded(GameObject object) {

            }

            @Override
            public void objectRemoved(GameObject object) {

            }

            @Override
            public void teamAdded(Team team) {

            }

            @Override
            public void addGUIItems(SplatoonPlayer player) {

            }

            @Override
            public void handleSplat(SplatoonPlayer player, SplatoonPlayer shooter, SplatoonProjectile projectile) {

                if(shooter instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) shooter;
                    player1.getPlayer().sendMessage(" §8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Du hast einen " + player.getTeam().getColor().prefix() + "Gegner §7erledigt!");

                }

            }
        });

    }

    @Override
    public MatchType getMatchType() {
        return MatchType.TUTORIAL;
    }


    private Location spawnPoint = null;
    private Location currentRespawnPoint = null;
    private HashMap<String, ArrayList<Vector>> foundJoints = new HashMap<>();
    private StoredPlaceholder jumpPointPlaceholder = null;
    private Vector pos = new Vector();

    public void createRails() {

        for(Map.Entry<String, ArrayList<Vector>> entry : foundJoints.entrySet()) {

            String key = entry.getKey();
            if(key.contains("riderail")) {

                RideRail rideRail = new RideRail(this, entry.getValue().toArray(new Vector[]{}));
                try {

                    rideRail.interpolateVectors();

                } catch (Exception e) {

                    e.printStackTrace();

                }
                addGameObject(rideRail);

            } else if(key.contains("inkrail")) {

                InkRail inkRail = new InkRail(this, entry.getValue().toArray(new Vector[]{}));
                try {

                    inkRail.interpolateVectors();

                } catch (Exception e) {

                    e.printStackTrace();

                }
                addGameObject(inkRail);

            }

        }

        LaunchPad pad = new LaunchPad(this, pos.clone().add(new Vector(0, -.4, 0)).toLocation(getWorld()), getRegisteredTeams().get(0).getColor(), spawnPoint.toVector(), () -> {

            SplatoonPlayer player = getAllPlayers().get(0);
            player.sendMessage("§8§l> §7Herzlichen Glückwunsch, du hast das Tutorial abgeschlossen.");
            rollback();

        });
        addGameObject(pad);

    }

    int textIndex = 0;
    public void handlePlaceholder(Vector vector, StoredPlaceholder placeholder) {

        if(placeholder.type == SchematicPlaceholder.Splatoon.SPAWN_POINT) {

            spawnPoint = new Location(getWorld(), vector.getX() + (placeholder.x + .5), vector.getY() + (placeholder.y), vector.getZ() + (placeholder.z + .5));
            currentRespawnPoint = spawnPoint.clone();

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.WEAPON_DROP) {

            String val = placeholder.getData().getOrDefault("weapon", "none");
            Location location = new Location(getWorld(), placeholder.x + .5, placeholder.y, placeholder.z + .5);
            location = location.add(vector);
            if(val.equalsIgnoreCase("dummy")) {

                location.setYaw(90f);
                Dummy dummy = new Dummy(this, location, 100);
                addGameObject(dummy);

            } else {

                if(!val.equalsIgnoreCase("none")) {

                    location = location.add(0, .5, 0);
                    WeaponDrop drop = new WeaponDrop(this, Integer.parseInt(val), location);
                    addGameObject(drop);

                }

            }

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.INK_RAIL_JOINT || placeholder.type == SchematicPlaceholder.Splatoon.RIDE_RAIL_JOINT) {

            Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
            vec = vec.add(vector);

            Map.Entry<String, String> entry = placeholder.getData().entrySet().iterator().next();
            String finalVal = entry.getKey() + entry.getValue();

            if(!foundJoints.containsKey(finalVal)) {

                ArrayList<Vector> vectors = new ArrayList<>();
                vectors.add(vec);
                foundJoints.put(finalVal, vectors);

            } else {

                foundJoints.get(finalVal).add(vec);

            }

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.SPONGE) {

            Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
            vec = vec.add(vector);
            Block block = getWorld().getBlockAt((int) vec.getX(), (int) vec.getY(), (int) vec.getZ());

            Sponge sponge = new Sponge(this, block);
            addGameObject(sponge);

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.CHECKPOINT) {

            Checkpoint checkpoint = new Checkpoint(this, new Location(getWorld(), placeholder.x + .5, placeholder.y + .0625, placeholder.z + .5).add(vector));
            addGameObject(checkpoint);

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.GUSHER) {

            Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
            vec = vec.add(vector);
            Gusher gusher = new Gusher(this, getWorld().getBlockAt((int) vec.getX(), (int) vec.getY(), (int) vec.getZ()), BlockFace.UP, 1.5d);
            addGameObject(gusher);

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.HOOK) {

            Vector vec = new Vector(placeholder.x + .5, placeholder.y - .5, placeholder.z + .5);
            vec = vec.add(vector);
            Hook hook = new Hook(this, vec.toLocation(getWorld()));
            addGameObject(hook);

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.AI_SPAWN) {

            Vector vec = new Vector(placeholder.x + .5, placeholder.y, placeholder.z + .5);
            vec = vec.add(vector);
            System.out.println("Spawn AI at " + vec);


            EntityNPC npc = new EntityNPC(vec.toLocation(getWorld()), getRegisteredTeams().get(1), this, TUTORIAL_PROPERTIES);
            npc.getEquipment().setPrimaryWeapon(23);
            TutorialAIEntity entity = new TutorialAIEntity(npc, vec.toLocation(getWorld()));
            aiEntities.add(entity);
            entity.npc.setSpawnPoint(vec.toLocation(getWorld()));

            addPlayer(npc);

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.TEXT) {

            Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
            vec = vec.add(vector);
            if(textIndex == 0) {

                addHologram(vec, "§fWillkommen bei §cSp§6la§et§ao§bo§5n", "§fIm folgenden Kurs wirst du die", "§fgrundlegenden Mechaniken des Spielmodus kennenlernen.", "", "§fNach Abschluss des Tutorials stehen dir", "§fneben Anfangsausrüstung und Waffen auch Kämpfe", "§fmit anderen Spielern zur Verfügung.");

            } else if(textIndex == 1) {

                addHologram(vec, "§e§lTintenfischform", "§7Du kannst:", "§aIn deiner Tinte schwimmen", "§aTinte regenerien", "§aWände hochschwimmen", "§aLeicht fliehen", "§fDrücke §6§l3 §foder", "§fwähle den §edritten Slot", "§fum dich zu einem Tinten-", "§ffisch zu verwandeln.");

            } else if(textIndex == 2) {

                addHologram(vec, "§e§lMenschliche Form", "§7Du kannst:", "§a- Waffen nutzen", "§a(Solange du Tinte hast)", "§a- Spezialwaffen einsetzen", "§a- Supersprünge ausführen", "", "§fDiese Form ist aktiv", "§fwenn nicht der §edritte", "§eSlot §fausgewählt ist");

            } else if(textIndex == 3) {

                addHologram(vec, "§aGut gemacht!", "§fDu erhältst nun deine erste Waffe.", "§fDer §eJunior-Kleckser§f.", "", "§fHebe ihn auf, wähle den ersten Slot", "§fund drücke Rechtsklick zum", "§fverschießen von Tinte.", "", "§fTinte kannst du jederzeit durch", "§eschwimmen in eigener Farbe", "§fmit der Tintenfischform regenerieren.");

            } else if(textIndex == 4) {

                addHologram(vec, "§aSuper!", "§fDu scheinst den Dreh raus zu haben.", "", "§fNeben Primärwaffen gibt es noch:", "§e- Sekundärwaffen", "§6bspw. Bomben, Sprinkler, Sprungpunkte", "§fund", "§e- Spezialwaffen", "§6Rüstung, Jetpack, Tintenschock, ...", "", "§fWarum probierst du nicht", "§fmal diese Klecksbomben aus?");

            } else if(textIndex == 5) {

                addHologram(vec, "§aWeiter gehts!", "§fNutze §ePrimär- §foder", "§eSekundärwaffe §fum die", "§fWand zu überqueren.", "", "§fWie du vorhin gemerkt hast", "§fkannst du Wände welche", "§fin deiner Farbe", "§feingefärbt sind hochschwimmen.");

            } else if(textIndex == 6) {

                addHologram(vec, "§aSepiadukt", "§fAktiviere das Sepiadukt indem du", "§fden vor dir liegenden Glasblock einfärbst.", "", "§fDu kannst die entstandene Linie anschließend", "§fin der Tintenfischform nachschwimmen.", "", "§fEin Sepiadukt kannst du jederzeit", "§fmit der §eLeertaste §foder einem Wechsel", "§fzur menschlichen Form verlassen.");

            } else if(textIndex == 7) {

                vec = vec.add(new Vector(0, 1.35, 0));
                addHologram(vec, "§aGitter", "§fIn menschlicher Form kannst du", "§fGitter nicht durchdringen.", "", "§fFärbe die Fläche unter dem Gitter ein", "§fund schwimme anschließend unter dem Gitter durch.", "", "§fTipp: Alle Tintenprojektile fliegen", "§fdurch Gitter hindurch.");
                //addHologram(vec, "§aSepiadukt", "§fAktiviere das Sepiadukt indem du", "§fden vor dir liegenden Glasblock einfärbst.", "", "§fDu kannst die entstandene Linie anschließend", "§fin der Tintenfischform nachschwimmen.", "", "§fEin Sepiadukt kannst du jederzeit", "§fmit der §eLeertaste §foder einem Wechsel", "§fzur menschlichen Form verlassen.");

            } else if(textIndex == 8) {

                addHologram(vec, "§aSurfschienen", "§fEine Surfschiene wird ähnlich wie ein", "§fSepiadukt aktiviert. Springe anschließend", "§fauf die Schiene. Du rutschst automatisch", "§fentlang der Schiene.", "", "§fDu kannst die Schiene", "§fals Mensch und Tintenfisch", "§fbenutzen. Zum Verlassen", "§fkannst du §6SHIFT §fdrücken.");

            } else if(textIndex == 9) {

                addHologram(vec, "§aGegnerische Tinte", "§fSie macht dich langsamer und", "§ffügt dir Schaden hinzu.", "", "§fDu solltest also vermeiden dich", "§fin feindlichem Territorium zu bewegen.", "", "§fAllerdings kannst du den", "§fSpieß auch umdrehen indem du", "§fdie gegnerische Farbe mit deiner", "§feigenen Tinte überdeckst.");


            } else if(textIndex == 10) {

                addHologram(vec, "§aTintenfountäne", "§fWenn ausgelöst schießt eine Tintenfountäne", "§fjede Menge Tinte in die Luft", "", "§fDu kannst dich außerdem von dem", "§fentstehenden Tintenstrom nach", "§foben katapulieren lassen.");

            } else if (textIndex == 11) {

                addHologram(vec, "§aZugpunkt", "§fWenn ausgelöst wirst du zu", "§fdiesem Anglerhaken angezogen.", "", "§fProbier's mal mit der", "§ePrimärwaffe§f!");
                //addHologram(vec, "§aGegnerische Tinte", "§fIn gegnerischer Tinte erleidest", "§fdu Schaden und bist sehr langsam.", "", "§fDu kannst allerdings gegnerische Tinte umfärben.", "§fVersuche dir einen Weg in", "§fdeiner Farbe zu erschaffen.");

            } else if(textIndex == 12) {

                addHologram(vec, "§aSchwamm", "§fJe mehr Tinte du ihm zuführst", "§fdesto größer wird er.", "", "§fAllerdings können Gegner den", "§fSchwamm auch ganz leicht wieder", "§fschrumpfen lassen.", "", "§fVersuche auf die obere Ebene zu gelanden!");
                //addHologram(vec, "§aTintenfountäne", "§fWenn ausgelöst schießt eine Tintenfountäne", "§fjede Menge Tinte in die Luft", "", "§fDu kannst dich außerdem von dem", "§fentstehenden Tintenstrom nach", "§foben katapulieren lassen.");

            } else if(textIndex == 13) {

                vec = vec.add(new Vector(0, 0, 1.5));
                addHologram(vec, "§aDas letzte Puzzleteil", "§fPrimär-, Sekundär-, und Spezial-", "§fWaffe formen ein §eWaffenset", "", "§fDiese Spezialwaffe ist die", "§fTintenrüstung. Sie bietet dir", "§fund Mitspielern in der Nähe", "§feine Rüstung für kurze Zeit.", "", "§f§oBleib jedoch trotzdem wachsam!");

            } else if(textIndex == 15) {

                addHologram(vec, "§aZu weit weg?", "§fEin Zugpunkt kann auch mit", "§feiner Bombe aktiviert werden.", "", "§fProbiers mit der", "§eKlecksbombe§f!");

            } else if(textIndex == 14) {

                addHologram(vec, "§aGenug aufgewärmt!", "§fDu bist nun bereit", "§fmit und gegen andere Spieler", "§fzu kämpfen. Zur Lobby kommst du", "§findem du als Tintenfisch in das", "§fvor dir liegende §6Sprungfeld §fschwimmst.", "", "§fViel Glück und viel Spaß!", "§a~ Xenyria Serverteam ~");

            }
            textIndex++;

        } else if(placeholder.type == SchematicPlaceholder.Splatoon.LAUNCHPAD_TUTORIAL) {

            jumpPointPlaceholder = placeholder;

            Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
            vec = vec.add(vector);
            pos = vec;

        }

    }
    public void addHologram(Vector vector, String... strings) {

        addGameObject(new Hologram(this, vector.toLocation(getWorld()), 10d, strings));

    }

}
