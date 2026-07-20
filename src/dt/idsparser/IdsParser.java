package dt.idsparser;

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

public class IdsParser 
{
  private final static Set<Integer> POS_METADATA = new HashSet<>();
  private static final Map<Integer, Integer> PART_SWAP = Map.of(
    // Creating 2 separate versions of the same part may be useful for font rendering, but not for visual assembly purposes.
    cpOf("牜"), cpOf("牛"),
    cpOf("𤣩"), cpOf("王"),
    cpOf("糹"), cpOf("糸"),
    cpOf("訁"), cpOf("言"),
    cpOf("釒"), cpOf("金"),
    cpOf("飠"), cpOf("食"),
    cpOf("孑"), cpOf("子"),
    cpOf("⺶"), cpOf("羊")
  );
  private static final int NO_DISASSEMBLY = "？".codePointAt(0);
  static
  {
    final String metadata = "⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿼⿽⿻㇯⿾⿿〾";
    metadata.codePoints().forEach(cp -> POS_METADATA.add(cp));
  }

  public Map<Integer, List<Integer>> parse(Path filePath) throws IOException
  {
    final int[] symlinks = new int[150];
    final Map<Integer, List<Integer>> disassembly = new HashMap<>();

    // assembly metadata ⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿼⿽⿻㇯⿾⿿〾
    try (Stream<String> lines = Files.lines(filePath)) 
    {
      lines.forEach(line -> {
        if(line.isEmpty())
        {
          return;
        }

        if(line.startsWith("#\t{"))
        {
          parseSymlink(line, symlinks, disassembly);
        }
        else if(line.startsWith("U+"))
        {
          parseEntry(line, symlinks, disassembly);
        }
      });
    }

    for(int i=0; i<symlinks.length; i++)
    {
      if(symlinks[i] != 0)
      {
        System.out.println(i +" -> " + Character.toString(symlinks[i]));
      }
    }

    for(final Integer character: disassembly.keySet())
    {
      final StringBuilder sb = new StringBuilder();
      sb.append(Character.toString(character)).append(" : ");
      final List<Integer> parts = disassembly.get(character);
      for(final Integer part : parts)
      {
        sb.append(Character.toString(part)).append(' ');
      }
      System.out.println(sb.toString());
    }

    // for(final Integer character : parts.keySet())
    // {
    //   final List<Integer> charParts = parts.get(character);
    //   for(final Integer charPart : charParts)
    //   {
    //     if(parts.containsKey(charPart))
    //     {
    //       charParts.addAll(parts.get(charPart));
    //     }
    //   }
    // }
    return disassembly;
  }

  private void parseEntry (String line, int[] symlinks, Map<Integer, List<Integer>> disassembly)
  {
    final String[] tokens = line.split("\t");
    final int character = tokens[1].codePointAt(0);
    final List<Integer> allParts = new ArrayList<>();
    for(int i=2; i<tokens.length; i++)
    {
      final String rawDisassembly = tokens[i];
      final int start = rawDisassembly.indexOf('^')+1;
      final int end = rawDisassembly.indexOf('$');
      allParts.addAll(parseDisassembly(rawDisassembly, symlinks, start, end));
    }
    disassembly.put(character, allParts);
  }

  private void parseSymlink(String line, int[] symlinks, Map<Integer, List<Integer>> disassembly)
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
      disassembly.put(part, symlinkParts);
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
