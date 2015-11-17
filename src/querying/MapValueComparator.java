package querying;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import SearchEngine.PatentDocument;

public class MapValueComparator implements Comparator<Map.Entry<PatentDocument, Double>> {

	@Override
	public int compare(Entry<PatentDocument, Double> entry1, Entry<PatentDocument, Double> entry2) {
		return entry1.getValue().compareTo(entry2.getValue());
	}

}
