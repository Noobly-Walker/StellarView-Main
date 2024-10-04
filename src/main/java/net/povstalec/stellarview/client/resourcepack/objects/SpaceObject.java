package net.povstalec.stellarview.client.resourcepack.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.stellarview.StellarView;
import net.povstalec.stellarview.client.resourcepack.ViewCenter;
import net.povstalec.stellarview.common.config.GeneralConfig;
import net.povstalec.stellarview.common.util.AxisRotation;
import net.povstalec.stellarview.common.util.Color;
import net.povstalec.stellarview.common.util.SpaceCoords;
import net.povstalec.stellarview.common.util.SpaceCoords.SpaceDistance;
import net.povstalec.stellarview.common.util.SphericalCoords;
import net.povstalec.stellarview.common.util.StellarCoordinates;
import net.povstalec.stellarview.common.util.TextureLayer;
import net.povstalec.stellarview.common.util.UV;

public abstract class SpaceObject
{
	public static final float DEFAULT_DISTANCE = 100.0F;
	
	public static final ResourceLocation SPACE_OBJECT_LOCATION = new ResourceLocation(StellarView.MODID, "space_object");
	public static final ResourceKey<Registry<SpaceObject>> REGISTRY_KEY = ResourceKey.createRegistryKey(SPACE_OBJECT_LOCATION);
	public static final Codec<ResourceKey<SpaceObject>> RESOURCE_KEY_CODEC = ResourceKey.codec(REGISTRY_KEY);
	
	@Nullable
	protected ResourceKey<SpaceObject> parentKey;

	@Nullable
	protected SpaceObject parent;
	
	protected ArrayList<SpaceObject> children = new ArrayList<SpaceObject>();
	
	protected SpaceCoords coords; // Absolute coordinates of the center (not necessarily the object itself, since it can be orbiting some other object for example)
	protected AxisRotation axisRotation;
	
	protected ArrayList<TextureLayer> textureLayers;
	
	protected FadeOutHandler fadeOutHandler;
	
	protected ResourceLocation location;
	protected double lastDistance = 0; // Last known distance of this object from the View Center, used for sorting
	
	public SpaceObject(Optional<ResourceKey<SpaceObject>> parentKey, Either<SpaceCoords, StellarCoordinates.Equatorial> coords, AxisRotation axisRotation, List<TextureLayer> textureLayers, FadeOutHandler fadeOutHandler)
	{
		if(parentKey.isPresent())
				this.parentKey = parentKey.get();
		
		if(coords.left().isPresent())
			this.coords = coords.left().get();
		else
			this.coords = coords.right().get().toGalactic().toSpaceCoords();
		
		this.axisRotation = axisRotation;
		
		this.textureLayers = new ArrayList<TextureLayer>(textureLayers);
		
		this.fadeOutHandler = fadeOutHandler;
	}
	
	public SpaceCoords getCoords()
	{
		return this.coords;
	}
	
	public Vector3f getPosition(ViewCenter viewCenter, AxisRotation axisRotation, long ticks, float partialTicks)
	{
		return new Vector3f();
	}
	
	public Vector3f getPosition(ViewCenter viewCenter, long ticks, float partialTicks)
	{
		return new Vector3f();
	}
	
	public AxisRotation getAxisRotation()
	{
		return axisRotation;
	}
	
	public ArrayList<TextureLayer> getTextureLayers()
	{
		return textureLayers;
	}
	
	public Optional<ResourceKey<SpaceObject>> getParentKey()
	{
		return Optional.ofNullable(parentKey);
	}
	
	public Optional<SpaceObject> getParent()
	{
		return Optional.ofNullable(parent);
	}
	
	public FadeOutHandler getFadeOutHandler()
	{
		return fadeOutHandler;
	}
	
	public void setResourceLocation(ResourceLocation resourceLocation)
	{
		this.location = resourceLocation;
	}
	
	public static double distanceSize(double distance)
	{
		return 1 / distance;
	}
	
	public static float dayBrightness(ViewCenter viewCenter, float size, long ticks, ClientLevel level, Camera camera, float partialTicks)
	{
		if(viewCenter.starsAlwaysVisible())
			return GeneralConfig.bright_stars.get() ? 0.5F * StellarView.lightSourceDimming(level, camera) : 0.5F;
		
		float brightness = level.getStarBrightness(partialTicks) * 2;
		
		if(GeneralConfig.bright_stars.get())
			brightness = brightness * StellarView.lightSourceDimming(level, camera);
		
		if(brightness < viewCenter.dayMaxBrightness && size > viewCenter.dayMinVisibleSize)
		{
			float aboveSize = size >= viewCenter.dayMaxVisibleSize ? viewCenter.dayVisibleSizeRange : size - viewCenter.dayMinVisibleSize;
			float brightnessPercentage = aboveSize / viewCenter.dayVisibleSizeRange;
			
			brightness = brightnessPercentage * viewCenter.dayMaxBrightness;
		}
		
		return brightness * StellarView.rainDimming(level, partialTicks);
	}
	
	public void setPosAndRotation(SpaceCoords coords, AxisRotation axisRotation)
	{
		removeCoordsAndRotationFromChildren(getCoords(), getAxisRotation());
		
		if(this.parent != null)
		{
			this.coords = coords.add(this.parent.getCoords());
			this.axisRotation = axisRotation.add(this.parent.getAxisRotation());
		}
		else
		{
			this.coords = coords;
			this.axisRotation = axisRotation;
		}
		
		addCoordsAndRotationToChildren(this.coords, this.axisRotation);
	}
	
	public void addChild(SpaceObject child)
	{
		if(child.parent != null)
		{
			StellarView.LOGGER.error(this.toString() + " already has a parent");
			return;
		}
		
		this.children.add(child);
		child.parent = this;
		child.coords = child.coords.add(this.coords);
		
		child.axisRotation = child.axisRotation.add(this.axisRotation);
		
		child.addCoordsAndRotationToChildren(this.coords, this.axisRotation);
	}
	
	protected void addCoordsAndRotationToChildren(SpaceCoords coords, AxisRotation axisRotation)
	{
		for(SpaceObject childOfChild : this.children)
		{
			childOfChild.coords = childOfChild.coords.add(coords);
			childOfChild.axisRotation = childOfChild.axisRotation.add(axisRotation);
			
			childOfChild.addCoordsAndRotationToChildren(coords, axisRotation);
		}
	}
	
	protected void removeCoordsAndRotationFromChildren(SpaceCoords coords, AxisRotation axisRotation)
	{
		for(SpaceObject childOfChild : this.children)
		{
			childOfChild.coords = childOfChild.coords.sub(coords);
			childOfChild.axisRotation = childOfChild.axisRotation.sub(axisRotation);
			
			childOfChild.removeCoordsAndRotationFromChildren(coords, axisRotation);
		}
	}
	
	
	public static void renderOnSphere(Color.FloatRGBA rgba, Color.FloatRGBA secondaryRGBA, ResourceLocation texture, UV.Quad uv,
			ClientLevel level, Camera camera, BufferBuilder bufferbuilder, Matrix4f lastMatrix, SphericalCoords sphericalCoords,
			long ticks, double distance, float partialTicks, float brightness, float size, float rotation, boolean shouldBlend)
	{
		Vector3f corner00 = new Vector3f(size, DEFAULT_DISTANCE, size);
		Vector3f corner10 = new Vector3f(-size, DEFAULT_DISTANCE, size);
		Vector3f corner11 = new Vector3f(-size, DEFAULT_DISTANCE, -size);
		Vector3f corner01 = new Vector3f(size, DEFAULT_DISTANCE, -size);
		
		Quaterniond quaternionX = new Quaterniond().rotateY(sphericalCoords.theta);
		quaternionX.mul(new Quaterniond().rotateX(sphericalCoords.phi));
		quaternionX.mul(new Quaterniond().rotateY(rotation));
		
		quaternionX.transform(corner00);
		quaternionX.transform(corner10);
		quaternionX.transform(corner11);
		quaternionX.transform(corner01);
		
		if(shouldBlend)
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		else
			RenderSystem.defaultBlendFunc();
		
		RenderSystem.setShaderColor(rgba.red() * secondaryRGBA.red(), rgba.green() * secondaryRGBA.green(), rgba.blue() * secondaryRGBA.blue(), brightness * rgba.alpha() * secondaryRGBA.alpha());
		
		RenderSystem.setShaderTexture(0, texture);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        bufferbuilder.vertex(lastMatrix, corner00.x, corner00.y, corner00.z).uv(uv.topRight().u(ticks), uv.topRight().v(ticks)).endVertex();
        bufferbuilder.vertex(lastMatrix, corner10.x, corner10.y, corner10.z).uv(uv.bottomRight().u(ticks), uv.bottomRight().v(ticks)).endVertex();
        bufferbuilder.vertex(lastMatrix, corner11.x, corner11.y, corner11.z).uv(uv.bottomLeft().u(ticks), uv.bottomLeft().v(ticks)).endVertex();
        bufferbuilder.vertex(lastMatrix, corner01.x, corner01.y, corner01.z).uv(uv.topLeft().u(ticks), uv.topLeft().v(ticks)).endVertex();
        
        BufferUploader.drawWithShader(bufferbuilder.end());
        
        RenderSystem.defaultBlendFunc();
	}
	
	/**
	 * Method for rendering an individual texture layer, override to change details of how this object's texture layers are rendered
	 * @param textureLayer
	 * @param level
	 * @param bufferbuilder
	 * @param lastMatrix
	 * @param sphericalCoords
	 * @param ticks
	 * @param distance
	 * @param partialTicks
	 */
	protected void renderTextureLayer(TextureLayer textureLayer, ViewCenter viewCenter, ClientLevel level, Camera camera, BufferBuilder bufferbuilder, Matrix4f lastMatrix, SphericalCoords sphericalCoords, long ticks, double distance, float partialTicks)
	{
		if(textureLayer.rgba().alpha() <= 0)
			return;
		
		float size = (float) textureLayer.mulSize(distanceSize(distance));
		
		if(size < textureLayer.minSize())
		{
			if(textureLayer.clampAtMinSize())
				size = (float) textureLayer.minSize();
			else
				return;
		}
		
		renderOnSphere(textureLayer.rgba(), Color.FloatRGBA.DEFAULT, textureLayer.texture(), textureLayer.uv(),
				level, camera, bufferbuilder, lastMatrix, sphericalCoords,
				ticks, distance, partialTicks, dayBrightness(viewCenter, size, ticks, level, camera, partialTicks), size, (float) textureLayer.rotation(), textureLayer.shoulBlend());
	}
	
	protected void renderTextureLayers(ViewCenter viewCenter, ClientLevel level, Camera camera, BufferBuilder bufferbuilder, Matrix4f lastMatrix, SphericalCoords sphericalCoords, long ticks, double distance, float partialTicks)
	{
		RenderSystem.setShader(GameRenderer::getPositionTexShader);

		for(TextureLayer textureLayer : textureLayers)
		{
			renderTextureLayer(textureLayer, viewCenter, level, camera, bufferbuilder, lastMatrix, sphericalCoords, ticks, distance, partialTicks);
		}
	}
	
	
	public void render(ViewCenter viewCenter, ClientLevel level, float partialTicks, PoseStack stack, Camera camera, 
			Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog, BufferBuilder bufferbuilder, 
			Vector3f parentVector, AxisRotation parentRotation)
	{
		long ticks = level.getDayTime();
		
		Vector3f positionVector = getPosition(viewCenter, parentRotation, ticks, partialTicks).add(parentVector); // Handles orbits 'n stuff

		// Add parent vector to current coords
		SpaceCoords coords = getCoords().add(positionVector);

		// Subtract coords of this from View Center coords to get relative coords
		SphericalCoords sphericalCoords = coords.skyPosition(level, viewCenter, partialTicks);
		
		lastDistance = sphericalCoords.r;
		sphericalCoords.r = DEFAULT_DISTANCE;
		
		// Render children behind the parent
		for(SpaceObject child : children)
		{
			if(child.lastDistance >= this.lastDistance)
				child.render(viewCenter, level, partialTicks, stack, camera, projectionMatrix, isFoggy, setupFog, bufferbuilder, positionVector, this.axisRotation);
		}
		
		// If the object isn't the same we're viewing everything from and it itsn't too far away, render it
		if(!viewCenter.objectEquals(this) && getFadeOutHandler().getFadeOutEndDistance().toKm() > lastDistance)
			renderTextureLayers(viewCenter, level, camera, bufferbuilder, stack.last().pose(), sphericalCoords, ticks, lastDistance, partialTicks);
		
		if(getFadeOutHandler().getMaxChildRenderDistance().toKm() > lastDistance)
		{
			// Render children in front of the parent
			for(SpaceObject child : children)
			{
				if(child.lastDistance < this.lastDistance)
					child.render(viewCenter, level, partialTicks, stack, camera, projectionMatrix, isFoggy, setupFog, bufferbuilder, positionVector, this.axisRotation);
			}
		}
	}
	
	// Sets View Center coords and then renders everything
	public void renderFrom(ViewCenter viewCenter, ClientLevel level, float partialTicks, PoseStack stack, Camera camera, 
			Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog, BufferBuilder bufferbuilder)
	{
		if(parent != null)
			viewCenter.addCoords(getPosition(viewCenter, parent.getAxisRotation(), level.getDayTime(), partialTicks));
		else
			viewCenter.addCoords(getPosition(viewCenter, level.getDayTime(), partialTicks));
		
		if(parent != null)
			parent.renderFrom(viewCenter, level, partialTicks, stack, camera, projectionMatrix, isFoggy, setupFog, bufferbuilder);
		else
			viewCenter.renderSkyObjects(this, level, partialTicks, stack, camera, projectionMatrix, isFoggy, setupFog, bufferbuilder);
	}
	
	@Override
	public String toString()
	{
		if(location != null)
			return location.toString();
		
		return this.getClass().toString();
	}
	
	
	
	public static class FadeOutHandler
	{
		public static final FadeOutHandler DEFAULT_PLANET_HANDLER = new FadeOutHandler(new SpaceDistance(70000000000D), new SpaceDistance(100000000000D), new SpaceDistance(100000000000D));
		public static final FadeOutHandler DEFAULT_STAR_HANDLER = new FadeOutHandler(new SpaceDistance(3000000L), new SpaceDistance(5000000L), new SpaceDistance(100000000000D));
		public static final FadeOutHandler DEFAULT_STAR_FIELD_HANDLER = new FadeOutHandler(new SpaceDistance(3000000L), new SpaceDistance(5000000L), new SpaceDistance(5000000L));
		
		private SpaceDistance fadeOutStartDistance;
		private SpaceDistance fadeOutEndDistance;
		private SpaceDistance maxChildRenderDistance;
		
		public static final Codec<FadeOutHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				SpaceDistance.CODEC.fieldOf("fade_out_start_distance").forGetter(FadeOutHandler::getFadeOutStartDistance),
				SpaceDistance.CODEC.fieldOf("fade_out_end_distance").forGetter(FadeOutHandler::getFadeOutEndDistance),
				SpaceDistance.CODEC.fieldOf("max_child_render_distance").forGetter(FadeOutHandler::getMaxChildRenderDistance)
				).apply(instance, FadeOutHandler::new));
		
		public FadeOutHandler(SpaceDistance fadeOutStartDistance, SpaceDistance fadeOutEndDistance, SpaceDistance maxChildRenderDistance)
		{
			this.fadeOutStartDistance = fadeOutStartDistance;
			this.fadeOutEndDistance = fadeOutEndDistance;
			this.maxChildRenderDistance = maxChildRenderDistance;
		}
		
		public SpaceDistance getFadeOutStartDistance()
		{
			return fadeOutStartDistance;
		}
		
		public SpaceDistance getFadeOutEndDistance()
		{
			return fadeOutEndDistance;
		}
		
		public SpaceDistance getMaxChildRenderDistance()
		{
			return maxChildRenderDistance;
		}
	}
}
