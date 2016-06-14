/**
 * Термограф для Овен
 */
package ua.drunia.thermoven;

import java.util.Observable;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ua.drunia.thermoven.ui.JMainFrame;

/**
 * @author drunia
 * Main thermograph class
 */
public class Thermoven extends Observable {
	private JMainFrame frame;
	private Journal journal;
	
	//Create JFreeChart
	public JFreeChart getChart() {
		XYSeries series = new XYSeries("Series");
		series.add(1, 1);
		series.add(2, 2);
		series.add(3, 3);
		series.add(4, 4);
		
		XYSeriesCollection XYCollection = new XYSeriesCollection(series);
		return ChartFactory.createTimeSeriesChart("Температура",
				"timeAxisLabel", "valueAxisLabel", XYCollection);
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
		
		journal.writeValue("Temp1", 1, 25);
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
	}

}
