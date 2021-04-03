package com.vividsolutions.jcs.conflate.coverage;

import java.util.*;

import com.vividsolutions.jcs.qa.FeatureSegment;

/**
 * An index of line segments.
 *
 * @author unascribed
 * @version 1.0
 */
public class SegmentIndex {
    
    //private boolean built = false;

    private final Set<FeatureSegment> segments = new HashSet<>();

    public SegmentIndex() {}

    public void add(FeatureSegment segment) {
        segments.add(segment);
    }
    
    public boolean contains(FeatureSegment testSegment) {
        return segments.contains(testSegment);
    }
    
    public int size() {return segments.size();}
    
}