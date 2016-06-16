/**
 * Singleton
 * 
 * ����� - ������ ����������� ������� � �����������
 * ������ ����� ��������� ������ ����������� � �������,
 * �������� �� �������� ��������. sqlite
 */
package ua.drunia.thermoven;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

/**
 * @author drunia
 */
public class Journal {
	public static final String DB_FILE_NAME = "journal.db";
	public static final int DB_VER = 1;
	
	//����������� ����������� ������ 
	private static Journal instance = new Journal();
	//��������� � ��
	public Connection dbConnection;
	
	/**
	 * ��������� ����������
	 * ����� ��������������� ���������� � db � �� �������������
	 */
	private Journal() {
		try {
			Class.forName("org.sqlite.JDBC");
			dbConnection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE_NAME);
			dbConnection.setAutoCommit(true);
		} catch (ClassNotFoundException e) {
			System.err.println("\nSqlite driver not found!");
			e.printStackTrace();
			return;
		} catch (SQLException e) {
			System.err.println("\nError connection to db!");
			e.printStackTrace();
			return;
		}
	
		/*
		 * �������� ������� ������ � ��
		 * ���� ������ � �� ��� - ������ ����� ������� ����� ��
		 * ���� ������� ���� - �������� ������ ��, ���� ����� �������
		 */
		Statement stmt = null;
		String sql = null;
		try {
			stmt = dbConnection.createStatement();
			
			//������ ����� �� �� - �������� ���-�� ������ �� DatabaseMetaData 
			DatabaseMetaData dmd = dbConnection.getMetaData(); 
			ResultSet result = dmd.getTables(null, null, "%", null);
			
			int rowsCount = -1;
			while (result.next()) rowsCount++;
			
			// ���� ������ � �� ���
			if (rowsCount == 0) { 
				// ������� ����� ��
				System.out.println(Journal.class.getName() +  ": Creating new db...");
				sql = "CREATE TABLE IF NOT EXISTS data " +
						"(id INTEGER PRIMARY KEY AUTOINCREMENT, dev_name TEXT NOT NULL, " + 
						"dev_type INTEGER NOT NULL, value INTEGER NOT NULL, timestamp INTEGER NOT NULL);";
				stmt.execute(sql);
				
				sql = "CREATE TABLE config (db_ver INTEGER);";
				stmt.execute(sql);
				
				sql = "INSERT INTO config (db_ver) VALUES (" + DB_VER + ");";
				stmt.executeUpdate(sql);
			} 
			
			// ������� ������ ��
			sql = "SELECT db_ver FROM config;";
			result = stmt.executeQuery(sql);
			
			int dbVer = result.getInt("db_ver");
			System.out.println(Journal.class.getName() + ": db ver: " + dbVer);
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		System.out.println(Journal.class.getName() + ": Database initialization...OK");
	}
	
	/**
	 * ��������� ������������� ��������� ������ ��� �������� ������
	 * @return ������ Journal 
	 */
	public static Journal getJournal() {
		return instance;
	}
	
	/**
	 * ��������� �������� � ���������� � ������
	 * @param value - �������� ��������� � ���������
	 * @param devName - ��� ����������
	 * @param devType - ��� ����������
	 */
	public boolean writeValue(String devName, int devType, int value) {
		String sql = "INSERT INTO data (dev_name, dev_type, value, timestamp) VALUES (?,?,?,?)";
		PreparedStatement stmt = null;
		try {
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, devName);
			stmt.setInt(2, devType);
			stmt.setInt(3, value);
			stmt.setLong(4, Calendar.getInstance().getTimeInMillis());
			return (stmt.executeUpdate() > 0);
		} catch (SQLException e) {
			//������ ��� ���������� �������
			e.printStackTrace();
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {
					// ������ ��� �������� ���������
					e.printStackTrace();
				}
		}
		return false;
	}
	
	/**
	 * ������ ������ �� ������� ��:
	 * @param devName - ����� ���������� ("" - �� ������)
	 * @param devType - ���� ����������  (0  - �� ������)
	 * @param timeStamp - ������� ������ (0  - �� ������)
	 * @return {@link ResultSet}
	 * @throws SQLException 
	 * 
	 * ���������� ������ > timeStamp!
	 * �������� ����� ������� ��� ������ �� ������� �� ���:
	 * 
	 * 	long now = Calendar.getInstance().getTimeInMillis();
	 * 	int hour = 60 * 60 * 1000; 
	 * 	readValue("", 0, now - hour, now);	
	 */
	public ResultSet readValue(String devName, int devType,
			long timeStampFrom, long timeStampTo) throws SQLException {
		
		String sql = "SELECT * FROM data WHERE dev_name LIKE ? " +
				" AND dev_type LIKE ? AND timestamp > ? AND timestamp < ?";
		PreparedStatement stmt = dbConnection.prepareStatement(sql);
		
		// ��� ����������
		if (devName.equals("")) {
			stmt.setString(1, "%");
		} else { 
			stmt.setString(1, devName);
		}
		// ��� ����������
		if (devType == 0) {
			stmt.setString(2, "%");
		} else {
			stmt.setInt(2, devType);
		}
		
		// ����� ������ �
		stmt.setLong(3, timeStampFrom);
		// ����� ������ ��
		stmt.setLong(4, timeStampTo);
		
		return stmt.executeQuery();
	}
	
	/**
	 * ��������� ���-�� ������� � �������
	 * @return int
	 */
	public int getRecordsCount() {
		String sql = "SELECT COUNT(*) FROM data";
		Statement stmt = null;
		try {
			stmt = dbConnection.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			int count = rs.getInt(1);
			stmt.close();
			return count;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
}
