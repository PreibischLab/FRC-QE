package autopilot.measures;

/**
 * Class with static methods and convenience methods for managing all focus
 * measures
 * 
 * @author royer
 */
public class FocusMeasures
{
	/*
	 The following constants ae the default values for focus measure parameters
	**/
	private static final int cNumberOfBins = 512;
	public static final int cNumberOfAngleBins = 8;
	public static double cPSFSupportDiameter = 3;
	public static double cOTFFilterRatio = 1 / cPSFSupportDiameter;
	public static double cDCFilterRatio = 0.01;
	public static double cLowHighFreqRatio = 0.5;
	public static int cBlockSize = 7;
	public static int cNumberOfTiles = 8;
	public static int cExponent = 4;
}
