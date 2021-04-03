package fr.michaelm.jump.plugin.topology;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;

import java.util.List;

/**
 * A geometry element representing a single segment of the geometry.
 */
public class SegmentElement extends GeometryElement {

    final Coordinate p0, p1;

    public SegmentElement(GeometryWrapper wGeometry, Coordinate c0, Coordinate c1) {
        super(wGeometry);
        this.p0 = c0;
        this.p1 = c1;
    }

    public int compare(Projection o1, Projection o2) {
        double diff = (d2(p0, o1.getCoord()) - d2(p0, o2.getCoord()));
        return diff > 0 ? 1 : diff < 0 ? -1 : 0;
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    public void insert(List<CoordinateList> list) {
        if (list.size() == 0) list.add(new CoordinateList());
        CoordinateList cl = list.get(list.size()-1);
        cl.add(p0, false);
        if (projections != null) {
            projections.sort(this);
            for (Projection projection : projections) {
                cl.add(projection.getCoord(), false);
            }
        }
        cl.add(p1, false);
    }

    public void split(List<CoordinateList> list) {
        if (list.size() == 0) list.add(new CoordinateList());
        CoordinateList cl = list.get(list.size()-1);
        cl.add(p0, false);
        if (projections != null) {
            projections.sort(this);
            for (Projection projection : projections) {
                cl.add(projection.getCoord(), false);
                cl = new CoordinateList();
                list.add(cl);
                cl.add(projection.getCoord(), false);
            }
        }
        cl.add(p1, false);
    }

}
