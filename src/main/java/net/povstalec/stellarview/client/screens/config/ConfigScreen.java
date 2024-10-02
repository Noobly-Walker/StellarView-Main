package net.povstalec.stellarview.client.screens.config;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen
{
	private final Screen parentScreen;

    private static final int BACK_BUTTON_WIDTH = 200;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int BACK_BUTTON_TOP_OFFSET = 26;

	
	public ConfigScreen(@Nullable Screen parentScreen)
	{
		super(Component.translatable("gui.stellarview.config"));
		this.parentScreen = parentScreen;
	}

	
	@Override
    public void init()
    {
		int l = this.height / 4;
		
		super.init();
		this.addRenderableWidget(new Button(this.width / 2 - 100, l, 200, 20, Component.translatable("gui.stellarview.config.general"), 
				(button) -> this.minecraft.setScreen(new GeneralConfigScreen(this))));
		
		this.addRenderableWidget(new Button(this.width / 2 - 100, l + 24 * 2, 200, 20, Component.translatable("gui.stellarview.config.overworld"),
				(button) -> this.minecraft.setScreen(new OverworldConfigScreen(this))));
		this.addRenderableWidget(new Button(this.width / 2 - 100, l + 24 * 3, 200, 20, Component.translatable("gui.stellarview.config.nether"),
				(button) -> this.minecraft.setScreen(new NetherConfigScreen(this))));
		this.addRenderableWidget(new Button(this.width / 2 - 100, l + 24 * 4, 200, 20, Component.translatable("gui.stellarview.config.end"),
				(button) -> this.minecraft.setScreen(new EndConfigScreen(this))));

		this.addRenderableWidget(new Button((this.width - BACK_BUTTON_WIDTH) / 2, this.height - BACK_BUTTON_TOP_OFFSET, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 
				CommonComponents.GUI_BACK, 
				(button) -> this.minecraft.setScreen(this.parentScreen)));
    }
	
	@Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 16777215);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
	
}
