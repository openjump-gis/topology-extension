package fr.michaelm.jump.plugin.topology;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;

import java.util.List;

/**
 * An {@link GeometryElement} of a 0-dimensional Geometry
 */
public class PointElement extends GeometryElement {

    final Coordinate p;

    public PointElement(GeometryWrapper wGeometry, Coordinate c) {
        super(wGeometry);
        this.p = c;
    }

    public int compare(Projection o1, Projection o2) {
        return 0;
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    public void insert(List<CoordinateList> list) {
        list.add(new CoordinateList(new Coordinate[]{p}));
    }

    public void split(List<CoordinateList> list) {
        list.add(new CoordinateList(new Coordinate[]{p}));
    }
}
