package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jump.feature.Feature;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GeometryWrapper has a pointer to the Feature containing this geometry,
 * and embeds a hierarchical structure containing all the {@link GeometryElement}
 * of this Geometry (i.e. SegmentElement or PointElement).
 * At construction time, a GeometryWrapper puts all its elements in a
 * spatial index.
 */
public abstract class GeometryWrapper {

    Feature feature;
    protected boolean geometryChanged;

    private GeometryWrapper(Feature feature, STRtree index) {
        this.feature = feature;
        if (!feature.getGeometry().isEmpty()) {
            index(index);
        }
    }

    public Feature getFeature() {
        return feature;
    }

    abstract protected void index(STRtree index);

    public void insert() {}

    public void split() {}

    static GeometryWrapper createWrapper(Feature feature, STRtree index) {
        Geometry geometry = feature.getGeometry();
        if (geometry instanceof Point) {
            return new WPoint(feature, index);
        } else if (geometry instanceof LineString) {
            return new WLineString(feature, index);
        } else if (geometry instanceof Polygon) {
            return new WPolygon(feature, index);
        } else if (geometry instanceof MultiPoint) {
            return new WMultiPoint(feature, index);
        } else if (geometry instanceof MultiLineString) {
            return new WMultiLineString(feature, index);
        } else if (geometry instanceof MultiPolygon) {
            return new WMultiPolygon(feature, index);
        } else {
            return new WGeometryCollection(feature, index);
        }
    }

    static class WPoint extends GeometryWrapper {
        GeometryElement element;
        WPoint(Feature feature, STRtree index) {
            super(feature, index);
        }
        protected void index(STRtree index) {
            element = getElements(this, (Point)feature.getGeometry(), index);
        }
    }

    static class WLineString extends GeometryWrapper {
        List<GeometryElement> elements;
        WLineString(Feature feature, STRtree index) {
            super(feature, index);
        }
        protected void index(STRtree index) {
            elements = getElements(this, (LineString)feature.getGeometry(), index);
        }
        public void insert() {
            if (geometryChanged) return;
            feature.setGeometry(insertInLineString(feature.getGeometry().getFactory(), elements));
            geometryChanged = true;
        }
        public void split() {
            if (geometryChanged) return;
            Geometry geom = split(feature.getGeometry().getFactory(), elements);
            if (geom.getNumGeometries() == 1) geom = geom.getGeometryN(0);
            feature.setGeometry(geom);
            geometryChanged = true;
        }
    }

    static class WPolygon extends GeometryWrapper {
        List<List<GeometryElement>> elements;
        WPolygon(Feature feature, STRtree index) {
            super(feature, index);
        }
        protected void index(STRtree index) {
            elements = getElements(this, (Polygon)feature.getGeometry(), index);
        }
        public void insert() {
            if (geometryChanged) return;
            LinearRing exteriorRing = insertInLinearRing(feature.getGeometry().getFactory(), elements.get(0));
            LinearRing[] interiorRings = new LinearRing[elements.size()-1];
            for (int i = 0 ; i < elements.size()-1 ; i++) {
                interiorRings[i] = insertInLinearRing(feature.getGeometry().getFactory(), elements.get(i+1));
            }
            feature.setGeometry(feature.getGeometry().getFactory().createPolygon(exteriorRing, interiorRings));
            geometryChanged = true;
        }
        public void split() {
            insert();
        }
    }

    static class WMultiPoint extends GeometryWrapper {
        List<GeometryElement> elements;
        WMultiPoint(Feature feature, STRtree index) {
            super(feature, index);
        }
        protected void index(STRtree index) {
            int numComponents = feature.getGeometry().getNumGeometries();
            elements = new ArrayList<>(numComponents);
            for (int i = 0 ; i < numComponents ; i++) {
                elements.add(getElements(this, (Point)feature.getGeometry().getGeometryN(i), index));
            }
        }
        public void insert() {}
        public void split() {}
    }

    static class WMultiLineString extends GeometryWrapper {
        List<List<GeometryElement>> elements;
        WMultiLineString(Feature feature, STRtree index) {
            super(feature, index);
        }
        protected void index(STRtree index) {
            int numComponents = feature.getGeometry().getNumGeometries();
            elements = new ArrayList<>(numComponents);
            for (int i = 0 ; i < numComponents ; i++) {
                elements.add(getElements(this, (LineString)feature.getGeometry().getGeometryN(i), index));
            }
        }
        public void insert() {
            if (geometryChanged) return;
            LineString[] lineStrings = new LineString[elements.size()-1];
            for (int i = 0 ; i < elements.size()-1 ; i++) {
                lineStrings[i] = insertInLineString(feature.getGeometry().getFactory(), elements.get(i + 1));
            }
            feature.setGeometry(feature.getGeometry().getFactory().createMultiLineString(lineStrings));
            geometryChanged = true;
        }
        public void split() {
            if (geometryChanged) return;
            List<LineString> lineStrings = new ArrayList<>(elements.size());
            for (int i = 0 ; i < elements.size() ; i++) {
                MultiLineString mls = split(feature.getGeometry().getFactory(), elements.get(i));
                for (int j = 0 ; j < mls.getNumGeometries() ; j++) {
                    lineStrings.add((LineString)mls.getGeometryN(j));
                }
            }
            feature.setGeometry(feature.getGeometry().getFactory().createMultiLineString(
                    lineStrings.toArray(new LineString[lineStrings.size()])));
            geometryChanged = true;
        }
    }

    static class WMultiPolygon extends GeometryWrapper {
        List<List<List<GeometryElement>>> elements;
        WMultiPolygon(Feature feature, STRtree index) {
            super(feature, index);
        }
        protected void index(STRtree index) {
            int numComponents = feature.getGeometry().getNumGeometries();
            elements = new ArrayList<>(numComponents);
            for (int i = 0 ; i < numComponents ; i++) {
                elements.add(getElements(this, (Polygon)feature.getGeometry().getGeometryN(i), index));
            }
        }
        public void insert() {
            if (geometryChanged) return;
            Polygon[] polygons = new Polygon[elements.size()];
            for (int i = 0 ; i < polygons.length ; i++) {
                LinearRing exteriorRing = insertInLinearRing(feature.getGeometry().getFactory(), elements.get(i).get(0));
                LinearRing[] interiorRings = new LinearRing[elements.get(i).size()-1];
                for (int j = 0 ; j < elements.get(i).size()-1 ; j++) {
                    interiorRings[j] = insertInLinearRing(feature.getGeometry().getFactory(), elements.get(i).get(j+1));
                }
                polygons[i] = feature.getGeometry().getFactory().createPolygon(exteriorRing, interiorRings);

            }
            feature.setGeometry(feature.getGeometry().getFactory().createMultiPolygon(polygons));
            geometryChanged = true;
        }
        public void split() {
            insert();
        }
    }

    static class WGeometryCollection extends GeometryWrapper {
        List<Object> elements;

        WGeometryCollection(Feature feature, STRtree index) {
            super(feature, index);
        }

        protected void index(STRtree index) {
            int numComponents = feature.getGeometry().getNumGeometries();
            elements = new ArrayList<>(numComponents);
            for (int i = 0 ; i < numComponents ; i++) {
                Geometry geom = feature.getGeometry().getGeometryN(i);
                if (geom instanceof Point) {
                    elements.add(getElements(this, (Point)geom, index));
                } else if (geom instanceof LineString) {
                    elements.add(getElements(this, (LineString)geom, index));
                } else if (geom instanceof Polygon) {
                    elements.add(getElements(this, (Polygon)geom, index));
                }
            }
        }
    }

    private static LineString insertInLineString(GeometryFactory factory, List<GeometryElement> elements) {
        if (elements.size() == 0) return factory.createLineString(new Coordinate[0]);
        List<CoordinateList> list = new ArrayList<>(1);
        for (GeometryElement element : elements) {
            element.insert(list);
        }
        return factory.createLineString(list.get(0).toCoordinateArray());
    }

    private static LinearRing insertInLinearRing(GeometryFactory factory, List<GeometryElement> elements) {
        if (elements.size() == 0) return factory.createLinearRing(new Coordinate[0]);
        List<CoordinateList> list = new ArrayList<>(1);
        for (GeometryElement element : elements) {
            element.insert(list);
        }
        System.out.println(Arrays.toString(list.get(0).toCoordinateArray()));
        return factory.createLinearRing(list.get(0).toCoordinateArray());
    }


    static MultiLineString split(GeometryFactory factory, List<GeometryElement> elements) {
        if (elements.size() == 0) return factory.createMultiLineString(new LineString[0]);
        List<CoordinateList> coordLists = new ArrayList<>();
        for (GeometryElement element : elements) {
            element.split(coordLists);
        }
        List<LineString> lineStrings = new ArrayList<>(coordLists.size());
        for (CoordinateList coordList : coordLists) {
            if (coordList.size() < 2) continue;
            lineStrings.add(factory.createLineString(coordList.toCoordinateArray()));
        }
        return factory.createMultiLineString(lineStrings.toArray(new LineString[lineStrings.size()]));
    }


    private static List<List<GeometryElement>> getElements(GeometryWrapper geom, Polygon poly, STRtree index) {
        List<List<GeometryElement>> elements = new ArrayList<>(poly.getNumInteriorRing()+1);
        elements.add(getElements(geom, poly.getExteriorRing(), index));
        for (int i = 0 ; i < poly.getNumInteriorRing() ; i++) {
            elements.add(getElements(geom, poly.getInteriorRingN(i), index));
        }
        return elements;
    }

    private static List<GeometryElement> getElements(GeometryWrapper geom, LineString line, STRtree index) {
        Coordinate[] cc = line.getCoordinates();
        List<GeometryElement> elements = new ArrayList<>(cc.length-1);
        for (int i = 0 ; i < cc.length - 1 ; i++) {
            GeometryElement element = new SegmentElement(geom, cc[i], cc[i+1]);
            elements.add(element);
            index.insert(new Envelope(cc[i], cc[i+1]), element);
        }
        return elements;
    }

    private static GeometryElement getElements(GeometryWrapper geom, Point point, STRtree index) {
        GeometryElement element = new PointElement(geom, point.getCoordinate());
        index.insert(new Envelope(point.getCoordinate()), element);
        return element;
    }

}
