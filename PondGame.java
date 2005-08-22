import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PondGame extends JFrame implements WindowListener {
	private static int DEFAULT_FPS = 30;

	private PondPanel pp;

	public PondGame(long period) {
		super("The Pond Game");
		makeGUI(period);

		addWindowListener(this);
		pack();
		setResizable(false);
		setVisible(true);
	}

	private void makeGUI(long period)  {
		Container c = getContentPane();

		pp = new PondPanel(this, period);
		c.add(pp, "Center");
	}	

	// ----------------------------------------------------

	public void windowActivated(WindowEvent e) {
		pp.resumeGame();
	}

	public void windowDeactivated(WindowEvent e) {
		pp.pauseGame();
	}


	public void windowDeiconified(WindowEvent e) {
		pp.resumeGame();
	}

	public void windowIconified(WindowEvent e) {
		pp.pauseGame(); 
	}


	public void windowClosing(WindowEvent e) {
		pp.stopGame();
	}


	public void windowClosed(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

	// ----------------------------------------------------

	public static void main(String args[]) { 
		int fps = DEFAULT_FPS;
		if (args.length != 0)
			fps = Integer.parseInt(args[0]);

		long period = (long) 1000.0/fps;
		
		System.out.println("fps: " + fps + "; period: " + period + " ms");

		new PondGame(period*1000000L);		 // ms --> nanosecs 
	}
}


