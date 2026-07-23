package dt.ids;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.ids.sqlite.DbRepo;
import dt.ids.sqlite.DisasmBreakdown;

public class DbService 
{
	private final DbRepo db;
	public DbService() throws IOException, ParseException, ClassNotFoundException, SQLException
	{
		db = new DbRepo();
	}

	public void saveIdsParse(Map<Integer, List<List<Integer>>> rawParse) throws SQLException
	{
		db.wipe();
		db.init();
		db.writeDb(converRawParseToBreakdown(rawParse));
	}

	private Map<String, List<DisasmBreakdown>> converRawParseToBreakdown(Map<Integer, List<List<Integer>>> rawParse)
	{
		final Map<String, List<DisasmBreakdown>> result = new HashMap<>();
		for(final int character : rawParse.keySet())
		{
			final String charString = Character.toString(character);
			result.put(charString, new ArrayList<>());
			for(final List<Integer> disassembly : rawParse.get(character))
			{
				final Map<String, Integer> counts = new HashMap<>();
				for(final int part : disassembly)
				{
					final String partString = Character.toString(part);
					final int currentCount = counts.getOrDefault(partString, 0);
					counts.put(partString, currentCount+1);
				}
				result.get(charString).add(new DisasmBreakdown(counts, disassembly.size()));
			}
		}
		return result;
	}
}
