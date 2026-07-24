package cn.gtemc.craftengine.item.settings;

import cn.gtemc.craftengine.util.RegistryUtils;
import net.momirealms.craftengine.core.item.setting.ItemSettingsModifierType;
import net.momirealms.craftengine.core.util.Key;

public final class ItemSettings {
    public static final ItemSettingsModifierType<AttributesSetting> ATTRIBUTES = RegistryUtils.registerItemSetting(Key.of("gtemc:attributes"), AttributesSetting.FACTORY);

    public static void register() {
    }
}
