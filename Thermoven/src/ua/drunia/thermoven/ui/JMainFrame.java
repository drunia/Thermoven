package ua.drunia.thermoven.ui;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jfree.chart.ChartPanel;

import ua.drunia.thermoven.Thermoven;

public class JMainFrame extends JFrame implements Observer {
	private static final long serialVersionUID = 5365960591112881449L;
	private Thermoven model;
	private ChartPanel chartPanel;
	
	//Constructor
	public JMainFrame(Observable model) {
		super("Thermoven");
		setSize(640, 480);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
		this.model = (Thermoven) model;
		model.addObserver(this);
		
		chartPanel = new ChartPanel(this.model.getChart());
	}
	
	
	//Вызывается при изменении данных в модели (Observable)
	@Override
	public void update(Observable o, Object arg) {
		System.out.println("data update!");
		JOptionPane.showMessageDialog(this, "data update!");
	}
	
}
