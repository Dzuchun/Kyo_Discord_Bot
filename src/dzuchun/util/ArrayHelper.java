package dzuchun.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ArrayHelper {
	public static <U, V> List<V> map(Iterable<U> source, Function<U, V> mapper) {
		List<V> res = new ArrayList<V>();
		source.forEach(u -> res.add(mapper.apply(u)));
		return res;
	}

	public static <U> String iterableToString(Iterable<U> source, Function<U, String> toString) {
		final StringBuilder res = new StringBuilder();
		source.forEach(u -> res.append(toString.apply(u)).append(", "));
		return res.substring(0, Math.max(0, res.length() - 3));
	}
}
