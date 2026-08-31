package cn.gtemc.craftengine.item.processor;

import cn.gtemc.craftengine.item.settings.AttributesSetting;
import cn.gtemc.craftengine.plugin.context.RandomNumberContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.processor.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.processor.SimpleNetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.DoubleTag;
import net.momirealms.craftengine.libraries.nbt.Tag;

import java.util.Date;
import java.util.List;
import java.util.Map;

public final class GetArgumentsProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<GetArgumentsProcessor> FACTORY = new Factory();
    private final boolean attribute;

    public GetArgumentsProcessor(boolean attribute) {
        this.attribute = attribute;
    }

    @Override
    public void apply(ItemBuildContext context) {
        Item item = context.item();
        if (item == null) return;
        RandomNumberContext randomNumberContext = RandomNumberContext.of(context.player(), item);
        if (this.attribute) {
            List<VanillaAttributeModifier> attributeModifiers = new ObjectArrayList<>();
            ItemDefinition itemDefinition = item.getDefinition().orElse(null);
            if (itemDefinition != null) {
                List<AttributesSetting.AttributeData> attributeDataList = itemDefinition.settings().getCustomData(AttributesSetting.ATTRIBUTES);
                for (AttributesSetting.AttributeData attributeData : attributeDataList) {
                    if (attributeData.expires() != null && attributeData.expires().before(new Date())) continue; // 过期不管
                    if (attributeData.conditions() != null && !attributeData.conditions().test(randomNumberContext)) continue; // 不符合条件不管
                    CompoundTag randomNumberData = item.getSparrowTag(RandomNumberContext.RANDOM_NUMBER_KEY) instanceof CompoundTag tag ? tag : null;
                    if (randomNumberData == null) {
                        continue;
                    }
                    for (Map.Entry<String, Tag> entry : randomNumberData.entrySet()) {
                        if (entry.getValue() instanceof DoubleTag tag) {
                            context.contexts().withParameter(ContextKey.direct("random_number_" + entry.getKey()), tag.value());
                        }
                    }
                }
            }
            item.attributeModifiers(attributeModifiers);
        }
    }

    public static class Factory implements ItemProcessorFactory<GetArgumentsProcessor> {

        @Override
        public GetArgumentsProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            return new GetArgumentsProcessor(
                    section.getBoolean("attribute")
            );
        }
    }
}
