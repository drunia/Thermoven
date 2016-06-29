/**
 * Термограф для Овен
 */
package ua.drunia.thermoven;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;

import org.jfree.data.time.Minute;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;


import ua.drunia.thermoven.ui.JMainFrame;

/**
 * @author drunia
 * Main thermograph class
 */
public class Thermoven extends Observable {
	private JMainFrame frame;
	private Journal journal;
	
	//Create JFreeChart
	public JFreeChart getChart(String chartTitle, String YTitle, String XTitle, String devName) {
		TimeSeries series = new TimeSeries(devName);
		
		//Получим данные с журнала за час
		long now = Calendar.getInstance().getTimeInMillis();
		long lastHour = now - 3600 * 1000;
		try {
			ResultSet rs = journal.readValues("", 0, lastHour, now);
			while (rs.next()) {
				Date d = rs.getDate("timestamp");
				float v = rs.getFloat("value");
				series.add(new Second(d), v);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		TimeSeriesCollection XYCollection = new TimeSeriesCollection(series);
		
		final JFreeChart chart = ChartFactory.createTimeSeriesChart(
				chartTitle, XTitle, YTitle, XYCollection
				);
		
		DateAxis axis = (DateAxis) chart.getXYPlot().getDomainAxis();
		axis.setMinimumDate(new Date(new Date().getTime() - 300000));
		axis.setMaximumDate(new Date((long) new Date().getTime() + 300000));
		
		return chart;
		
	}
	
	//Constructor
	public Thermoven() {
		journal = Journal.getJournal();
		
		//Run GUI in other thread
		SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					frame = new JMainFrame(Thermoven.this);
					frame.setVisible(true);
				}
		});
		
		System.out.println("Total records: " + journal.getRecordsCount());
		
	}
	
	/**
	 * Вызывается при приходе данных
	 * @param t - температура
	 */
	private void recieveData(int t) {
		
		//Извещаем подписчиков
		setChanged();
		notifyObservers();
	}

	/**
	 * Launch
	 * @param args
	 */
	public static void main(String[] args) {		
		Thermoven therm = new Thermoven();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		
		//Типа приняли данные с градусника
		therm.recieveData(20);
		
		ValueGenerator tempGenerator = new ValueGenerator("DS18B22", 1);
		tempGenerator.start();
	}

}


// Для эмуляции поступления данных от датчиков
class ValueGenerator extends Timer {
	private Journal jrn = Journal.getJournal();
	private String devName;
	private int devType;
	private boolean started = false;
	
	private TimerTask task = new TimerTask() {
		@Override
		public void run() {
			float value = (float) (Math.random() * 100);
			jrn.writeValue(devName, devType, value);
			System.out.println( devName + ": generated value: " + value);
		}
	};
	
	// Конструктор blocked
	private ValueGenerator() {}
	
	// Рабочий конструктор
	public ValueGenerator(String devName, int devType) {
		super();
		this.devName = devName;
		this.devType = devType;
	}
	
	// Запуск задания
	public void start() {
		if (!started) {
			schedule(task, 0, ((int) (Math.random() * 3000)) + 1000);
			started = true;
		} else {
			System.out.println("ValueGenerator: Task already started!");
		}
	}
	
	// Остановка
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		started = false;
		super.cancel();
		System.out.println("ValueGenerator: Task stoped!");
	}
}
