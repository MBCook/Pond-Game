import java.awt.*;
import java.io.*;
import java.lang.Math;
import java.awt.image.*;
import java.util.TreeSet;
import java.util.Iterator;

public class LillyPadManager {

	// This class manages the LillyPad(s) for us.

	static final int WIDTH = 32;
	static final int HEIGHT = 32;

	private final double NORMAL_CHANCE = 0.8;
	private final double BAD_CHANCE = 0.05;
	private final double NONE_CHANCE = 0.15;
	private final double GROW_CHANCE = 0.2;
	private final int GROW_MAX = 3;

	private int pWidth, pHeight;

	private int across, tall;

	private LillyPad array[][];

	private TreeSet<Integer> deadList;
	private TreeSet<Integer> goodList;
	private TreeSet<Integer> badList;

	static final int GOOD_PAD = 0;
	static final int BAD_PAD = 1;
	static final int NO_PAD = 2;
	private int specialPad;	// The pad that moves

	public LillyPadManager(int panelWidth, int panelHeight) {

		// Setup some basic stuff

		pWidth = panelWidth;
		pHeight = panelHeight;

		across = pWidth / WIDTH;
		tall = pHeight / HEIGHT;

		deadList = new TreeSet<Integer>();	// So we know where there are no lilly pads
		goodList = new TreeSet<Integer>();	// So we know where there are no lilly pads
		badList = new TreeSet<Integer>();	// So we know where there are no lilly pads
		specialPad = -1;			// No special pad now

		// Now we set up our array of lilly pads.

		prepareArray();
	}

	public void startSlide(int padX, int padY, int direction) {
		int padNum = xyToNum(padX, padY);

		if (specialPad != -1) {
			stopSlide(padNum);
		}

		specialPad = padNum;
		getPad(numToX(padNum), numToY(padNum)).setDirection(direction);
	}

	public void stopSlide(int padNum) {
		getPad(numToX(padNum), numToY(padNum)).stopMoving();
	}

	public int getAcross() {
		return across;
	}

	public int getTall() {
		return tall;
	}

	private int xyToNum(int x, int y) {
		return x + y * across;
	}

	private int numToX(int num) {
		return num % across;
	}

	private int numToY(int num) {
		return num / across;
	}

	public int numToXCoord(int num) {
		return numToX(num) * WIDTH;
	}

	public int numToYCoord(int num) {
		return numToY(num) * HEIGHT;
	}

	private void prepareArray() {
		// First, allocate the array

		array = new LillyPad[tall][across];

		deadList.clear();	// Empty the list

		int type = GOOD_PAD;
		double ran;

		array[0][0] = new LillyPad(0, 0);

		LillyPad temp;
		LillyPad padZero = getPad(0, 0);

		padZero.loadImages();

		BufferedImage good = padZero.getGoodImage();
		BufferedImage bad = padZero.getBadImage();

		for(int y = 0; y < tall; y++) {
			for(int x = 0; x < across; x++) {

				ran = Math.random();

				if (ran < NORMAL_CHANCE) {
					type = GOOD_PAD;
					goodList.add(xyToNum(x, y));		// Add it ot the dead set
				} else if (ran < (NORMAL_CHANCE + BAD_CHANCE)) {
					type = BAD_PAD;
					badList.add(xyToNum(x, y));		// Add it ot the dead set
				} else if (ran < (NORMAL_CHANCE + BAD_CHANCE + NONE_CHANCE)) {
					type = NO_PAD;
					deadList.add(xyToNum(x, y));		// Add it ot the dead set
				}

				temp = new LillyPad(x * WIDTH, y * HEIGHT);
				temp.setImages(good, bad);
				temp.setType(type);

				setPad(x, y, temp);
			}
		}
	}

	public LillyPad getPad(int x, int y) {
		return array[y][x];
	}

	private void setPad(int x, int y, LillyPad thePad) {
		array[y][x] = thePad;
	}

	private void growPad(int x, int y, int type) {
		if (type == GOOD_PAD) {
			growPad(x, y);
		} else if (type == BAD_PAD) {
			wiltPad(x, y);
		} else {
			killPad(x, y);
		}
	}

	public void growPad(int x, int y) {
		getPad(x, y).setType(GOOD_PAD);
		deadList.remove(xyToNum(x, y));
		badList.remove(xyToNum(x, y));
		goodList.add(xyToNum(x, y));
	}

	public void killSpecialPad() {
		if (specialPad != -1) {
			killPad(numToX(specialPad), numToY(specialPad));
			specialPad = -1;
		}
	}

	public void killPad(int x, int y) {
		getPad(x, y).setType(NO_PAD);
		deadList.add(xyToNum(x, y));
		goodList.remove(xyToNum(x, y));
		badList.remove(xyToNum(x, y));
	}

	public void wiltPad(int x, int y) {
		getPad(x, y).setType(BAD_PAD);
		goodList.remove(xyToNum(x, y));
		deadList.remove(xyToNum(x, y));
		badList.add(xyToNum(x, y));
	}

	private void setType(int x, int y, int newType) {
		getPad(x, y).setType(newType);
	}

	public int getType(int x, int y) {
		return getPad(x, y).getType();
	}

	public void move() {
		// We don't move, but this can be used for animation later.
		if (specialPad != -1) {
			LillyPad thePad = getPad(numToX(specialPad), numToY(specialPad));
			if (thePad.getMoving())
				thePad.move();
			else
				specialPad = -1;	// Move is complete
		}
	}

	private int randomPad(TreeSet<Integer> padList) {
		if (padList.size() == 0)
			return -1;
		if (padList.size() == 1)
			return padList.first();

		Iterator<Integer> theIt = padList.iterator();		// Get an iterator

		double tempArraySize = padList.size() + 1;

		int whichNum = (int) (tempArraySize * Math.random());	// Pick an element

		int padNum = padList.first();

		for(int i = 0; i < whichNum; i++) {
			padNum = theIt.next();
		}

		return padNum;
	}

	public int goodListSize() {
		return goodList.size();
	}

	public int badListSize() {
		return badList.size();
	}

	public int deadListSize() {
		return deadList.size();
	}

	public int randomGood() {
		return randomPad(goodList);
	}

	public int randomBad() {
		int padNum = randomPad(badList);
		return padNum;
	}

	public int randomDead() {
		return randomPad(deadList);
	}

	public void tryToGrow() {
		// Our lillypads don't move, but this is where we grow new pads.

		double ran;
		int type, padNum, x, y, num = 0;

		while (num < GROW_MAX) {

			// We can grow wup to GROW_MAX items per turn

			num++;

			if (!deadList.isEmpty()) {
				// OK, there is at least one dead pad. Time to grow one (maybe)
	
				ran = Math.random();	// Will we grow one this turn?
	
				if (ran < GROW_CHANCE) {
					ran = Math.random() * (1 - NONE_CHANCE);	// So we always grow a pad
	
					type = GOOD_PAD;
		
					if (ran < NORMAL_CHANCE) {
						type = GOOD_PAD;
					} else if (ran < (NORMAL_CHANCE + BAD_CHANCE)) {
						type = BAD_PAD;
					}
	
					padNum = randomDead();
	
					if (specialPad != -1) {	// If there is a special pad, we have to take some
						if ((padNum == specialPad + 1) ||	// special steps to prevent anything
							(padNum == specialPad - 1) ||	// odd from happening.
							(padNum == specialPad + across) ||
							(padNum == specialPad - across)) {
							// This way pads don't appear under us as we slide.
						} else {
							x = numToX(padNum);
							y = numToY(padNum);

							growPad(x, y, type);
						}
					} else {	
						x = numToX(padNum);					// Decode it
						y = numToY(padNum);
	
						growPad(x, y, type);
					}
				}
			}
		}
	}

	public void quickDraw(Graphics g) {
		LillyPad aPad = array[0][0];
		BufferedImage good = aPad.getGoodImage();
		BufferedImage bad = aPad.getBadImage();

		int theNum;

		// Draw good pads

		TreeSet<Integer> listCopy = new TreeSet<Integer>(goodList);
			// We make a copy of the list because the draw and update code are
			//		asyncronus and thus we run into threading issues if we don't.

		Iterator<Integer> theIt = listCopy.iterator();		// Get an iterator

		if (specialPad != -1) {
			while (theIt.hasNext()) {
				theNum = theIt.next();

				if (theNum != specialPad) {
					g.drawImage(good, (theNum % across) * WIDTH, (theNum / across) * HEIGHT, null);
				}
			}
		} else {
			while (theIt.hasNext()) {
				theNum = theIt.next();

				g.drawImage(good, (theNum % across) * WIDTH, (theNum / across) * HEIGHT, null);
			}
		}

		// Draw bad pads

		listCopy = new TreeSet<Integer>(badList);

		theIt = listCopy.iterator();

		while (theIt.hasNext()) {
			theNum = theIt.next();
			g.drawImage(bad, (theNum % across) * WIDTH, (theNum / across) * HEIGHT, null);
		}

		// Draw special pad

		if (specialPad != -1) {
			aPad = getPad(numToX(specialPad), numToY(specialPad));
			aPad.draw(g);
		}
	}

	public void draw(Graphics g) {
		// Pass the draw command onto each pad
		for(int y = 0; y < tall; y++) {
			for(int x = 0; x < across; x++) {
				getPad(x, y).draw(g);
			}
		}
	}
}

