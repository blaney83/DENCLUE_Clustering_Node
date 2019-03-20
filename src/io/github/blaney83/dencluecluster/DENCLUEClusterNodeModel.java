package io.github.blaney83.dencluecluster;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
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
		// Step 1

		// Turn all rows into feature vectors (postponed/not needed at this time)

		// Create d-dimensional hyper cubes, each with nFeatureVectorMembers- pointers
		// at feature vectors
		// n-number of members
		// nSum- linear sum of feature vectors
		double[] hyperCubeDimensions = new double[m_columnDomains.size()];
		double totalHyperCubes = 1;
		Map<Integer, double[][]> m_hyperCubeBoundaries = new LinkedHashMap<Integer, double[][]>();

		int indexCount = 0;
		// potentially move this index creation to the configure method
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
		// choosing the "order" of the b+ tree using the total records/ size of key in
		// Bytes - 1
		// may need further optimization later on
		int branchingFactor = (int) ((dataTable.size()) / ((m_hyperCubeBoundaries.size() * 4) - 1));

		// STORE CUBES OR KEYS (CHOOSE LATER BASED ON PERFORMANCE
		// x subset of allCubes such that x has no set membership with denseCubes; x =
		// sparsely populated cubes
		// highly populated cube keys
		ArrayList<int[]> denseCubeKeys = new ArrayList<int[]>();
		// all populated cube keys
		ArrayList<int[]> allCubeKeys = new ArrayList<int[]>();
		// highly populated cubes
		ArrayList<DENCLUEHyperCube> denseCubes = new ArrayList<DENCLUEHyperCube>();
		// all populated cubes
		ArrayList<DENCLUEHyperCube> allCubes = new ArrayList<DENCLUEHyperCube>();

		DENCLUEBPlusTree<int[], DENCLUEHyperCube> bTree = new DENCLUEBPlusTree<int[], DENCLUEHyperCube>(
				branchingFactor);
		// potentially expand to multi-threaded index searching for row feature vectors
		// also, explore bulk loading for HyperCubes into B+ tree
		for (DataRow row : dataTable) {
			int[] indexedKey = new int[m_hyperCubeBoundaries.size()];
			double[] featureVector = new double[m_hyperCubeBoundaries.size()];
			int count = 0;
			for (Map.Entry<Integer, double[][]> entry : m_hyperCubeBoundaries.entrySet()) {
				double rowColVal = ((DoubleCell) row.getCell(entry.getKey())).getDoubleValue();
				featureVector[count] = rowColVal;
				// binary search
				int lowInd = 0;
				int highInd = entry.getValue().length;

				while (lowInd <= highInd) {

					int middleInd = (highInd + lowInd) / 2;

					if (entry.getValue()[middleInd][0] > rowColVal) {
						highInd = middleInd;
					} else if (entry.getValue()[middleInd][1] <= rowColVal) {
						lowInd = middleInd - 1;
					} else {
						indexedKey[count] = middleInd;
					}
				}
				count++;
			}
			DENCLUEHyperCube rowMasterCube = bTree.search(indexedKey);
			if (rowMasterCube != null) {
				boolean isHighlyPopulated = rowMasterCube.addMember(row.getKey(), featureVector);
				if (isHighlyPopulated) {
					denseCubeKeys.add(indexedKey);
					denseCubes.add(rowMasterCube);
				}
			} else {
				rowMasterCube = new DENCLUEHyperCube(indexedKey, row.getKey(), featureVector);
				bTree.insert(indexedKey, rowMasterCube);
				allCubeKeys.add(indexedKey);
				allCubes.add(rowMasterCube);
			}
		}

		if (denseCubes.size() < 1) {
			throw new ExecutionException(
					"The parameters used in your search classified all data as noise. Please re-evaluate your parameter choices and "
							+ "re-execute this node.");
		}

		//Complexity Csp * Cp; Csp << Cp
		for (DENCLUEHyperCube cube : denseCubes) {
			for (DENCLUEHyperCube sparseCube : allCubes) {
				if (!cube.equals(sparseCube)) {
					if (cube.isNeighbor(sparseCube)) {
						if (cube.isConnected(sparseCube, m_sigmaValue.getDoubleValue())) {
							cube.addNeighbor(sparseCube);
						}
					}
				}
			}
		}
	
		//this new btree will hold all clusters and old btree after loop will be noise cluster holding only sparse noise cubes
		int clusterFactor = (int) ((denseCubes.size()) / ((m_hyperCubeBoundaries.size() * 4) - 1));
		DENCLUEBPlusTree<int[], DENCLUEHyperCube> clusterTree = new DENCLUEBPlusTree<int[], DENCLUEHyperCube>(
				clusterFactor);
		
		//consolidating cubes based on their neighbors. Deleting old cubes as they are merged and checking for partially merged
		// super-hyper cubes
		for (DENCLUEHyperCube cube : denseCubes) {
			for(int[] indexKey : cube.getNeighborCells()) {
				DENCLUEHyperCube mergingCube = bTree.search(indexKey);
				DENCLUEHyperCube potentialSuperCube = clusterTree.search(indexKey);
				if(mergingCube != null) {
					cube.addNeighbor(mergingCube);
					bTree.delete(indexKey);
				}
				if(potentialSuperCube != null) {
					cube.addNeighbor(potentialSuperCube);
					clusterTree.delete(indexKey);
				}
			}
			bTree.delete(cube.getCubeKey());
			clusterTree.insert(cube.getCubeKey(), cube);
		}
		
		// in theory at this point, we have merged supercubes and sporadic noise cubes in two separate b+ search trees and
		// part 1 is complete...

		// TODO PART1
		
		// possibly create class IndexedKey implementing Comparable to check for matches
		// to the key
		// the first .equals check could be if sum1 != sum2 return false to speed
		// evaluation time
		// then for each int in both int[]1 and int[]2 stop evaluation at first
		// mis-match
		// this could extend the ability of the node to handle dimensionality > 20 as in
		// higher
		// dimensions the joined integer value of indicies would exceed Integer.MAX
		
		// combine iterations where possible to improve performance
		
		// may need to change cube collections to sets
		
		// j-unit tests for part 1

		// Step 2

		// gradient hill-climbing, localalized density functions etc., cluster
		// assignments

		// Step 3

		// for x*, create model and export at out-port 2
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
