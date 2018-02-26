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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
class Util {
	final static String tilePath(
			final String tileFormat,
			final int scaleLevel,
			final double scale,
			final long x,
			final long y,
			final long z,
			final int tileWidth,
			final int tileHeight,
			final long row,
			final long column) {
		return String.format( tileFormat, scaleLevel, scale, x, y, z, tileWidth, tileHeight, row, column );
	}

	final static BufferedImage draw(
			final Image img,
			final int type) {
		final BufferedImage imgCopy = new BufferedImage( img.getWidth( null ), img.getHeight( null ), type );
		imgCopy.createGraphics().drawImage( img, 0, 0, null );
		return imgCopy;
	}

	final static BufferedImage readTile(
			final String url,
			final BufferedImage alternative) {
		if ( url.startsWith("file://") ) {
			return readTileFromFile(url.substring("file://".length()), alternative);
		} else if ( url.startsWith("http://") || url.startsWith("https://") ) {
			return readTileFromUrl(url, alternative);
		} else {
			return readTileFromFile(url, alternative);
		}
	}

	final static BufferedImage readTileFromFile(
			final String url,
			final BufferedImage alternative) {
		File f = new File( url );
		if ( f.exists() ) {
			try {
				return ImageIO.read( new File( url ) );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
		return alternative;
	}

	final static BufferedImage readTileFromUrl(
			final String urlString,
			final BufferedImage alternative) {
		HttpURLConnection conn = null;
		InputStream connInputStream = null;
		try {
			final URL url = new URL( urlString );
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.setRequestMethod("GET");
			int statusCode = conn.getResponseCode();
			if (statusCode != HttpURLConnection.HTTP_OK) {
				return alternative;
			}
			connInputStream = conn.getInputStream();
			return ImageIO.read(connInputStream);
		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			if (connInputStream != null) {
				try {
					connInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
		return alternative;
	}

	final static void writeTile(
			final BufferedImage img,
			final String url,
			final String format,
			final float quality ) throws IOException
	{
		if ( url.startsWith("file://") ) {
			writeTileToFile(img, url.substring("file://".length()), format, quality);
		} else if ( url.startsWith("http://") || url.startsWith("https://") ) {
			writeTileToUrl(img, url, format, quality);
		} else {
			writeTileToFile(img, url, format, quality);
		}
	}

	final static private void writeTileToFile(
			final BufferedImage img,
			final String tileFile,
			final String format,
			final float quality ) throws IOException
	{
		new File( tileFile ).getParentFile().mkdirs();
		final FileOutputStream os = new FileOutputStream( new File( tileFile ) );
		try {
			writeTile(img, os, format, quality);
		} finally {
			os.close();
		}
	}

	final static private void writeTileToUrl(
			final BufferedImage img,
			final String httpUrl,
			final String format,
			final float quality ) throws IOException
	{
		URL url = new URL(httpUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/octet-stream");
		OutputStream os = conn.getOutputStream();
		InputStream is = null;
		try {
			writeTile(img, os, format, quality);
			int statusCode = conn.getResponseCode();
			if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
				System.err.printf("Error response from %s: %d\n", httpUrl, statusCode);
			}
			is = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader((is)));
			String serverOutput;
			while ((serverOutput = br.readLine()) != null) {
				System.out.println(serverOutput);
			}
		} catch (IOException e) {
			System.err.printf("Error writing to %s %d\n", httpUrl, conn.getExpiration());
			throw e;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			conn.disconnect();
		}
	}

	final static private void writeTile(
			final BufferedImage img,
			final OutputStream outputStream,
			final String format,
			final float quality ) throws IOException
	{
		final ImageWriter writer = ImageIO.getImageWritersByFormatName( format ).next();
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
		writer.setOutput(ios);
		if ( format.equalsIgnoreCase( "jpg" ) )
		{
			final ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode( ImageWriteParam.MODE_EXPLICIT );
			param.setCompressionQuality( quality );
			writer.write( null, new IIOImage( img.getRaster(), null, null ), param );
		}
		else
		{
			writer.write( img );
		}
		ios.flush();
		outputStream.flush();
		writer.dispose();
	}

}
