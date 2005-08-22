import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.awt.Graphics2D.*;
import java.net.URL;

public class Fish {

	private final int WIDTH = 16;
	private final int HEIGHT = 16;
	private final double MIN_SPEED = 3.0;	// The minimum speed when starting a move.
	private final double MAX_SPEED = 6.0;
	private final double STOP_THRESHHOLD = 0.5;
	private final double SLOW_RATE = 5.0 / 100.0;		// The rate we slow the fish per frame

	private BufferedImage image;

	private int pWidth, pHeight;
	private double heading;
	private double hSin, hCos;	// Sine and Cosine of the heading so we don't calculate them so much
	private double speed;
	private double x, y;

	public Fish(int pW, int pH) {

		// Setup some basic stuff

		pWidth = pW;
		pHeight = pH;
		hSin = 0.0;
		hCos = 0.0;

		// Load our image

		image = null;

		// Figure out our X and Y coords to start

		x = (pWidth - WIDTH) * Math.random();
		y = (pHeight - pHeight) * Math.random();

		// Figure out a new direction to move in

		makeMove();

		// That's it.
	}

	public void setImage(BufferedImage source) {
		image = source;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void makeMove() {
		// Make the fishie move!

		heading = 2 * Math.PI * Math.random();
		hSin = Math.sin(heading);
		hCos = Math.cos(heading);

		speed = ((MAX_SPEED - MIN_SPEED) * Math.random()) + MIN_SPEED;
	}

	public void move() {
		// First, is the fishy stopped (or close enough?)

		if (speed <= STOP_THRESHHOLD) {
			// There is a 50% chance the fish will move again, a 50% chance they'll stay still

			if (Math.random() > 0.5) {
				makeMove();				// Make him move
			} else {
				speed = 0.0;				// Wait a turn
			}
		}

		// Now that we know how to move the fish, we move him some

		if (speed > 0.0) {
			x += hCos * speed;
			y += hSin * speed;

			boolean xCol = false;
			boolean yCol = false;

			// Make sure he's still in the pond

			if (x < 0.0) {
				x = 0.0;
				xCol = true;
			}
			
			if (y < 0.0) {
				y = 0.0;
				yCol = true;
			}

			if (x > (pWidth - WIDTH)) { 
				x = pWidth - WIDTH;
				xCol = true;
			}

			if (y > (pHeight - HEIGHT)) {
				y = pHeight - HEIGHT;
				yCol = true;
			}

			// Now turn the fish if we need to

			if (xCol || yCol) {
				// This is easiest if we just extract the X and Y parts and put them back

				if (xCol)
					hCos = hCos * -1.0;
				if (yCol)
					hSin = hSin * -1.0;

				heading = Math.atan(hSin/hCos);	// Get the new angle back (I hope)

				while(heading < 0.0) {
					heading += Math.PI;
				}

				while(heading > (2.0 * Math.PI)) {
					heading -= Math.PI;
				}
			}

			// Now slow the fish down 

			speed = speed * (1.0 - SLOW_RATE);
		}
	}

	public boolean loadImage() {
		try {
			URL file = this.getClass().getResource("fish.gif");
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
			System.out.println("Loading of image 'fish.gif' failed: " + e);
			return false;
		}
	}

	public void draw(Graphics g) {
		// First, figure out where the fish should be

		int drawX = (int) x;
		int drawY = (int) y;
		if (hCos > 0) {
			// Facing right, flip image
			g.drawImage(image, drawX + WIDTH , drawY, drawX, drawY + HEIGHT,
								0, 0, WIDTH, HEIGHT, null);
								
		} else {
			// Facing left, nothing special
			g.drawImage(image, drawX, drawY, null);
		}
	}
}

