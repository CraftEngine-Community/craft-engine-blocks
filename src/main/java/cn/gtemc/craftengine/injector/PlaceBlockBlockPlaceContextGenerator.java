package cn.gtemc.craftengine.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.libraries.reflection.clazz.SparrowClass;
import net.momirealms.craftengine.libraries.reflection.constructor.SConstructor5;
import net.momirealms.craftengine.libraries.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.craftengine.libraries.reflection.method.matcher.MethodMatcher;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.context.BlockPlaceContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.context.UseOnContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;

import java.lang.reflect.Array;

public final class PlaceBlockBlockPlaceContextGenerator {
    private static SConstructor5 constructor$PlaceBlockBlockPlaceContext;

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(BlockPlaceContextProxy.CLASS)
                .name("cn.gtemc.craftengine.injector.PlaceBlockBlockPlaceContext")
                .method(ElementMatchers.is(SparrowClass.of(BlockPlaceContextProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getNearestLookingDirection"))))
                .intercept(MethodDelegation.to(DirectionHandler.INSTANCE))
                .method(ElementMatchers.is(SparrowClass.of(BlockPlaceContextProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getNearestLookingVerticalDirection"))))
                .intercept(MethodDelegation.to(VerticalDirectionHandler.INSTANCE))
                .method(ElementMatchers.is(SparrowClass.of(BlockPlaceContextProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getNearestLookingDirections"))))
                .intercept(MethodDelegation.to(DirectionsHandler.INSTANCE));

        SparrowClass<?> clazz = SparrowClass.of(builder.make()
                .load(PlaceBlockBlockPlaceContextGenerator.class.getClassLoader())
                .getLoaded());
        constructor$PlaceBlockBlockPlaceContext = clazz.getSparrowConstructor(ConstructorMatcher.takeArguments(
                LevelProxy.CLASS, PlayerProxy.CLASS, InteractionHandProxy.CLASS, ItemStackProxy.CLASS, BlockHitResultProxy.CLASS
        )).asm$5();
    }

    public static class DirectionHandler {
        public static final DirectionHandler INSTANCE = new DirectionHandler();

        @RuntimeType
        public Object getNearestLookingDirection(@This Object context) {
            Object hitResult = UseOnContextProxy.INSTANCE.getHitResult(context);
            return BlockHitResultProxy.INSTANCE.getDirection(hitResult);
        }
    }

    public static class VerticalDirectionHandler {
        public static final VerticalDirectionHandler INSTANCE = new VerticalDirectionHandler();

        @RuntimeType
        public Object getNearestLookingVerticalDirection(@This Object context) {
            Object hitResult = UseOnContextProxy.INSTANCE.getHitResult(context);
            Object direction = BlockHitResultProxy.INSTANCE.getDirection(hitResult);
            return direction == DirectionProxy.UP ? DirectionProxy.UP : DirectionProxy.DOWN;
        }
    }

    public static class DirectionsHandler {
        public static final DirectionsHandler INSTANCE = new DirectionsHandler();

        @RuntimeType
        public Object[] getNearestLookingDirections(@This Object context) {
            Object hitResult = UseOnContextProxy.INSTANCE.getHitResult(context);
            Object direction = BlockHitResultProxy.INSTANCE.getDirection(hitResult);
            Object directions = Array.newInstance(DirectionProxy.CLASS, DirectionProxy.VALUES.length);
            Array.set(directions, 0, direction);
            Array.set(directions, DirectionProxy.VALUES.length - 1, DirectionProxy.INSTANCE.getOpposite(direction));
            int i = 0;
            for (Object direction1 : DirectionProxy.VALUES) {
                if (direction1 != direction && direction1 != DirectionProxy.INSTANCE.getOpposite(direction)) {
                    Array.set(directions, ++i, direction);
                }
            }
            return (Object[]) directions;
        }
    }

    public static Object create(Object level, Object hand, Object itemStack, Object hitResult) {
        return constructor$PlaceBlockBlockPlaceContext.newInstance(level, null, hand, itemStack, hitResult);
    }
}
