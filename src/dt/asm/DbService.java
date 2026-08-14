package dt.asm;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dt.asm.sqlite.DbRepo;
import dt.asm.sqlite.DisasmBreakdown;

public class DbService 
{
	private static final Logger logger = Logger.getLogger(DbService.class.getName());
  private static final Map<String, String> SHORTHAND = Map.ofEntries(
		Map.entry(" ", ""),
		Map.entry("x", "㐅"),
		Map.entry("++", "艹"),
		Map.entry("B", "阝"),
		Map.entry("|", "丨"),
		Map.entry("\\", "丶"),
		Map.entry("^", "𠆢"),
		Map.entry("zigzag", "幺") // few surviving original nicknames from 2017
	);

	private final DbRepo db;
	public DbService() throws IOException, ParseException, ClassNotFoundException, SQLException
	{
		db = new DbRepo();
	}
	
	public List<String> lookupByParts(String query) throws SQLException
	{
		final String noShorthand = replaceShorthand(query.strip());
		final Map<String, Integer> freqMap = new HashMap<>();
		noShorthand.codePoints().forEach(cp -> {
			final String character = Character.toString(cp);
			final int current = freqMap.getOrDefault(character, 0);
			freqMap.put(character, current+1);
		});
		return db.lookupByParts(freqMap);
	}

	public List<String> getPartsFor(String chinese) throws SQLException
	{
		final int[] codepoints = chinese.codePoints().toArray();
		if(codepoints.length > 1)
		{
			logger.info("more than 1 character, only checking the first " + chinese);
		}
		return db.getParts(Character.toString(codepoints[0]));
	}

	private String replaceShorthand(String input) 
	{
		if (input == null || input.isEmpty()) 
		{
			return input;
		}

		// Escape keys and join them with '|' to create a single regex pattern
		final String regex = SHORTHAND.keySet().stream()
				.map(Pattern::quote)
				.collect(Collectors.joining("|"));
		
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(input);
		final StringBuilder sb = new StringBuilder(input.length());

		while(matcher.find()) 
		{
			final String match = matcher.group();
			// Use Matcher.quoteReplacement to protect special characters in the replacement
			matcher.appendReplacement(sb, Matcher.quoteReplacement(SHORTHAND.get(match)));
		}
		matcher.appendTail(sb);

		return sb.toString();
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
