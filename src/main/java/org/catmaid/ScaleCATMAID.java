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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * <p>A standalone command line application to generate the scale pyramid of an
 * existing scale level 0 tile set for the CATMAID interface.</p>
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
 * <dt>tileWidth</dt>
 * <dd>width of image tiles in pixels (int, 256)</dd>
 * <dt>tileHeight</dt>
 * <dd>height of image tiles in pixels (int, 256)</dd>
 * <dt>minZ</dt>
 * <dd>first <em>z</em>-section index (long, 0)</dd>
 * <dt>maxZ</dt>
 * <dd>last <em>z</em>-section index (long, Long.MAX_VALUE)</dd>
 * <dt>basePath</dt>
 * <dd>base path to the scale level 0 tile set, that's where scaled tiles will
 * be exported too (string, "")</dd>
 * <dt>tilePattern</dt>
 * <dd>tilePattern the file name convention for tile coordinates without
 * extension and base path, must contain "&lt;s&gt;","&lt;z&gt;", "&lt;r&gt;",
 * "&lt;c&gt;" (string, "&lt;z&gt;/&lt;r&gt;_&lt;c&gt;_&lt;s&gt;")
 * <dt>format</dt>
 * <dd>image tile file format, e.g. "jpg" or "png" (string, "jpg")</dd>
 * <dt>quality</dt>
 * <dd>quality for jpg-compression if format is "jpg" (float, 0.85)</dd>
 * <dt>type</dt>
 * <dd>the type of export tiles, either "rgb or "gray" (string, "rgb")</dd>
 * </dl>
 * <p>Parameters are passed as properties to the JVM virtual machine, e.g.
 * <code>./java -jar ScaleCATMAID.jar</code></p>
 * 
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class ScaleCATMAID
{
	static protected class Param
	{
		public int tileWidth;
		public int tileHeight;
		public long minC;
		public long maxC;
		public long minR;
		public long maxR;
		public long minZ;
		public long maxZ;
		public String tileFormat;
		public String format;
		public float quality;
		public int type;
		public boolean ignoreEmptyTiles;
	}

	private ScaleCATMAID() {}

	static protected Param parseParameters() {
		final Param p = new Param();
		p.tileWidth = Integer.parseInt( System.getProperty( "tileWidth", "256" ) );
		p.tileHeight = Integer.parseInt( System.getProperty( "tileHeight", "256" ) );

		p.minC = Long.parseLong( System.getProperty( "minC", "0" ) );
		p.maxC = Long.parseLong( System.getProperty( "maxC", "" + Integer.MAX_VALUE ) );
		p.minR = Long.parseLong( System.getProperty( "minR", "0" ) );
		p.maxR = Long.parseLong( System.getProperty( "maxR", "" + Integer.MAX_VALUE ) );
		p.minZ = Long.parseLong( System.getProperty( "minZ", "0" ) );
		p.maxZ = Long.parseLong( System.getProperty( "maxZ", "" + Long.MAX_VALUE ) );
		final String basePath = System.getProperty( "basePath", "" );
		p.format = System.getProperty( "format", "jpg" );
		final String tileFormat = System.getProperty( "tileFormat", "%5$d/%8$d_%9$d_%1$d");
		p.tileFormat = basePath + "/" + tileFormat + "." + p.format;

		System.out.println("Tile pattern: " + p.tileFormat );

		p.quality = Float.parseFloat( System.getProperty( "quality", "0.85" ) );
		final String type = System.getProperty( "type", "rgb" );
		if ( type.equalsIgnoreCase( "gray" ) || type.equalsIgnoreCase( "grey" ) )
			p.type = BufferedImage.TYPE_BYTE_GRAY;
		else
			p.type = BufferedImage.TYPE_INT_RGB;
		p.ignoreEmptyTiles = Boolean.valueOf(System.getProperty( "ignoreEmptyTiles"));

		return p;
	}

	final static protected BufferedImage open(
			final String path,
			final BufferedImage alternative,
			final int type )
	{
		final File file = new File( path );
		if ( file.exists() )
		{
			try
			{
				return ImageIO.read( new File( path ) );
			}
			catch ( final IOException e )
			{
				return alternative;
			}
		}
		else
			return alternative;
	}

	/**
	 * Generate scaled tiles from a range of an existing scale level 0 tile
	 * stack.
	 * 
	 * @param tileFormat format string addressing tiles including basePath
	 * @param tileWidth
	 * @param tileHeight
	 * @param minZ the first z-index to be scaled 
	 * @param maxZ the last z-index to be scaled
	 * @param format file format, e.g. "jpg" or "png"
	 * @param quality quality for jpg-compression if format is "jpg"
	 * @param type the type of export tiles, e.g.
	 * 		{@link BufferedImage#TYPE_BYTE_GRAY}
	 * @param ignoreEmptyTiles - if true don't save empty tiles
	 * 
	 * @throws Exception
	 */
	final public static void scale(
			final String tileFormat,
			final int tileWidth,
			final int tileHeight,
			final long minX,
			final long maxX,
			final long minY,
			final long maxY,
			final long minZ,
			final long maxZ,
			final String format,
			final float quality,
			final int type,
			final boolean ignoreEmptyTiles) throws Exception
	{
		final BufferedImage alternative = new BufferedImage( tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB );

		final int[] targetPixels = new int[ tileWidth * tileHeight ];
		final BufferedImage target = new BufferedImage( tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB );
		
		final BufferedImage sourceImage = new BufferedImage( tileWidth * 2, tileHeight * 2, BufferedImage.TYPE_INT_RGB );
		final Graphics2D g = sourceImage.createGraphics();
		final int[] sourcePixels = new int[ tileWidth * tileHeight * 4 ];

		for ( long z = minZ; z <= maxZ; z++ )
		{
			System.out.println( "z-index: " +  z );
			boolean workToDo = true;
			for ( int s = 1; workToDo; ++s )
			{
				System.out.println( "scale: " +  s );
				final int iScale = 1 << s;
				final double scale = 1.0 / iScale;

				final int s1 = s - 1;
				final int iScale1 = 1 << s1;
				final double scale1 = 1.0 / iScale1;
				int nResultTiles = 0;

				workToDo = false;
				for ( long y = minY / iScale1; y < maxY / iScale1; y += 2 * tileHeight )
				{
					workToDo = true;
					final long yt = y / (2 * tileHeight);
					for ( long x = minX / iScale1; x < maxX / iScale1; x += 2 * tileWidth )
					{
						nResultTiles++;
						final long xt = x / (2 * tileWidth);
						final Image imp1 = open(
								String.format( tileFormat, s1, scale1, x * iScale1, y * iScale1, z, tileWidth * iScale1, tileHeight * iScale1, 2 * yt, 2 * xt ),
								alternative,
								type );

						final Image imp2 = open(
								String.format( tileFormat, s1, scale1, ( x + tileWidth ) * iScale1, y * iScale1, z, tileWidth * iScale1, tileHeight * iScale1, 2 * yt, 2 * xt + 1 ),
								alternative,
								type );

						final Image imp3 = open(
								String.format( tileFormat, s1, scale1, x * iScale1, ( y + tileHeight ) * iScale1, z, tileWidth * iScale1, tileHeight * iScale1, 2 * yt + 1, 2 * xt ),
								alternative,
								type );

						final Image imp4 = open(
								String.format( tileFormat, s1, scale1, ( x + tileWidth ) * iScale1, ( y + tileHeight ) * iScale1, z, tileWidth * iScale1, tileHeight * iScale1, 2 * yt + 1, 2 * xt + 1 ),
								alternative,
								type );

						if (imp1 == alternative && imp2 == alternative && imp3 == alternative && imp4 == alternative) {
							continue;
						}

						g.drawImage( imp1, 0, 0, null );
						g.drawImage( imp2, tileWidth, 0, null );
						g.drawImage( imp3, 0, tileHeight, null );
						g.drawImage( imp4, tileWidth, tileHeight, null );

						final PixelGrabber pg = new PixelGrabber( sourceImage, 0, 0, tileWidth * 2, tileHeight * 2, sourcePixels, 0, tileWidth * 2 );
						pg.grabPixels();

						boolean notEmpty = Downsampler.downsampleRGB( sourcePixels, targetPixels, tileWidth * 2, tileHeight * 2 );

						if (notEmpty || !ignoreEmptyTiles) {
							target.getRaster().setDataElements( 0, 0, tileWidth, tileHeight, targetPixels );
							final BufferedImage targetCopy = Util.draw( target, type );
	
							Util.writeTile(
									targetCopy,
									String.format( tileFormat, s, scale, x * iScale, y * iScale, z, tileWidth * iScale, tileHeight * iScale, yt, xt ),
									format,
									quality );
						}
					} // end for x
				} // end for y
				if (nResultTiles <= 1) {
					workToDo = false;
				}
			} // end for s
		} // end for z
	}
	
	final static public void scale( final Param p ) throws Exception
	{
		scale(p.tileFormat,
			p.tileWidth,
			p.tileHeight,
			p.minC * p.tileWidth,
			(p.maxC + 1) * p.tileWidth, // include maxC
			p.minR * p.tileHeight,
			(p.maxR + 1) * p.tileHeight, // include maxR
			p.minZ,
			p.maxZ,
			p.format,
			p.quality,
			p.type,
			p.ignoreEmptyTiles);
	}

	
	final static public void main( final String... args ) throws Exception
	{
		scale( parseParameters() );
	}
}
