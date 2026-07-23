package dt.ids.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdsUtils 
{
	public static void breakdownDisasm(Map<Integer, List<List<Integer>>> rawParse)
	{
		// You need to deep clone the raw parse first before adding additional entries
		// or you can create a chain reaction of breaking down more than 1 level.
		final Map<Integer, List<List<Integer>>> originalRaw = deepClone(rawParse);
		for(final int character : rawParse.keySet())
		{
			final boolean mainBlock = character >= 0x4E00 && character <= 0x9FFF;
			final boolean aBlock = character >= 0x3400 && character <= 0x4DBF;
			if(!mainBlock && !aBlock)
			{
				// These are obscure characters. Don't waste effort on them.
				continue;
			}

			// System.out.println("Attempting further breakdown of " + Character.toString(character));
			final List<List<Integer>> allExpansions = new ArrayList<>();
			for(final List<Integer> disasm : rawParse.get(character))
			{
				// Prevent adding part subdisassemblies that reference the character itself or another part.
				// This is most likely to happen with the "subtraction" metadata operator where 2 characters
				// get stuck in an infinite loop. 
				// - char a has disassembly referencing char b. 
				// - char b has a disassembly with the subtraction metadata referencing char a
				final Set<Integer> visited = new HashSet<>(disasm);
				visited.add(character);
				final List<List<Integer>> expansions = expand(visited, disasm, List.of(new ArrayList<>()), 0, originalRaw);
				for(final List<Integer> expansion : expansions)
				{
					// many times you can't make an expansion, so the expand function returns the original disassembly
					if(!expansion.equals(disasm))
					{
						allExpansions.add(expansion);
					}
				}
			}
			if(!allExpansions.isEmpty())
			{
				rawParse.get(character).addAll(allExpansions);
			}
		}
	}

	private static List<List<Integer>> expand(Set<Integer> visited, List<Integer> parts, List<List<Integer>> resultsSoFar, int start, Map<Integer, List<List<Integer>>> rawParse)
	{
		if(start == parts.size())
		{
			return resultsSoFar;
		}

		for(int i=start; i<parts.size(); i++)
		{
			final int part = parts.get(i);
			if(rawParse.containsKey(part))
			{
				final List<List<Integer>> subDisasms = rawParse.get(part);
				final List<List<Integer>> newResultsSoFar = new ArrayList<>();
				for(final List<Integer> subDisasm : subDisasms)
				{
					if(subDisasm.isEmpty())
					{
						System.out.println("Should not get an empty sub disassembly " + Character.toString(part));
						continue;
					}

					// every part's sub disassemblies should not create the infinite loop described above
					boolean useable = true;
					for(final int subpart : subDisasm)
					{
						if(visited.contains(subpart))
						{
							useable = false;
							break;
						}
					}
					if(!useable)
					{
						continue;
					}
					
					for(List<Integer> singleResultSoFar : resultsSoFar)
					{
						final List<Integer> copy = new ArrayList<>(singleResultSoFar);
						copy.addAll(subDisasm);
						newResultsSoFar.add(copy);
					}
				}

				if(newResultsSoFar.size() > 0)
				{
					return expand(visited, parts, newResultsSoFar, i+1, rawParse);
				}
				else
				{
					// all the sub disassemblies were useless, add the part as you're in the
					// same situation as if you didn't have any sub disassemblies like the else below
					for(List<Integer> singleResultSoFar : resultsSoFar)
					{
						singleResultSoFar.add(part);
					}
				}
			}
			else // part does not have its own disassembly, add it back to the result
			{
				for(List<Integer> singleResultSoFar : resultsSoFar)
				{
					singleResultSoFar.add(part);
				}
			}
		}

		return resultsSoFar;
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
