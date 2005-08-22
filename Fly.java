import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.net.URL;

public class Fly {

	static final int WIDTH = 32;
	static final int HEIGHT = 32;

	static final double MAX_SPEED = 12.0;
	static final double LAND_THRESHOLD = 2.0;

	private BufferedImage image;

	private LillyPadManager pads;

	private int x, y;
	private double trueX, trueY;
	private int pWidth, pHeight;

	private boolean visible;
	private boolean needToMove;

	public Fly(int w, int h, LillyPadManager lpm) {

		// Load our image

		loadImage();

		// Prepare some other stuff

		pWidth = w;
		pHeight = h;

		trueX = -WIDTH;
		trueY = -HEIGHT;

		x = -1;
		y = -1;

		visible = true;

		pads = lpm;
		needToMove = true;

		landFly();
	}

	public void landFly() {
		// Land the fly
		needToMove = true;

		int oldX = x;
		int oldY = y;

		while ((oldX == x) && (oldY == y)) {							// So the fly doesn't spawn
			int padNum = pads.randomGood();								//		under the frog.
			setXY(pads.numToXCoord(padNum), pads.numToYCoord(padNum));
		}

		// Now we find a starting coord for the fly

		if (Math.random() > 0.5) {
			// Come from the left or right side
			if (Math.random() > 0.5) {
				trueX = -WIDTH;
			} else {
				trueX = pWidth;
			}
			trueY = (int) (Math.random() * (double) pHeight);
		} else {
			// Come from the top or bottom
			if (Math.random() > 0.5) {
				trueY = -HEIGHT;
			} else {
				trueY = pHeight;
			}
 			trueX = (int) (Math.random() * (double) pWidth);
		}
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
		double theAngle;
		if (needToMove) {
			if ((trueX != x) || (trueY != y)) {
				// Move the fly closer to his target

				double yDiff = trueY - (double) y;
				double xDiff = trueX - (double) x;

				double dist = Math.sqrt(Math.pow(xDiff, 2.0) + Math.pow(yDiff, 2.0));

				if (dist > MAX_SPEED) {
					dist = MAX_SPEED;
				}

				theAngle = (trueY - (double) y) / (trueX - (double) x);
				theAngle = Math.atan(theAngle);

				double speedX = Math.abs(dist * Math.cos(theAngle));
				double speedY = Math.abs(dist * Math.sin(theAngle));

				if (trueX < x)
					trueX += speedX;
				else
					trueX -= speedX;

				if (trueY < y)
					trueY += speedY;
				else
					trueY -= speedY;
				boolean xDone = false;

				if ((trueX > ((double) x - LAND_THRESHOLD)) && (trueX < ((double) x + LAND_THRESHOLD))) {
					trueX = (double) x;
					xDone = true;
				}

				if ((trueY > ((double) y - LAND_THRESHOLD)) && (trueY < ((double) y + LAND_THRESHOLD))) {
					trueY = (double) y;
					if (xDone)
						needToMove = false;
				}
			}
		}
	}

	public boolean loadImage() {
		try {
			URL file = this.getClass().getResource("fly.gif");
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
			System.out.println("Loading of image 'fly.gif' failed: " + e);
			return false;
		}
	}

	public void draw(Graphics g) {
		if (visible)
			g.drawImage(image, (int) trueX, (int) trueY, null);
	}
}

