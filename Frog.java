import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.awt.Graphics2D.*;
import java.awt.geom.AffineTransform;
import java.net.URL;

public class Frog {

	static final int WIDTH = 32;
	static final int HEIGHT = 32;
	static final int FROG_SPEED = 32;

	static final int SLIDE_SPEED = 2;	// If this and the speed in LillyPad don't match,
										//		you'll find some ODD looking behavior
	private BufferedImage image;

	static final int NORTH = 0;
	static final int SOUTH = 2;
	static final int EAST = 1;
	static final int WEST = 3;

	private boolean alive;

	private boolean sliding;
	private boolean slidingOut;

	private int slideDistance;
	private int direction;
	private int x, y;
	private int destX, destY;
	private int pWidth, pHeight;

	public Frog(int w, int h) {

		// Load our image

		loadImage();

		// Figure out our X and Y coords to start

		x = 8 * WIDTH;		// Start in roughly the middle of the window
		y = 6 * HEIGHT;
		destX = x;
		destY = y;
		direction = SOUTH;
		pWidth = w;
		pHeight = h;

		alive = true;

		sliding = false;
		slidingOut = true;
		slideDistance = 0;

		// That's it.
	}

	public void startSlide() {
		slidingOut = true;
		sliding = true;
		slideDistance = 0;
	}

	public boolean isSliding() {
		return sliding;
	}

	public void stopSlide() {
		sliding = false;
		slideDistance = 0;
	}

	public boolean hop(int dir) {
		// If we do have to move...
		// 	first, place the frog where he was supposed to be.
		// This avoids problems of the frog ending up where he isn't
		//	supposed to be because of the update code.

		if (alive) {
			switch(dir) {
				case NORTH:
					if (destY == 0) {
						return false;
					} else {
						y = destY;
						x = destX;
						destY -= HEIGHT;
						direction = dir;
						return true;
					}
				case EAST:
					if (destX == pWidth - WIDTH) {
						return false;
					} else {
						y = destY;
						x = destX;
						destX += WIDTH;
						direction = dir;
						return true;
					}			
				case SOUTH:
					if (destY == pHeight - HEIGHT) {
						return false;
					} else {
						y = destY;
						x = destX;
						destY += HEIGHT;
						direction = dir;
						return true;
					}
				case WEST:
					if (destX == 0) {
						return false;
					} else {
						y = destY;
						x = destX;
						destX -= WIDTH;
						direction = dir;
						return true;
					}
				default:
					return false;	// Shouldn't ever get here
			}
		} else {
			return false;	// Dead frogs don't move.
		}
	}

	public void kill() {
		alive = false;
	}

	public void finalizeSlide(int oldDir) {
		/*	Note that we go through all this direction swapping stuff
				because when we find out if a move succeedes, we change
				the frog's direction. Therefor, the direction we are
				currently facing is not neccessarily the one we slid in.
		*/
		if (sliding) {
			int newDir = direction;
			direction = oldDir;
			destX = getPadX();
			x = destX;
			destY = getPadY();
			y = destY;
			stopSlide();
			direction = newDir;
		}
	}

	public int getPadX() {
		if ((!sliding) || (slideDistance < (WIDTH / 2)))
			return destX;
		else
			if (direction == EAST)
				return destX + WIDTH;
			else if (direction == WEST)
				return destX - WIDTH;
			else
				return destX;
	}

	public int getPadY() {
		if ((!sliding) || (slideDistance < (HEIGHT / 2)))
			return destY;
		else
			if (direction == SOUTH)
				return destY + HEIGHT;
			else if (direction == NORTH)
				return destY - HEIGHT;
			else
				return destY;
	}

	public int getX() {
		if (!sliding)
			return destX;
		else
			if (direction == EAST)
				return destX + slideDistance;
			else if (direction == WEST)
				return destX - slideDistance;
			else
				return destX;
	}

	public int getY() {
		if (!sliding)
			return destY;
		else
			if (direction == SOUTH)
				return destY + slideDistance;
			else if (direction == NORTH)
				return destY - slideDistance;
			else
				return destY;
	}

	private int getDrawX() {
		if (!sliding)
			return x;
		else
			if (direction == EAST)
				return x + slideDistance;
			else if (direction == WEST)
				return x - slideDistance;
			else
				return x;
	}

	private int getDrawY() {
		if (!sliding)
			return y;
		else
			if (direction == SOUTH)
				return y + slideDistance;
			else if (direction == NORTH)
				return y - slideDistance;
			else
				return y;
	}

	public void setXY(int newX, int newY) {
		x = newX;
		destX = x;
		y = newY;
		destY = y;

		if (sliding)
			stopSlide();
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int newDir) {
		direction = newDir;
	}

	public void move() {
		if (alive && ((destX != x) || (destY != y))) {
			switch(direction) {
				case NORTH:
					y -= FROG_SPEED;
					if (y < destY)
						y = destY;
					break;
				case EAST:
					x += FROG_SPEED;
					if (x > destX)
						x = destX;

					break;
				case SOUTH:
					y += FROG_SPEED;
					if (y > destY)
						y = destY;

					break;
				case WEST:
					x -= FROG_SPEED;
					if (x < destX)
						x = destX;

					break;
			}
		}

		// Slide code

		int sign = 1;

		if (sliding) {
			if (slidingOut) {
				slideDistance += SLIDE_SPEED;
				if (slideDistance > WIDTH)
					slideDistance = WIDTH;
				if (slideDistance == WIDTH)	{// Width == height so we just chose one
					slidingOut = false;
				}
			} else {
				slideDistance -= SLIDE_SPEED;
				if (slideDistance == 0) {	// Width == height so we just chose one
					stopSlide();
				}
			}
		}
	}

	public boolean loadImage() {
		try {
			URL file = this.getClass().getResource("frog.gif");
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
			System.out.println("Loading of image 'frog.gif' failed: " + e);
			return false;
		}
	}

	public void draw(Graphics g) {
		// First, figure out where the fish should be

		if (alive) {
			Graphics2D g2d = (Graphics2D) g;	// Get our graphics2D object so we can rotate
			AffineTransform myAT;
			AffineTransformOp myATO;

			switch(direction) {
				case NORTH:
					myAT = new AffineTransform();
					myAT.setToRotation(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
					myATO = new AffineTransformOp(myAT, null);
					g2d.drawImage(image, myATO, getDrawX(), getDrawY());
					break;
				case EAST:
					myAT = new AffineTransform();
					myAT.setToRotation(-1.0 * Math.PI / 2.0, WIDTH / 2.0, HEIGHT / 2.0);
					myATO = new AffineTransformOp(myAT, null);
					g2d.drawImage(image, myATO, getDrawX(), getDrawY());
					break;
				case SOUTH:
					g.drawImage(image, getDrawX(), getDrawY(), null);
					break;
				case WEST:
					myAT = new AffineTransform();
					myAT.setToRotation(Math.PI / 2.0, WIDTH / 2.0, HEIGHT / 2.0);
					myATO = new AffineTransformOp(myAT, null);
					g2d.drawImage(image, myATO, getDrawX(), getDrawY());
					break;
			}
		}
	}
}

