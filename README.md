#catmaid-tools

##TileCATMAID

A standalone command line application to export image tiles representing
scale level 0 of the tiled scale pyramids for the CATMAID interface from
existing CATMAID image stacks.  The program enables to crop a box of
interest from an existing CATMAID stack and to export only a subset of the
tile set.  It is thus easily possible (and desired) to parallelize the
export on a cluster.

The main utility of the program is to export resliced (*xyz*, *xzy*, *zyx*)
representations of existing CATMAID stacks.

Separation of scale level 0 tile generation and scaling is necessary to
enable the desired level of parallelization.  One would typically generate
the scale level 0 tile set parallelized in volumes that cover a moderate
number of source tiles.  Only after a *z*-slice is fully exported,
it can be used to generate the scale pyramid.  I.e., scaling can be
parallelized by *z*-slice but not within a *z*-slice.

The TileCATMAID main program comes in the fat jar catmaid-tile.jar that can
be generated with Eclipse using catmaid-tile.jardesc.  It is executed by:

    ./java -jar ScaleCATMAID.jar -DsourceBaseUrl=http://...

passing parameters as properties to the JVM virtual machine.  It accepts
the following parameters, type and default in parantheses:

<dl>
<dt>sourceBaseUrl</dt>
<dd>base path of the source CATMAID stack (string, ""), not required if <code>sourceUrlFormat</code> includes it</dd>
<dt>sourceUrlFormat</dt>
<dd>URL format String to address CATMAID tiles(string, sourceBaseUrl + "%5$d/%8$d_%9$d_%1$d.jpg").</dd>
</dl>

> Tiles are addressed, in this order, by their

> 1. scale_level,
> 1. scale,
> 1. x,
> 1. y,
> 1. z,
> 1. tile width,
> 1. tile height,
> 1. tile row, and
> 1. tile column.

> Examples:
> <dl>
> <dt>"http://catmaid.org/my-data/xy/%5$d/%8$d_%9$d_%1$d.jpg"</dt>
>  <dd>CATMAID DefaultTileSource (type 1)</dd>
>  <dt>"http://catmaid.org/my-data/xy/?x=%3$d&y=%4$d&width=%6d&height=%7$d&row=%8$d&col=%9$d&scale=%2$f&z=%4$d"</dt>
>  <dd>CATMAID RequestTileSource (type 2)</dd>
>  <dt>"http://catmaid.org/my-data/xy/%1$d/%5$d/%8$d/%9$d.jpg"</dt>
>  <dd>CATMAID LargeDataTileSource (type 5)</dd>
> </dl>

<dl>
<dt>sourceWidth</dt>
<dd>width of the source in scale level 0 pixels in <em>xyz</em> orientation
(long, 0)</dd>
<dt>sourceHeight</dt>
<dd>height of the source in scale level 0 pixels in <em>xyz</em> orientation
(long, 0)</dd>
<dt>sourceDepth</dt>
<dd>depth of the source in scale level 0 pixels in <em>xyz</em> orientation
(long, 0)</dd>
<dt>sourceScaleLevel</dt>
<dd>source scale level to be used for export scale level 0 (long, 0)</dd>
<dt>sourceTileWidth</dt>
<dd>width of source image tiles in pixels (int, 256)</dd>
<dt>sourceTileHeight</dt>
<dd>height of source image tiles in pixels (int, 256)</dd>
<dt>sourceResXY</dt>
<dd>source stack <em>x,y</em>-resolution (double, 1.0 )</dd>
<dt>sourceResZ</dt>
<dd>source stack <em>z</em>-resolution (double, 1.0 )</dd>

<dt>minX</dt>
<dd>minimum <em>x</em>-coordinate of the box in the source stack to be
exported in scale level 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
<dt>minY</dt>
<dd>minimum <em>y</em>-coordinate of the box in the source stack to be
exported in scale level 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
<dt>minZ</dt>
<dd>minimum <em>z</em>-coordinate of the box in the source stack to be
exported in scale level 0 pixels in <em>xyz</em> orientation (long, 0)</dd>
<dt>width</dt>
<dd>width of the the box in the source stack to be exported in scale level
0 pixels in <em>xyz</em> orientation (long, 0)</dd>
<dt>height</dt>
<dd>height of the the box in the source stack to be exported in scale level
0 pixels in <em>xyz</em> orientation (long, 0)</dd>
<dt>depth</dt>
<dd>depth of the the box in the source stack to be exported in scale level
0 pixels in <em>xyz</em> orientation (long, 0)</dd>
<dt>orientation</dt>
<dd>orientation of exported stack, possible values "xy", "xz", "zy" (string,
"xy")</dd>
<dt>tileWidth</dt>
<dd>width of exported image tiles in pixels (int, 256)</dd>
<dt>tileHeight</dt>
<dd>height of exported image tiles in pixels (int, 256)</dd>
<dt>exportedMinZ</dt>
<dd>first <em>z</em>-section index to be exported (long, 0)</dd>
<dt>exportedMaxZ</dt>
<dd>last <em>z</em>-section index to be exported (long, depth-1)</dd>
<dt>exportedMinX</dt>
<dd>first X in level 0 pixel coordinates to be exported (long, 0)</dd>
<dt>exportedMaxX</dt>
<dd>last X in level 0 pixel coordinates to be exported (long, width-1)</dd>
<dt>exportedMinY</dt>
<dd>first Y in level 0 pixel coordinates to be exported (long, 0)</dd>
<dt>exportedMaxY</dt>
<dd>last Y in level 0 pixel coordinates to be exported (long, depth-1)</dd>
<dt>exportBasePath</dt>
<dd>base path for the stack to be exported (string, "")</dd>
<dt>tilePattern</dt>
<dd>tilePattern the file name convention for export tile coordinates without
extension and base path, must contain "&lt;s&gt;","&lt;z&gt;", "&lt;r&gt;",
"&lt;c&gt;" (string, "&lt;z&gt;/&lt;r&gt;_&lt;c&gt;_&lt;s&gt;")
<dt>format</dt>
<dd>image tile file format for export, e.g. "jpg" or "png" (string,
"jpg")</dd>
<dt>quality</dt>
<dd>quality for export jpg-compression if format is "jpg" (float, 0.85)</dd>
<dt>type</dt>
<dd>the type of export tiles, either "rgb" or "gray" (string, "rgb")</dd>
<dt>interpolation</dt>
<dd>interpolation scheme used, either nearest neighbor "NN" or n-linear "NL"
(string, "NN")</dd>
</dl>

Alternatively, it can be executed by the accompanying Bash-script **retile**
that is called with a config file as its single parameter.  The config file
contains all parameters in key=value rows (escaped according to Bash's needs).

###Examples:

    sourceUrlFormat='http://catmaid.org/stack/%5$d/%8$d_%9$d_%1$d.jpg'
    sourceWidth=22775
    sourceHeight=18326
    sourceDepth=462
    sourceScaleLevel=2
    sourceTileWidth=256
    sourceTileHeight=256
    sourceResXY=4.0
    sourceResZ=45.0
    minX=1300
    minY=1000
    minZ=0
    width=2000	
    height=2000
    depth=100
    orientation=xy
    exportBasePath=/var/www/catmaid/test/xy/
    tilePattern="%5$d/%8$d_%9$d_%1$d.jpg"
    format=jpg
    quality=0.85
    type=gray
    interpolation=NL
