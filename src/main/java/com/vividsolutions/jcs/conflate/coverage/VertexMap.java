/*
 * The JCS Conflation Suite (JCS) is a library of Java classes that
 * can be used to build automated or semi-automated conflation solutions.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jcs.conflate.coverage;

import org.locationtech.jts.geom.Coordinate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps {@link Coordinate}s to their corresponding Vertices,
 * via their original coordinate.
 */
public class VertexMap {

    // mmichaud : TreeMap seems expensive and not justified
    // replaced by a simple HashMap
    Map<Coordinate,Vertex> map = new HashMap<>();

    public VertexMap() {}

    public boolean contains(Coordinate p) {
        return map.containsKey(p);
    }

    /**
     * get the Vertex which source coordinate is p.
     * Note that if the vertex does not exists, it is created and returned by
     * this method.
     * See usage in Shell.
     */
    public Vertex get(Coordinate p) {
        Vertex v = map.get(p);
        if (v == null) {
            v = new Vertex(p);
            map.put(p, v);
        }
        return v;
    }

    public Collection<Vertex> getVertices() {
        return map.values();
    }
}
