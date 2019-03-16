package io.github.blaney83.dencluecluster;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;



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
	
	
	// only two parameters needed (unless optional "E"_c)
	
	// 1) "E"- minimum density
	
	// 2) "litte sigma"- influence factor 
	
	// 
	
    protected DENCLUEClusterNodeDialog() {
        super();
        
		
		addDialogComponent(new DialogComponentNumber(
				new SettingsModelDoubleBounded(DENCLUEClusterNodeModel.CFGKEY_SIGMA_VALUE, DENCLUEClusterNodeModel.DEFAULT_SIGMA_VALUE, 0, Double.MAX_VALUE), 
				"Select a \\&#x3C3; (point influence factor)", .03
		));
		
		addDialogComponent(new DialogComponentNumber(
				new SettingsModelDoubleBounded(DENCLUEClusterNodeModel.CFGKEY_XI_VALUE, DENCLUEClusterNodeModel.DEFAULT_XI_VALUE, 0, Double.MAX_VALUE), 
				"Select a \\&#x3BE; (min density level for density-attractor to be significant)", .03
		));                    
    }
}

