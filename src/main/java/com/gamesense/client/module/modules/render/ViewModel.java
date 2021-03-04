package com.gamesense.client.module.modules.render;

import com.gamesense.api.event.events.TransformSideFirstPersonEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.DoubleSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.client.module.Module;
import com.gamesense.client.module.modules.Category;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

/**
 * @Author GL_DONT_CARE (Viewmodel Transformations)
 * @Author NekoPvP (Item FOV)
 */

@Module.Declaration(name = "ViewModel", category = Category.Render)
public class ViewModel extends Module {

    public BooleanSetting cancelEating;
    ModeSetting type;
    DoubleSetting xRight;
    DoubleSetting yRight;
    DoubleSetting zRight;
    DoubleSetting xLeft;
    DoubleSetting yLeft;
    DoubleSetting zLeft;
    DoubleSetting fov;

    public void setup() {
        ArrayList<String> types = new ArrayList<>();
        types.add("Value");
        types.add("FOV");
        types.add("Both");

        type = registerMode("Type", types, "Value");
        cancelEating = registerBoolean("No Eat", false);
        xLeft = registerDouble("Left X", 0.0, -2.0, 2.0);
        yLeft = registerDouble("Left Y", 0.2, -2.0, 2.0);
        zLeft = registerDouble("Left Z", -1.2, -2.0, 2.0);
        xRight = registerDouble("Right X", 0.0, -2.0, 2.0);
        yRight = registerDouble("Right Y", 0.2, -2.0, 2.0);
        zRight = registerDouble("Right Z", -1.2, -2.0, 2.0);
        fov = registerDouble("Item FOV", 130, 70, 200);
    }

    @EventHandler
    private final Listener<TransformSideFirstPersonEvent> eventListener = new Listener<>(event -> {
        if (type.getValue().equalsIgnoreCase("Value") || type.getValue().equalsIgnoreCase("Both")) {
            if (event.getEnumHandSide() == EnumHandSide.RIGHT) {
                GlStateManager.translate(xRight.getValue(), yRight.getValue(), zRight.getValue());
            } else if (event.getEnumHandSide() == EnumHandSide.LEFT) {
                GlStateManager.translate(xLeft.getValue(), yLeft.getValue(), zLeft.getValue());
            }
        }
    });

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (type.getValue().equalsIgnoreCase("FOV") || type.getValue().equalsIgnoreCase("Both")) {
            event.setFOV(fov.getValue().floatValue());
        }
    }

    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}