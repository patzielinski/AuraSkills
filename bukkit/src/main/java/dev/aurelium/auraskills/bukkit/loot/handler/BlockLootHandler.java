package dev.aurelium.auraskills.bukkit.loot.handler;

import dev.aurelium.auraskills.api.event.loot.LootDropEvent;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.source.XpSource;
import dev.aurelium.auraskills.api.source.type.BlockXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.hooks.SlimefunHook;
import dev.aurelium.auraskills.bukkit.loot.Loot;
import dev.aurelium.auraskills.bukkit.loot.LootPool;
import dev.aurelium.auraskills.bukkit.loot.LootTable;
import dev.aurelium.auraskills.bukkit.loot.context.SourceContext;
import dev.aurelium.auraskills.bukkit.loot.provider.SkillLootProvider;
import dev.aurelium.auraskills.bukkit.loot.type.CommandLoot;
import dev.aurelium.auraskills.bukkit.loot.type.ItemLoot;
import dev.aurelium.auraskills.bukkit.skills.excavation.ExcavationLootProvider;
import dev.aurelium.auraskills.bukkit.source.BlockLeveler;
import dev.aurelium.auraskills.common.config.Option;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.data.Pair;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockLootHandler extends LootHandler implements Listener {

    private final Random random = new Random();
    private final Map<Skill, SkillLootProvider> lootProviders = new HashMap<>();

    public BlockLootHandler(AuraSkills plugin) {
        super(plugin);
        registerLootProviders();
    }

    private void registerLootProviders() {
        lootProviders.put(Skills.EXCAVATION, new ExcavationLootProvider(plugin, this));
    }

    public Pair<BlockXpSource, Skill> getSource(Block block) {
        return plugin.getLevelManager().getLeveler(BlockLeveler.class).getSource(block, BlockXpSource.BlockTriggers.BREAK);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (getSource(block) == null) return;

        // Check block replace
        if (plugin.configBoolean(Option.CHECK_BLOCK_REPLACE_ENABLED) && plugin.getRegionManager().isPlacedBlock(block)) {
            return;
        }

        Player player = event.getPlayer();

        if (failsChecks(player, block.getLocation())) return;

        if (plugin.getHookManager().isRegistered(SlimefunHook.class)) {
            if (plugin.getHookManager().getHook(SlimefunHook.class).hasBlockInfo(block.getLocation())) {
                return;
            }
        }

        User user = plugin.getUser(player);

        var originalSource = getSource(block);

        BlockXpSource source = originalSource.first();
        Skill skill = originalSource.second();

        // Get the loot provider for getting chance and cause
        SkillLootProvider provider = lootProviders.get(skill);

        LootTable table = plugin.getLootTableManager().getLootTable(skill);
        if (table == null) return;
        for (LootPool pool : table.getPools()) {
            // Ignore non-applicable sources
            if (provider != null && !provider.isApplicable(pool, source)) {
                continue;
            }
            // Calculate chance for pool
            double chance;
            if (provider != null) {
                chance = provider.getChance(pool, user);
            } else {
                chance = getCommonChance(pool, user);
            }

            LootDropEvent.Cause cause;
            if (provider != null) {
                cause = provider.getCause(pool);
            } else {
                cause = LootDropEvent.Cause.UNKNOWN;
            }
            // Select pool and give loot
            if (selectBlockLoot(table, pool, player, chance, source, event, skill, cause)) {
                break;
            }
        }
    }

    private boolean selectBlockLoot(LootTable table, LootPool pool, Player player, double chance, XpSource originalSource, BlockBreakEvent event, Skill skill, LootDropEvent.Cause cause) {
        if (random.nextDouble() < chance) { // Pool is selected
            Loot selectedLoot = selectLoot(pool, new SourceContext(originalSource));
            // Give loot
            if (selectedLoot != null) {
                if (selectedLoot instanceof ItemLoot itemLoot) {
                    giveBlockItemLoot(player, itemLoot, event, skill, cause, table);
                } else if (selectedLoot instanceof CommandLoot commandLoot) {
                    giveCommandLoot(player, commandLoot, null, skill);
                }
                // Override vanilla loot if enabled
                if (pool.overridesVanillaLoot()) {
                    event.setDropItems(false);
                }
                return true;
            }
        }
        return false;
    }

}
