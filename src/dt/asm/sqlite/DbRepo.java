package dt.asm.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DbRepo 
{
	private static final String TABLE_DISASM = "Disassembly";
	private static final String COL_DISASM_ID = "id";
	private static final String COL_DISASM_CHAR = "character";
	private static final String COL_DISASM_TOTAL = "totalParts";
	private static final String TABLE_PARTS = "Parts";
	private static final String COL_PARTS_DISASMID = "disassemblyId";
	private static final String COL_PARTS_PART = "part";
	private static final String COL_PARTS_COUNT = "count";

	private Connection db;

	public DbRepo() throws SQLException, ClassNotFoundException
	{
		final Path sqlitePath = Path.of(System.getProperty("user.home"), "Programs", "ids.sqlite");
		// final String sqlitePath = "/tmp/ids.sqlite";
		Class.forName("org.sqlite.JDBC");
		this.db = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
		db.setAutoCommit(false);
	}

	public List<String> lookupByParts(Map<String, Integer> searchFreq) throws SQLException
	{
		final List<String> results = new ArrayList<>();
		final String baseSelect = String.format("""
			select DISTINCT %s.%s
			from %s join %s on %s.%s = %s.%s
			where %s = ? and %s >= ?
				""", TABLE_DISASM, COL_DISASM_CHAR,
				TABLE_DISASM, TABLE_PARTS, TABLE_DISASM, COL_DISASM_ID, TABLE_PARTS, COL_PARTS_DISASMID,
				COL_PARTS_PART, COL_PARTS_COUNT);
		final String INTERSECT = "\nINTERSECT\n";

		final StringBuilder sb = new StringBuilder();
		int added = 0;
		for(final String part : searchFreq.keySet())
		{
			sb.append(baseSelect);
			added++;
			if(added < searchFreq.size())
			{
				sb.append(INTERSECT);
			}
		}

		final String sql = sb.toString();
		try(final PreparedStatement pstLookup = db.prepareStatement(sql))
		{
			int prepped = 1;
			for(final String part : searchFreq.keySet())
			{
				pstLookup.setString(prepped, part);
				pstLookup.setInt(++prepped, searchFreq.get(part));
				prepped++;
			}

			try(final ResultSet rs = pstLookup.executeQuery())
			{
				while(rs.next())
				{
					results.add(rs.getString(COL_DISASM_CHAR));
				}
			}
		}

		return results;
	}

	public List<String> getParts(String character) throws SQLException
	{
		final List<String> results = new ArrayList<>();
		final String sql = String.format("""
			select DISTINCT %s.%s 
			from %s join %s on %s.%s = %s.%s
			where %s.%s = ?
		""", TABLE_PARTS, COL_PARTS_PART,
			TABLE_PARTS, TABLE_DISASM, TABLE_PARTS, COL_PARTS_DISASMID, TABLE_DISASM, COL_DISASM_ID,
			TABLE_DISASM, COL_DISASM_CHAR);
		
		try(final PreparedStatement pstParts = db.prepareStatement(sql))
		{
			pstParts.setString(1, character);
			try(final ResultSet rs = pstParts.executeQuery())
			{
				while(rs.next())
				{
					results.add(rs.getString(COL_PARTS_PART));
				}
			}
		}
		return results;
	}

	public void init() throws SQLException
	{
		final String disassemblyTable = String.format("""
			CREATE TABLE "%s" (
				"%s"	INTEGER NOT NULL,
				"%s"	TEXT NOT NULL,
				"%s"	INTEGER NOT NULL,
				PRIMARY KEY("%s" AUTOINCREMENT)
				);  
				""", TABLE_DISASM, COL_DISASM_ID, COL_DISASM_CHAR, COL_DISASM_TOTAL, COL_DISASM_ID);
		
		final String partsTable = String.format("""
			CREATE TABLE "%s" (
				"%s"	INTEGER NOT NULL,
				"%s"	TEXT NOT NULL,
				"%s"	INTEGER NOT NULL,
				FOREIGN KEY("%s") REFERENCES "%s"("%s")
			);        
				""", 
				TABLE_PARTS, COL_PARTS_DISASMID, COL_PARTS_PART, COL_PARTS_COUNT,
				COL_PARTS_DISASMID, TABLE_DISASM, COL_DISASM_ID);
		
		final String indexPartsPart = String.format("""
				CREATE INDEX "idx_parts_part" ON "%s" ("%s");
		""", TABLE_PARTS, COL_PARTS_PART);

		for(final String sql : List.of(disassemblyTable, partsTable, indexPartsPart))
		{
			try(final Statement stmt = db.createStatement())
			{
				stmt.execute(sql);
			}
		}
	}

	public void wipe() throws SQLException
	{
		db.setAutoCommit(true);
		try(final Statement rm = db.createStatement())
		{
			for (final String table : List.of(TABLE_DISASM , TABLE_PARTS))
			{
				rm.execute("drop table if exists " + table + ";");
			}
		}

		try(final Statement vaccuum = db.createStatement())
		{
			vaccuum.execute("vacuum;");
		}
		db.setAutoCommit(false);
	}

	public void writeDb(Map<String, List<DisasmBreakdown>> allDisassemblies) throws SQLException
	{
		for(final String character : allDisassemblies.keySet())
		{
			int id = -1;
			for(final DisasmBreakdown disassembly : allDisassemblies.get(character))
			{
				final int length = disassembly.totalParts();
				final String sqlDisasmEntry = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)", TABLE_DISASM, COL_DISASM_CHAR, COL_DISASM_TOTAL);
				try(final PreparedStatement pstDisasmEntry = db.prepareStatement(sqlDisasmEntry))
				{
					pstDisasmEntry.setString(1, character);
					pstDisasmEntry.setInt(2, length);
					pstDisasmEntry.execute();

					try(
						final PreparedStatement getId = db.prepareStatement("select last_insert_rowid() as id;");
						final ResultSet getIdResults = getId.executeQuery())
					{
						id = getIdResults.getInt("id");
					}
				}

				final String sqlPart = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)", TABLE_PARTS, COL_PARTS_DISASMID, COL_PARTS_PART, COL_PARTS_COUNT);
				for(final String part : disassembly.partCount().keySet())
				{
					try(final PreparedStatement pstPartEntry = db.prepareStatement(sqlPart))
					{
						pstPartEntry.setInt(1, id);
						pstPartEntry.setString(2, part);
						pstPartEntry.setInt(3, disassembly.partCount().get(part));
						pstPartEntry.execute();
					}
				}
			}
		}
		db.commit();
	}
}
