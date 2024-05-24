package net.povstalec.stellarview.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class OverworldConfig
{
	public static final String PREFIX = "client.";
	
	public static StellarViewConfigValue.BooleanValue disable_sun;

	public static StellarViewConfigValue.BooleanValue disable_moon;
	public static StellarViewConfigValue.BooleanValue disable_moon_phases;

	public static StellarViewConfigValue.IntValue meteor_shower_chance;
	public static StellarViewConfigValue.IntValue shooting_star_chance;
	
	public static StellarViewConfigValue.IntValue milky_way_x;
	public static StellarViewConfigValue.IntValue milky_way_y;
	public static StellarViewConfigValue.IntValue milky_way_z;

	public static StellarViewConfigValue.IntValue overworld_year_length;
	
	public static StellarViewConfigValue.IntValue overworld_z_rotation_multiplier;
	
	public static StellarViewConfigValue.IntValue milky_way_x_axis_rotation;
	public static StellarViewConfigValue.IntValue milky_way_y_axis_rotation;
	public static StellarViewConfigValue.IntValue milky_way_z_axis_rotation;
	
	public static void init(ForgeConfigSpec.Builder client)
	{
		disable_sun = new StellarViewConfigValue.BooleanValue(client, PREFIX + "disable_sun", 
				false, 
				"Disables the Sun");
		
		disable_moon = new StellarViewConfigValue.BooleanValue(client, PREFIX + "disable_moon", 
				false, 
				"Disables the Moon");
		disable_moon_phases = new StellarViewConfigValue.BooleanValue(client, PREFIX + "disable_moon_phases", 
				false, 
				"Disables Moon phases");
		
		
		
		meteor_shower_chance = new StellarViewConfigValue.IntValue(client, PREFIX + "meteor_shower_chance", 
				10, 0, 100, 
				"Chance of a meteor shower happening each day");
		shooting_star_chance = new StellarViewConfigValue.IntValue(client, PREFIX + "shooting_star_chance", 
				10, 0, 100, 
				"Chance of a shooting star appearing each 1000 ticks");
		
		
		
		overworld_year_length = new StellarViewConfigValue.IntValue(client, PREFIX + "overworld_year_length", 
				96, 1, 512, 
				"Specifies the number of days it takes for the Earth to complete one orbit around the Sun");

		overworld_z_rotation_multiplier = new StellarViewConfigValue.IntValue(client, "client.overworld_z_rotation_multiplier", 
				3000, 1, 3000, 
				"Controls how much the Overworld sky rotates when moving along the Z-axis");
		
		
		
		milky_way_x = new StellarViewConfigValue.IntValue(client, PREFIX + "milky_way_x", 
				0, -45, 45, 
				"Specifies Milky Way X position");
		milky_way_y = new StellarViewConfigValue.IntValue(client, PREFIX + "milky_way_y", 
				0, -45, 45, 
				"Specifies Milky Way Y position");
		milky_way_z = new StellarViewConfigValue.IntValue(client, PREFIX + "milky_way_z", 
				16, -45, 45, 
				"Specifies Milky Way Z position");

		milky_way_x_axis_rotation = new StellarViewConfigValue.IntValue(client, PREFIX + "milky_way_x_axis_rotation", 
				18, 0, 360, 
				"Specifies Milky Way Alpha rotation");
		milky_way_y_axis_rotation = new StellarViewConfigValue.IntValue(client, PREFIX + "milky_way_y_axis_rotation", 
				0, 0, 360, 
				"Specifies Milky Way Beta rotation");
		milky_way_z_axis_rotation = new StellarViewConfigValue.IntValue(client, PREFIX + "milky_way_z_axis_rotation", 
				90, 0, 360, 
				"Specifies Milky Way Gamma rotation");
	}
}
