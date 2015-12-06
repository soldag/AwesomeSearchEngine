package utilities;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class MapValueComparator<T1, T2 extends Comparable<T2>> implements Comparator<Map.Entry<T1, T2>> {

	@Override
	public int compare(Entry<T1, T2> entry1, Entry<T1, T2> entry2) {
		return entry1.getValue().compareTo(entry2.getValue());
	}
	
	
	/**
	 * Returns a new comparator, that compares the values of the map in natural order.
	 * @return
	 */
	public static <T1, T2 extends Comparable<T2>> Comparator<Map.Entry<T1, T2>> natural() {
    	return new MapValueComparator<T1, T2>();
    }
	
	/**
	 * Returns a new comparator, that compares the values of the map in revered order.
	 * @return
	 */
	public static <T1, T2 extends Comparable<T2>> Comparator<Map.Entry<T1, T2>> reverse() {
    	return Collections.reverseOrder(new MapValueComparator<T1, T2>());
    }
}
