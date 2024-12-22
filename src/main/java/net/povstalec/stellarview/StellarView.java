package net.povstalec.stellarview;

import java.util.Optional;
import java.util.function.BiFunction;

import net.minecraftforge.eventbus.api.EventPriority;
import net.povstalec.stellarview.compatibility.aether.StellarViewAetherEffects;
import net.povstalec.stellarview.compatibility.twilightforest.StellarViewTwilightForestEffects;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.povstalec.stellarview.client.render.level.StellarViewEndEffects;
import net.povstalec.stellarview.client.render.level.StellarViewNetherEffects;
import net.povstalec.stellarview.client.render.level.StellarViewOverworldEffects;
import net.povstalec.stellarview.client.resourcepack.ResourcepackReloadListener;
import net.povstalec.stellarview.client.resourcepack.Space;
import net.povstalec.stellarview.client.screens.config.ConfigScreen;
import net.povstalec.stellarview.common.config.StellarViewConfig;
import net.povstalec.stellarview.common.util.KeyBindings;

@Mod(StellarView.MODID)
public class StellarView
{
	public static final String MODID = "stellarview";
	
	public static final String ENHANCED_CELESTIALS_MODID = "enhancedcelestials";
	public static final String TWILIGHT_FOREST_MODID = "twilightforest";
	public static final String AETHER_MODID = "aether";
    
    private static Optional<Boolean> isEnhancedCelestialsLoaded = Optional.empty();
	private static Optional<Boolean> isTwilightForestLoaded = Optional.empty();
	private static Optional<Boolean> isAetherLoaded = Optional.empty();
    
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static Minecraft minecraft = Minecraft.getInstance();
    
    public static StellarViewOverworldEffects overworld;
    public static StellarViewNetherEffects nether;
    public static StellarViewEndEffects end;
	
	public static StellarViewTwilightForestEffects twilightForest;
	public static StellarViewAetherEffects aether;
	
	private static float starBrightness = 0F;
	private static float dustCloudBrightness = 0F;

	public StellarView()
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, StellarViewConfig.CLIENT_CONFIG, MODID + "-client.toml");
		
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, 
				() -> new ConfigScreenHandler.ConfigScreenFactory(new BiFunction<Minecraft, Screen, Screen>()
				{
					@Override
					public Screen apply(Minecraft mc, Screen screen)
					{
						return new ConfigScreen(screen);
					}
				}));
		
		MinecraftForge.EVENT_BUS.register(this);
	}
    
    @Mod.EventBusSubscriber(modid = StellarView.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
    	@SubscribeEvent(priority = EventPriority.LOWEST)
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event)
		{
			overworld = new StellarViewOverworldEffects();
			nether = new StellarViewNetherEffects();
			end = new StellarViewEndEffects();
			
			event.register(StellarViewOverworldEffects.OVERWORLD_EFFECTS, overworld);
			event.register(StellarViewNetherEffects.NETHER_EFFECTS, nether);
			event.register(StellarViewEndEffects.END_EFFECTS, end);
			
			if(isTwilightForestLoaded())
			{
				twilightForest = new StellarViewTwilightForestEffects();
				event.register(StellarViewTwilightForestEffects.TWILIGHT_FOREST_EFFECTS, twilightForest);
			}
			
			if(isAetherLoaded())
			{
				aether = new StellarViewAetherEffects();
				event.register(StellarViewAetherEffects.AETHER_EFFECTS, aether);
			}
        }
    	

    	@SubscribeEvent
        public static void registerClientReloadListener(RegisterClientReloadListenersEvent event)
        {
    		ResourcepackReloadListener.ReloadListener.registerReloadListener(event);
        }

    	@SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event)
        {
        	event.register(KeyBindings.OPEN_CONFIG_KEY);
        }
    }
    
    public static boolean isEnhancedCelestialsLoaded()
    {
    	if(isEnhancedCelestialsLoaded.isEmpty())
    		isEnhancedCelestialsLoaded = Optional.of(ModList.get().isLoaded(ENHANCED_CELESTIALS_MODID));
    	
    	return isEnhancedCelestialsLoaded.get();	
    }
	
	public static boolean isTwilightForestLoaded()
	{
		if(isTwilightForestLoaded.isEmpty())
			isTwilightForestLoaded = Optional.of(ModList.get().isLoaded(TWILIGHT_FOREST_MODID));
		
		return isTwilightForestLoaded.get();
	}
	
	public static boolean isAetherLoaded()
	{
		if(isAetherLoaded.isEmpty())
			isAetherLoaded = Optional.of(ModList.get().isLoaded(AETHER_MODID));
		
		return isAetherLoaded.get();
	}
    
    public static void updateSpaceObjects()
    {
		if(minecraft.level == null)
    		return;
    	
    	Space.updateSol();
    	Space.resetStarFields();
    }
    
    public static float lightSourceStarDimming(ClientLevel level, Camera camera)
    {
    	// Brightness of the position where the player is standing, 15 is subtracted from the ambient skylight, that way only block light is accounted for
    	int brightnessAtBlock = level.getLightEngine().getRawBrightness(camera.getEntity().getOnPos().above(), 15);
    	float brightness = 0.5F + 1.5F * ((15F - brightnessAtBlock) / 15F);
		
		if(starBrightness < brightness)
		{
			starBrightness += 0.01F;
			
			if(starBrightness > brightness)
				starBrightness = brightness;
		}
		else if(starBrightness > brightness)
		{
			starBrightness -= 0.01F;
			
			if(starBrightness < brightness)
				starBrightness = brightness;
		}
		
    	return starBrightness;
    }
	
	public static float lightSourceDustCloudDimming(ClientLevel level, Camera camera)
	{
		// Brightness of the position where the player is standing, 15 is subtracted from the ambient skylight, that way only block light is accounted for
		int brightnessAtBlock = level.getLightEngine().getRawBrightness(camera.getEntity().getOnPos().above(), 15);
		float brightness = 1.5F * ((7F - brightnessAtBlock) / 15F);
		
		if(brightness < 0)
			brightness = 0;
		
		if(dustCloudBrightness < brightness)
		{
			dustCloudBrightness += 0.001F;
			
			if(dustCloudBrightness > brightness)
				dustCloudBrightness = brightness;
		}
		else if(dustCloudBrightness > brightness)
		{
			dustCloudBrightness -= 0.001F;
			
			if(dustCloudBrightness < brightness)
				dustCloudBrightness = brightness;
		}
		
		return dustCloudBrightness;
	}
    
    public static float rainDimming(ClientLevel level, float partialTicks)
    {
    	return 1F - level.getRainLevel(partialTicks);
    }
}	
