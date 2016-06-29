package ua.drunia.thermoven.ui;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jfree.chart.ChartPanel;

import ua.drunia.thermoven.Thermoven;

public class JMainFrame extends JFrame implements Observer {
	private static final long serialVersionUID = 5365960591112881449L;
	private Thermoven model;
	private ChartPanel chartPanel;
	
	//Constructor
	public JMainFrame(final Observable model) {
		super("Thermoven");
		setSize(640, 480);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
		this.model = (Thermoven) model;
		model.addObserver(this);
		
		chartPanel = new ChartPanel(
				this.model.getChart("Температура", "t C'", "По часам", "DS18B20")
				);
		add(chartPanel);
		
		Timer timer = new Timer("Update");
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				chartPanel.setChart(((Thermoven) model).getChart("Температура", "t C'", "По часам", "DS18B20"));
				System.out.println("Timer: Update!");
			}
		}, 0, 1000);
	}
	
	
	//Вызывается при изменении данных в модели (Observable)
	@Override
	public void update(Observable o, Object arg) {
		System.out.println("data update!");
	}
	
	
	
}
