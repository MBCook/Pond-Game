import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.awt.Graphics2D.*;
import java.net.URL;

public class LillyPad {

	private final int WIDTH = 32;
	private final int HEIGHT = 32;

	static final int GOOD_PAD = 0;
	static final int BAD_PAD = 1;
	static final int NO_PAD = 2;

	static final int NORTH = 0;
	static final int SOUTH = 2;
	static final int EAST = 1;
	static final int WEST = 3;

	static final int SPEED = 2;

	private boolean moving;
	private boolean out;

	private int x, y;
	private int padType;
	private int direction;
	private int distance;

	private BufferedImage goodPad, badPad;

	public LillyPad(int theX, int theY) {

		// Setup some basic stuff

		moving = false;
		goodPad = badPad = null;
		padType = NO_PAD;
		x = theX;
		y = theY;
	}

	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}

	public void setDirection(int newDir) {
		direction = newDir;
		out = true;
		moving = true;
	}

	public void stopMoving() {
		moving = false;
		distance = 0;
	}

	public boolean getMoving() {
		return moving;
	}

	public int getRealX() {
		if ((direction == NORTH) || (direction == SOUTH)) {
			return x;
		} else {
			if (direction == EAST)
				return x + distance;
			else
				return x - distance;
		}
	}

	public int getRealY() {
		if ((direction == EAST) || (direction == WEST)) {
			return y;
		} else {
			if (direction == SOUTH)
				return y + distance;
			else
		 		return y - distance;
		 }
	}

	public int getDistance() {
		return distance;
	}

	public int getDirection() {
		return direction;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setType(int newType) {
		moving = false;	// If the pad type is changed, we shouldn't be sliding
		out = true;
		distance = 0;
		padType = newType;
	}

	public int getType() {
		return padType;
	}

	public BufferedImage getGoodImage() {
		return goodPad;
	}

	public BufferedImage getBadImage() {
		return badPad;
	}

	public void setImages(BufferedImage good, BufferedImage bad) {
		goodPad = good;
		badPad = bad;
	}

	public void move() {
		if (moving) {
			int sign = 1;
			if (out) {
				distance += SPEED;
				if (distance > WIDTH)
					distance = WIDTH;
				if (distance == WIDTH)	// Width == height so we just chose one
					out = false;
			} else {
				distance -= SPEED;
				if (distance == 0) {	// Width == height so we just chose one
					out = true;
					moving = false;		// Stop moving
				}
			}
		}
	}

	public boolean loadImages() {

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		int transparency;
		BufferedImage copy;
		Graphics2D g2d;

		try {
			URL file = this.getClass().getResource("lillypad.gif");
			goodPad = ImageIO.read(file);

			transparency = goodPad.getColorModel().getTransparency();

			copy = gc.createCompatibleImage(WIDTH, HEIGHT, transparency);

			// Now that we've loaded the image and created a place for it, copy it there.

			g2d = copy.createGraphics();
	
			g2d.drawImage(goodPad, 0, 0, null);
			g2d.dispose();

			goodPad = copy;
		} catch(IOException e) {
			System.out.println("Loading of image 'lillypad.gif' failed: " + e);
			return false;
		}

		try {
			URL file = this.getClass().getResource("lillypad-bad.gif");
			badPad = ImageIO.read(file);

			transparency = badPad.getColorModel().getTransparency();

			copy = gc.createCompatibleImage(WIDTH, HEIGHT, transparency);

			// Now that we've loaded the image and created a place for it, copy it there.

			g2d = copy.createGraphics();
	
			g2d.drawImage(badPad, 0, 0, null);
			g2d.dispose();

			badPad = copy;
		} catch(IOException e) {
			System.out.println("Loading of image 'lillypad-bad.gif' failed: " + e);
			return false;
		}

		return true;
	}

	public void draw(Graphics g) {
		// Figure out if we need to draw, and if so draw the right type of pad.

		if (padType != NO_PAD) {

			BufferedImage image;

			if (padType == GOOD_PAD) {
				image = goodPad;
			} else {
				image = badPad;
			}

			g.drawImage(image, getRealX(), getRealY(), null);
		}
	}
}

