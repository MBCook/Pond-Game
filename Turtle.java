import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.net.URL;

public class Turtle {

	static final int WIDTH = 32;
	static final int HEIGHT = 32;

	private BufferedImage image;

	private LillyPadManager pads;

	private int x, y;
	private int pWidth, pHeight;

	private boolean visible;

	public Turtle(int w, int h, LillyPadManager lpm) {

		// Load our image

		loadImage();

		// Prepare some other stuff

		pWidth = w;
		pHeight = h;

		visible = true;

		pads = lpm;

		hideTurtle();
	}

	public void hideTurtle() {
		int padNum = pads.randomBad();

		if (visible && ((padNum == -1) || (pads.badListSize() == 1))) {	// No pads for us
			visible = false;	// Hide the turtle
		} else {		// Pad to hide under
			int oldX = x;
			int oldY = y;

			visible = true;

			setXY(pads.numToXCoord(padNum), pads.numToYCoord(padNum));

			while ((oldX == x) && (oldY == y)) {							// So the turtle always
				padNum = pads.randomBad();									//		goes somewhere new.
				setXY(pads.numToXCoord(padNum), pads.numToYCoord(padNum));
			}
		}
	}

	public boolean hidingUnder(int theX, int theY) {
		if ((theX == x / WIDTH) && (theY == y / HEIGHT))
			return true;
		else
			return false;
	}

	public boolean getVisible() {
		return visible;
	}

	public void setVisible(boolean newValue) {
		visible = newValue;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}

	public void move() {
		// Doesn't move himself, but we use this to hide him if we had run out of pads.
		if (!visible) {
			hideTurtle();
		}
	}

	public boolean loadImage() {
		try {
			URL file = this.getClass().getResource("turtle.gif");
			image = ImageIO.read(file);

			int transparency = image.getColorModel().getTransparency();

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

			BufferedImage copy = gc.createCompatibleImage(WIDTH, HEIGHT, transparency);

			// Now that we've loaded the image and created a place for it, copy it there.

			Graphics2D g2d = copy.createGraphics();
	
			g2d.drawImage(image, 0, 0, null);
			g2d.dispose();

			image = copy;
			
			return true;
		} catch(IOException e) {
			System.out.println("Loading of image 'turtle.gif' failed: " + e);
			return false;
		}
	}

	public void draw(Graphics g) {
		if (visible)
			g.drawImage(image, x, y, null);
	}
}

