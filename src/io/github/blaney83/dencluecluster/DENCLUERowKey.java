package io.github.blaney83.dencluecluster;

import org.knime.core.data.RowKey;

public class DENCLUERowKey implements Comparable<DENCLUERowKey>{
	
	private final RowKey m_rowKey;
	
	public DENCLUERowKey(final RowKey rowKey) {
		this.m_rowKey = rowKey;
	}
	
	public RowKey getKey () {
		return this.m_rowKey;
	}
	//should return long in future
	public int getValue() {
		return Integer.valueOf(this.m_rowKey.getString().substring(3));
	}


	@Override
	public int compareTo(DENCLUERowKey o) {
		if(m_rowKey.getString().equals(o.getKey().getString())) {
			return 0;
		}else if(this.getValue() < o.getValue()){
			return -1;
		}else {
			return 1;
		}
	}

}
