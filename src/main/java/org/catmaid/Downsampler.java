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

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class Downsampler
{
	final static private int averageByte( final int i1, final int i2, final int i3, final int i4, final byte[] data )
	{
		return (
				( data[ i1 ] & 0xff ) +
				( data[ i2 ] & 0xff ) +
				( data[ i3 ] & 0xff ) +
				( data[ i4 ] & 0xff ) ) / 4;
	}
	
	final static private int averageColorRed( final int rgb1, final int rgb2, final int rgb3, final int rgb4 )
	{
		return (
			( ( rgb1 >> 16 ) & 0xff ) +
			( ( rgb2 >> 16 ) & 0xff ) +
			( ( rgb3 >> 16 ) & 0xff ) +
			( ( rgb4 >> 16 ) & 0xff ) ) / 4;
	}
	
	final static private int averageColorGreen( final int rgb1, final int rgb2, final int rgb3, final int rgb4 )
	{
		return (
			( ( rgb1 >> 8 ) & 0xff ) +
			( ( rgb2 >> 8 ) & 0xff ) +
			( ( rgb3 >> 8 ) & 0xff ) +
			( ( rgb4 >> 8 ) & 0xff ) ) / 4;
	}
	
	final static private int averageColorBlue( final int rgb1, final int rgb2, final int rgb3, final int rgb4 )
	{
		return (
			( rgb1 & 0xff ) +
			( rgb2 & 0xff ) +
			( rgb3 & 0xff ) +
			( rgb4 & 0xff ) ) / 4;
	}
	
	final static private int averageColor( final int i1, final int i2, final int i3, final int i4, final int[] data )
	{
		final int rgb1 = data[ i1 ];
		final int rgb2 = data[ i2 ];
		final int rgb3 = data[ i3 ];
		final int rgb4 = data[ i4 ];
		
		final int red = averageColorRed( rgb1, rgb2, rgb3, rgb4 );
		final int green = averageColorGreen( rgb1, rgb2, rgb3, rgb4 );
		final int blue = averageColorBlue( rgb1, rgb2, rgb3, rgb4 );
		return ( ( ( ( 0xff000000 | red ) << 8 ) | green ) << 8 ) | blue;
	}
	
	final static public void downsampleBytes( final byte[] aPixels, final byte[] bPixels, final int wa, final int ha )
	{
		assert aPixels.length == wa * ha && bPixels.length == wa / 2 * ( ha / 2 ) : "Input dimensions do not match.";
		
		final int wa2 = wa + wa;
		
		final int wb = wa / 2;
		final int hb = ha / 2;
		final int nb = hb * wb;
		
		for ( int ya = 0, yb = 0; yb < nb; ya += wa2, yb += wb )
		{
			final int ya1 = ya + wa;
			for ( int xa = 0, xb = 0; xb < wb; xa += 2, ++xb )
			{
				final int xa1 = xa + 1;
				final int s = averageByte(
						ya + xa,
						ya + xa1,
						ya1 + xa,
						ya1 + xa1,
						aPixels );
				bPixels[ yb + xb ] = ( byte )s;
			}
		}
	}

	final static public boolean downsampleRGB( final int[] aPixels, final int[] bPixels, final int wa, final int ha, int bgValue )
	{
		assert aPixels.length == wa * ha && bPixels.length == wa / 2 * ( ha / 2 ) : "Input dimensions do not match.";
		
		final int wa2 = wa + wa;
		
		final int wb = wa / 2;
		final int hb = ha / 2;
		final int nb = hb * wb;
		boolean bresult = false;
		for ( int ya = 0, yb = 0; yb < nb; ya += wa2, yb += wb )
		{
			final int ya1 = ya + wa;
			for ( int xa = 0, xb = 0; xb < wb; xa += 2, ++xb )
			{
				final int xa1 = xa + 1;
				int c = averageColor( ya + xa, ya + xa1, ya1 + xa, ya1 + xa1, aPixels );
				bPixels[ yb + xb ] = c;
				if ((c & 0xFFFFFF) != bgValue) {
					bresult = true; // return true since at least one pixel is non black
				}
			}
		}
		return bresult;
	}
}
