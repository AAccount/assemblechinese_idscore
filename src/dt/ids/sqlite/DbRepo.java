package dt.ids.sqlite;

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
		// final String sqlitePath = System.getProperty("user.home") + "/Programs/mdbg2_1.sqlite";
		final String sqlitePath = "/tmp/ids.sqlite";
		Class.forName("org.sqlite.JDBC");
		this.db = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
		db.setAutoCommit(false);
	}

	public List<String> findMatch(Map<String, Integer> searchFreq)
	{
		final List<String> results = new ArrayList<>();
		final String baseSelect = String.format("""
				select DISTINCT Disassembly.character 
from Disassembly join Parts on Disassembly.id = Parts.disassemblyId
where part = "日"
				""", TABLE_DISASM)	;
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
