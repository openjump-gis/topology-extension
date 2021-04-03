package fr.michaelm.jump.plugin.topology;

import com.vividsolutions.jcs.conflate.coverage.CoverageCleaner;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;


/**
 * Created by UMichael on 10/06/2016.
 */
public class CoverageCleanerTest {

    static WKTReader reader = new WKTReader();


    FeatureCollection createDataset(Geometry...geometries) {
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        FeatureCollection ds = new FeatureDataset(schema);
        for (Geometry g : geometries) {
            Feature feature1 = new BasicFeature(schema);
            feature1.setGeometry(g);
            ds.add(feature1);
        }
        return ds;
    }

    /** Projette le point sur la première ligne */
    @Test
    public void coverageCleanerTest1() throws ParseException {
        Geometry poly1 = reader.read("POLYGON (( 677942.5 6863565.5 75.1, 677944.7 6863557 75.1, 677932.4 6863553.7 75.1, 677930.1 6863562.1 75.1, 677942.5 6863565.5 75.1 ))");
        Geometry poly2 = reader.read("POLYGON (( 677938 6863555.2 75.1, 677942.7 6863556.5 75.1, 677943.7 6863552.3 75.1, 677939.1 6863551 75.3, 677938 6863555.2 75.1 ))");
        FeatureCollection fc = createDataset(poly1, poly2);
        CoverageCleaner cleaner = new CoverageCleaner(fc, new DummyTaskMonitor());
        cleaner.process(new CoverageCleaner.Parameters(0.3, 22));
        FeatureCollection result = cleaner.getAdjustedFeatures();
        // 1 objet ajusté
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void coverageCleanerTest2() throws ParseException {
        Geometry poly1 = reader.read("POLYGON (( 796702.4 6303661.9 63.9, 796702.4 6303668.8 63.9, 796713.1 6303670.6 63.9, 796713.1 6303661.5 63.9, 796712.7 6303661.5 63.9, 796702.4 6303661.9 63.9 ))");
        Geometry poly2 = reader.read("POLYGON (( 796712.7 6303651.3 63.7, 796712.7 6303661.5 63.7, 796717.7 6303661.3 63.7, 796717.6 6303651 63.7, 796712.7 6303651.3 63.7 ))");
        FeatureCollection fc = createDataset(poly1, poly2);
        CoverageCleaner cleaner = new CoverageCleaner(fc, new DummyTaskMonitor());
        cleaner.process(new CoverageCleaner.Parameters(0.3, 22));
        FeatureCollection result = cleaner.getAdjustedFeatures();
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void coverageCleanerTest3() throws ParseException {
        Geometry poly1 = reader.read("POLYGON (( 808524 6302863, 808524 6302873, 808542 6302873, 808542 6302863, 808524 6302863 ), ( 808526 6302865, 808531.0022255091 6302864.729235475, 808540 6302865, 808540 6302871, 808526 6302871, 808526 6302865 ))");
        Geometry poly2 = reader.read("POLYGON (( 808526 6302865, 808526 6302871, 808540 6302871, 808540 6302865, 808526 6302865 ))");
        FeatureCollection fc = createDataset(poly1, poly2);
        CoverageCleaner cleaner = new CoverageCleaner(fc, new DummyTaskMonitor());
        cleaner.process(new CoverageCleaner.Parameters(0.3, 22));
        FeatureCollection result = cleaner.getAdjustedFeatures();
        Assert.assertEquals(1, result.size());
    }
}
