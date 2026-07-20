import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import dt.idsparser.IdsParser;

public class App 
{
	public static void main(String[] args) throws Exception 
	{
		final Path filePath = Paths.get("/home/daniel/workspace3/IDS.TXT");
		//final Path filePath = Paths.get("/tmp/bad.txt");
		final Map<Integer, List<Integer>> parts =new IdsParser().parse(filePath);
		System.out.print("done");
	}
}
