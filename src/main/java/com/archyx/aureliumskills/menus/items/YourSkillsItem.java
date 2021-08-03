package com.archyx.aureliumskills.menus.items;

import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.lang.Lang;
import com.archyx.aureliumskills.lang.MenuMessage;
import com.archyx.aureliumskills.util.text.TextUtil;
import com.archyx.slate.item.provider.SingleItemProvider;
import com.archyx.slate.menu.ActiveMenu;
import org.bukkit.entity.Player;

import java.util.Locale;

public class YourSkillsItem extends AbstractItem implements SingleItemProvider {

    public YourSkillsItem(AureliumSkills plugin) {
        super(plugin);
    }

    @Override
    public String onPlaceholderReplace(String placeholder, Player player, ActiveMenu activeMenu) {
        Locale locale = plugin.getLang().getLocale(player);
        switch (placeholder) {
            case "your_skills":
                return TextUtil.replace(Lang.getMessage(MenuMessage.YOUR_SKILLS, locale),
                        "{player}", player.getName());
            case "desc":
                return Lang.getMessage(MenuMessage.YOUR_SKILLS_DESC, locale);
            case "hover":
                return Lang.getMessage(MenuMessage.YOUR_SKILLS_HOVER, locale);
            case "click":
                return Lang.getMessage(MenuMessage.YOUR_SKILLS_CLICK, locale);
        }
        return placeholder;
    }
}
