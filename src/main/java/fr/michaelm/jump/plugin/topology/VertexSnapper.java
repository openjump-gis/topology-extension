package fr.michaelm.jump.plugin.topology;

import com.vividsolutions.jump.feature.Feature;

/**
 * A VertexSnapper specifies if a point can be projected to a
 * {@link GeometryElement} and if it can be snapped to one of the
 * element's coordinates.
 */
interface VertexSnapper {

    /**
     * Return a {@link Projection} if source has been projected to the
     * GeometryElement, and null otherwise.
     * @param source the source feature to be projected
     * @param element a Geometry element
     * @return a Projection or null
     */
    Projection snap(Feature source, GeometryElement element);

}
