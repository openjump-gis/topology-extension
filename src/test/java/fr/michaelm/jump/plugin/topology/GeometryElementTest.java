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
import java.util.Map;

/**
 *
 */
public class GeometryElementTest {

    static WKTReader reader = new WKTReader();
    static MaxDistanceVertexSnapper aSnapper5 = new MaxDistanceVertexSnapper(5.0);
    static MaxDistanceVertexSnapper aSnapper50 = new MaxDistanceVertexSnapper(50.0);
    //static MaxLateralDistanceVertexSnapper rSnapper5_5 = new MaxLateralDistanceVertexSnapper(5.0, 5.0);
    //static MaxLateralDistanceVertexSnapper rSnapper5_0 = new MaxLateralDistanceVertexSnapper(5.0, 0.0);
    FeatureSchema schema;
    GeometryWrapper wline1, wpoint, wline2;
    STRtree index;

    @Before
    public void before() throws Exception {
        schema = new FeatureSchema();
        schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);

        index = new STRtree();

        Geometry line1 = reader.read("LINESTRING(0 0, 10 0, 20 0)");
        Feature fline1 = new BasicFeature(schema);
        fline1.setGeometry(line1);
        wline1 = new GeometryWrapper.WLineString(fline1, index);

        Geometry point = reader.read("POINT(30 0)");
        Feature fpoint = new BasicFeature(schema);
        fpoint.setGeometry(point);
        wpoint = new GeometryWrapper.WPoint(fpoint, index);

        Geometry line2 = reader.read("LINESTRING(40 0, 50 0, 60 0)");
        Feature fline2 = new BasicFeature(schema);
        fline2.setGeometry(line2);
        wline2 = new GeometryWrapper.WLineString(fline2, index);
    }

    /** Projette le point sur la première ligne */
    @Test
    public void projectSingleOnLineTest1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 3)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(5.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper5, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }

    /** Projette le point sur la première ligne (tous les candidats sont présents) */
    @Test
    public void projectSingleOnLineTest2() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 3)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(50.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper5, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }

    /** Ne projette pas le point sur la première ligne (hors tolérance)*/
    @Test
    public void projectSingleOnLineTest3() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(-1 5)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(5.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper5, candidates, true);
        Assert.assertNull(proj);
    }

    /** Projette le point sur la première ligne (plusieurs target objects possibles) */
    @Test
    public void projectSingleOnLineTest4() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(0 10)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(50.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper50, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(0, 0));
    }

    /** Projette le point sur la première ligne en snappant sur un vertex */
    @Test
    public void projectSingleOnLineTest5() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(6 10)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(50.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper50, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(10, 0));
    }

    /** Projette le point sur la première ligne (projection orthogonale - création d'un nouveau point) */
    @Test
    public void projectSingleOnLineTest6() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(5 50)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(50.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper50, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(5, 0));
    }

    /** Projette le point sur le point du milieu */
    @Test
    public void projectSingleOnLineTest7() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(28 30)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(50.0);
        List<GeometryElement> candidates = index.query(env);
        Projection proj = GeometryElement.projectSingle(source, aSnapper50, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(source, proj.getSourceFeature());
        Assert.assertEquals(proj.getCoord(), new Coordinate(30, 0));
    }

    /** Projette le point sur le point du milieu */
    @Test
    public void projectMultiple1() throws ParseException {
        BasicFeature source = new BasicFeature(schema);
        source.setGeometry(reader.read("POINT(30 10)"));
        Envelope env = source.getGeometry().getEnvelopeInternal();
        env.expandBy(50.0);
        List<GeometryElement> candidates = index.query(env);
        Map<Feature,Projection> proj = GeometryElement.projectMultiple(source, aSnapper50, candidates, true);
        Assert.assertNotNull(proj);
        Assert.assertEquals(3, proj.size());
        Assert.assertEquals(proj.get(wline1.getFeature()).getCoord(), new Coordinate(20, 0));
        Assert.assertEquals(proj.get(wpoint.getFeature()).getCoord(), new Coordinate(30, 0));
        Assert.assertEquals(proj.get(wline2.getFeature()).getCoord(), new Coordinate(40, 0));
    }

}
