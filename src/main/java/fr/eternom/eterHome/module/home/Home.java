package fr.eternom.eterHome.module.home;

import fr.eternom.eterLib.helper.sql.Row;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Un home, sur n'importe quel serveur du réseau.
 * Toujours vérifier {@link #isOn(String)} avant {@link #getLocation()} : un monde "world"
 * existe sur tous les serveurs, seul le nom du serveur dit si le home est ici.
 */
public class Home {

    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,32}");

    private final UUID owner;
    private final String server;
    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String icon; // nom d'un Material choisi par le joueur, null = icône automatique

    public Home(UUID owner, String server, String name, String world, double x, double y, double z, float yaw, float pitch,
                String icon) {
        this.owner = owner;
        this.server = server;
        this.name = normalize(name);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.icon = icon == null || icon.isEmpty() ? null : icon;
    }

    public static Home of(UUID owner, String server, String name, Location location, Material icon) {
        return new Home(owner, server, name, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), icon == null ? null : icon.name());
    }

    public static Home fromRow(Row row) {
        return new Home(row.getUUID("owner"), row.getString("server"), row.getString("name"), row.getString("world"),
                row.getDouble("x"), row.getDouble("y"), row.getDouble("z"), row.getFloat("yaw"), row.getFloat("pitch"),
                row.getString("icon"));
    }

    /** Format Redis : server;x;y;z;yaw;pitch;icon;world (monde en dernier, il peut contenir un ';'). */
    public static Home deserialize(UUID owner, String name, String value) {
        String[] parts = value.split(";", 8);
        return new Home(owner, parts[0], name, parts[7],
                Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]), Float.parseFloat(parts[5]), parts[6]);
    }

    /** Les noms de homes ne tiennent pas compte de la casse : /home Base = /home base. */
    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /** Lettres, chiffres, _ et -, 32 caractères max (taille de la colonne). */
    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public String serialize() {
        return server + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch + ";" + (icon == null ? "" : icon) + ";" + world;
    }

    /** Valeurs à passer à Database#set (HashMap : l'icône peut être null). */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<>();
        values.put("owner", owner);
        values.put("server", server);
        values.put("name", name);
        values.put("world", world);
        values.put("x", x);
        values.put("y", y);
        values.put("z", z);
        values.put("yaw", yaw);
        values.put("pitch", pitch);
        values.put("icon", icon);
        return values;
    }

    public Home withIcon(String icon) {
        return new Home(owner, server, name, world, x, y, z, yaw, pitch, icon);
    }

    public boolean isOn(String server) {
        return this.server.equals(server);
    }

    /** La position sur ce serveur, ou null si son monde n'y existe pas. */
    public Location getLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    /**
     * Icône à afficher : celle choisie par le joueur, sinon selon la dimension (monde normal, Nether, End).
     * Un home d'un autre serveur, dont on ne connaît pas le monde ici, est une perle de l'End.
     */
    public Material getDisplayIcon(String currentServer) {
        Material chosen = icon == null ? null : Material.matchMaterial(icon);
        if (chosen != null && chosen.isItem() && !chosen.isAir()) {
            return chosen;
        }
        if (!isOn(currentServer)) {
            return Material.ENDER_PEARL;
        }
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return Material.BARRIER;
        }
        return switch (bukkitWorld.getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    public UUID getOwner() {
        return owner;
    }

    public String getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getIcon() {
        return icon;
    }
}
