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
import java.io.IOException;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class Tiler
{
	final protected RandomAccessibleInterval< ARGBType > source;
	
	
	public Tiler( final RandomAccessibleInterval< ARGBType > source )
	{
		this.source = source;
	}
	
	
	static public enum Orientation
	{
		XY, XZ, ZY
	}
	
	
	/**
	 * Copy the contents of two
	 * {@link RandomAccessibleInterval RandomAccessibleIntervals} that have the
	 * same dimensions.
	 * 
	 * @param sourceTile
	 * @param targetTile
	 */
	final static private boolean copyTile(
			final RandomAccessibleInterval< ARGBType > sourceTile,
			final RandomAccessibleInterval< ARGBType > targetTile )
	{
		final Cursor< ARGBType > src = Views.flatIterable( sourceTile ).cursor();
		final Cursor< ARGBType > dst = Views.flatIterable( targetTile ).cursor();

		boolean hasInfo = false;
		while ( src.hasNext() ) {
			ARGBType spixel = src.next();
			if ((spixel.get() & 0xFFFFFF) != 0) hasInfo = true;
			dst.next().set(spixel);
		}
		return hasInfo;
	}
	

	/**
	 * Copy a {@link RandomAccessibleInterval} into another
	 * {@link RandomAccessibleInterval} of the same or larger size.
	 * If targetTile is larger than sourceTile then fill the rest with the
	 * passed background value.
	 * Copy order can be either row by row or column by column
	 * 
	 * @param sourceTile
	 * @param targetTile
	 * @param yx
	 * @param bg background value
	 */
	final static protected boolean copyTile(
			final RandomAccessibleInterval< ARGBType > sourceTile,
			final RandomAccessibleInterval< ARGBType > targetTile,
			final boolean yx,
			final ARGBType bg )
	{
		/* if sourceTile is smaller than targetTile, create the corresponding
		 * window in target and clear the rest */
		RandomAccessibleInterval< ARGBType > raiTarget;
		if ( Intervals.equalDimensions( sourceTile, targetTile ) )
			raiTarget = targetTile;
		else
		{
			/* clear */
			for ( final ARGBType p : Views.iterable( targetTile ) )
				p.set( bg );
			
			raiTarget = Views.interval( targetTile, sourceTile );
		}
		
		final RandomAccessibleInterval< ARGBType > raiSource;
		if ( yx )
		{
			raiSource = Views.permute( sourceTile, 0, 1 );
			raiTarget = Views.permute( raiTarget, 0, 1 );
		}
		else
			raiSource = sourceTile;
		
		return copyTile( raiSource, raiTarget );
	}

	/**
	 * Generate a subset of a CATMAID tile stack of an {@link Interval} of the
	 * source {@link RandomAccessibleInterval}.  That is you can choose the
	 * window to be exported, and export only a subset thereof.
	 * 
	 * @param sourceInterval the interval of the source to be exported 
	 * @param orientation the export orientation
	 * @param tileWidth
	 * @param tileHeight
	 * @param minZ the first z-index to be exported
	 * @param maxZ the last z-index to be exported
	 * @param minR the first tile-row at scale-level 0 to be exported
	 * @param maxR the last tile-row at scale-level 0 to be exported
	 * @param minC the first tile-column at scale-level 0 to be exported
	 * @param maxC the last tile-column at scale-level 0 to be exported
	 * @param exportPath base path for export
	 * @param tilePattern the file name convention for tile coordinates without
	 * 		extension and base path, must contain "&lt;s&gt;","&lt;z&gt;",
	 * 		"&lt;r&gt;", "&lt;c&gt;".
	 * @param format
	 * @param quality
	 * @param type
	 * @throws IOException
	 */
	public void tile(
			final Interval sourceInterval,
			final Orientation orientation,
			final int tileWidth,
			final int tileHeight,
			final long minZ,
			final long maxZ,
			final long minR,
			final long maxR,
			final long minC,
			final long maxC,
			final String exportPath,
			final String tilePattern,
			final String format,
			final float quality,
			final int type,
			final boolean ignoreEmptyTiles ) throws IOException
	{
		/* orientation */
		final RandomAccessibleInterval< ARGBType > view;
		final Interval viewInterval;
		switch ( orientation )
		{
		case XZ:
			view = Views.permute( source, 1, 2 );
			viewInterval = new FinalInterval(
					new long[]{ sourceInterval.min( 0 ), sourceInterval.min( 2 ), sourceInterval.min( 1 ) },
					new long[]{ sourceInterval.max( 0 ), sourceInterval.max( 2 ), sourceInterval.max( 1 ) } );
			break;
		case ZY:
			view = Views.permute( source, 0, 2 );
			viewInterval = new FinalInterval(
					new long[]{ sourceInterval.min( 2 ), sourceInterval.min( 1 ), sourceInterval.min( 0 ) },
					new long[]{ sourceInterval.max( 2 ), sourceInterval.max( 1 ), sourceInterval.max( 0 ) } );
			break;
		default:
			view = source;
			viewInterval = sourceInterval;
		}

		final long[] min = new long[ 3 ];
		final long[] size = new long[ 3 ];
		size[ 2 ] = 1;

		final int[] tilePixels = new int[ tileWidth * tileHeight ];
		final ArrayImg< ARGBType, IntArray > tile = ArrayImgs.argbs( tilePixels, tileWidth, tileHeight );
		final BufferedImage img = new BufferedImage( tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB );

		for ( long z = minZ; z <= maxZ; ++z )
		{
			min[ 2 ] = z + viewInterval.min( 2 );
			for ( long r = minR; r <= maxR; ++r )
			{
				min[ 1 ] = r * tileHeight + viewInterval.min( 1 );
				final long max1 = Math.min( viewInterval.max( 1 ), min[ 1 ] + tileHeight - 1 );
				size[ 1 ] = max1 - min[ 1 ] + 1;
				for ( long c = minC; c <= maxC; ++c )
				{
					min[ 0 ] = c * tileWidth + viewInterval.min( 0 );
					final long max0 = Math.min( viewInterval.max( 0 ), min[ 0 ] + tileWidth - 1 );
					size[ 0 ] = max0 - min[ 0 ] + 1;

					final RandomAccessibleInterval< ARGBType > sourceTile = Views.hyperSlice( Views.offsetInterval( view, min, size ), 2, 0 );

					boolean tileIsNotEmpty = copyTile( sourceTile, tile, orientation == Orientation.ZY, new ARGBType( 0 ) );
					if (tileIsNotEmpty || !ignoreEmptyTiles) {
						img.getRaster().setDataElements(0, 0, tileWidth, tileHeight, tilePixels);
						final BufferedImage imgCopy = Util.draw(img, type);
						final String tilePath =
								new StringBuffer(exportPath != null && exportPath.trim().length() > 0 ? exportPath : "")
										.append(exportPath != null && exportPath.trim().length() > 0 ? "/" : "")
										.append(String.format(tilePattern, 0, 1.0, min[0], min[1], z, tileWidth, tileHeight, r, c))
										.toString();

						Util.writeTile(imgCopy, tilePath, format, quality);
					}
				}
			}
		}
	}
	
	
	/**
	 * Generate a CATMAID tile stack of an {@link Interval} of the source
	 * {@link RandomAccessibleInterval}.  That is you can choose the
	 * window to be exported.
	 * 
	 * @param sourceInterval the interval of the source to be exported 
	 * @param orientation the export orientation
	 * @param tileWidth
	 * @param tileHeight
	 * @param exportPath base path for export
	 * @param tilePattern the file name convention for tile coordinates without
	 * 		extension and base path, must contain "&lt;s&gt;","&lt;z&gt;",
	 * 		"&lt;r&gt;", "&lt;c&gt;".
	 * @param format
	 * @param quality
	 * @param type
	 * @throws IOException
	 */
	public void tile(
			final Interval sourceInterval,
			final Orientation orientation,
			final int tileWidth,
			final int tileHeight,
			final String exportPath,
			final String tilePattern,
			final String format,
			final float quality,
			final int type,
			final boolean ignoreEmptyTiles ) throws IOException
	{
		final long maxC;
		final long maxR;
		final long maxZ;
		
		switch ( orientation )
		{
		case XZ:
			maxC = ( long )Math.ceil( ( double )sourceInterval.dimension( 0 ) / ( double )tileWidth ) - 1;
			maxR = ( long )Math.ceil( ( double )sourceInterval.dimension( 2 ) / ( double )tileHeight ) - 1;
			maxZ = sourceInterval.dimension( 1 ) - 1;
			break;
		case ZY:
			maxC = ( long )Math.ceil( ( double )sourceInterval.dimension( 2 ) / ( double )tileWidth ) - 1;
			maxR = ( long )Math.ceil( ( double )sourceInterval.dimension( 1 ) / ( double )tileHeight ) - 1;
			maxZ = sourceInterval.dimension( 0 ) - 1;
			break;
		default:
			maxC = ( long )Math.ceil( ( double )sourceInterval.dimension( 0 ) / ( double )tileWidth ) - 1;
			maxR = ( long )Math.ceil( ( double )sourceInterval.dimension( 1 ) / ( double )tileHeight ) - 1;
			maxZ = sourceInterval.dimension( 2 ) - 1;
		}
		
		tile(
				sourceInterval,
				orientation,
				tileWidth,
				tileHeight,
				0,
				maxZ,
				0,
				maxR,
				0,
				maxC,
				exportPath,
				tilePattern,
				format,
				quality,
				type,
				ignoreEmptyTiles );
	}
	
	
	/**
	 * Generate a CATMAID tile stack of the source
	 * {@link RandomAccessibleInterval}.
	 * 
	 * @param orientation the export orientation
	 * @param tileWidth
	 * @param tileHeight
	 * @param exportPath base path for export
	 * @param tilePattern the file name convention for tile coordinates without
	 * 		extension and base path, must contain "&lt;s&gt;","&lt;z&gt;",
	 * 		"&lt;r&gt;", "&lt;c&gt;".
	 * @param format
	 * @param quality
	 * @param type
	 * @throws IOException
	 */
	public void tile(
			final Orientation orientation,
			final int tileWidth,
			final int tileHeight,
			final String exportPath,
			final String tilePattern,
			final String format,
			final float quality,
			final int type,
			final boolean ignoreEmptyTiles ) throws IOException
	{
		tile(
				source,
				orientation,
				tileWidth,
				tileHeight,
				exportPath,
				tilePattern,
				format,
				quality,
				type,
				ignoreEmptyTiles );
	}
	
	
	/**
	 * Generate a subset of a CATMAID tile stack of the source
	 * {@link RandomAccessibleInterval}.
	 * 
	 * @param orientation the export orientation
	 * @param tileWidth
	 * @param tileHeight
	 * @param minZ the first z-index to be exported
	 * @param maxZ the last z-index to be exported
	 * @param minR the first tile-row at scale-level 0 to be exported
	 * @param maxR the last tile-row at scale-level 0 to be exported
	 * @param minC the first tile-column at scale-level 0 to be exported
	 * @param maxC the last tile-column at scale-level 0 to be exported
	 * @param exportPath base path for export
	 * @param tilePattern the file name convention for tile coordinates without
	 * 		extension and base path, must contain "&lt;s&gt;","&lt;z&gt;",
	 * 		"&lt;r&gt;", "&lt;c&gt;".
	 * @param format file format, e.g. "jpg" or "png"
	 * @param quality quality for jpg-compression if format is "jpg"
	 * @param type the type of export tiles, e.g.
	 * 		{@link BufferedImage#TYPE_BYTE_GRAY}
	 * @throws IOException
	 */
	public void tile(
			final Orientation orientation,
			final int tileWidth,
			final int tileHeight,
			final long minZ,
			final long maxZ,
			final long minR,
			final long maxR,
			final long minC,
			final long maxC,
			final String exportPath,
			final String tilePattern,
			final String format,
			final float quality,
			final int type,
			final boolean ignoreEmptyTiles ) throws IOException
	{
		tile(
				source,
				orientation,
				tileWidth,
				tileHeight,
				minZ,
				maxZ,
				minR,
				maxR,
				minC,
				maxC,
				exportPath,
				tilePattern,
				format,
				quality,
				type,
				ignoreEmptyTiles );
	}
}
