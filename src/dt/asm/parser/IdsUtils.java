package dt.asm.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdsUtils 
{
	public static void breakdownDisasm2(Map<Integer, List<List<Integer>>> rawParse)
	{
		final Map<Integer, List<List<Integer>>> original = deepClone(rawParse);
		for(final int character : original.keySet())
		{
			for(final List<Integer> parts : original.get(character))
			{
				// Get a list of each part's breakdowns
				final List<List<List<Integer>>> partBreakdowns = new ArrayList<>(); // [[[[1,2,3][4,5,6]][[7,8,9][10,11,12]]]]
				int nosubparts = 0;
				for(final int part : parts)
				{
					if(original.containsKey(part))
					{
						partBreakdowns.add(original.get(part));
					}
					else
					{
						nosubparts++;
						partBreakdowns.add( List.of(List.of(part)));
					}
				}

				if(nosubparts == parts.size())
				{
					continue;
				}

				List<List<Integer>> resultsSoFar = new ArrayList<>(); //[[99,98,97][23,24,25]]
				resultsSoFar.add(List.of());

				// for each part's possibly many breakdowns, go through the results so far and each breakdown to each
				// to each result so far: [99,98,97,1,2,3][23,24,25,1,2,3][99,98,97,4,5,6][23,24,25,4,5,6]
				for(final List<List<Integer>> partBreakdown : partBreakdowns) // [[1,2,3][4,5,6]]
				{
					final List<List<Integer>> acc = new ArrayList<>();
					for(final List<Integer> subparts: partBreakdown) // [1,2,3]
					{
						for(List<Integer> resultSoFar : resultsSoFar)
						{
							final List<Integer> copy = new ArrayList<>(resultSoFar);
							copy.addAll(subparts);
							acc.add(copy);
						}
					}
					resultsSoFar = acc;
				}

				rawParse.get(character).addAll(resultsSoFar);
			}
		}
	}

	private static Map<Integer, List<List<Integer>>> deepClone(Map<Integer, List<List<Integer>>> rawParse) 
	{
		if (rawParse == null) 
		{
			return null;
		}

		final Map<Integer, List<List<Integer>>> rawClone = new HashMap<>(rawParse.size());
		for (Map.Entry<Integer, List<List<Integer>>> entry : rawParse.entrySet()) 
		{
			final List<List<Integer>> disassemblies = new ArrayList<>(entry.getValue().size());
			for (List<Integer> disassembly : entry.getValue()) 
			{
				// Since Integer is immutable, copying the list structure is enough
				disassemblies.add(new ArrayList<>(disassembly));
			}
			
			rawClone.put(entry.getKey(), disassemblies);
		}

		return rawClone;
	}
}
