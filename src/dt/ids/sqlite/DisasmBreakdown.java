package dt.ids.sqlite;

import java.util.Map;

public record DisasmBreakdown(Map<String, Integer> partCount, int totalParts){}
