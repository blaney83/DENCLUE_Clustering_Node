package io.github.blaney83.dencluecluster;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.knime.core.data.RowKey;

public class DENCLUEHyperCube {

	private final DENCLUEIndexKey m_cubeKey;
//	private final int[] m_cubeKey;

	private double[] m_linearSum;

	private double[] m_meanVector;

	private int m_numFeatureVectors = 0;

	private Set<DENCLUEIndexKey> m_neighbors;

	private boolean m_isNoise;

	private Map<RowKey, double[]> m_clusterMembers;

	private Map<RowKey, double[]> m_noiseMembers;

	private Map<Double, RowKey> m_nearX;

	private Map<Double, RowKey> m_allOrderedMembers;

	private Set<RowKey> m_memberRows;

	private Set<RowKey> m_clusterRows;

	private Set<RowKey> m_noiseRows;

	private int k = 4;

	private double m_xi;

	public DENCLUEHyperCube(final DENCLUEIndexKey cubekey, final RowKey rowKey, final double[] featureVector,
			final double xi) {
		m_cubeKey = cubekey;
		m_memberRows = new HashSet<RowKey>();
		m_memberRows.add(rowKey);
		m_linearSum = new double[featureVector.length];
		System.arraycopy(featureVector, 0, m_linearSum, 0, m_linearSum.length);
		m_numFeatureVectors++;
		m_isNoise = true;
		m_neighbors = new HashSet<DENCLUEIndexKey>();
		m_clusterMembers = new LinkedHashMap<RowKey, double[]>();
		m_noiseMembers = new LinkedHashMap<RowKey, double[]>();
		m_clusterMembers.put(rowKey, featureVector);
		m_clusterRows = new HashSet<RowKey>();
		m_noiseRows = new HashSet<RowKey>();
		m_xi = xi;
	}

	public boolean addMember(final RowKey rowKey, final double[] featureVector) {
		addMember(rowKey);
		updateLinearSum(featureVector);
		m_clusterMembers.put(rowKey, featureVector);
		System.out.println("GETTING FV " + rowKey.toString() + m_clusterMembers.get(rowKey));
		System.out.println(m_numFeatureVectors);
		// as points are added, as the points exceed a certain threshhold,
		// then return the key of this cube to be stored in a highly populated key[]
		// and change the isNoise to false
		// temp value
		boolean highlyPopulated = false;
		if (m_numFeatureVectors >= (m_xi / (2 * m_linearSum.length))) {
			highlyPopulated = true;
		}
		if (highlyPopulated) {
			return true;
		} else {
			return false;
		}
	}

	public void addMember(final RowKey rowKey) {
		m_memberRows.add(rowKey);
		m_numFeatureVectors++;
	}

	public void updateLinearSum(final double[] otherLinearSum) {
		for (int i = 0; i < m_linearSum.length; i++) {
			m_linearSum[i] += otherLinearSum[i];
		}
	}

	public boolean isNeighbor(final DENCLUEHyperCube otherCube) {
		DENCLUEIndexKey otherKey = otherCube.getCubeKey();
		int diffSum = 0;
		for (int i = 0; i < otherKey.size(); i++) {
			int absDiff = Math.abs(otherKey.getValue(i) - m_cubeKey.getValue(i));
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
		// euclidian distance
		double[] otherMean = otherCube.findMean();
		m_meanVector = this.findMean();
		double currSum = 0;
		for (int i = 0; i < otherMean.length; i++) {
			double diff = (m_meanVector[i] - otherMean[i]);
			currSum += (diff * diff);

		}
		double finalValue = Math.sqrt(currSum);
		double maxNeighborThreshhold = 4 * sigma;
		if (finalValue <= maxNeighborThreshhold) {
			return true;
		} else {
			return false;
		}
	}

	protected double[] findMean() {
//		if (m_meanVector == null) {
			m_meanVector = new double[m_linearSum.length];
			for (int i = 0; i < m_linearSum.length; i++) {
				m_meanVector[i] = (m_linearSum[i] / m_numFeatureVectors);
			}
//		}
		return m_meanVector;
	}

	protected void addNeighbor(final DENCLUEIndexKey otherCubeKey) {
		m_neighbors.add(otherCubeKey);
	}

	protected void mergeNeighbor(final DENCLUEHyperCube mergingCube) {
		this.m_numFeatureVectors += mergingCube.getNumFeatureVectors();
		this.m_clusterMembers.putAll(mergingCube.getClusterMembers());
		this.updateLinearSum(mergingCube.getLinearSum());
		this.m_memberRows.addAll(mergingCube.getMemberRows());
//		for(RowKey joiningKey : mergingCube.getMemberRows()) {
//			this.addMember(joiningKey);
//		}
		this.findMean();
	}

	protected DENCLUEIndexKey getCubeKey() {
		return this.m_cubeKey;
	}

	protected double[] getLinearSum() {
		return this.m_linearSum;
	}

	protected Set<RowKey> getMemberRows() {
		return this.m_memberRows;
	}

	protected Set<DENCLUEIndexKey> getNeighborCubes() {
		return this.m_neighbors;
	}

	protected int getNumFeatureVectors() {
		return this.m_numFeatureVectors;
	}

	protected Map<RowKey, double[]> getClusterMembers() {
		return this.m_clusterMembers;
	}

	protected Map<RowKey, double[]> getNoiseMembers() {
		return this.m_noiseMembers;
	}

	public void createNearXSet(final double sigma) {
		this.findMean();
		// creates ordinality in descending order based on the distance from the mean
		m_nearX = new TreeMap<Double, RowKey>(new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				// TODO Auto-generated method stub
				return o2.compareTo(o1);
			}
		});
		m_allOrderedMembers = new TreeMap<Double, RowKey>(new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				// TODO Auto-generated method stub
				return o2.compareTo(o1);
			}
		});
		for (Map.Entry<RowKey, double[]> entry : m_clusterMembers.entrySet()) {
			double euclDist = euclidianDistance(m_meanVector, entry.getValue());
			RowKey currKey = entry.getKey();
			System.out.println("NEAR X " + entry.getKey().toString() + " EUCL DIST " + euclDist + " K*SIG" + (k*sigma) + " " + (euclDist > k * sigma));
			if (euclDist > k * sigma) {
				// I believe the members aren't disqualified yet based on d(mean(c), x)
				// m_noiseMembers.put(currKey, m_clusterMembers.remove(currKey));
			} else if (euclDist <= k * sigma) {
				m_nearX.put(euclDist, currKey);
				m_clusterRows.add(currKey);
			}
			m_allOrderedMembers.put(euclDist, currKey);
		}
	}

	public double localDensityFunction(final double[] featureVector, final RowKey currRow, final double sigma) {
		// consider ERROR(x) in future dev
		double sum = 0.0;
		for (Map.Entry<Double, RowKey> entry : m_nearX.entrySet()) {
			double sigmaDistanceParameterCheck = euclidianDistance(featureVector,
					m_clusterMembers.get(entry.getValue()));
			double numerator = Math.pow(sigmaDistanceParameterCheck, 2);
			// sigma ^ 2 may need to be swapped for variance
			double denominator = 2 * (Math.pow(sigma, 2));
			double partialSum = Math.E * (-1 * (numerator / denominator));
			sum += partialSum;
			// algorithm time saving step: if threshold distance sigma/2 is greater than
			// distance between feature vector and
			// any single near(x) set member, then the point gets cluster membership status
			if (sigmaDistanceParameterCheck <= (sigma / 2)) {
				System.out.println("CLUSTER ROW: " + currRow.getString() + " SIGMA/2 " + (sigma/2) + " PARAMCHECK " + sigmaDistanceParameterCheck);
				m_clusterRows.add(currRow);
			} else if (!m_nearX.containsValue(currRow)) {
				System.out.println("NOISE ROW: " + currRow.getString() + " SIGMA/2 " + (sigma/2) + " PARAMCHECK " + sigmaDistanceParameterCheck);
				m_noiseRows.add(currRow);
			} else {
				System.out.println("DEFAULT NOISE: " + currRow.getString() + " SIGMA/2 " + (sigma/2) + " PARAMCHECK " + sigmaDistanceParameterCheck);
				m_noiseRows.add(currRow);
			}
		}
		return sum;
	}
	
	//ALTERNATE APPROACH- Don't merge cubes, just find neighbors
	//as calculating near x for a single cube, still add d<sigma/2 points
	//then find density attractor for each cube that is "highly populated"
	//then for each neighbor of dense cubes and neighbors of dense cubes, if 
	//density attr > density attr_1, density attr is cluster max density point
	//if distance between density attr and density attr_1 is < 4*sigma, then clusters 
	//are connected
	//recursively process cubes efficiently (to prevent duplicate processing)
	//then assign clusters

	public boolean findClusterDensityAttractor(final double xi, final double sigma) {
		RowKey densityAttrKey;

		double densityAttr = 0;
		for (Map.Entry<Double, RowKey> entry : m_allOrderedMembers.entrySet()) {
			System.out.println(entry.getValue().toString());
			double localDensityX = localDensityFunction(m_clusterMembers.get(entry.getValue()), entry.getValue(),
					sigma);
			if (localDensityX >= densityAttr) {
				densityAttr = localDensityX;
				densityAttrKey = entry.getValue();
			} else {
				break;
			}
		}
		// must be true to meet cluster density threshold defined by user
		if (densityAttr >= xi) {
			return true;
		} else {
			// set isNoise to true, all points now noise based on user chosen xi value
			// (hyper-plane slice of Gaussian Density
			// is taken "too high" in n-dimensional space
			return false;
		}
	}

	public double euclidianDistance(final double[] setOne, final double[] setTwo) {
		double currSum = 0;
		System.out.print("SET ONE " + setOne);
		if (setOne != null && setTwo != null) {
			for (int i = 0; i < setOne.length; i++) {
				double diff = (setOne[i] - setTwo[i]);
				currSum += (diff * diff);
			}
			double finalValue = Math.sqrt(currSum);
			return finalValue;
		}else {
			return 0;
		}
	}

	public boolean clusterHyperCube(final double sigma, final double xi) {
		createNearXSet(sigma);
		return findClusterDensityAttractor(xi, sigma);
	}

	public Set<RowKey> getClusterRows() {
		return m_clusterRows;
	}

	public Set<RowKey> getNoiseRows() {
		return m_noiseRows;
	}

	// TODO

	// override equals and hashcode

	// implement highly populated check in addMember(arg1, arg2)

	// LIKELY NOT needed unless improvement on performance

	// additional constructor (takes two cubes, makes one cube)

	// method combine cubes

	// VERY LIKELY NOT needed

	// method find neighbors
	// search b tree for all cube key +/- 1
	// if neighbor != null, run check neighbor
	// if neighbor distance < 4sigma, then set as neighbor
	// else remove as neighbor
	// then update neighbor cube to prevent redundant searches
}
