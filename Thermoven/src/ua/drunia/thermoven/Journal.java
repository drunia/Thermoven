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
 * Singleton
 * 
 * Класс - журнал оперирующий данными от датчиков
 * Данный класс сохраняет данные поступающие с датчика,
 * и получает их за указаный интервал. БД sqlite
 * @author drunia
 */
public class Journal {
	public static final String DB_FILE_NAME = "journal.db";
	public static final int DB_VER = 1;
	
	//Статический конструктор класса 
	private static Journal instance = new Journal();
	//Сединение с бд
	public Connection dbConnection;
	
	/**
	 * Приватный конструтор
	 * Здесь устанавливается соединения с db и ее инициализация
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
		 * Проверим наличие таблиц в бд
		 * Если таблиц в бд нет - значит нужно создать новую бд
		 * Если таблицы есть - проверим версию бд, если нужно обновим
		 */
		Statement stmt = null;
		String sql = null;
		try {
			stmt = dbConnection.createStatement();
			
			//Узнаем пуста ли бд - запросим кол-во таблиц из DatabaseMetaData 
			DatabaseMetaData dmd = dbConnection.getMetaData(); 
			ResultSet result = dmd.getTables(null, null, "%", null);
			
			int rowsCount = -1;
			while (result.next()) rowsCount++;
			
			// Если таблиц в БД нет
			if (rowsCount == 0) { 
				// Создаем новую БД
				System.out.println(Journal.class.getName() +  ": Creating new db...");
				sql = "CREATE TABLE IF NOT EXISTS data " +
						"(id INTEGER PRIMARY KEY AUTOINCREMENT, dev_name TEXT NOT NULL, " + 
						"dev_type INTEGER NOT NULL, value REAL NOT NULL, timestamp INTEGER NOT NULL);";
				stmt.execute(sql);
				
				sql = "CREATE TABLE config (db_ver INTEGER);";
				stmt.execute(sql);
				
				sql = "INSERT INTO config (db_ver) VALUES (" + DB_VER + ");";
				stmt.executeUpdate(sql);
			} 
			
			// Получим версию БД
			sql = "SELECT db_ver FROM config;";
			result = stmt.executeQuery(sql);
			
			int dbVer = result.getInt("db_ver");
			System.out.println(Journal.class.getName() + ": db ver = " + dbVer);
			
			// Проверим нужно ли обновлять текущую БД
			if (dbVer < DB_VER) {
				
				System.out.println(Journal.class.getName() +
						": Need update db " + dbVer + " -> " + DB_VER);
				
				// Обновляем ...
				dbConnection.setAutoCommit(false);
				for (int i = (dbVer + 1); i <= DB_VER; i++) {
					switch (i) {
					case 2:
						// Обновляем БД до 2-й версии (Пример)
						try {
							sql = "CREATE TABLE abc (" +
									"id INTEGER PRIMARY KEY AUTOINCREMENT, value INTEGER)";
							// Вносим изменения в БД
							stmt.executeUpdate(sql);
							
						} catch (SQLException e) {
							e.printStackTrace();
							dbConnection.rollback();
							return;
						}
						break;
					case 3:
						// Обновляем БД до 3-й версии
						sql = "insert your sql hear";
						try {
							// Вносим изменения в БД
							stmt.executeUpdate(sql);
						} catch (SQLException e) {
							e.printStackTrace();
							dbConnection.rollback();
							return;
						}
						break;
					default:
						System.err.println(Journal.class.getName() + ": Error db version!");
						break;
					}
					
					//Пишем новую версию текущей БД
					sql = "UPDATE config SET db_ver=" + i;
					try {
						stmt.executeUpdate(sql);
					} catch (SQLException e) {
						e.printStackTrace();
						dbConnection.rollback();
						return;
					}
					
				}
				dbConnection.commit();
				dbConnection.setAutoCommit(true);
				
				System.out.println(Journal.class.getName() +
						": database version from " + dbVer +
						" to " + DB_VER + " updated sucessfully!");
			} else {
				System.out.println(Journal.class.getName() + ": No update db needed.");
			}
			
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
	 * Возращает автоматически созданный обьект при загрузке класса
	 * @return обьект Journal 
	 */
	public static Journal getJournal() {
		return instance;
	}
	
	/**
	 * Сохраняет значение с устройства в журнал
	 * @param value - Значение полученое с устройства
	 * @param devName - Имя устройства
	 * @param devType - Тип устройства
	 */
	public boolean writeValue(String devName, int devType, float value) {
		String sql = "INSERT INTO data (dev_name, dev_type, value, timestamp) VALUES (?,?,?,?)";
		PreparedStatement stmt = null;
		try {
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, devName);
			stmt.setInt(2, devType);
			stmt.setFloat(3, value);
			stmt.setLong(4, Calendar.getInstance().getTimeInMillis());
			return (stmt.executeUpdate() > 0);
		} catch (SQLException e) {
			//Ошибка при выполнении инсерта
			e.printStackTrace();
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {
					// Ошибка при закрытии стэтмэнта
					e.printStackTrace();
				}
		}
		return false;
	}
	
	/**
	 * Читает заипси из журнала по:
	 * @param devName - Имени устройства ("" - не задано)
	 * @param devType - Типу устройства  (0  - не задано)
	 * @param timeStampFrom - Времени записи с
	 * @param timeStampTo - Времени записи по
	 *  
	 * <br> 
	 * Например нужно выбрать все записи по времени за час:
	 * 
	 * <blockquote><pre>
	 * 	long now = Calendar.getInstance().getTimeInMillis();
	 * 	int hour = 60 * 60 * 1000; 
	 * 	ResultSet rs = readValues("", 0, now - hour, now); 
	 * </pre></blockquote>
	 *  
	 *  @return {@link ResultSet}
	 *  @throws SQLException 	
	 */
	public ResultSet readValues(String devName, int devType,
			long timeStampFrom, long timeStampTo) throws SQLException {
		
		String sql = "SELECT * FROM data WHERE dev_name LIKE ? " +
				" AND dev_type LIKE ? AND timestamp > ? AND timestamp < ?";
		PreparedStatement stmt = dbConnection.prepareStatement(sql);
		
		// Имя устройства
		if (devName.equals("")) {
			stmt.setString(1, "%");
		} else { 
			stmt.setString(1, devName);
		}
		// Тип устройства
		if (devType == 0) {
			stmt.setString(2, "%");
		} else {
			stmt.setInt(2, devType);
		}
		
		// Время записи С
		stmt.setLong(3, timeStampFrom);
		// Время записи ПО
		stmt.setLong(4, timeStampTo);
		
		return stmt.executeQuery();
	}
	
	/**
	 * Возращает кол-во записей в журнале
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
