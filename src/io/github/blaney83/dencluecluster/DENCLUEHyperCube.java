package io.github.blaney83.dencluecluster;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

public class DENCLUEHyperCube {

	private final int[] m_cubeKey;

	private double[] m_linearSum;

	private double[] m_meanVector;

	private int m_numFeatureVectors = 0;

	private List<DENCLUEHyperCube> m_neighbors;

	private boolean m_isNoise;

	private ArrayList<double[]> m_lowerBounds;

	private ArrayList<double[]> m_upperBounds;

	private ArrayList<RowKey> m_memberRows;

	public DENCLUEHyperCube(final int[] cubekey, final RowKey rowKey, final double[] featureVector) {
		m_cubeKey = cubekey;
		m_memberRows = new ArrayList<RowKey>();
		m_memberRows.add(rowKey);
		m_linearSum = new double[featureVector.length];
		System.arraycopy(featureVector, 0, m_linearSum, 0, m_linearSum.length);
		m_numFeatureVectors++;
		m_isNoise = true;
	}

	public boolean addMember(final RowKey rowKey, final double[] featureVector) {
		m_memberRows.add(rowKey);
		for (int i = 0; i < m_linearSum.length; i++) {
			m_linearSum[i] += featureVector[i];
		}
		m_numFeatureVectors++;

		// as points are added, as the points exceed a certain threshhold,
		// then return the key of this cube to be stored in a highly populated key[]
		// and change the isNoise to false
		// temp value
		boolean highlyPopulated = false;

		if (highlyPopulated) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isNeighbor(final DENCLUEHyperCube otherCube) {
		int[] otherKey = otherCube.getCubeKey();
		int currSum = 0;
		int otherSum = 0;
		int diffSum = 0;
		for (int i = 0; i < otherKey.length; i++) {
			int absDiff = Math.abs(otherKey[i] - m_cubeKey[i]);
			if (absDiff > 1) {
				return false;
			} else if (absDiff == 1) {
				diffSum++;
			} else {
				continue;
			}
			if (diffSum > 1) {
				return false;
			}
		}
		return true;
	}

	public boolean isConnected(final DENCLUEHyperCube otherCube, final double sigma) {
		//euclidian distance
		double[] otherMean = otherCube.findMean();
		m_meanVector = this.findMean();
		double currSum = 0;
		for (int i = 0; i < otherMean.length; i++) {
			double diff = (m_meanVector[i] - otherMean[i]);
			currSum += (diff * diff);

		}
		double finalValue = Math.sqrt(currSum);
		double maxNeighborThreshhold = 4 * sigma;
		if(finalValue <= maxNeighborThreshhold) {
			return true;
		}else {
			return false;
		}
	}

	protected double[] findMean() {
		if (m_meanVector == null) {
			m_meanVector = new double[m_linearSum.length];
			for (int i = 0; i < m_linearSum.length; i++) {
				m_meanVector[i] = (m_linearSum[i] / m_numFeatureVectors);
			}
		}
		return m_meanVector;
	}

	// additional constructor (takes two cubes, makes one cube)

	// method find neighbors
	// search b tree for all cube key +/- 1
	// if neighbor != null, run check neighbor
	// if neighbor distance < 4sigma, then set as neighbor
	// else remove as neighbor
	// then update neighbor cube to prevent redundant searches

	// method combine cubes

	protected int[] getCubeKey() {
		return this.m_cubeKey;
	}
}
