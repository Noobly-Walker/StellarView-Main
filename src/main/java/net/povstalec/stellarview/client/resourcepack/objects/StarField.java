package net.povstalec.stellarview.client.resourcepack.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.nbt.CompoundTag;
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
import net.povstalec.stellarview.client.render.shader.StellarViewShaders;
import net.povstalec.stellarview.client.render.shader.StellarViewVertexFormat;
import net.povstalec.stellarview.client.resourcepack.StarInfo;
import net.povstalec.stellarview.client.resourcepack.ViewCenter;
import net.povstalec.stellarview.common.config.GeneralConfig;

public class StarField extends SpaceObject
{
	public static final ResourceLocation DEFAULT_DUST_CLOUD_TEXTURE = new ResourceLocation(StellarView.MODID,"textures/environment/dust_cloud.png");
	
	public static final String SEED = "seed";
	public static final String DIAMETER_LY = "diameter_ly";
	public static final String STARS = "stars";
	public static final String TOTAL_STARS = "total_stars";
	public static final String STAR_INFO = "star_info";
	public static final String SPIRAL_ARMS = "spiral_arms";
	public static final String CLUMP_STARS_IN_CENTER = "clump_stars_in_center";
	public static final String X_STRETCH = "x_stretch";
	public static final String Y_STRETCH = "y_stretch";
	public static final String Z_STRETCH = "z_stretch";
	public static final String DUST_CLOUDS = "dust_clouds";
	public static final String TOTAL_DUST_CLOUDS = "total_dust_clouds";
	public static final String DUST_CLOUD_INFO = "dust_cloud_info";
	public static final String DUST_CLOUD_TEXTURE = "dust_cloud_texture";
	
	@Nullable
	protected DustCloudBuffer dustCloudBuffer;
	protected DustCloudData dustCloudData;
	protected int dustClouds;
	protected int totalDustClouds;
	protected ResourceLocation dustCloudTexture;
	
	@Nullable
	protected StarBuffer starBuffer;
	protected StarData starData;

	protected StarInfo starInfo;
	protected DustCloudInfo dustCloudInfo;
	
	protected long seed;
	protected boolean clumpStarsInCenter;
	
	protected int diameter;
	protected int stars;
	
	protected double xStretch;
	protected double yStretch;
	protected double zStretch;
	
	protected ArrayList<SpiralArm> spiralArms;
	
	protected int totalStars;
	
	protected boolean hasTexture = GeneralConfig.textured_stars.get();
	
	public static final Codec<StarField> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.optionalFieldOf("parent").forGetter(StarField::getParentLocation),
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
	
	public StarField() {}
	
	public StarField(Optional<ResourceLocation> parent, Either<SpaceCoords, StellarCoordinates.Equatorial> coords, AxisRotation axisRotation,
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
	
	protected void generateStars(BufferBuilder bufferBuilder, Random random)
	{
		for(int i = 0; i < stars; i++)
		{
			// This generates random coordinates for the Star close to the camera
			double distance = clumpStarsInCenter() ? random.nextDouble() : Math.cbrt(Math.abs(random.nextDouble()));
			double theta = random.nextDouble() * 2F * Math.PI;
			double phi = Math.acos(2F * random.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens
			
			Vector3d cartesian = new SphericalCoords(distance * getDiameter(), theta, phi).toCartesianD();
			
			cartesian.x *= xStretch;
			cartesian.y *= yStretch;
			cartesian.z *= zStretch;
			
			axisRotation.quaterniond().transform(cartesian);

			starData.newStar(starInfo, bufferBuilder, random, cartesian.x, cartesian.y, cartesian.z, hasTexture, i);
		}
	}
	
	protected void generateDustClouds(BufferBuilder bufferBuilder, Random random)
	{
		for(int i = 0; i < dustClouds; i++)
		{
			// This generates random coordinates for the Star close to the camera
			double distance = clumpStarsInCenter() ? random.nextDouble() : Math.cbrt(random.nextDouble());
			double theta = random.nextDouble() * 2F * Math.PI;
			double phi = Math.acos(2F * random.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens
			
			Vector3d cartesian = new SphericalCoords(distance * getDiameter(), theta, phi).toCartesianD();
			
			cartesian.x *= xStretch;
			cartesian.y *= yStretch;
			cartesian.z *= zStretch;
			
			axisRotation.quaterniond().transform(cartesian);
			
			dustCloudData.newDustCloud(dustCloudInfo, bufferBuilder, random, cartesian.x, cartesian.y, cartesian.z, 1, i);
		}
	}
	
	protected RenderedBuffer generateStarBuffer(BufferBuilder bufferBuilder, Random random)
	{
		bufferBuilder.begin(VertexFormat.Mode.QUADS, hasTexture ? StellarViewVertexFormat.STAR_POS_COLOR_LY_TEX : StellarViewVertexFormat.STAR_POS_COLOR_LY);
		
		double sizeMultiplier = diameter / 30D;
		
		starData = new StarData(totalStars);
		
		generateStars(bufferBuilder, random);
		
		int numberOfStars = stars;
		for(SpiralArm arm : spiralArms) //Draw each arm
		{
			arm.generateStars(bufferBuilder, axisRotation, starData, starInfo, random, numberOfStars, sizeMultiplier, hasTexture);
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
		
		bufferbuilder$renderedbuffer = generateStarBuffer(bufferBuilder, new Random(getSeed()));
		
		starBuffer.bind();
		starBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
		
		return this;
	}
	
	
	
	protected RenderedBuffer generateDustCloudBuffer(BufferBuilder bufferBuilder, Random random)
	{
		bufferBuilder.begin(VertexFormat.Mode.QUADS, StellarViewVertexFormat.STAR_POS_COLOR_LY_TEX);
		
		double sizeMultiplier = diameter / 30D;
		
		dustCloudData = new DustCloudData(totalDustClouds);
		
		generateDustClouds(bufferBuilder, random);
		
		int numberOfDustClouds = dustClouds;
		for(SpiralArm arm : spiralArms) //Draw each arm
		{
			arm.generateDustClouds(bufferBuilder, axisRotation, dustCloudData, dustCloudInfo, random, numberOfDustClouds, sizeMultiplier);
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
		
		bufferbuilder$renderedbuffer = generateDustCloudBuffer(bufferBuilder, new Random(getSeed()));
		
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
	
	@Override
	public void fromTag(CompoundTag tag)
	{
		super.fromTag(tag);
		
		dustClouds = tag.getInt(DUST_CLOUDS);
		totalDustClouds = tag.getInt(TOTAL_DUST_CLOUDS);
		dustCloudTexture = new ResourceLocation(tag.getString(DUST_CLOUD_TEXTURE));
		
		seed = tag.getLong(SEED);
		
		diameter = tag.getInt(DIAMETER_LY);
		stars = tag.getInt(STARS);
		totalStars = tag.getInt(TOTAL_STARS);
		
		clumpStarsInCenter = tag.getBoolean(CLUMP_STARS_IN_CENTER);
		
		xStretch = tag.getDouble(X_STRETCH);
		yStretch = tag.getDouble(Y_STRETCH);
		zStretch = tag.getDouble(Z_STRETCH);
		
		this.spiralArms = new ArrayList<SpiralArm>();
		CompoundTag armsTag = tag.getCompound(SPIRAL_ARMS);
		for(int i = 0; i < armsTag.size(); i++)
		{
			SpiralArm arm = new SpiralArm();
			arm.fromTag(armsTag.getCompound("spiral_arm_" + i));
			spiralArms.add(arm);
		}
		
		this.starInfo = StarInfo.fromTag(tag.getCompound(STAR_INFO));
		
		this.dustCloudInfo = DustCloudInfo.fromTag(tag.getCompound(DUST_CLOUD_INFO));
	}
	
	
	
	public static class SpiralArm
	{
		public static final String STARS = "stars";
		public static final String ARM_ROTATION = "arm_rotation";
		public static final String ARM_LENGTH = "arm_length";
		public static final String ARM_THICKNESS = "arm_thickness";
		public static final String CLUMP_STARS_IN_CENTER = "clump_stars_in_center";
		
		@Nullable
		protected DustCloudInfo dustCloudInfo;
		protected int armDustClouds;
		
		protected int armStars;
		protected double armRotation;
		protected double armLength;
		protected double armThickness;
		protected boolean clumpStarsInCenter;
		
		public static final Codec<SpiralArm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf(DUST_CLOUDS, 0).forGetter(SpiralArm::armDustClouds),
				DustCloudInfo.CODEC.optionalFieldOf(DUST_CLOUD_INFO).forGetter(SpiralArm::getDustCloudInfo),
				
				Codec.INT.fieldOf(STARS).forGetter(SpiralArm::armStars),
				Codec.DOUBLE.fieldOf(ARM_ROTATION).forGetter(SpiralArm::armRotation),
				Codec.DOUBLE.fieldOf(ARM_LENGTH).forGetter(SpiralArm::armLength),
				Codec.DOUBLE.fieldOf(ARM_THICKNESS).forGetter(SpiralArm::armThickness),
				Codec.BOOL.optionalFieldOf(CLUMP_STARS_IN_CENTER, true).forGetter(SpiralArm::clumpStarsInCenter)
		).apply(instance, SpiralArm::new));
		
		public SpiralArm() {}
		
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
		
		protected void generateStars(BufferBuilder bufferBuilder, AxisRotation axisRotation, StarData starData, StarInfo starInfo, Random random, int numberOfStars, double sizeMultiplier, boolean hasTexture)
		{
			for(int i = 0; i < armStars; i++)
			{
				// Milky Way is 90 000 ly across
				
				double progress = (double) i / armStars;
				
				double phi = armLength * Math.PI * progress - armRotation;
				double r = StellarCoordinates.spiralR(5, phi, armRotation);
				
				// This generates random coordinates for the Star close to the camera
				double distance = clumpStarsInCenter() ? random.nextDouble() : Math.cbrt(random.nextDouble());
				double theta = random.nextDouble() * 2F * Math.PI;
				double sphericalphi = Math.acos(2F * random.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens

				Vector3d cartesian = new SphericalCoords(distance * armThickness(), theta, sphericalphi).toCartesianD();
				
				double x =  r * Math.cos(phi) + cartesian.x * armThickness() / (progress * 1.5);
				double z =  r * Math.sin(phi) + cartesian.z * armThickness() / (progress * 1.5);
				double y =  cartesian.y * armThickness() / (progress * 1.5);
				
				cartesian.x = x * sizeMultiplier;
				cartesian.y = y * sizeMultiplier;
				cartesian.z = z * sizeMultiplier;
				
				axisRotation.quaterniond().transform(cartesian);
				
				starData.newStar(starInfo, bufferBuilder, random, cartesian.x, cartesian.y, cartesian.z, hasTexture, numberOfStars + i);
			}
		}
		
		protected void generateDustClouds(BufferBuilder bufferBuilder, AxisRotation axisRotation, DustCloudData dustCloudData, DustCloudInfo dustCloudInfo, Random random, int numberOfDustClouds, double sizeMultiplier)
		{
			for(int i = 0; i < armDustClouds; i++)
			{
				// Milky Way is 90 000 ly across
				
				double progress = (double) i / armDustClouds;
				
				double phi = armLength * Math.PI * progress - armRotation;
				double r = StellarCoordinates.spiralR(5, phi, armRotation);
				progress++;
				
				// This generates random coordinates for the Star close to the camera
				double distance = clumpStarsInCenter() ? random.nextDouble() : Math.cbrt(random.nextDouble());
				double theta = random.nextDouble() * 2F * Math.PI;
				double sphericalphi = Math.acos(2F * random.nextDouble() - 1F); // This prevents the formation of that weird streak that normally happens
				
				Vector3d cartesian = new SphericalCoords(distance * armThickness(), theta, sphericalphi).toCartesianD();
				
				double x =  r * Math.cos(phi) + cartesian.x * armThickness() / (progress * 1.5);
				double z =  r * Math.sin(phi) + cartesian.z * armThickness() / (progress * 1.5);
				double y =  cartesian.y * armThickness() / (progress * 1.5);
				
				cartesian.x = x * sizeMultiplier;
				cartesian.y = y * sizeMultiplier;
				cartesian.z = z * sizeMultiplier;
				
				axisRotation.quaterniond().transform(cartesian);
				
				dustCloudData.newDustCloud(this.dustCloudInfo == null ? dustCloudInfo : this.dustCloudInfo, bufferBuilder, random, cartesian.x, cartesian.y, cartesian.z, (1 / progress) + 0.2, numberOfDustClouds + i);
			}
		}
		
		public void fromTag(CompoundTag tag)
		{
			armDustClouds = tag.getInt(DUST_CLOUDS);
			
			if(tag.contains(DUST_CLOUD_INFO))
				dustCloudInfo = DustCloudInfo.fromTag(tag.getCompound(DUST_CLOUD_INFO));
			else
				dustCloudInfo = null;
			
			armStars = tag.getInt(STARS);
			
			armRotation = tag.getDouble(ARM_ROTATION);
			armLength = tag.getDouble(ARM_LENGTH);
			armThickness = tag.getDouble(ARM_THICKNESS);
			
			clumpStarsInCenter = tag.getBoolean(CLUMP_STARS_IN_CENTER);
		}
	}
}
