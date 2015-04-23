package tutorial;

import java.util.HashMap;
import java.util.Map;

public class Sample {

	public static void main(String[] args) {
		Map <Integer, Map<Integer, Integer>> map1 = new HashMap<Integer, Map<Integer, Integer>>();
		//map1.put(0, new HashMap<Integer, Integer>());
		if(!map1.get(0).containsKey(0))
			map1.get(0).put(0, 0);
		System.out.println(map1.get(0));
		System.out.println(map1.get(1));
	}

}
