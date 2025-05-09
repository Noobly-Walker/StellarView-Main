package net.povstalec.stellarview.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public abstract class StellarViewConfigValue
{
	public static class BooleanValue
	{
		public ModConfigSpec.BooleanValue boolean_value;
		
		public BooleanValue(ModConfigSpec.Builder builder, String path, boolean defaultValue, String comment)
		{
			this.boolean_value = builder
					.comment(comment)
					.define(path, defaultValue);
		}
		
		public void set(boolean value)
		{
			boolean_value.set(value);
			boolean_value.save();
		}
		
		public boolean get()
		{
			return boolean_value.get();
		}
		
		public boolean getDefault()
		{
			return boolean_value.getDefault();
		}
		
	}
	
	public static class IntValue
	{
		public ModConfigSpec.IntValue int_value;
		protected int min;
		protected int max;
		
		public IntValue(ModConfigSpec.Builder builder, String path, int defaultValue, int min, int max, String comment)
		{
			this.int_value = builder
					.comment(comment)
					.defineInRange(path, defaultValue, min, max);
			this.min = min;
			this.max = max;
		}
		
		public void set(int value)
		{
			int_value.set(value);
			int_value.save();
		}
		
		public int get()
		{
			return int_value.get();
		}
		
		public int getDefault()
		{
			return int_value.getDefault();
		}
		
		public int getMin()
		{
			return this.min;
		}
		
		public int getMax()
		{
			return this.max;
		}
	}
	
	public static class LongValue
	{
		public ModConfigSpec.LongValue long_value;
		
		public LongValue(ModConfigSpec.Builder builder, String path, long defaultValue, long min, long max, String comment)
		{
			this.long_value = builder
					.comment(comment)
					.defineInRange(path, defaultValue, min, max);
		}
		
		public void set(long value)
		{
			long_value.set(value);
			long_value.save();
		}
		
		public long get()
		{
			return long_value.get();
		}
		
		public long getDefault()
		{
			return long_value.getDefault();
		}
	}
	
	public static class RGBAValue
	{
		public ModConfigSpec.IntValue red_value;
		public ModConfigSpec.IntValue green_value;
		public ModConfigSpec.IntValue blue_value;
		public ModConfigSpec.IntValue alpha_value;
		
		public RGBAValue(ModConfigSpec.Builder builder, String path, int red, int blue, int green, int alpha, String comment)
		{
			builder.comment(comment);
			this.red_value = builder
					.defineInRange(path + ".red", red, 0, 255);
			this.green_value = builder
					.defineInRange(path + ".green", blue, 0, 255);
			this.blue_value = builder
					.defineInRange(path + ".blue", green, 0, 255);
			this.alpha_value = builder
					.defineInRange(path + ".alpha", alpha, 0, 255);
		}
		
		public void setRed(int value)
		{
			red_value.set(value);
			red_value.save();
		}
		
		public void setGreen(int value)
		{
			green_value.set(value);
			green_value.save();
		}
		
		public void setBlue(int value)
		{
			blue_value.set(value);
			blue_value.save();
		}
		
		public void setAlpha(int value)
		{
			alpha_value.set(value);
			alpha_value.save();
		}
		
		public int getRed()
		{
			return red_value.get();
		}
		
		public int getGreen()
		{
			return green_value.get();
		}
		
		public int getBlue()
		{
			return blue_value.get();
		}
		
		public int getAlpha()
		{
			return alpha_value.get();
		}
		
		public int getRedDefault()
		{
			return red_value.getDefault();
		}
		
		public int getGreenDefault()
		{
			return green_value.getDefault();
		}
		
		public int getBlueDefault()
		{
			return blue_value.getDefault();
		}
		
		public int getAlphaDefault()
		{
			return alpha_value.getDefault();
		}
	}
}
