package io.github.blaney83.dencluecluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * This is the model implementation of DENCLUECluster. Clusters data using the
 * DENCLUE algorithm. This density-based method used Gaussian distribution and
 * locates local maxima using hill-climbing. Data is pre-processed into grid
 * cells (using a variation of the OptiGrid approach) and the summation of
 * maxima is restricted to neighboring cells keep runtime low. For this reason,
 * the DENCLUE method is faster than DBScan and OPTICS algorithms.
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

	private final SettingsModelDoubleBounded m_sigmaValue = new SettingsModelDoubleBounded(
			DENCLUEClusterNodeModel.CFGKEY_SIGMA_VALUE, DENCLUEClusterNodeModel.DEFAULT_SIGMA_VALUE, 0,
			Double.MAX_VALUE);
	private final SettingsModelDoubleBounded m_xiValue = new SettingsModelDoubleBounded(
			DENCLUEClusterNodeModel.CFGKEY_XI_VALUE, DENCLUEClusterNodeModel.DEFAULT_XI_VALUE, 0, Double.MAX_VALUE);

	private int m_numDimensions = 0;
	private ArrayList<Integer> m_columnIndices = new ArrayList<Integer>();
	private LinkedHashMap<Integer, DataColumnDomain> m_columnDomains = new LinkedHashMap<Integer, DataColumnDomain>();

	/**
	 * Constructor for the node model.
	 */
	protected DENCLUEClusterNodeModel() {

		super(1, 1);

		// export cluster models at 2nd out-port
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		BufferedDataTable dataTable = inData[IN_PORT];
		double[] hyperCubeDimensions = new double[m_columnDomains.size()];
		double totalHyperCubes = 1;
		Map<Integer, double[][]> m_hyperCubeBoundaries = new LinkedHashMap<Integer, double[][]>();


		int indexCount = 0;

		for (Map.Entry<Integer, DataColumnDomain> entry : m_columnDomains.entrySet()) {

			double colLowerBound = ((DoubleCell) entry.getValue().getLowerBound()).getDoubleValue();
			double colUpperBound = ((DoubleCell) entry.getValue().getUpperBound()).getDoubleValue();

			double columnRange = Math.abs(colUpperBound) + Math.abs(colLowerBound);
			double hyperCubeColumnNumber = columnRange / (2 * m_sigmaValue.getDoubleValue());
			totalHyperCubes *= hyperCubeColumnNumber;
			hyperCubeDimensions[indexCount] = hyperCubeColumnNumber;

			double[][] m_columnBoundaries = new double[(int) Math.ceil(hyperCubeColumnNumber)][2];

			double currentLowBound = colLowerBound;
			for (int i = 0; i < hyperCubeColumnNumber; i++) {
				if (currentLowBound <= colUpperBound) {
					double[] colBounds = new double[] { currentLowBound,
							currentLowBound + (i * (2 * m_sigmaValue.getDoubleValue())) };
					m_columnBoundaries[i] = colBounds;
				}
				currentLowBound += (2 * m_sigmaValue.getDoubleValue());
			}
			
			m_hyperCubeBoundaries.put(entry.getKey(), m_columnBoundaries);
			indexCount++;
		}
		//choosing the "order" of the b+ tree using the total records/ size of key in Bytes - 1
		// may need further optimization later on
		int branchingFactor = (int)((dataTable.size())/((m_hyperCubeBoundaries.size() * 4) - 1));
		
		BPlusTree<int[], DENCLUEHyperCube> bTree = new BPlusTree<int[], DENCLUEHyperCube>(branchingFactor);
		//potentially expand to multi-threaded index searching for row feature vectors
		//also, explore bulk loading for HyperCubes into B+ tree
		for(DataRow row: dataTable) {
			int[] indexedKey = new int[m_hyperCubeBoundaries.size()];
			double[] featureVector = new double[m_hyperCubeBoundaries.size()];
			int count = 0;
			for(Map.Entry<Integer, double[][]> entry : m_hyperCubeBoundaries.entrySet()) {
				double rowColVal = ((DoubleCell) row.getCell(entry.getKey())).getDoubleValue();
				featureVector[count] = rowColVal;
				//binary search
			    int lowInd = 0;
			    int highInd = entry.getValue().length;

			    while(lowInd <= highInd){

			        int middleInd = (highInd + lowInd) / 2;

			        if(entry.getValue()[middleInd][0] > rowColVal){
			            highInd = middleInd;
			        }else if(entry.getValue()[middleInd][1] <= rowColVal){
			            lowInd = middleInd - 1;
			        }else{
			            indexedKey[count] = middleInd;
			        }
			    }
			    count ++;
			}
			DENCLUEHyperCube rowMasterCube = bTree.search(indexedKey);
			if(rowMasterCube != null) {
				rowMasterCube.addMember(row.getKey(), featureVector);
			}else {
				bTree.insert(indexedKey, new DENCLUEHyperCube(indexedKey, row.getKey(), featureVector));
			}
		}
		
		
		// ex hyperCubeDimensions [25, 25, 25]
		// for for(int j = 0; j < hyperCubeDimensions[i]; j ++)
		// int current x val = columnLowerBounds [i]
		// while (xval < columnUpperBounds[i])
		// if(i = hyperCubeDimenstions.length -1) (if this is the last column, then new
		// hyper cube with current values (x-min, y-min, z-min) and
		// (x-min + 2 sigma, y-min + 2 sigma, z-min + 2 sigma) as dimensions. Add cube
		// to array with label 1, 1, 1, then 1, 1, 2, then 1, 1, 3
		// then 1, 2, 1, then, 1,2, 2, then, 1,2 3, then 1,3,1 then, 1,3,2, then 1,3,3,
		// then, 2,1,1, then 2,1,2, then 2,1,3, then 2,2,1, 2,2,2, etc.

	

	// Step 1

	// Turn all rows into feature vectors

	// Create d-dimensional hyper cubes, each with nFeatureVectorMembers- pointers
	// at feature vectors
	// n-number of members
	// nSum- linear sum of feature vectors

	// For each Cp cubes, if(Csp member)=>For each Csp
	// if(d(c_curr, c_iter) <= 4(little sigma))
	// c_curr has c_iter as neighbor
	// Csp member = Csp member of Cp where numPoints >= min numPoints at defined by
	// ("E" min points per d-dimensional cube)
	// NOTE: B+ Tree structure needed

	// Step 2

	// gradient hill-climbing, localalized density functions etc., cluster
	// assignments

	// Step 3

	// for x*, create model and export at out-port 2
	}

	private void processColumn(final int dimensionLength, final double lowerBound, final double upperBound) {

		for (int j = 0; j < dimensionLength; j++) {
			if (lowerBound < upperBound) {

			}
		}

	}

	@Override
	protected void reset() {

	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		DataTableSpec tableSpecs = inSpecs[IN_PORT];

		for (int i = 0; i < tableSpecs.getNumColumns(); i++) {
			DataColumnSpec colSpecs = tableSpecs.getColumnSpec(i);

			// Validate Double compatibility
			if (!colSpecs.getType().isCompatible(DoubleValue.class)) {
				continue;
			}

			// Validate domains exist (else throw error- must calculate domains prior to
			// execution)
			DataColumnDomain colDomain = colSpecs.getDomain();
			if (!colDomain.hasLowerBound() || !colDomain.hasUpperBound()) {
				throw new InvalidSettingsException(
						"Domains have not been calculated for a column(s) in your table. Please pre-process your data table with a Domain"
								+ " creator node and reconfigure.");
			}

			// determine d-dimensions
			m_numDimensions++;
			m_columnIndices.add(i);
			m_columnDomains.put(i, colDomain);
		}

		// Validate Double compatibility
		if (m_numDimensions == 0) {
			throw new InvalidSettingsException("The data table provided does not contain any numeric data");
		}

		// data table output spec and configure "Cluster" column
		DataColumnSpec clusterColSpec = createClusterColumnSpec();
		DataTableSpec appendSpec = new DataTableSpec(clusterColSpec);
		DataTableSpec outputSpec = new DataTableSpec(inSpecs[IN_PORT], appendSpec);

		// cluster summary table output spec
		DataColumnSpec summaryColSpec = createSummaryColumnSpec();
		DataTableSpec summaryTableSpec = new DataTableSpec(DENCLUEClusterNodeModel.OUTPUT_SUMMARY_TABLE_NAME,
				summaryColSpec);

		// return new data table with cluster column and summary table specs
		return new DataTableSpec[] { outputSpec, summaryTableSpec };
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
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

	}

	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

	}

	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

	}

}
