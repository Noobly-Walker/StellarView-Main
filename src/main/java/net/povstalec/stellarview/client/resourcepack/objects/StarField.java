package net.povstalec.stellarview.client.resourcepack.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.stellarview.StellarView;
import net.povstalec.stellarview.client.resourcepack.DustCloudInfo;
import net.povstalec.stellarview.common.util.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.povstalec.stellarview.client.render.shader.StellarViewShaders;
import net.povstalec.stellarview.client.render.shader.StellarViewVertexFormat;
import net.povstalec.stellarview.client.resourcepack.StarInfo;
import net.povstalec.stellarview.client.resourcepack.ViewCenter;
import net.povstalec.stellarview.common.config.GeneralConfig;

public class StarField extends SpaceObject
{
	public static final ResourceLocation DEFAULT_DUST_CLOUD_TEXTURE = new ResourceLocation(StellarView.MODID,"textures/environment/dust_cloud.png");
	
	@Nullable
	protected DustCloudBuffer dustCloudBuffer;
	protected DustCloudData dustCloudData;
	protected final int dustClouds;
	protected final int totalDustClouds;
	protected ResourceLocation dustCloudTexture;
	
	@Nullable
	protected StarBuffer starBuffer;
	protected StarData starData;

	protected StarInfo starInfo;
	protected DustCloudInfo dustCloudInfo;
	
	protected final long seed;
	protected final boolean clumpStarsInCenter;
	
	protected final int diameter;
	protected final int stars;
	
	private final double xStretch;
	private final double yStretch;
	private final double zStretch;
	
	protected final ArrayList<SpiralArm> spiralArms;
	
	protected final int totalStars;
	
	protected boolean hasTexture = GeneralConfig.textured_stars.get();
	
	public static final Codec<StarField> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RESOURCE_KEY_CODEC.optionalFieldOf("parent").forGetter(StarField::getParentKey),
			Codec.either(SpaceCoords.CODEC, StellarCoordinates.Equatorial.CODEC).fieldOf("coords").forGetter(object -> Either.left(object.getCoords())),
			AxisRotation.CODEC.fieldOf("axis_rotation").forGetter(StarField::getAxisRotation),

			SpaceObject.FadeOutHandler.CODEC.optionalFieldOf("fade_out_handler", SpaceObject.FadeOutHandler.DEFAULT_STAR_FIELD_HANDLER).forGetter(StarField::getFadeOutHandler),
			
			Codec.intRange(0, 4000).optionalFieldOf("dust_clouds", 0).forGetter(StarField::getDustClouds),
			DustCloudInfo.CODEC.optionalFieldOf("dust_cloud_info", DustCloudInfo.DEFAULT_DUST_CLOUD_INFO).forGetter(StarField::getDustCloudInfo),
			ResourceLocation.CODEC.optionalFieldOf("dust_cloud_texture", DEFAULT_DUST_CLOUD_TEXTURE).forGetter(StarField::getDustCloudTexture),
			
			StarInfo.CODEC.optionalFieldOf("star_info", StarInfo.DEFAULT_STAR_INFO).forGetter(StarField::getStarInfo),
			Codec.LONG.fieldOf("seed").forGetter(StarField::getSeed),
			Codec.INT.fieldOf("diameter_ly").forGetter(StarField::getDiameter),
			
			Codec.intRange(0, 30000).fieldOf("stars").forGetter(StarField::getStars),
			Codec.BOOL.optionalFieldOf("clump_stars_in_center", true).forGetter(StarField::clumpStarsInCenter),
			
			Codec.DOUBLE.optionalFieldOf("x_stretch", 1.0).forGetter(StarField::xStretch),
			Codec.DOUBLE.optionalFieldOf("y_stretch", 1.0).forGetter(StarField::yStretch),
			Codec.DOUBLE.optionalFieldOf("z_stretch", 1.0).forGetter(StarField::zStretch),
			
			SpiralArm.CODEC.listOf().optionalFieldOf("spiral_arms", new ArrayList<SpiralArm>()).forGetter(starField -> starField.spiralArms)
			).apply(instance, StarField::new));
	
	public StarField(Optional<ResourceKey<SpaceObject>> parent, Either<SpaceCoords, StellarCoordinates.Equatorial> coords, AxisRotation axisRotation,
			FadeOutHandler fadeOutHandler, int dustClouds, DustCloudInfo dustCloudInfo, ResourceLocation dustCloudTexture, StarInfo starInfo, long seed, int diameter, int numberOfStars, boolean clumpStarsInCenter,
			double xStretch, double yStretch, double zStretch, List<SpiralArm> spiralArms)
	{
		super(parent, coords, axisRotation, fadeOutHandler);
		
		this.dustClouds = dustClouds;
		this.dustCloudInfo = dustCloudInfo;
		this.dustCloudTexture = dustCloudTexture;
		
		this.starInfo = starInfo;
		this.seed = seed;
		this.diameter = diameter;
		
		this.stars = numberOfStars;
		this.clumpStarsInCenter = clumpStarsInCenter;
		
		this.xStretch = xStretch;
		this.yStretch = yStretch;
		this.zStretch = zStretch;
		
		this.spiralArms = new ArrayList<SpiralArm>(spiralArms);

		// Calculate the total amount of stars
		int totalStars = stars;
		int totalDustClouds = dustClouds;
		for(SpiralArm arm : this.spiralArms)
		{
			totalStars += arm.armStars();
			totalDustClouds += arm.armDustClouds;
		}
		
		this.totalStars = totalStars;
		this.totalDustClouds = totalDustClouds;
	}
	
	public int getDustClouds()
	{
		return dustClouds;
	}
	
	public DustCloudInfo getDustCloudInfo()
	{
		return dustCloudInfo;
	}
	
	public ResourceLocation getDustCloudTexture()
	{
		return dustCloudTexture;
	}
	
	public StarInfo getStarInfo()
	{
		return starInfo;
	}
	
	public long getSeed()
	{
		return seed;
	}
	
	public int getDiameter()
	{
		return diameter;
	}
	
	public int getStars()
	{
		return stars;
	}
	
	public boolean clumpStarsInCenter()
	{
		return clumpStarsInCenter;
	}
	
	public double xStretch()
	{
		return xStretch;
	}
	
	public double yStretch()
	{
		return yStretch;
	}
	
	public double zStretch()
	{
		return zStretch;
	}
	
	public List<SpiralArm> getSpiralArms()
	{
		return spiralArms;
	}
	
	public boolean requiresSetup()
	{
		return starBuffer == null;
	}
	
	public boolean requiresReset()
	{
		return hasTexture != GeneralConfig.textured_stars.get();
	}
	
	public void reset()
	{
		starBuffer = null;
	}
	
	protected void generateStars(BufferBuilder bufferBuilder, RandomSource randomsource)
	{
		for(int i = 0; i < stars; i++)
		{
			// This generates random coordinates for the Star close to the camera
			double distance = clumpStarsInCenter ? randomsource.nextDouble() : Math.cbrt(randomsource.nextDouble());
			double theta = randomsource.nextDouble() * 2F * Math.PI;
			double phi = Math.acos(2F * randomsource.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens
			
			Vector3d cartesian = new SphericalCoords(distance * diameter, theta, phi).toCartesianD();
			
			cartesian.x *= xStretch;
			cartesian.y *= yStretch;
			cartesian.z *= zStretch;
			
			axisRotation.quaterniond().transform(cartesian);

			starData.newStar(starInfo, bufferBuilder, randomsource, cartesian.x, cartesian.y, cartesian.z, hasTexture, i);
		}
	}
	
	protected void generateDustClouds(BufferBuilder bufferBuilder, RandomSource randomsource)
	{
		for(int i = 0; i < dustClouds; i++)
		{
			// This generates random coordinates for the Star close to the camera
			double distance = clumpStarsInCenter ? randomsource.nextDouble() : Math.cbrt(randomsource.nextDouble());
			double theta = randomsource.nextDouble() * 2F * Math.PI;
			double phi = Math.acos(2F * randomsource.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens
			
			Vector3d cartesian = new SphericalCoords(distance * diameter, theta, phi).toCartesianD();
			
			cartesian.x *= xStretch;
			cartesian.y *= yStretch;
			cartesian.z *= zStretch;
			
			axisRotation.quaterniond().transform(cartesian);
			
			dustCloudData.newDustCloud(dustCloudInfo, bufferBuilder, randomsource, cartesian.x, cartesian.y, cartesian.z, 1, i);
		}
	}
	
	protected RenderedBuffer generateStarBuffer(BufferBuilder bufferBuilder)
	{
		RandomSource randomsource = RandomSource.create(seed);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, hasTexture ? StellarViewVertexFormat.STAR_POS_COLOR_LY_TEX : StellarViewVertexFormat.STAR_POS_COLOR_LY);
		
		double sizeMultiplier = diameter / 30D;
		
		starData = new StarData(totalStars);
		
		generateStars(bufferBuilder, randomsource);
		
		int numberOfStars = stars;
		for(SpiralArm arm : spiralArms) //Draw each arm
		{
			arm.generateStars(bufferBuilder, axisRotation, starData, starInfo, randomsource, numberOfStars, sizeMultiplier, hasTexture);
			numberOfStars += arm.armStars();
		}
		
		return bufferBuilder.end();
	}
	
	protected RenderedBuffer getStarBuffer(BufferBuilder bufferBuilder)
	{
		bufferBuilder.begin(VertexFormat.Mode.QUADS, hasTexture ? StellarViewVertexFormat.STAR_POS_COLOR_LY_TEX : StellarViewVertexFormat.STAR_POS_COLOR_LY);
		
		for(int i = 0; i < totalStars; i++)
		{
			starData.createStar(bufferBuilder, hasTexture, i);
		}
		return bufferBuilder.end();
	}
	
	public StarField setStarBuffer()
	{
		if(starBuffer != null)
			starBuffer.close();
		
		hasTexture = GeneralConfig.textured_stars.get();
		
		starBuffer = new StarBuffer();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer;
		
		bufferbuilder$renderedbuffer = getStarBuffer(bufferBuilder);
		
		starBuffer.bind();
		starBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
		
		return this;
	}
	
	public StarField setupBuffer()
	{
		if(starBuffer != null)
			starBuffer.close();
		
		starBuffer = new StarBuffer();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer;
		
		bufferbuilder$renderedbuffer = generateStarBuffer(bufferBuilder);
		
		starBuffer.bind();
		starBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
		
		return this;
	}
	
	
	
	protected RenderedBuffer generateDustCloudBuffer(BufferBuilder bufferBuilder)
	{
		RandomSource randomsource = RandomSource.create(seed);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, StellarViewVertexFormat.STAR_POS_COLOR_LY_TEX);
		
		double sizeMultiplier = diameter / 30D;
		
		dustCloudData = new DustCloudData(totalDustClouds);
		
		generateDustClouds(bufferBuilder, randomsource);
		
		int numberOfDustClouds = dustClouds;
		for(SpiralArm arm : spiralArms) //Draw each arm
		{
			arm.generateDustClouds(bufferBuilder, axisRotation, dustCloudData, dustCloudInfo, randomsource, numberOfDustClouds, sizeMultiplier);
			numberOfDustClouds += arm.armDustClouds();
		}
		
		return bufferBuilder.end();
	}
	
	public StarField setupDustCloudBuffer()
	{
		if(dustCloudBuffer != null)
			dustCloudBuffer.close();
		
		dustCloudBuffer = new DustCloudBuffer();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer;
		
		bufferbuilder$renderedbuffer = generateDustCloudBuffer(bufferBuilder);
		
		dustCloudBuffer.bind();
		dustCloudBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
		
		return this;
	}
	
	
	
	public void renderDustClouds(ViewCenter viewCenter, ClientLevel level, float partialTicks, PoseStack stack, Camera camera,
								 Matrix4f projectionMatrix, Runnable setupFog, float brightness)
	{
		SpaceCoords difference = viewCenter.getCoords().sub(getCoords());
		
		if(dustCloudBuffer == null)
			setupDustCloudBuffer();
		
		if(brightness > 0.0F)
		{
			stack.pushPose();
			
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			RenderSystem.setShaderColor(1, 1, 1, brightness);
			RenderSystem.setShaderTexture(0, getDustCloudTexture());
			FogRenderer.setupNoFog();
			
			Quaternionf q = SpaceCoords.getQuaternionf(level, viewCenter, partialTicks);
			
			stack.mulPose(q);
			
			this.dustCloudBuffer.bind();
			this.dustCloudBuffer.drawWithShader(stack.last().pose(), projectionMatrix, difference, StellarViewShaders.starDustCloudShader());
			VertexBuffer.unbind();
			
			setupFog.run();
			stack.popPose();
		}
	}
	
	@Override
	public void render(ViewCenter viewCenter, ClientLevel level, float partialTicks, PoseStack stack, Camera camera,
			Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog, BufferBuilder bufferbuilder,
			Vector3f parentVector, AxisRotation parentRotation)
	{
		SpaceCoords difference = viewCenter.getCoords().sub(getCoords());
		
		if(requiresSetup())
			setupBuffer();
		else if(requiresReset())
			setStarBuffer();
		
		float starBrightness = StarLike.getStarBrightness(viewCenter, level, camera, partialTicks);
		
		if(!GeneralConfig.disable_stars.get() && starBrightness > 0.0F)
		{
			stack.pushPose();
			
			//stack.translate(0, 0, 0);
			if(hasTexture)
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			RenderSystem.setShaderColor(1, 1, 1, starBrightness);
			if(hasTexture)
				RenderSystem.setShaderTexture(0, getStarInfo().getStarTexture());
			FogRenderer.setupNoFog();
			
			Quaternionf q = SpaceCoords.getQuaternionf(level, viewCenter, partialTicks);
			
			stack.mulPose(q);
			this.starBuffer.bind();
			this.starBuffer.drawWithShader(stack.last().pose(), projectionMatrix, difference, hasTexture ? StellarViewShaders.starTexShader() : StellarViewShaders.starShader());
			VertexBuffer.unbind();
			
			setupFog.run();
			stack.popPose();
		}
		
		for(SpaceObject child : children)
		{
			child.render(viewCenter, level, partialTicks, stack, camera, projectionMatrix, isFoggy, setupFog, bufferbuilder, parentVector, new AxisRotation(0, 0, 0));
		}
	}
	
	public static float dustCloudBrightness(ViewCenter viewCenter, ClientLevel level, Camera camera, float partialTicks)
	{
		float brightness = level.getStarBrightness(partialTicks);
		
		if(viewCenter.starsAlwaysVisible() && brightness < 0.5F)
			brightness = 0.5F;
		
		if(GeneralConfig.bright_stars.get())
			brightness = brightness * StellarView.lightSourceDustCloudDimming(level, camera);
		
		brightness = brightness * StellarView.rainDimming(level, partialTicks);
		
		return brightness;
	}
	
	public static class SpiralArm
	{
		@Nullable
		protected final DustCloudInfo dustCloudInfo;
		protected final int armDustClouds;
		
		protected final int armStars;
		protected final double armRotation;
		protected final double armLength;
		protected final double armThickness;
		protected final boolean clumpStarsInCenter;
		
		public static final Codec<SpiralArm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("dust_clouds", 0).forGetter(SpiralArm::armDustClouds),
				DustCloudInfo.CODEC.optionalFieldOf("dust_cloud_info").forGetter(SpiralArm::getDustCloudInfo),
				
				Codec.INT.fieldOf("stars").forGetter(SpiralArm::armStars),
				Codec.DOUBLE.fieldOf("arm_rotation").forGetter(SpiralArm::armRotation),
				Codec.DOUBLE.fieldOf("arm_length").forGetter(SpiralArm::armLength),
				Codec.DOUBLE.fieldOf("arm_thickness").forGetter(SpiralArm::armThickness),
				Codec.BOOL.optionalFieldOf("clump_stars_in_center", true).forGetter(SpiralArm::clumpStarsInCenter)
				).apply(instance, SpiralArm::new));
		
		public SpiralArm(int armDustClouds, Optional<DustCloudInfo> dustCloudInfo, int armStars, double armRotationDegrees, double armLength, double armThickness, boolean clumpStarsInCenter)
		{
			this.armDustClouds = armDustClouds;
			if(dustCloudInfo.isPresent())
				this.dustCloudInfo = dustCloudInfo.get();
			else
				this.dustCloudInfo = null;
			
			this.armStars = armStars;
			this.armRotation = Math.toRadians(armRotationDegrees);
			this.armLength = armLength;
			this.armThickness = armThickness;
			
			this.clumpStarsInCenter = clumpStarsInCenter;
		}
		
		public int armDustClouds()
		{
			return armDustClouds;
		}
		
		public Optional<DustCloudInfo> getDustCloudInfo()
		{
			return Optional.ofNullable(dustCloudInfo);
		}
		
		public int armStars()
		{
			return armStars;
		}
		
		public double armRotation()
		{
			return armRotation;
		}
		
		public double armLength()
		{
			return armLength;
		}
		
		public double armThickness()
		{
			return armThickness;
		}
		
		public boolean clumpStarsInCenter()
		{
			return clumpStarsInCenter;
		}
		
		protected void generateStars(BufferBuilder bufferBuilder, AxisRotation axisRotation, StarData starData, StarInfo starInfo, RandomSource randomsource, int numberOfStars, double sizeMultiplier, boolean hasTexture)
		{
			for(int i = 0; i < armStars; i++)
			{
				// Milky Way is 90 000 ly across
				
				double progress = (double) i / armStars;
				
				double phi = armLength * Math.PI * progress - armRotation;
				double r = StellarCoordinates.spiralR(5, phi, armRotation);
				
				// This generates random coordinates for the Star close to the camera
				double distance = clumpStarsInCenter ? randomsource.nextDouble() : Math.cbrt(randomsource.nextDouble());
				double theta = randomsource.nextDouble() * 2F * Math.PI;
				double sphericalphi = Math.acos(2F * randomsource.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens

				Vector3d cartesian = new SphericalCoords(distance * armThickness, theta, sphericalphi).toCartesianD();
				
				double x =  r * Math.cos(phi) + cartesian.x * armThickness / (progress * 1.5);
				double z =  r * Math.sin(phi) + cartesian.z * armThickness / (progress * 1.5);
				double y =  cartesian.y * armThickness / (progress * 1.5);
				
				cartesian.x = x * sizeMultiplier;
				cartesian.y = y * sizeMultiplier;
				cartesian.z = z * sizeMultiplier;
				
				axisRotation.quaterniond().transform(cartesian);
				
				starData.newStar(starInfo, bufferBuilder, randomsource, cartesian.x, cartesian.y, cartesian.z, hasTexture, numberOfStars + i);
			}
		}
		
		protected void generateDustClouds(BufferBuilder bufferBuilder, AxisRotation axisRotation, DustCloudData dustCloudData, DustCloudInfo dustCloudInfo, RandomSource randomsource, int numberOfDustClouds, double sizeMultiplier)
		{
			for(int i = 0; i < armDustClouds; i++)
			{
				// Milky Way is 90 000 ly across
				
				double progress = (double) i / armDustClouds;
				
				double phi = armLength * Math.PI * progress - armRotation;
				double r = StellarCoordinates.spiralR(5, phi, armRotation);
				progress++;
				
				// This generates random coordinates for the Star close to the camera
				double distance = clumpStarsInCenter ? randomsource.nextDouble() : Math.cbrt(randomsource.nextDouble());
				double theta = randomsource.nextDouble() * 2F * Math.PI;
				double sphericalphi = Math.acos(2F * randomsource.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens
				
				Vector3d cartesian = new SphericalCoords(distance * armThickness, theta, sphericalphi).toCartesianD();
				
				double x =  r * Math.cos(phi) + cartesian.x * armThickness / (progress * 1.5);
				double z =  r * Math.sin(phi) + cartesian.z * armThickness / (progress * 1.5);
				double y =  cartesian.y * armThickness / (progress * 1.5);
				
				cartesian.x = x * sizeMultiplier;
				cartesian.y = y * sizeMultiplier;
				cartesian.z = z * sizeMultiplier;
				
				axisRotation.quaterniond().transform(cartesian);
				
				dustCloudData.newDustCloud(this.dustCloudInfo == null ? dustCloudInfo : this.dustCloudInfo, bufferBuilder, randomsource, cartesian.x, cartesian.y, cartesian.z, (1 / progress) + 0.2, numberOfDustClouds + i);
			}
		}
	}
}
