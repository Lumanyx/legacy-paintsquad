package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public enum PrimaryWeaponType {

    SPLATTERSHOT("Kleckser", "§7§o§lNormale Schusswaffe.\n§eRechtsklick §7zum verschießen von Tinte.", Material.STONE_HOE, ResourcePackItemOption.SPLATTERSHOT.getDamageValue()),
    CHARGER("Konzentrator", "§7§o§lAufladbare Schusswaffe für Langstreckenziele.\n§eRechtsklick §7zum Aufladen eines Schusses.\n§eLoslassen §7zum Abfeuern eines Schusses.", Material.GOLDEN_HOE, ResourcePackItemOption.DEFAULT_CHARGER.getDamageValue()),
    ROLLER("Roller", "§7§o§lSchwingbarer Farbroller.\n§eRechtsklick §7zum Schwingen des Rollers.\n§7Halten der §erechten Maustaste §7zum rollen.\n\n§7Überrollst du einen Gegner\n§7so erhält dieser viel Schaden.", Material.IRON_SHOVEL, ResourcePackItemOption.ROLLER_IDLE.getDamageValue()),
    SLOSHER("Schwapper", "§7§o§lFarbeimer.\n§eRechtsklick §7zum Schwingen des Eimers.", Material.BUCKET, (short)0),
    DUALIES("Doppler", "§7§o§lDoppelwaffe.\n§eRechtsklick §7zum verschießen von Tinte.\n§eLinksklick §7zum ausweichen.\n\n§7Das Ausweichen benötigt Tinte\n§7und kann je nach Waffe\n§7nur begrenzt oft innerhalb eines\n§7kurzen Zeitraums verwendet werden.", Material.WOODEN_HOE, ResourcePackItemOption.DUALIES.getDamageValue()),
    SPLATLING("Splatling", "§7§o§lAufladbare Schusswaffe.\n§eRechtsklick §7zum Aufladen einer Salve.\n§eLoslassen §7zum Abfeuern einer Salve.", Material.IRON_HORSE_ARMOR, (short)0),
    BRELLA("Pluviator", "§7§o§lSchusswaffe mit mobiler Deckung.\n§eRechtsklick §7zum abfeuern.\n§7Halte solange die §erechte Maustaste §7bis\n§7der Schirm sich löst um erneut zu schießen.", Material.STICK, (short)0),
    BLASTER("Blaster", "§7§o§lLangsame Schusswaffe mit explosiver Munition.\n§eRechtsklick §7zum verschießen von Tinte.\n§7Projektile explodieren beim Aufprall mit Gegnern\n§7bzw. nach kurzer Zeit in der Luft.", Material.IRON_HOE, ResourcePackItemOption.BLASTER.getDamageValue()),
    BRUSH("Pinsel", "§7§o§lSchwingbarer Farbpinsel.\n§7Rechtsklick §7zum schnellen hin- und her schwingen.\n§7Halte die §erechte Maustaste §7zur\n§7schnelleren Fortbewegung.", Material.DIAMOND_SHOVEL, ResourcePackItemOption.INKBRUSH_IDLE.getDamageValue()),
    MODE_EXCLUSIVE("Spielmodus-exklusiv", "Spezialanfertigung für einen Spielmodus", Material.BARRIER, (short)0);


    private String name;
    public String getName() { return name; }

    private String description;
    public String getDescription() { return description; }

    private Material representiveMaterial;
    public Material getRepresentiveMaterial() { return representiveMaterial; }

    private short durability;
    public short getDurability() { return durability; }

    PrimaryWeaponType(String name, String description, Material representiveMaterial, short durability) {

        this.name = name;
        this.description = description;
        this.representiveMaterial = representiveMaterial;

    }

}
