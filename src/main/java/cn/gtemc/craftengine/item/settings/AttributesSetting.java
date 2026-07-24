package cn.gtemc.craftengine.item.settings;

import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.item.processor.AttributeModifiersProcessor;
import net.momirealms.craftengine.core.item.setting.CustomItemSettingType;
import net.momirealms.craftengine.core.item.setting.ItemSettings;
import net.momirealms.craftengine.core.item.setting.ItemSettingsModifier;
import net.momirealms.craftengine.core.item.setting.ItemSettingsModifierFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class AttributesSetting implements ItemSettingsModifier {
    public static final CustomItemSettingType<List<AttributeData>> ATTRIBUTES = CustomItemSettingType.simple();
    public static final ItemSettingsModifierFactory<AttributesSetting> FACTORY = new Factory();
    private final List<AttributeData> data;

    public AttributesSetting(List<AttributeData> data) {
        this.data = data;
    }

    @Override
    public void apply(ItemSettings settings) {
        settings.addCustomData(ATTRIBUTES, this.data);
    }

    private static class Factory implements ItemSettingsModifierFactory<AttributesSetting> {
        private static final String[] EXPIRY_TIME = new String[]{"expiry_time", "expiry-time"};
        private static final String[] CONDITIONS = new String[] {"conditions", "condition"};

        @Override
        public AttributesSetting create(ConfigValue value) {
            List<AttributeData> attributeData = value.getAsList(it -> {
                ConfigSection section = it.getAsSection();
                Key type = AttributeModifiersProcessor.getNativeAttributeName(section.getNonNullIdentifier("type"));
                AttributeModifier.Slot slot = section.getNonNullEnum("slot", AttributeModifier.Slot.class);
                Key id = section.getNonNullIdentifier("id");
                NumberProvider amount = section.getNonNullNumber("amount");
                AttributeModifier.Operation operation = section.getNonNullEnum("operation", AttributeModifier.Operation.class);
                AttributeModifiersProcessor.PreModifier.PreDisplay display = null;
                if (VersionHelper.isOrAbove1_21_6 && section.containsKey("display")) {
                    ConfigSection displayConfig = section.getNonNullSection("display");
                    AttributeModifier.Display.Type displayType = displayConfig.getNonNullEnum("type", AttributeModifier.Display.Type.class);
                    if (displayType == AttributeModifier.Display.Type.OVERRIDE) {
                        String miniMessageValue = displayConfig.getNonEmptyString("value");
                        display = new AttributeModifiersProcessor.PreModifier.PreDisplay(displayType, miniMessageValue);
                    } else {
                        display = new AttributeModifiersProcessor.PreModifier.PreDisplay(displayType, null);
                    }
                }
                AttributeModifiersProcessor.PreModifier preProcessor = new AttributeModifiersProcessor.PreModifier(type.value(), slot, Optional.of(id), amount, operation, display);
                Date expires = section.getValue(EXPIRY_TIME, v -> v.is(Date.class) ? (Date) v.value() : null);
                Predicate<Context> conditions = MiscUtils.allOf(section.getSectionList(CONDITIONS, CommonConditions::fromConfig));
                return new AttributeData(preProcessor, expires, conditions);
            });
            return new AttributesSetting(attributeData);
        }
    }

    public record AttributeData(
            AttributeModifiersProcessor.PreModifier modifier,
            @Nullable Date expires,
            @Nullable Predicate<Context> conditions
    ) {}
}
