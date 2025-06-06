package dev.aurelium.auraskills.common.reward.builder;

import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.reward.SkillReward;
import dev.aurelium.auraskills.common.reward.type.ItemReward;
import dev.aurelium.auraskills.common.util.data.Validate;

public class ItemRewardBuilder extends MessagedRewardBuilder {

    private NamespacedId itemKey;
    private int amount;

    public ItemRewardBuilder(AuraSkillsPlugin plugin) {
        super(plugin);
    }

    public ItemRewardBuilder itemKey(NamespacedId itemKey) {
        this.itemKey = itemKey;
        return this;
    }

    public ItemRewardBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public SkillReward build() {
        Validate.allNotNull("key", itemKey);
        return new ItemReward(plugin, skill, menuMessage, chatMessage, itemKey, amount);
    }

}
