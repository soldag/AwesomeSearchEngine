package querying;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class MapValueComparator<T1, T2 extends Comparable<T2>> implements Comparator<Map.Entry<T1, T2>> {

	@Override
	public int compare(Entry<T1, T2> entry1, Entry<T1, T2> entry2) {
		return entry1.getValue().compareTo(entry2.getValue());
	}

}
