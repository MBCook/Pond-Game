import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.awt.Graphics2D.*;
import java.net.URL;

public class Water {

	private final int WIDTH = 16;
	private final int HEIGHT = 16;

	private BufferedImage image;

	private int pWidth, pHeight;

	public Water(int pW, int pH) {

		// Setup some basic stuff

		pWidth = pW;
		pHeight = pH;

		// Load our image

		loadImage();
	}

	public void move() {
		// Our water doesn't move
	}

	public boolean loadImage() {
		try {
			URL file = this.getClass().getResource("water.gif");
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

	public void drawFast(Graphics g) {
		g.setColor(new Color(131, 147, 202));
		g.fillRect(0, 0, pWidth, pHeight);
	}

	public void draw(Graphics g) {
		// First, figure out where the fish should be

		int x, y;

		int yTarget = pHeight / HEIGHT;
		int xTarget = pWidth / WIDTH;

		for(y = 0; y < yTarget; y++) {
			for(x = 0; x < xTarget; x++) {
				g.drawImage(image, x * WIDTH, y * HEIGHT, null);
			}
		}
	}
}

