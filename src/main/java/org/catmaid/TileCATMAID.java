/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.catmaid;

import java.awt.image.BufferedImage;

import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorARGBFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.catmaid.Tiler.Orientation;



/**
 * <p>A standalone command line application to export image tiles representing
 * scale level 0 of the tiled scale pyramids for the CATMAID interface from
 * existing CATMAID image stacks.  The program enables to crop a box of
 * interest from an existing CATMAID stack and to export only a subset of the
 * tile set.  It is thus easily possible (and desired) to parallelize the
 * export on a cluster.</p>
 * 
 * <p>The main utility of the program is to export resliced (xyz, xzy, zyx)
 * representations of existing CATMAID stacks.</p>
 * 
 * <p>Separation of scale level 0 tile generation and scaling is necessary to
 * enable the desired level of parallelization.  One would typically generate
 * the scale level 0 tile set parallelized in volumes that cover a moderate
 * number of source tiles.  Only after a <em>z</em>-section is fully exported,
 * it can be used to generate the scale pyramid.  I.e., scaling can be
 * parallelized by <em>z</em>-section but not within a <em>z</em>-section.</p>
 * 
 * <p>The program accepts the following parameters, type and default in
 * parantheses:</p>
 * <dl>
 * <dt>sourceBaseUrl</dt>
 * <dd>base path of the source CATMAID stack (string, ""), not required if <code>sourceUrlFormat</code> includes it</dd>
 * <dt>sourceUrlFormat</dt>
 * <dd>URL format String to address CATMAID tiles(string, sourceBaseUrl + "%5$d/%8$d_%9$d_%1$d.jpg")</dd>
 * <dt>sourceWidth</dt>
 * <dd>width of the source in scale level 0 pixels in <em>xyz</em> orientation
 * (long, 0)</dd>
 * <dt>sourceHeight</dt>
 * <dd>height of the source in scale level 0 pixels in <em>xyz</em> orientation
 * (long, 0)</dd>
 * <dt>sourceDepth</dt>
 * <dd>depth of the source in scale level 0 pixels in <em>xyz</em> orientation
 * (long, 0)</dd>
 * <dt>sourceScaleLevel</dt>
 * <dd>source scale level to be used for export scale level 0 (long, 0)</dd>
 * <dt>sourceTileWidth</dt>
 * <dd>width of source image tiles in pixels (int, 256)</dd>
 * <dt>sourceTileHeight</dt>
 * <dd>height of source image tiles in pixels (int, 256)</dd>
 * <dt>sourceResXY</dt>
 * <dd>source stack <em>x,y</em>-resolution (double, 1.0 )</dd>
 * <dt>sourceResZ</dt>
 * <dd>source stack <em>z</em>-resolution (double, 1.0 )</dd>
 * 
 * <dt>minX</dt>
 * <dd>minimum <em>x</em>-coordinate of the box in the source stack to be
 * exported in scale level 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
 * <dt>minY</dt>
 * <dd>minimum <em>y</em>-coordinate of the box in the source stack to be
 * exported in scale level 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
 * <dt>minZ</dt>
 * <dd>minimum <em>z</em>-coordinate of the box in the source stack to be
 * exported in scale level 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
 * <dt>width</dt>
 * <dd>width of the the box in the source stack to be exported in scale level
 * 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
 * <dt>height</dt>
 * <dd>height of the the box in the source stack to be exported in scale level
 * 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
 * <dt>depth</dt>
 * <dd>depth of the the box in the source stack to be exported in scale level
 * 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
 * <dt>orientation</dt>
 * <dd>orientation of exported stack, possible values "xy", "xz", "zy" (string,
 * "xy")</dd>
 * <dt>tileWidth</dt>
 * <dd>width of exported image tiles in pixels (int, 256)</dd>
 * <dt>tileHeight</dt>
 * <dd>height of exported image tiles in pixels (int, 256)</dd>
 * <dt>exportMinZ</dt>
 * <dd>first <em>z</em>-section index to be exported (long, 0)</dd>
 * <dt>exportMaxZ</dt>
 * <dd>last <em>z</em>-section index to be exported (long, depth-1)</dd>
 * <dt>exportMinR</dt>
 * <dd>first row of tiles to be exported (long, 0)</dd>
 * <dt>exportMaxR</dt>
 * <dd>last row of tiles to be exported (long, depth-1)</dd>
 * <dt>exportMinC</dt>
 * <dd>first column of tiles to be exported (long, 0)</dd>
 * <dt>exportMaxC</dt>
 * <dd>last column of tiles to be exported (long, depth-1)</dd>
 * <dt>exportBasePath</dt>
 * <dd>base path for the stakc to be exported (string, "")</dd>
 * <dt>tilePattern</dt>
 * <dd>tilePattern the file name convention for export tile coordinates without
 * extension and base path, must contain "&lt;s&gt;","&lt;z&gt;", "&lt;r&gt;",
 * "&lt;c&gt;" (string, "&lt;z&gt;/&lt;r&gt;_&lt;c&gt;_&lt;s&gt;")
 * <dt>format</dt>
 * <dd>image tile file format for export, e.g. "jpg" or "png" (string,
 * "jpg")</dd>
 * <dt>quality</dt>
 * <dd>quality for export jpg-compression if format is "jpg" (float, 0.85)</dd>
 * <dt>type</dt>
 * <dd>the type of export tiles, either "rgb or "gray" (string, "rgb")</dd>
 * </dl>
 * 
 * <p>Parameters are passed as properties to the JVM virtual machine, e.g.
 * <code>./java -jar ScaleCATMAID.jar -DsourceBaseUrl=http://...</code></p>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class TileCATMAID
{
	static public enum Interpolation { NN, NL };
	
	static protected class Param
	{
		/* CATMAID source stack, representing an xyz-orientation */
		public String sourceUrlFormat;
		public long sourceWidth;
		public long sourceHeight;
		public long sourceDepth;
		public long sourceScaleLevel;
		public int sourceTileWidth;
		public int sourceTileHeight;
		public double sourceResXY;
		public double sourceResZ;
		
		/* export */
		/* source interval (crop area) in isotropic pixel coordinates */
		public Interval sourceInterval;
		public Tiler.Orientation orientation;
		public int tileWidth;
		public int tileHeight;
		public long minZ;
		public long maxZ;
		public long minR;
		public long maxR;
		public long minC;
		public long maxC;
		public String exportPath;
		public String tilePattern;
		public String format;
		public float quality;
		public int type;
		public TileCATMAID.Interpolation interpolation;
	}
	
	static protected Param parseParameters()
	{
		final Param p = new Param();
		
		/* CATMAID source stack */
		final String sourceBaseUrl = System.getProperty( "sourceBaseUrl", "" );
		p.sourceUrlFormat = System.getProperty( "sourceUrlFormat", sourceBaseUrl + "%5$d/%8$d_%9$d_%1$d.jpg" );

		p.sourceWidth = Long.parseLong( System.getProperty( "sourceWidth", "0" ) );
		p.sourceHeight = Long.parseLong( System.getProperty( "sourceHeight", "0" ) );
		p.sourceDepth = Long.parseLong( System.getProperty( "sourceDepth", "0" ) );
		p.sourceScaleLevel = Long.parseLong( System.getProperty( "sourceScaleLevel", "0" ) );

		final int scaleXYDiv = 1 << p.sourceScaleLevel;

		p.sourceTileWidth = Integer.parseInt( System.getProperty( "sourceTileWidth", "256" ) );
		p.sourceTileHeight = Integer.parseInt( System.getProperty( "sourceTileHeight", "256" ) );
		p.sourceResXY = Double.parseDouble( System.getProperty( "sourceResXY", "1.0" ) );
		p.sourceResZ = Double.parseDouble( System.getProperty( "sourceResZ", "1.0" ) );

		final double scaleZDiv = scaleXYDiv * p.sourceResXY / p.sourceResZ;

		/* export */
		final long minX = Long.parseLong( System.getProperty( "minX", "0" ) );
		final long minY = Long.parseLong( System.getProperty( "minY", "0" ) );
		final long minZ = Long.parseLong( System.getProperty( "minZ", "0" ) );
		final long width = Long.parseLong( System.getProperty( "width", "0" ) );
		final long height = Long.parseLong( System.getProperty( "height", "0" ) );
		final long depth = Long.parseLong( System.getProperty( "depth", "0" ) );
		p.sourceInterval = new FinalInterval(
				new long[]{ minX, minY, minZ },
				new long[]{ minX + width - 1, minY + height - 1, minZ + depth - 1 } );
		final FinalDimensions orientedSourceInterval;
		final String orientation = System.getProperty( "orientation", "xy" );
		if ( orientation.equalsIgnoreCase( "xz" ) )
		{
			p.orientation = Orientation.XZ;
			orientedSourceInterval = new FinalDimensions(
					p.sourceInterval.dimension( 0 ) / scaleXYDiv,
					( long )( p.sourceInterval.dimension( 2 ) / scaleZDiv ),
					p.sourceInterval.dimension( 1 ) / scaleXYDiv );
		}
		else if ( orientation.equalsIgnoreCase( "zy" ) )
		{
			p.orientation = Orientation.ZY;
			orientedSourceInterval = new FinalDimensions(
					( long )( p.sourceInterval.dimension( 2 ) / scaleZDiv ),
					p.sourceInterval.dimension( 1 ) / scaleXYDiv,
					p.sourceInterval.dimension( 0 ) / scaleXYDiv );
		}
		else
		{
			p.orientation = Orientation.XY;
			orientedSourceInterval = new FinalDimensions(
					p.sourceInterval.dimension( 0 ) / scaleXYDiv,
					p.sourceInterval.dimension( 1 ) / scaleXYDiv,
					( long )( p.sourceInterval.dimension( 2 ) / scaleZDiv ) );
		}

		p.tileWidth = Integer.parseInt( System.getProperty( "tileWidth", "256" ) );
		p.tileHeight = Integer.parseInt( System.getProperty( "tileHeight", "256" ) );
		p.minZ = Long.parseLong( System.getProperty( "exportMinZ", "0" ) );
		p.maxZ = Long.parseLong( System.getProperty(
				"exportMaxZ",
				Long.toString( orientedSourceInterval.dimension( 2 ) - 1 ) ) );
		p.minR = Long.parseLong( System.getProperty( "exportMinR", "0" ) );
		p.maxR = Long.parseLong( System.getProperty(
				"exportMaxR",
				Long.toString( ( long )Math.ceil( ( double )orientedSourceInterval.dimension( 1 ) / ( double )p.tileHeight ) - 1 ) ) );
		p.minC = Long.parseLong( System.getProperty( "exportMinC", "0" ) );
		p.maxC = Long.parseLong( System.getProperty(
				"exportMaxC",
				Long.toString( ( long )Math.ceil( ( double )orientedSourceInterval.dimension( 0 ) / ( double )p.tileWidth ) - 1 ) ) );

		p.exportPath = System.getProperty( "exportBasePath", "" );
		p.tilePattern = System.getProperty( "tilePattern", "%5$d/%8$d_%9$d_%1$d" ); // default is z/row_col_scale
		p.format = System.getProperty( "format", "jpg" );
		p.quality = Float.parseFloat( System.getProperty( "quality", "0.85" ) );
		final String type = System.getProperty( "type", "rgb" );
		if ( type.equalsIgnoreCase( "gray" ) || type.equalsIgnoreCase( "grey" ) )
			p.type = BufferedImage.TYPE_BYTE_GRAY;
		else
			p.type = BufferedImage.TYPE_INT_RGB;
		
		final String interpolation = System.getProperty( "interpolation", "NN" );
		if ( interpolation.equalsIgnoreCase( "nl" ) || interpolation.equalsIgnoreCase( "NL" ) )
			p.interpolation = Interpolation.NL;
		else
			p.interpolation = Interpolation.NN;
		
		return p;
	}
	
	/**
	 * Create a {@link Tiler} from a CATMAID stack.
	 * 
	 * @param urlFormat
	 * @param width	of scale level 0 in pixels
	 * @param height of scale level 0 in pixels
	 * @param depth	of scale level 0 in pixels
	 * @param s scale level to be used, using anything &gt;0 will create an
	 * 		accordingly scaled source stack 
	 * @param tileWidth
	 * @param tileHeight
	 * @param resXY <em>x,y</em>-resolution
	 * @param real valued offset in CATMAID scale level 0 pixels
	 * @param resZ <em>z</em>-resolution, what matters is only the ratio
	 * 		between <em>z</em>- and <em>x,y</em>-resolution to scale the source
	 * 		to isotropic resolution (if that is desired, you will want to do
	 * 		it when exporting re-sliced stacks, not when extracting at the
	 * 		original orientation).
	 *  
	 * @return
	 */
	static public Tiler fromCATMAID(
			final String urlFormat,
			final long width,
			final long height,
			final long depth,
			final long s,
			final int tileWidth,
			final int tileHeight,
			final double resXY,
			final double resZ,
			final RealLocalizable offset,
			final Interpolation interpolation )
	{
		final CATMAIDRandomAccessibleInterval catmaidStack =
				new CATMAIDRandomAccessibleInterval(
						urlFormat,
						width,
						height,
						depth,
						s,
						tileWidth,
						tileHeight );

		/* scale and re-raster */
		final double scaleXY = 1.0 / ( 1 << s );
		final double scaleZ = resZ / resXY * scaleXY;
		
		final double offsetX = offset.getDoublePosition( 0 ) * scaleXY;
		final double offsetY = offset.getDoublePosition( 1 ) * scaleXY;
		final double offsetZ = offset.getDoublePosition( 2 ) * scaleZ;
		
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set(
				1, 0, 0, -offsetX,
				0, 1, 0, -offsetY,
				0, 0, scaleZ, -offsetZ );
		final RealRandomAccessible< ARGBType > interpolant;
		switch ( interpolation )
		{
		case NL:
			interpolant = Views.interpolate( catmaidStack, new NLinearInterpolatorARGBFactory() );
			break;
		default:
			interpolant = Views.interpolate( catmaidStack, new NearestNeighborInterpolatorFactory< ARGBType >() );
		}
		final RandomAccessible< ARGBType > scaledInterpolant = RealViews.affine( interpolant, transform );
		final RandomAccessibleInterval< ARGBType > scaled =
				Views.interval(
						scaledInterpolant,
						new FinalInterval(
								( long )( scaleXY * width - offsetX ),
								( long )( scaleXY * height - offsetY ),
								( long )( scaleZ * depth - offsetZ ) ) );
		
		return new Tiler( scaled );
	}
	
	
	
	
	final static public void main( final String[] args ) throws Exception
	{
		final Param p = parseParameters();
		
		System.out.println( "sourceInterval: " + Util.printInterval( p.sourceInterval ) );
		
		final RealPoint min = new RealPoint( 3 );
		p.sourceInterval.min( min );
		
		final int scaleXYDiv = 1 << p.sourceScaleLevel;
		final double scaleZDiv = scaleXYDiv * p.sourceResXY / p.sourceResZ;
		
		final FinalInterval cropDimensions = new FinalInterval(
				p.sourceInterval.dimension( 0 ) / scaleXYDiv,
				p.sourceInterval.dimension( 1 ) / scaleXYDiv,
				( long )( p.sourceInterval.dimension( 2 ) / scaleZDiv ) );
		
		fromCATMAID(
				p.sourceUrlFormat,
				p.sourceWidth,
				p.sourceHeight,
				p.sourceDepth,
				p.sourceScaleLevel,
				p.sourceTileWidth,
				p.sourceTileHeight,
				p.sourceResXY,
				p.sourceResZ,
				min,
				p.interpolation ).tile(
						cropDimensions,
						p.orientation,
						p.tileWidth,
						p.tileHeight,
						p.minZ,
						p.maxZ,
						p.minR,
						p.maxR,
						p.minC,
						p.maxC,
						p.exportPath,
						p.tilePattern,
						p.format,
						p.quality,
						p.type );
	}
}
