package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Created by Michaël on 25/05/14.
 */
public class MaxLateralDistanceVertexSnapperTest {

    static WKTReader reader = new WKTReader();
    static MaxDistanceVertexSnapper snapper0 = new MaxLateralDistanceVertexSnapper(5.0, 0.0);
    static MaxDistanceVertexSnapper snapper5 = new MaxLateralDistanceVertexSnapper(5.0, 5.0);
    FeatureSchema schema;
    GeometryWrapper wpoint, wline, wpoly, wmpoint, wmline, wmpoly, wgcoll;
    STRtree index;

    @Before
    public void before() throws Exception {
        schema = new FeatureSchema();
        schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        index = new STRtree();

        Geometry point = reader.read("POINT(0 0)");
        Feature fpoint = new BasicFeature(schema);
        fpoint.setGeometry(point);
        wpoint = new GeometryWrapper.WPoint(fpoint, index);

        Geometry line = reader.read("LINESTRING(0 0, 10 0, 20 10)");
        Feature fline = new BasicFeature(schema);
        fline.setGeometry(line);
        wline = new GeometryWrapper.WLineString(fline, index);

        Geometry poly = reader.read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0),(2 2, 8 2, 8 8, 2 8, 2 2))");
        Feature fpoly = new BasicFeature(schema);
        fpoly.setGeometry(poly);
        wpoly = new GeometryWrapper.WPolygon(fpoly, index);

        Geometry mpoint = reader.read("MULTIPOINT((0 0),(10 0),(10 10),(0 10))");
        Feature fmpoint = new BasicFeature(schema);
        fmpoint.setGeometry(mpoint);
        wmpoint = new GeometryWrapper.WMultiPoint(fmpoint, index);

        Geometry mline = reader.read("MULTILINESTRING((0 0, 10 0, 20 10),(0 10, 10 10, 20 20))");
        Feature fmline = new BasicFeature(schema);
        fmline.setGeometry(mline);
        wmline  = new GeometryWrapper.WMultiLineString(fmline, index);

        Geometry mpoly = reader.read("MULTIPOLYGON(((0 0, 10 0, 10 10, 0 10, 0 0),(2 2, 8 2, 8 8, 2 8, 2 2)), ((100 100, 110 100, 110 110, 100 110, 100 100)))");
        Feature fmpoly = new BasicFeature(schema);
        fmpoly.setGeometry(mpoly);
        wmpoly  = new GeometryWrapper.WMultiPolygon(fmpoly, index);

        Geometry gcoll = reader.read("GEOMETRYCOLLECTION(POLYGON((0 0, 10 0, 10 10, 0 10, 0 0),(2 2, 8 2, 8 8, 2 8, 2 2)), LINESTRING(20 0, 30 0, 40 10), POINT(50 0))");
        Feature fgcoll = new BasicFeature(schema);
        fgcoll.setGeometry(gcoll);
        wgcoll  = new GeometryWrapper.WGeometryCollection(fgcoll, index);

    }

    /** Snap point to a point which distance is < maxDist */
    @Test
    public void snapPointElementTrue1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(4.999 0)"));
        Projection proj = snapper0.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), wpoint.getFeature().getGeometry().getCoordinate());
    }

    /** Snap point to a point which distance is = maxDist */
    @Test
    public void snapPointElementTrue2() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5 0)"));
        Projection proj = snapper0.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), wpoint.getFeature().getGeometry().getCoordinate());
    }

    /** Do not snap point to a point which distance is > maxDist */
    @Test
    public void snapPointElementTrue3() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5.001 0)"));
        Projection proj = snapper0.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertNull(proj);
    }

    /** Snap point which is projected out of the target segment with a no-tolerance snapper
     * but with a distance < maxDistance*/
    @Test
    public void snapSegmentElementTrue1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 3)"));
        Projection proj = snapper0.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }

    /** Try to snap point which is projected out of the segment with a tolerant snapper */
    @Test
    public void snapSegmentElementTrue2() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 3)"));
        Projection proj = snapper5.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }

    /** Try to snap point which is projected out of the target segment with a no-tolerance snapper */
    @Test
    public void snapSegmentElementTrue3() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 5)"));
        Projection proj = snapper0.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertNull(proj);
    }

    /** Try to snap point which is projected out of the segment with a tolerant snapper
     * but with a total distance > maxDistance */
    @Test
    public void snapSegmentElementTrue4() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 5)"));
        Projection proj = snapper5.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertNull(proj);
    }

    /** Try to snap point which is projected exactly on a segment endpoint with a zero tolerance snapper */
    @Test
    public void snapSegmentElementTrue5() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(0 5)"));
        Projection proj = snapper0.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }

    /** Try to snap point which is projected on a segment with a zero tolerance snapper */
    @Test
    public void snapSegmentElementTrue6() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(1 5)"));
        Projection proj = snapper0.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(1, 0));
    }

    /** Try to snap point which is projected on a segment with a tolerant snapper
     * but a total distance to endpoint > maxDist */
    @Test
    public void snapSegmentElementTrue7() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(1 5)"));
        Projection proj = snapper5.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(1, 0));
    }

    /** Try to snap point which is projected on a segment with a tolerant snapper
     * but a total distance to endpoint > maxDist */
    @Test
    public void snapSegmentElementTrue8() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(1 3)"));
        Projection proj = snapper5.snap(source, new SegmentElement(
                wline, new Coordinate(0,0), new Coordinate(10,0)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }
}
