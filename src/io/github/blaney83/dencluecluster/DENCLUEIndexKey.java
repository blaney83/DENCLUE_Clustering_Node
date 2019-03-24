package io.github.blaney83.dencluecluster;

public class DENCLUEIndexKey implements Comparable<DENCLUEIndexKey>{
	
	private int[] m_indexArray;
	
	public DENCLUEIndexKey(final int size) {
		m_indexArray = new int[size];
	}
	
	public void setValue(final int keyIndex, final int value) {
		m_indexArray[keyIndex] = value;
	}
	
	public int getValue(final int keyIndex) {
		return m_indexArray[keyIndex];
	}
	
	public int size() {
		return m_indexArray.length;
	}

	@Override
	public int compareTo(DENCLUEIndexKey o) {
		for(int i = 0; i < o.size(); i ++) {
			if(o.getValue(i)==m_indexArray[i]) {
				continue;
			}else if(o.getValue(i)>m_indexArray[i]) {
				return -1;
			}else {
				return 1;
			}
		}
		return 0;
	}
}
