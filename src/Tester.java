import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import dt.asm.DbService;

public class Tester 
{
	public static void main(String[] args) throws Exception 
	{
		final Path filePath = Paths.get("/home/daniel/workspace3/IDS.TXT");
		// final Path filePath = Paths.get("/tmp/bad.txt");
		// final Map<Integer, List<List<Integer>>> disasm =new IdsParser().parse(filePath);
		// IdsUtils.breakdownDisasm(disasm);
		final DbService dbService = new DbService();
		// dbService.saveIdsParse(disasm);

		try (Scanner scanner = new Scanner(System.in)) 
		{
			System.out.println("Type something (type 'exit' to quit):");
			
			// Loop while there is another line of input available
			while (scanner.hasNextLine()) 
			{
				String line = scanner.nextLine();
				
				if ("exit".equalsIgnoreCase(line.trim())) 
				{
					break;
				}
				
				final List<String> results = line.charAt(0) == 'd' ? dbService.getPartsFor(line.substring(1)) : dbService.lookupByParts(line);
				System.out.println("Got " + results.size() + " results");
				results.forEach(System.out::println);

				System.out.println("Type something (type 'exit' to quit):");
			}
		}

	// entry(cpOf("㐅"), cpOf("x")),
	// entry(cpOf("艹"), cpOf("*")), // mental hack shortcut of ++ (addition) to multiplication (*)
		// entry(cpOf("阝"), cpOf("B")),
	// entry(cpOf("丨"), cpOf("|")),
	// entry(cpOf("丶"), cpOf("`")),
	// entry(cpOf("𠆢"), cpOf("^"))
	// entry(cpOf("幺"), cpOf("Z")) // "zig zag" one of the few original surviving names form 2017
		
		// final Map<Integer, Integer> partUsage = new HashMap<>();
		// for(final int character : disasm.keySet())
		// {
		// 	for(final List<Integer> possibledism : disasm.get(character))
		// 	{
		// 		for(final int part : possibledism)
		// 		{
		// 		final int count = partUsage.getOrDefault(part, 0);
		// 		partUsage.put(part, count+1);
		// 		}
		// 	}
		// }

		// Set<String> over100 = new HashSet<>();
	// Map<Integer, Integer> sortedDesc = partUsage.entrySet().stream()
	//	.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
	//	 .collect(Collectors.toMap(
	//					 Map.Entry::getKey,
	//					 Map.Entry::getValue,
	//					 (e1, e2) -> e1,
	//					 LinkedHashMap::new
	//			 ));		
		// for(int key : sortedDesc.keySet())
		// {
		// 	if(sortedDesc.get(key) >= 100)
		// 	{
		// 		System.out.println(Character.toString(key) + " : " + sortedDesc.get(key));
		// 		over100.add(Character.toString(key));
		// 	}
		// }

		// Set<String> primitives = new HashSet<>();
		// Set<String> commonPrimitive = new HashSet<>();

		// for(final Integer character: disasm.keySet())
	// {
	//   final List<List<Integer>> disasms = disasm.get(character);
	//   if(disasms.isEmpty())
	//   {
	//	 System.out.println(Character.toString(character));
		// 		primitives.add(Character.toString(character));
		// 		if(over100.contains(Character.toString(character)))
		// 		{
		// 			commonPrimitive.add(Character.toString(character));
		// 		}
	//   }
	// }
		// System.out.println(commonPrimitive);
		System.out.println("done");
	}
}
