package io.github.blaney83.dencluecluster;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "DENCLUECluster" Node.
 * Clusters data using the DENCLUE algorithm. This density-based method used Gaussian distribution and locates local maxima using hill-climbing. Data is pre-processed into grid cells (using a variation of the OptiGrid approach) and the summation of maxima is restricted to neighboring cells keep runtime low. For this reason, the DENCLUE method is faster than DBScan and OPTICS algorithms.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Benjamin Laney
 */
public class DENCLUEClusterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring DENCLUECluster node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected DENCLUEClusterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    DENCLUEClusterNodeModel.CFGKEY_COUNT,
                    DENCLUEClusterNodeModel.DEFAULT_COUNT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "Counter:", /*step*/ 1, /*componentwidth*/ 5));
                    
    }
}

