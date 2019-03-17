package io.github.blaney83.dencluecluster;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

public class DENCLUEHyperCube {
	
	private final int[] m_cubeKey;
	
	private double[] m_linearSum;
	
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
		m_numFeatureVectors ++;
		m_isNoise = true;
	}
	
	public boolean addMember(final RowKey rowKey, final double[] featureVector) {
		m_memberRows.add(rowKey);
		for(int i = 0; i < m_linearSum.length; i ++) {
			m_linearSum[i] += featureVector[i];
		}
		m_numFeatureVectors ++;
		
		//as points are added, as the points exceed a certain threshhold,
		// then return the key of this cube to be stored in a highly populated key[]
		// and change the isNoise to false
		//temp value
		boolean highlyPopulated = true;
		
		if(highlyPopulated) {
			return true;
		}else {
			return false;
		}

	}
	
	//method find neighbors
	//search b tree for all cube key +/- 1
	//if neighbor != null, run check neighbor
	//if neighbor distance < 4sigma, then set as neighbor
	// else remove as neighbor
	//then update neighbor cube to prevent redundant searches
}
