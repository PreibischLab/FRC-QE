package net.preibisch.qualityest;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import autopilot.image.DoubleArrayImage;
import autopilot.measures.FocusMeasureInterface;
import autopilot.measures.implementations.spectral.NormDCTEntropyShannon;
import autopilot.measures.implementations.spectral.NormDCTEntropyShannonMedianFiltered;
import autopilot.measures.implementations.spectral.NormDFTEntropyShannon;
import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog.Result;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.KDTree;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.preibisch.mvrecon.fiji.plugin.util.GUIHelper;
import net.preibisch.mvrecon.process.export.DisplayImage;
import net.preibisch.mvrecon.process.quality.FRCRealRandomAccessible;
import net.preibisch.mvrecon.process.quality.FRCTools;

public class Estimate_Quality implements PlugIn
{
	public static int defaultImg = 0;
	public static int defaultRFRCDist = 10;
	public static int defaultFRCStepSize = 1;
	public static int defaultFFTSize = 256;
	public static boolean defaultVisualize = false;

	public static long[] defaultMin, defaultMax;

	public static final String[] methodChoices = new String[] {
			"relative FRC (Fourier Ring Correlation)",
			"Normalized DCT Entropy (Shannon)",
			"Normalized DCT Entropy (Shannon), median filtered",
			"Normalized DFT Entropy (Shannon)" };
	public static int defaultMethodChoice = 0;

	public static int defaultAreaChoice = -1;
	public static final String[] areaChoices = new String[] {
			"Entire image",
			"Use selected 2D ROI (if available)",
			"Define interactively (3D with BDV)" };

	@Override
	public void run( String arg )
	{
		// get list of open image stacks
		final int[] idList = WindowManager.getIDList();

		if ( idList == null || idList.length == 0 )
		{
			IJ.error( "You need at least one open 3d image." );
			return;
		}

		// map all id's to image title for those who are 3d stacks
		final String[] imgList =
				Arrays.stream( idList ).
					//filter( id -> WindowManager.getImage( id ).getStackSize() > 1  ). // Cannot check here as id's are mixed up then
						mapToObj( id -> WindowManager.getImage( id ).getTitle() ).
							toArray( String[]::new );

		if ( defaultImg >= imgList.length )
			defaultImg = 0;

		GenericDialog gd = new GenericDialog( "Quality-Estimation" );

		gd.addChoice( "Image_stack for quality estimation", imgList, imgList[ defaultImg ] );
		gd.addChoice( "Quality_method", methodChoices, methodChoices[ defaultMethodChoice ] );

		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		// don't do it by name as often multiple images have the same name
		final ImagePlus imp = WindowManager.getImage( idList[ defaultImg = gd.getNextChoiceIndex() ] );

		final int methodChoice = defaultMethodChoice = gd.getNextChoiceIndex();

		if ( imp.getNSlices() == 1 )
		{
			IJ.log( "The image has only one slice, cannot perform quality estimation\n"
					+ " (Please check Image>Properties and set it right if necessary)." );
			return;
		}

		if ( imp.getNFrames() > 1 )
		{
			IJ.log( "The image has multiple frames, please duplicate a single timepoint to run\n"
					+ " (Please call Image>Duplicate and select a single timepoint)." );
			return;
		}

		// do not bother with multichannel images for now
		if ( imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB || imp.getNChannels() != 1 )
		{
			IJ.log( "The selected image has multiple channels, please split them and process individually\n"
					+ " (Please check Image>Color>Split Channels)." );
			return;
		}

		// check if the user has set a rectangular Region of Interest (ROI)
		if ( imp.getRoi() != null && imp.getRoi().getType() != Roi.RECTANGLE )
		{
			IJ.log( "Only rectangular rois are supported..." );
			return;
		}

		final Rectangle rect;

		if ( imp.getRoi() == null )
		{
			rect = null;
			if ( defaultAreaChoice == -1 || defaultAreaChoice == 1 )
				defaultAreaChoice = 0;
		}
		else
		{
			rect = imp.getRoi().getBounds();
			if ( defaultAreaChoice == -1 )
				defaultAreaChoice = 1;
		}

		gd = new GenericDialog( "Quality-Estimation Paramters" );
		gd.addChoice( "Area_for_quality estimation", areaChoices, areaChoices[ defaultAreaChoice ] );

		if ( methodChoice == 0 )
		{
			gd.addMessage( "Note: XY size should be identical across experiments you want to compare.", GUIHelper.mediumstatusfont, Color.RED );
	
			gd.addMessage(
					"Entire image: " + imp.getWidth() + "x" + imp.getHeight() + " [x" + imp.getStackSize() + "] px; " +
					(rect == null ? 
					"No ROI selected" :
					"Selected ROI: " + rect.width + "x" + rect.height + " [x" + imp.getStackSize() + "] px" ),
					GUIHelper.smallStatusFont );
	
			gd.addMessage( "" );
	
			gd.addNumericField( "FFT_size (xy)", defaultFFTSize, 0 );
			gd.addNumericField( "Step_size (z)", defaultFRCStepSize, 0 );
			gd.addNumericField( "Relative_FRC_distance (z)", defaultRFRCDist, 0 );
			gd.addCheckbox( "Visualize result as image", defaultVisualize );
		}

		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		final int areaChoice = defaultAreaChoice = gd.getNextChoiceIndex();

		if ( areaChoice == 1 && rect == null )
		{
			IJ.log( "You chose to use the ROI but no ROI selected ... stopping." );
			return;
		}

		final Interval frcInterval;

		if ( areaChoice == 0 )
			frcInterval = new FinalInterval( new long[] { 0, 0, 0 }, new long[] { imp.getWidth() - 1, imp.getHeight() - 1, imp.getStackSize() - 1 } );
		else if ( areaChoice == 1 )
			frcInterval = new FinalInterval( new long[] { rect.x, rect.y, 0 }, new long[] { rect.x + rect.width - 1, rect.y + rect.height - 1, imp.getStackSize() - 1 } );
		else
			frcInterval = interactiveROI( (RandomAccessibleInterval)ImageJFunctions.wrapReal( imp ), imp.getDisplayRangeMin(), imp.getDisplayRangeMax() );

		if ( frcInterval == null )
			return;

		if ( methodChoice == 0 )
		{
			final int fftSize = defaultFFTSize = (int)Math.round( gd.getNextNumber() );
			final int zStepSize = defaultFRCStepSize = (int)Math.round( gd.getNextNumber() );
			final int rFRCDist = defaultRFRCDist = (int)Math.round( gd.getNextNumber() );
			final boolean visualize = defaultVisualize = gd.getNextBoolean();

			if ( frcInterval.dimension( 2 ) < 2 * rFRCDist + 1 )
			{
				IJ.log( "z-size (" + frcInterval.dimension( 2 ) + ") is too small given the relative FRC distance (" + rFRCDist + "), should be at least " + (2 * rFRCDist + 1) );
				return;
			}

			computeRFRC( Views.interval( (RandomAccessibleInterval)ImageJFunctions.wrapReal( imp ), frcInterval), zStepSize, fftSize, rFRCDist, visualize );
		}
		else
		{
			computeShannon( Views.interval( (RandomAccessibleInterval)ImageJFunctions.wrapReal( imp ), frcInterval), methodChoice );
		}

	}

	public < T extends RealType< T > > void computeShannon( final RandomAccessibleInterval< T > input, final int methodChoice )
	{
		final FocusMeasureInterface measure;
		final String measureDesc;

		if ( methodChoice == 1 )
		{
			measure = new NormDCTEntropyShannon();
			measureDesc = "DCT (Shannon)";
		}
		else if ( methodChoice == 2 )
		{
			measure = new NormDCTEntropyShannonMedianFiltered();
			measureDesc = "median DCT (Shannon)";
		}
		else
		{
			measure = new NormDFTEntropyShannon();
			measureDesc = "DFT (Shannon)";
		}

		final RandomAccessibleInterval<DoubleType> di = Converters.convert(
				input,
				new Converter<T, DoubleType>()
				{
					@Override
					public void convert(T input, DoubleType output) { output.setReal( input.getRealDouble() ); }
				},
				new DoubleType() );

		final ResultsTable rt = new ResultsTable();
		float[] x = new float[ (int)input.dimension( 2 ) ]; // x-coordinates
		float[] y = new float[ (int)input.dimension( 2 ) ]; // y-coordinates

		double max = -Double.MAX_VALUE;
		double min = Double.MAX_VALUE;

		IJ.showProgress(0, (int)input.dimension( 2 ) );

		for ( int z = 0; z < input.dimension( 2 ); ++z )
		{
			final RandomAccessibleInterval<DoubleType>  slice = Views.hyperSlice(di, 2, z );

			final double[] array = new double[ (int)slice.dimension( 0 ) * (int)slice.dimension( 1 ) ];

			final Cursor<DoubleType> c = Views.flatIterable( slice ).cursor();
			
			for ( int i = 0; i < array.length; ++i )
				array[ i ] = c.next().get();

			final double value = measure.computeFocusMeasure( new DoubleArrayImage((int)slice.dimension( 0 ), (int)slice.dimension( 1 ), array ) );

			IJ.showProgress(z, (int)input.dimension( 2 ) );

			min = Math.min( value, min );
			max = Math.max( value, max );

			x[ (int)z ] = z;
			y[ (int)z ] = (float)value;

			rt.incrementCounter();
			rt.addValue( "z", z );
			rt.addValue( "quality", value );
		}

		IJ.showProgress(1.0);

		rt.show("Image Quality (" + measureDesc + ")");

		PlotWindow.noGridLines = false; // draw grid lines
		Plot plot = new Plot("Image Quality Plot (" + measureDesc + ")","z Position","Quality",x,y);
		plot.setLimits( input.min( 2 ), input.max( 2 ), Math.min( 0, min ), max );
		plot.setLineWidth(2);
		plot.show();
	}

	public < T extends RealType< T > > void computeRFRC( final RandomAccessibleInterval< T > input, final int zStepSize, final int fftSize, final int rFRCDist, final boolean visualize )
	{
		final ArrayList< Point > locations = new ArrayList<>();

		final ArrayList< Pair< Long, Long > > xyPositions = FRCTools.distributeSquaresXY( input, fftSize, 0.25 );

		IJ.log( "For the following coordinates rFRC will be computed: " );

		for ( final Pair< Long, Long > xy : xyPositions )
			IJ.log( "x,y: " + xy.getA() + "," + xy.getB() + " (size around each spot r=" + fftSize/2 + ")" );

		for ( int z = rFRCDist; z < input.dimension( 2 ) - rFRCDist; z += zStepSize )
			for ( final Pair< Long, Long > xy : xyPositions )
				locations.add( new Point( xy.getA(), xy.getB(), z ) );

		final FRCRealRandomAccessible< T > frc = new FRCRealRandomAccessible< T >( input, locations, fftSize, true, true, null );

		final NearestNeighborSearch< FloatType > search = new NearestNeighborSearchOnKDTree<>( new KDTree<>( frc.getQualityList() ) );

		final ResultsTable rt = new ResultsTable();
		float[] x = new float[ (int)input.dimension( 2 ) ]; // x-coordinates
		float[] y = new float[ (int)input.dimension( 2 ) ]; // x-coordinates

		double maxMedian = 0;
		double minMedian = Double.MAX_VALUE;

		for ( long z = input.min( 2 ); z <= input.max( 2 ); ++z )
		{
			double[] values = new double[ xyPositions.size() ];
			int i = 0;

			for ( final Pair< Long, Long > xy : xyPositions )
			{
				search.search( new Point( xy.getA(), xy.getB(), z ) );
				values[ i++ ] = search.getSampler().get().get();
			}

			final double median = Util.median( values );

			minMedian = Math.min( median, minMedian );
			maxMedian = Math.max( median, maxMedian );

			x[ (int)z ] = z;
			y[ (int)z ] = (float)median;

			rt.incrementCounter();
			rt.addValue( "z", z );
			rt.addValue( "quality", median );
		}

		rt.show("Image Quality");

		PlotWindow.noGridLines = false; // draw grid lines
		Plot plot = new Plot("Image Quality Plot (rFRC)","z Position","Quality",x,y);
		plot.setLimits( input.min( 2 ), input.max( 2 ), 0/*minMedian*/, maxMedian );
		plot.setLineWidth(2);
		plot.show();

		if ( visualize )
			DisplayImage.getImagePlusInstance( frc.getRandomAccessibleInterval(), false, "Quality Rendering", Double.NaN, Double.NaN ).show();
	}

	protected < T extends RealType< T > > Interval interactiveROI( final RandomAccessibleInterval< T > img, final double min, final double max )
	{
		BdvOptions options = Bdv.options().numSourceGroups( 1 ).frameTitle( "Preview" ).numRenderingThreads( 8 );
		BdvStackSource< ? > preview = BdvFunctions.show( img, "weights", options );
		preview.setDisplayRange( min, max );

		if ( defaultMin == null || defaultMax == null )
		{
			defaultMin = new long[] { img.min( 0 ) + 25, img.min( 1 ) + 25, img.min( 2 ) };
			defaultMax = new long[] { img.max( 0 ) - 25, img.max( 1 ) - 25, img.max( 2 ) };
		}

		for ( int d = 0; d < defaultMin.length; ++d )
		{
			if ( defaultMin[ d ] >= defaultMax[ d ] )
			{
				defaultMin[ d ] = img.min( d );
				defaultMax[ d ] = img.max( d );
			}

			defaultMin[ d ] = Math.max( defaultMin[ d ], img.min( d ) );
			defaultMax[ d ] = Math.min( defaultMax[ d ], img.max( d ) );
		}

		final Interval initialInterval = Intervals.createMinMax( defaultMin[ 0 ], defaultMin[ 1 ], defaultMin[ 2 ], defaultMax[ 0 ], defaultMax[ 1 ], defaultMax[ 2 ] ); // the initially selected bounding box
		final Interval rangeInterval = Intervals.createMinMax( img.min( 0 ), img.min( 1 ), img.min( 2 ), img.max( 0 ), img.max( 1 ), img.max( 2 ) ); // the range (bounding box of possible bounding boxes)

		final BoxSelectionOptions bboptions = new BoxSelectionOptions().title( "Select 3D area" );

		final TransformedBoxSelectionDialog dialog =
				new TransformedBoxSelectionDialog(
						preview.getBdvHandle().getViewerPanel(),
						preview.getBdvHandle().getSetupAssignments(),
						new InputTriggerConfig(),
						preview.getBdvHandle().getTriggerbindings(),
						new AffineTransform3D(), initialInterval, rangeInterval, bboptions );

		dialog.setVisible( true );

		final Result result = dialog.getResult();

		preview.close();

		if ( result.isValid() )
			return result.getInterval();
		else
			return null;
	}

	public static void main( String[] args )
	{
		// for testing from command line / eclipse
		new ImageJ();

		final ImagePlus imp;

		if ( args == null || args.length == 0 )
			imp = new ImagePlus( "/Users/spreibi/Documents/BIMSB/Publications/NM Perspective Clearing/anisotropic_FRC_example/Fructose_org4.tif" );
		else
			imp = new ImagePlus( args[ 0 ] );

		imp.show();
		imp.setSlice( imp.getNSlices() / 2 );
		imp.setRoi( new Rectangle( 10, 10, 200, 200 ) );

		Estimate_Quality.defaultAreaChoice = 0;
		new Estimate_Quality().run( null );
	}
}
