package com.ggrgg.createredstonelinkgui.compat.emi;

import java.util.List;
import java.util.function.Consumer;

import com.ggrgg.createredstonelinkgui.client.screen.RedstoneLinkConfigScreen;
import com.ggrgg.createredstonelinkgui.client.screen.VoidLinkConfigScreen;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public class AddonEmiPlugin implements EmiPlugin {

    private static final List<String> SYMBOL_NAMES = List.of(
        "symbol_1", "symbol_2", "symbol_3", "symbol_4", "symbol_5",
        "symbol_6", "symbol_7", "symbol_8", "symbol_9", "symbol_0",
        "symbol_a", "symbol_b", "symbol_c", "symbol_d", "symbol_e",
        "symbol_f", "symbol_g", "symbol_h", "symbol_i", "symbol_j",
        "symbol_k", "symbol_l", "symbol_m", "symbol_n", "symbol_o",
        "symbol_p", "symbol_q", "symbol_r", "symbol_s", "symbol_t",
        "symbol_u", "symbol_v", "symbol_w", "symbol_x", "symbol_y",
        "symbol_z",
        "symbol_a_small", "symbol_b_small", "symbol_c_small", "symbol_d_small", "symbol_e_small",
        "symbol_f_small", "symbol_g_small", "symbol_h_small", "symbol_i_small", "symbol_j_small",
        "symbol_k_small", "symbol_l_small", "symbol_m_small", "symbol_n_small", "symbol_o_small",
        "symbol_p_small", "symbol_q_small", "symbol_r_small", "symbol_s_small", "symbol_t_small",
        "symbol_u_small", "symbol_v_small", "symbol_w_small", "symbol_x_small", "symbol_y_small",
        "symbol_z_small",
        "symbol_up_arrow", "symbol_down_arrow", "symbol_left_arrow", "symbol_right_arrow",
        "symbol_darrow_up", "symbol_darrow_down", "symbol_darrow_left", "symbol_darrow_right",
        "symbol_skull",
        "symbol_creeperhead"
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(RedstoneLinkConfigScreen.class, new EMIDragDropHandler<>());
        registry.addDragDropHandler(VoidLinkConfigScreen.class, new EMIDragDropHandler<>());
        registry.addExclusionArea(RedstoneLinkConfigScreen.class, (screen, consumer) -> {
            addArea(screen.blockPreviewBounds, consumer);
            addArea(screen.presetPanelBounds, consumer);
        });
        registry.addExclusionArea(VoidLinkConfigScreen.class, (screen, consumer) -> {
            addArea(screen.blockPreviewBounds, consumer);
            addArea(screen.presetPanelBounds, consumer);
        });

        for (String name : SYMBOL_NAMES) {
            ResourceLocation id = new ResourceLocation("frequency", name);
            if (!BuiltInRegistries.ITEM.containsKey(id)) continue;
            var item = BuiltInRegistries.ITEM.get(id);
            registry.addEmiStack(EmiStack.of(new ItemStack(item)));
        }
    }

    private static void addArea(net.minecraft.client.renderer.Rect2i rect, Consumer<Bounds> consumer) {
        if (rect != null) {
            consumer.accept(new Bounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
        }
    }
}
