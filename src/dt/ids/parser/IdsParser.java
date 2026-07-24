package dt.ids.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import static java.util.Map.entry; // Static import makes the code cleaner

public class IdsParser 
{
  private final static Set<Integer> POS_METADATA = new HashSet<>();
  
  // Creating 2 separate versions of the same part may be useful for font rendering, but not for visual assembly purposes.
  private static final Map<Integer, Integer> PART_SWAP = Map.ofEntries(
    entry(cpOf("牜"), cpOf("牛")),
    entry(cpOf("𤣩"), cpOf("王")),
    entry(cpOf("糹"), cpOf("糸")),
    entry(cpOf("訁"), cpOf("言")),
    entry(cpOf("釒"), cpOf("金")),
    entry(cpOf("飠"), cpOf("食")),
    entry(cpOf("孑"), cpOf("子")),
    entry(cpOf("⺶"), cpOf("羊")), // entries below here are manually added
    entry(cpOf("囗"), cpOf("口")),
    entry(cpOf("⺼"), cpOf("月")),
    entry(cpOf("⺝"), cpOf("月")),
    entry(cpOf("土"), cpOf("士")),
    entry(cpOf("𫜹"), cpOf("")), // there are 3 of these including symlink 42 and 43. using 42 
    entry(cpOf("卝"), cpOf("艹")), // ++ variant used in　罐 and other non plant related
    entry(cpOf("𠕁"), cpOf("冊"))
  );
  private static final int NO_DISASSEMBLY = cpOf("？");
  static
  {
    final String metadata = "⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿼⿽⿻⿾⿿〾㇯";
    metadata.codePoints().forEach(cp -> POS_METADATA.add(cp));
  }

  public Map<Integer, List<List<Integer>>> parse(Path filePath) throws IOException
  {
    final int[] symlinks = new int[150];
    final Map<Integer, List<List<Integer>>> allDisasm = new HashMap<>();

    try (Stream<String> lines = Files.lines(filePath)) 
    {
      lines.forEach(line -> {
        if(line.isEmpty())
        {
          return;
        }

        if(line.startsWith("#\t{"))
        {
          parseSymlink(line, symlinks, allDisasm);
        }
        else if(line.startsWith("U+"))
        {
          parseEntry(line, symlinks, allDisasm);
        }
      });
    }
    return allDisasm;
  }

  private void parseEntry (String line, int[] symlinks, Map<Integer, List<List<Integer>>> allDisasm)
  {
    final String[] tokens = line.split("\t");
    final int character = tokens[1].codePointAt(0);
    if(isObscure(character))
    {
      return;
    }

    final Set<List<Integer>> allParts = new HashSet<>();
    for(int i=2; i<tokens.length; i++)
    {
      final String rawDisassembly = tokens[i];
      final int start = rawDisassembly.indexOf('^')+1;
      final int end = rawDisassembly.indexOf('$');
      if(start == 0 || end == -1)
      {
        continue;
      }
      final List<Integer> singleDisasm = parseDisassembly(rawDisassembly, symlinks, start, end);
      if(!singleDisasm.isEmpty() && !(singleDisasm.size() == 1 && (singleDisasm.get(0) == character)))
      {
        allParts.add(singleDisasm);
      }
    }
    if(!allParts.isEmpty())
    {
      allDisasm.put(character, new ArrayList<>(allParts));
    }
  }

  private boolean isObscure(int character)
  {
    final boolean mainBlock = character >= 0x4E00 && character <= 0x9FFF;
		final boolean aBlock = character >= 0x3400 && character <= 0x4DBF;
		return !(mainBlock || aBlock);
  }

  private void parseSymlink(String line, int[] symlinks, Map<Integer, List<List<Integer>>> disassembly)
  {
    final int curlyOpen = line.indexOf('{');
    final int curlyClose = line.indexOf('}');
    final int symlinkNr = Integer.parseInt(line.substring(curlyOpen+1, curlyClose));

    final int lastBracket = line.lastIndexOf(')');
    if(lastBracket == -1)
    {
      // symlink 63 is missing its entry. maybe this will happen more in the future?
      return;
    }
    final int part = line.codePointBefore(lastBracket);
    symlinks[symlinkNr] = part;

    // 42 and 43 are the same to the untrained eye
    if(symlinkNr == 43)
    {
      symlinks[43] = symlinks[42];
      return;
    }

    if(lastBracket+2 == line.length())
    {
      // Some lines end abruptly with no disassembly or question mark for the character in the symlink
      return;
    }

    final List<Integer> symlinkParts = parseDisassembly(line, symlinks, lastBracket+2, line.length());
    if(!symlinkParts.isEmpty())
    {
      final List<List<Integer>> result = new ArrayList<>();
      result.add(symlinkParts);
      disassembly.put(part, result);
    }
  }

  private List<Integer> parseDisassembly(String s, int[] symlinks, int start, int end)
  {
    if(s.codePointAt(start) == NO_DISASSEMBLY)
    {
      return List.of();
    }

    final List<Integer> result = new ArrayList<>();
    int index = start;
    int symlinkStart = -1;
    while(index < end)
    {
      final int cp = s.codePointAt(index);
      if(POS_METADATA.contains(cp))
      {
        index = index + Character.charCount(cp);
        continue;
      }
      if(PART_SWAP.containsKey(cp))
      {
        result.add(PART_SWAP.get(cp));
      }
      else if(cp == '{')
      {
        symlinkStart = index;
      }
      else if(cp == '}')
      {
        final int symlink = Integer.parseInt(s.substring(symlinkStart+1, index));
        result.add(symlinks[symlink]);
        symlinkStart = -1;
      }
      else if(symlinkStart == -1)
      {
        result.add(cp);
      }
      index = index + Character.charCount(cp);
    }
    return result;
  }

  private static int cpOf(String s)
  {
    return s.codePointAt(0);
  }
}
