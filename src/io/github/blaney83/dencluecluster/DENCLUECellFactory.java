package io.github.blaney83.dencluecluster;

import java.util.ArrayList;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;

public class DENCLUECellFactory extends SingleCellFactory{
	
	private ArrayList<Set<RowKey>> m_clusterSets;
	private Set<RowKey> m_noiseSet;

	public DENCLUECellFactory(DataColumnSpec newColSpec, final ArrayList<Set<RowKey>> clusterSets, final Set<RowKey> noiseSet) {
		super(newColSpec);
		m_clusterSets = clusterSets;
		m_noiseSet = noiseSet;
	}

	@Override
	public DataCell getCell(DataRow row) {
		if(m_noiseSet.contains(row.getKey())) {
			return new StringCell("Noise");
		}else {
			int i = 0;
			for(Set<RowKey> clusterSet : m_clusterSets) {
				if(clusterSet.contains(row.getKey())) {
					return new StringCell("Cluster_" + i);
				}
				i++;
			}
		}
		return new StringCell("Noise");
	}

}
