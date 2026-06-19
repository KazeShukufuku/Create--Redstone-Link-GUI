package com.ggrgg.createredstonelinkgui.common;

import java.lang.reflect.Method;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class VoidLinkHelper {
    private static final String[] BEHAVIOUR_CLASS_NAMES = {
            "io.github.jasonsimpart.createutilitiesj.blocks.voidtypes.VoidLinkBehaviour",
            "me.duquee.createutilities.blocks.voidtypes.VoidLinkBehaviour"
    };

    private static boolean checked;
    private static BehaviourType<?> voidLinkType;
    private static Method testHitMethod;
    private static Method getOwnerMethod;
    private static Method setOwnerMethod;
    private static Method canInteractMethod;
    private static Method setFrequencyMethod;
    private static Method getFrequencyStackMethod;

    private static void check() {
        if (checked) return;
        checked = true;

        for (String className : BEHAVIOUR_CLASS_NAMES) {
            try {
                Class<?> behaviourClass = Class.forName(className);
                voidLinkType = (BehaviourType<?>) behaviourClass.getField("TYPE").get(null);
                testHitMethod = behaviourClass.getMethod("testHit", int.class, Vec3.class);
                getOwnerMethod = behaviourClass.getMethod("getOwner");
                setOwnerMethod = behaviourClass.getMethod("setOwner", GameProfile.class);
                canInteractMethod = behaviourClass.getMethod("canInteract", Player.class);
                setFrequencyMethod = behaviourClass.getMethod("setFrequency", boolean.class, ItemStack.class);
                getFrequencyStackMethod = behaviourClass.getMethod("getFrequencyStack", boolean.class);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    public static Object getBehaviour(Level level, BlockPos pos) {
        check();
        if (voidLinkType == null) return null;

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return BlockEntityBehaviour.get(be, voidLinkType);
    }

    public static boolean isHitOnAnySlot(Object behaviour, Vec3 hitLocation) {
        if (behaviour == null || testHitMethod == null) return false;
        try {
            return (boolean) testHitMethod.invoke(behaviour, 0, hitLocation)
                    || (boolean) testHitMethod.invoke(behaviour, 1, hitLocation)
                    || (boolean) testHitMethod.invoke(behaviour, 2, hitLocation);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean canInteract(Object behaviour, Player player) {
        if (behaviour == null || canInteractMethod == null) return true;
        try {
            return (boolean) canInteractMethod.invoke(behaviour, player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static GameProfile getOwner(Object behaviour) {
        if (behaviour == null || getOwnerMethod == null) return null;
        try {
            return (GameProfile) getOwnerMethod.invoke(behaviour);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void setOwner(Object behaviour, GameProfile owner) {
        if (behaviour == null || setOwnerMethod == null) return;
        try {
            setOwnerMethod.invoke(behaviour, owner);
        } catch (Throwable ignored) {
        }
    }

    public static ItemStack getFrequencyStack(Object behaviour, boolean firstSlot) {
        if (behaviour == null || getFrequencyStackMethod == null) return ItemStack.EMPTY;
        try {
            ItemStack stack = (ItemStack) getFrequencyStackMethod.invoke(behaviour, firstSlot);
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static void setFrequency(Object behaviour, boolean firstSlot, ItemStack stack) {
        if (behaviour == null || setFrequencyMethod == null) return;
        try {
            setFrequencyMethod.invoke(behaviour, firstSlot, stack.copy());
        } catch (Throwable ignored) {
        }
    }
}
