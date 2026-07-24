package cn.gtemc.craftengine.item;

import cn.gtemc.craftengine.item.processor.EnchantmentsLoreProcessor;
import cn.gtemc.craftengine.item.processor.GetArgumentsProcessor;
import cn.gtemc.craftengine.item.processor.RandomNumberProcessor;
import cn.gtemc.craftengine.util.RegistryUtils;
import net.momirealms.craftengine.core.item.processor.ItemProcessorType;
import net.momirealms.craftengine.core.util.Key;

public final class ItemProcessors {
    private ItemProcessors() {}

    public static final ItemProcessorType<RandomNumberProcessor> RANDOM_NUMBER = RegistryUtils.registerItemProcessorType(Key.of("gtemc:random_number"), RandomNumberProcessor.FACTORY);
    public static final ItemProcessorType<GetArgumentsProcessor> GET_ARGUMENTS = RegistryUtils.registerItemProcessorType(Key.of("gtemc:get_arguments"), GetArgumentsProcessor.FACTORY);
    public static final ItemProcessorType<EnchantmentsLoreProcessor> ENCHANTMENTS_LORE = RegistryUtils.registerItemProcessorType(Key.of("gtemc:enchantments_lore"), EnchantmentsLoreProcessor.FACTORY);

    public static void register() {
    }
}
