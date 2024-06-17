package io.github.sefiraat.networks.slimefun.network;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NetworkController extends NetworkObject {

    private static final String CRAYON = "crayon";
    private static final Map<Location, NetworkRoot> NETWORKS = new HashMap<>();
    private static final Set<Location> CRAYONS = new HashSet<>();

    private final ItemSetting<Integer> maxNodes;
    protected final Map<Location, Boolean> firstTickMap = new HashMap<>();

    public NetworkController(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.CONTROLLER);

        maxNodes = new IntRangeSetting(this, "max_nodes", 10, 8000, 10000);
        addItemSetting(maxNodes);

        addItemHandler(
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return false;
                }

                @Override
                public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {

                    if (!firstTickMap.containsKey(block.getLocation())) {
                        onFirstTick(block, data);
                        firstTickMap.put(block.getLocation(), true);
                    }

                    addToRegistry(block);
                    NetworkRoot networkRoot = new NetworkRoot(block.getLocation(), NodeType.CONTROLLER, maxNodes.getValue());
                    networkRoot.addAllChildren();

                    boolean crayon = CRAYONS.contains(block.getLocation());
                    if (crayon) {
                        networkRoot.setDisplayParticles(true);
                    }

                    NETWORKS.put(block.getLocation(), networkRoot);
                }
            }
        );
    }

    private void onFirstTick(@Nonnull Block block, @Nonnull SlimefunBlockData data) {
        final String crayon = data.getData(CRAYON);
        if (Boolean.parseBoolean(crayon)) {
            CRAYONS.add(block.getLocation());
        }
    }

    public static Map<Location, NetworkRoot> getNetworks() {
        return NETWORKS;
    }

    public static Set<Location> getCrayons() {
        return CRAYONS;
    }

    public static void addCrayon(@Nonnull Location location) {
        StorageCacheUtils.setData(location, CRAYON, String.valueOf(true));
        CRAYONS.add(location);
    }

    public static void removeCrayon(@Nonnull Location location) {
        StorageCacheUtils.removeData(location, CRAYON);
        CRAYONS.remove(location);
    }

    public static boolean hasCrayon(@Nonnull Location location) {
        return CRAYONS.contains(location);
    }

    public static void wipeNetwork(@Nonnull Location location) {
        for (NetworkNode node : NETWORKS.remove(location).getChildrenNodes()) {
            NetworkStorage.removeNode(node.getNodePosition());
        }
    }
}
