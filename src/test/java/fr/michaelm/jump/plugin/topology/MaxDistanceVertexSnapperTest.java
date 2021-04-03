package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Michaël on 23/05/14.
 */
public class MaxDistanceVertexSnapperTest {

    static WKTReader reader = new WKTReader();
    static MaxDistanceVertexSnapper snapper = new MaxDistanceVertexSnapper(5.0);
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
        Projection proj = snapper.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), wpoint.getFeature().getGeometry().getCoordinate());
    }

    /** Don't snap point to a point which distance is > maxDist */
    @Test
    public void snapPointElementFalse1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5.001 0)"));
        Projection proj = snapper.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertNull(proj);
    }

    /** Snap point to a point which distance is = maxDist */
    @Test
    public void snapPointElementTrue2() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5 0)"));
        Projection proj = snapper.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), wpoint.getFeature().getGeometry().getCoordinate());
    }

    /** Snap point to a point which coordinate equals source coordinate */
    @Test
    public void snapPointElementTrue3() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(0 0)"));
        Projection proj = snapper.snap(source, new PointElement(
                wpoint, wpoint.getFeature().getGeometry().getCoordinate()));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), wpoint.getFeature().getGeometry().getCoordinate());
    }


    /** Snap point to a linestring which distance is < maxDist */
    @Test
    public void snapLineElementTrue1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(2 4.999)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wline,
                wline.getFeature().getGeometry().getCoordinates()[0],
                wline.getFeature().getGeometry().getCoordinates()[1]));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(2, 0));
    }

    /** Snap point to a linestring which distance = maxDist */
    @Test
    public void snapLineElementTrue2() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(2 5)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wline,
                wline.getFeature().getGeometry().getCoordinates()[0],
                wline.getFeature().getGeometry().getCoordinates()[1]));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(2, 0));
    }

    /** Don't snap point to a linestring which distance > maxDist */
    @Test
    public void snapLineElementFalse() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(2 5.001)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wline,
                wline.getFeature().getGeometry().getCoordinates()[0],
                wline.getFeature().getGeometry().getCoordinates()[1]));
        Assert.assertNull(proj);
    }

    /** Snap point to an existing vertex located at a distance <= maxDist */
    @Test
    public void snapLineElementTrue3() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(1 4)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wline,
                wline.getFeature().getGeometry().getCoordinates()[0],
                wline.getFeature().getGeometry().getCoordinates()[1]));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
        // Distance is not the actual distance to the projected vertex
        // but the minimum distance to the segment
        Assert.assertEquals(proj.getD2(), 4 * 4, 0.0);
    }

    /** Snap point to a linestring which distance is < maxDist */
    @Test
    public void snapPolygonElementTrue1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5 14.999)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wpoly,
                new Coordinate(10, 10),
                new Coordinate(0, 10)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(5, 10));
    }

    /** Snap point to a linestring which distance = maxDist */
    @Test
    public void snapPolygonElementTrue2() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5 15)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wpoly,
                new Coordinate(10, 10),
                new Coordinate(0, 10)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(5, 10));
    }

    /** Don't snap point to a linestring which distance > maxDist */
    @Test
    public void snapPolygonElementFalse() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5 15.001)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wpoly,
                new Coordinate(10, 10),
                new Coordinate(0, 10)));
        Assert.assertNull(proj);
    }

    /** Snap point to an existing vertex located at a distance <= maxDist */
    @Test
    public void snapPolygonElementTrue3() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(1 14)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wpoly,
                new Coordinate(10, 10),
                new Coordinate(0, 10)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 10));
        // Distance is not the actual distance to the projected vertex
        // but the minimum distance to the segment
        Assert.assertEquals(proj.getD2(), 4 * 4, 0.0);
    }

    /** Snap point to an existing vertex located at a distance <= maxDist */
    @Test
    public void snapPolygonElementTrue4() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(4 4)"));
        Projection proj = snapper.snap(source, new SegmentElement(
                wpoly,
                new Coordinate(2, 2),
                new Coordinate(8, 2)));
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(2, 2));
        // Distance is not the actual distance to the projected vertex
        // but the minimum distance to the segment
        Assert.assertEquals(proj.getD2(), 2 * 2, 0.0);
    }

    /** Snap point to an existing vertex located at a distance <= maxDist */
    @Test
    public void testIndex() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(4 4)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(5.0); // maxDistance
        List<GeometryElement> candidates = index.query(env);
        Set<Projection> projections = new TreeSet<>();
        for (GeometryElement element : candidates) {
            Projection proj = snapper.snap(source, element);
            if (proj != null) projections.add(proj);
        }
        Assert.assertTrue(projections.iterator().hasNext());
        Assert.assertTrue(projections.iterator().next().getTargetFeature() == wpoly.getFeature() ||
                projections.iterator().next().getTargetFeature() == wmpoly.getFeature() ||
                projections.iterator().next().getTargetFeature() == wgcoll.getFeature());
    }
}
