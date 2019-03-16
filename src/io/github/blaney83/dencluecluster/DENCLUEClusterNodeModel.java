package io.github.blaney83.dencluecluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of DENCLUECluster.
 * Clusters data using the DENCLUE algorithm. This density-based method used Gaussian distribution and locates local maxima using hill-climbing. Data is pre-processed into grid cells (using a variation of the OptiGrid approach) and the summation of maxima is restricted to neighboring cells keep runtime low. For this reason, the DENCLUE method is faster than DBScan and OPTICS algorithms.
 *
 * @author Benjamin Laney
 */
public class DENCLUEClusterNodeModel extends NodeModel {
	
	static final int IN_PORT = 0;
	
	static final String OUTPUT_SUMMARY_TABLE_NAME = "Summary Table";
    
	static final String CFGKEY_SIGMA_VALUE = "sigmaValue";
	static final String CFGKEY_XI_VALUE = "xiValue";
	
	static final double DEFAULT_SIGMA_VALUE = .3;
	static final double DEFAULT_XI_VALUE = .3;
	
	private final SettingsModelDoubleBounded m_sigmaValue = new SettingsModelDoubleBounded(DENCLUEClusterNodeModel.CFGKEY_SIGMA_VALUE, 
			DENCLUEClusterNodeModel.DEFAULT_SIGMA_VALUE, 0, Double.MAX_VALUE);
	private final SettingsModelDoubleBounded m_xiValue = new SettingsModelDoubleBounded(DENCLUEClusterNodeModel.CFGKEY_XI_VALUE, 
			DENCLUEClusterNodeModel.DEFAULT_XI_VALUE, 0, Double.MAX_VALUE);
	
	
	private int m_numDimensions = 0;
	private ArrayList<Integer> m_columnIndices = new ArrayList<Integer>();
	private LinkedHashMap<Integer, DataColumnDomain> m_columnDomains = new LinkedHashMap<Integer, DataColumnDomain>();

    /**
     * Constructor for the node model.
     */
    protected DENCLUEClusterNodeModel() {

        super(1, 1);
        
        //export cluster models at 2nd out-port 
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	//Pre-Requisite Calculations- Ensure domain calculations up to date.
    	
    	// Step 1
    	
    	// Turn all rows into feature vectors
    	
    	// Create d-dimensional hyper cubes, each with nFeatureVectorMembers- pointers at feature vectors
    	//											n-number of members
    	//											nSum- linear sum of feature vectors
    	
    	// For each Cp cubes, if(Csp member)=>For each Csp
    	//										if(d(c_curr, c_iter) <= 4(little sigma))
    	//												c_curr has c_iter as neighbor
    	// Csp member = Csp member of Cp where numPoints >= min numPoints at defined by ("E" min points per d-dimensional cube)
        //	NOTE: B+ Tree structure needed
    	
    	// Step 2
    	
    	//gradient hill-climbing, localalized density functions etc., cluster assignments
    	
    	// Step 3 
    	
    	// for x*, create model and export at out-port 2
    }

    @Override
    protected void reset() {

    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	
    	DataTableSpec tableSpecs = inSpecs[IN_PORT];
    	
    	for(int i = 0; i < tableSpecs.getNumColumns(); i ++) {
    		DataColumnSpec colSpecs = tableSpecs.getColumnSpec(i);
    		
    		//Validate Double compatibility
    		if(!colSpecs.getType().isCompatible(DoubleValue.class)) {
    			continue;
    		}
    		
    		//Validate domains exist (else throw error- must calculate domains prior to execution)
    		DataColumnDomain colDomain = colSpecs.getDomain();
    		if(!colDomain.hasLowerBound() || !colDomain.hasUpperBound()) {
    			throw new InvalidSettingsException("Domains have not been calculated for a column(s) in your table. Please pre-process your data table with a Domain"
    					+ " creator node and reconfigure.");
    		}
    		
    		//determine d-dimensions
    		m_numDimensions ++;
    		m_columnIndices.add(i);
    		m_columnDomains.put(i, colDomain);
    	}
    	
    	//Validate Double compatibility
    	if(m_numDimensions == 0) {
    		throw new InvalidSettingsException("The data table provided does not contain any numeric data");
    	}
    	
    	//data table output spec and configure "Cluster" column
		DataColumnSpec clusterColSpec = createClusterColumnSpec();
		DataTableSpec appendSpec = new DataTableSpec(clusterColSpec);
		DataTableSpec outputSpec = new DataTableSpec(inSpecs[IN_PORT], appendSpec);
		
		//cluster summary table output spec
		DataColumnSpec summaryColSpec = createSummaryColumnSpec();
		DataTableSpec summaryTableSpec = new DataTableSpec(DENCLUEClusterNodeModel.OUTPUT_SUMMARY_TABLE_NAME, summaryColSpec);
   	
    	//return new data table with cluster column and summary table specs
        return new DataTableSpec[]{outputSpec, summaryTableSpec};
    }
    
	private DataColumnSpec createClusterColumnSpec() {
		DataColumnSpecCreator newColSpecCreator = new DataColumnSpecCreator("Cluster", StringCell.TYPE);
		DataColumnSpec newColSpec = newColSpecCreator.createSpec();
		return newColSpec;
	}
	
	private DataColumnSpec createSummaryColumnSpec() {
		DataColumnSpecCreator newColSpecCreator = new DataColumnSpecCreator("Count", IntCell.TYPE);
		DataColumnSpec newColSpec = newColSpecCreator.createSpec();
		return newColSpec;
	}

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    }

}

