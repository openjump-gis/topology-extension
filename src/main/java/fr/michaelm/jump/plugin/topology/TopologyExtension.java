/*
 * (C) 2011 Micha&euml;l Michaud
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
 * michael.michaud@free.fr
 *
 */

package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jcs.plugin.clean.*;
import com.vividsolutions.jcs.plugin.qa.*;

import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

// History
// 1.1.0 (2021-08-08) : refactoring for new I18N management
// 1.0.0 (2021-04-03) : clean, generify and refactor for OpenJUMP2
// 0.9.3 (2018-09-12) : fix a bug in CoverageCleaner
// 0.9.2 (2018-01-27) : add finnish translations
// 0.9.1 (2016-11-21) : remove RemoveMicroSegmentsPlugIn, replaced by
//                      com.vividsolutions.jump.workbench.ui.plugin.analysis.RemoveSmallSegments
//                      included in the core of OpenJUMP
// 0.9.0 (2016-06-18) : add an option to interpolate z in CoverageCleanerPlugIn
// 0.8.2 (2016-06-12) : fix two bugs in CoverageCleanerPlugIn (angle between
//                      - angle between segments was not computed correctly
//                      - holes were not managed in some cases
// 0.8.1 (2014-05-27) : fix a bug in project point on line affecting multiple
//                      target option
// 0.8.0 (2014-05-26) : rewrite project point on line to handle correctly
//                      the insertion of several points on a single segment
// 0.7.1 (2014-05-17) : fix a bug in ProjectPointsOnLinesPlugIn
// 0.7.0 (2013-01-27) : add CoverageCleanerPlugIn
// 0.6.1 (2013-01-09) : add es/it/fi language files (from G. Aruta and J. Rahkonen)
// 0.6.0 (2012-09-15) : complete rewrite of ProjectPointsOnLines to make multi-
//                      projections possible
// 0.5.1 (2012-08-30) : fixed a problem in the UI (line_operation was not always 
//                      available when line layer was editable)
// 0.5.0 (2012-06-25) : add ProjectPointsOnLinesPlugIn
// 0.4.0 (2012-05-20) : fix a bug in node degree computation
//                    : add an option checking attribute equality before snapping
// 0.3.4 (2012-05-17) : nodes inserted in wrong segment in reference layer
//                      deactivate clean (not robust enough for 1.5.2 release)
//                      clean code (remove comments and system.out)
// 0.3.3 (2011-11-22) : makes clean available again
// 0.3.2 (2011-11-08) : fix a typo in fr language file
// 0.3   (2011-04-22) : inclusion in a larger topology extension
// 0.2   (2011-04-11) : upgrade to new MultiInputDialog/MultiTabInputDialog
//                      coming in OpenJUMP version 1.4.1
// 0.1b  (2010-05-21) : beta version
// 0.1a  (2010-05-14) : alpha version
// 0.1   (2010-04-22) : initial version

public class TopologyExtension extends Extension {

    public String getName() {
        return "Topology Extension (Micha\u00EBl Michaud)";
    }

    public String getVersion() {
        return "1.1.0 (2021-08-08)";
    }

    public void configure(PlugInContext context) throws Exception {
        
        new MatchedSegmentsPlugIn().initialize(context);
        new OverlapFinderPlugIn().initialize(context);
        new CoverageGapPlugIn().initialize(context);
        new CoverageOverlapFinderPlugIn().initialize(context);
        new CloseVertexFinderPlugIn().initialize(context);
        //new OffsetBoundaryCornerFinderPlugIn().initialize(context);        
        new NetworkTopologyCleaningPlugIn().initialize(context);
        new ProjectPointsOnLinesPlugIn().initialize(context);
        new CoverageCleanerPlugIn().initialize(context);
    }

}