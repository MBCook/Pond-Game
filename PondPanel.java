import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.text.DecimalFormat;

public class PondPanel extends JPanel implements Runnable, KeyListener {
	private static final int PANEL_WIDTH = 32*16;
	private static final int PANEL_HEIGHT = 32*12; 

	private static long MAX_STATS_INTERVAL = 1000000000L;	// Stats every second or so.

	private final int MOVE_SCORE = 10;
	private final int FLY_SCORE = 100;
	private final int TURTLE_SCORE = 50;

	// Number of uninterrupted runs before we force a breaka
	private static final int TIME_TO_YIELD = 16;

	private static int MAX_FRAME_SKIPS = 5;		// Maximum number of frames to skip at once

	private static int NUM_FPS = 10;		// How many FPS we keep for calculations

	// Statistics stuff

	private long statsInterval = 0L;		// in ns
	private long prevStatsTime;		
	private long totalElapsedTime = 0L;
	private long gameStartTime;
	private int timeSpentInGame = 0;		// in seconds

	private long frameCount = 0;
	private double fpsStore[];
	private long statsCount = 0;
	private double averageFPS = 0.0;

	private long framesSkipped = 0L;
	private long totalFramesSkipped = 0L;
	private double upsStore[];
	private double averageUPS = 0.0;

	private DecimalFormat df = new DecimalFormat("0.##");		 // 2 dp
	private DecimalFormat timedf = new DecimalFormat("0.####");	 // 4 dp

	// Animation stuff

	private Thread animator;
	private boolean running = false;	 // used to stop the animation thread
	private boolean isPaused = false;

	private long period;			// period between drawing in ns

	// Game stuff

	private final boolean debug;

	private PondGame pg;

	private Water theWater;
	private Fish theFishA;
	private Fish theFishB;
	private Fish theFishC;
	private Fish theFishD;
	private LillyPadManager pads;
	private Frog bob;
	private Fly joe;
	private Turtle bill;

	// Used when done
	
	private boolean gameOver = false;
	private int score = -10;			// So if the frog jumps into the water his first turn,
	private Font font;				//	the score will be 0 and not 10.
	private FontMetrics metrics;

	// Off screen rendering

	private Graphics dbg; 
	private Image dbImage = null;

	// And now.. methods!

	public PondPanel(PondGame thePG, long period) {
		pg = thePG;
		this.period = period;

		debug = false;

		setBackground(Color.white);
		setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

		setFocusable(true);
		requestFocus();		 	// the JPanel now has focus, so receives key events
		addKeyListener(this);	// Recieve key events

		// create game components

		if(debug)
			System.out.println("Creating the fish.");

		theFishA = new Fish(PANEL_WIDTH, PANEL_HEIGHT);
		theFishB = new Fish(PANEL_WIDTH, PANEL_HEIGHT);
		theFishC = new Fish(PANEL_WIDTH, PANEL_HEIGHT);
		theFishD = new Fish(PANEL_WIDTH, PANEL_HEIGHT);

		theFishA.loadImage();						// Load and share the image so we save mem
		theFishB.setImage(theFishA.getImage());
		theFishC.setImage(theFishA.getImage());
		theFishD.setImage(theFishA.getImage());

		if(debug)
			System.out.println("Creating the water.");

		theWater = new Water(PANEL_WIDTH, PANEL_HEIGHT);

		if(debug)
			System.out.println("Creating the pads.");

		pads = new LillyPadManager(PANEL_WIDTH, PANEL_HEIGHT);

		if(debug)
			System.out.println("Creating the frog.");

		bob = new Frog(PANEL_WIDTH, PANEL_HEIGHT);

		bob.setDirection(Frog.EAST);

		if(debug)
			System.out.println("Creating the fly.");

		joe = new Fly(PANEL_WIDTH, PANEL_HEIGHT, pads);

		if(debug)
			System.out.println("Creating the turtle.");

		bill = new Turtle(PANEL_WIDTH, PANEL_HEIGHT, pads);

		pads.growPad(8, 6);	// Make sure there is something for the frog to sit on

		// Set up the mouse

		addMouseListener(	new MouseAdapter() {
								public void mousePressed(MouseEvent e) {
									testPress(e.getX(), e.getY());
								}
							});

		// Set up message font
		
		font = new Font("SansSerif", Font.BOLD, 24);
		metrics = this.getFontMetrics(font);

		// Initialise timing elements

		fpsStore = new double[NUM_FPS];
		upsStore = new double[NUM_FPS];

		for (int i=0; i < NUM_FPS; i++) {
			fpsStore[i] = 0.0;
			upsStore[i] = 0.0;
		}
	}

	private boolean padWouldSlide(int frogX, int frogY, int direction) {
		int x = frogX / LillyPadManager.WIDTH;
		int y = frogY / LillyPadManager.HEIGHT;

		int across = pads.getAcross();
		int tall = pads.getTall();

		if (((x == 0) && (direction == Frog.WEST)) ||
			((x == across - 1) && (direction == Frog.EAST)) ||
			((y == 0) && (direction == Frog.NORTH)) ||
			((y == tall - 1) && (direction == Frog.SOUTH))) {
			// Would slide off the screen, can't have that.
			return false;
		}

		// Next, find out if there is a pad where we would slide to.

		int newX = x;
		int newY = y;

		switch (direction) {
			case Frog.NORTH:
				y--;
				break;
			case Frog.EAST:
				x++;
				break;
			case Frog.SOUTH:
				y++;
				break;
			case Frog.WEST:
				x--;
				break;
		}

		// Now we find if ther is a pad there.

		if (pads.getType(x, y) == LillyPadManager.NO_PAD)
			return true;
		else
			return false;
	}

	public void moveFrog(int keyCode) {

		int direction;

		switch(keyCode) {
			case KeyEvent.VK_LEFT:
				direction = Frog.WEST;
				break;
			case KeyEvent.VK_RIGHT:
				direction = Frog.EAST;
				break;
			case KeyEvent.VK_UP:
				direction = Frog.NORTH;
				break;
			case KeyEvent.VK_DOWN:
				direction = Frog.SOUTH;
				break;
			default:
				return;		// We don't use any other keys. We'll never get here,
							//									but Java doesn't know that.
		}

		int x = bob.getPadX() / Frog.WIDTH;
		int y = bob.getPadY() / Frog.HEIGHT;

//		System.out.println("Before: Frog on pad " + x + ", " + y);

		int oldDir = bob.getDirection();	// Needed if we are sliding

		if ((bob != null) && (bob.hop(direction))) {

			// First we move the turtle in case that is needed

			if (bill.hidingUnder(x, y)) {
				if(debug)
					System.out.println("Moving the turtle.");
				// Player was on the turtle, move the turtle
				bill.hideTurtle();
				if (debug)
					System.out.println("Turtle has moved.");
			}

			if(debug)
				System.out.println("Frog moved.");

			// Move succeded, now we handle it

			if (bob.isSliding())
				pads.killSpecialPad();

			bob.finalizeSlide(oldDir);	// Finish any slides immediatly
			
			switch(onPadType()) {
				case LillyPadManager.BAD_PAD:
					if(debug)
						System.out.println("Stepped on bad pad.");
					getPadOn().setType(LillyPad.NO_PAD);	// Pad dies away when stepped on
					if ((bob.getX() == bill.getX()) && (bob.getY() == bill.getY())) {
						// Landed on the turtle, they're ok.
						score += TURTLE_SCORE;	// Bonus for finding the turtle
						if(debug)
							System.out.println("Saved by the turtle.");
					} else {
						if(debug)
							System.out.println("Player died.");
						gameOver = true;	// Player died.
						bob.kill();
					}
					break;
				case LillyPadManager.NO_PAD:
					if(debug)
						System.out.println("Player died.");
					gameOver = true;		// Player died.
					bob.kill();
					break;
				case LillyPadManager.GOOD_PAD:
					// Nothing happens for now
					if (padWouldSlide(bob.getX(), bob.getY(), direction)) {
						pads.startSlide(bob.getX() / 32, bob.getY() / 32, direction);
						bob.startSlide();
					}
					break;
			}

			// Now we kill the lilly pad that we were on before

			pads.killPad(x, y);

			// And now we improve the player's score

			score += MOVE_SCORE;

			// Now try to grow a new lilly pad

			if(debug)
				System.out.println("Growing more pads.");

			pads.tryToGrow();

			// Now we deal with the fly

			if((bob.getX() == joe.getX()) && (bob.getY() == joe.getY())) {
				// The player caught the fly
				score += FLY_SCORE;

				if(debug)
					System.out.println("Caught the fly, finding new spot.");

				// Find a new pad for him

				joe.landFly();

				if(debug)
					System.out.println("Found a new spot for the fly.");
			}

//		x = bob.getPadX() / Frog.WIDTH;
//		y = bob.getPadY() / Frog.HEIGHT;

//		System.out.println("After:  Frog on pad " + x + ", " + y);

		} else {
			if(debug)
				System.out.println("Move failed.");
			// Move failed
		}
	}

	private int onPadType() {
		int x = bob.getX() / Frog.WIDTH;
		int y = bob.getY() / Frog.HEIGHT;

		return pads.getType(x, y);
	}

	private LillyPad getPadOn() {
		int x = bob.getX() / Frog.WIDTH;
		int y = bob.getY() / Frog.HEIGHT;

		return pads.getPad(x, y);
	}

	// ------- Key Stuff --------

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if ((keyCode == KeyEvent.VK_ESCAPE) ||
			(keyCode == KeyEvent.VK_Q) ||
			(keyCode == KeyEvent.VK_END) ||
			((keyCode == KeyEvent.VK_C) && e.isControlDown()) ) {

			running = false;
		}

		if ((keyCode == KeyEvent.VK_LEFT) || 
			(keyCode == KeyEvent.VK_RIGHT) || 
			(keyCode == KeyEvent.VK_UP) || 
			(keyCode == KeyEvent.VK_DOWN)) {

			moveFrog(keyCode);
		}
	}

	public void keyReleased(KeyEvent e) {
		// We don't use this
	}

	public void keyTyped(KeyEvent e) {
		// We don't use this
	}

	// -------------------------

	public void addNotify() {
		// Wait for the JPanel to be added to the JFrame before starting
		super.addNotify();		// Creates the peer
		startGame();			// Start the thread
	}

	private void startGame() {
		// Initialise and start the thread 
		
		if (animator == null || !running) {
			animator = new Thread(this);
			animator.start();
		}
	}	

	// ------------- game life cycle methods ------------
	// Called by the JFrame's window listener methods

	public void resumeGame(){
		// Called when the JFrame is activated / deiconified
		isPaused = false;
	} 

	public void pauseGame() {
		// Called when the JFrame is deactivated / iconified
		isPaused = true;
	} 

	public void stopGame() {
		// Called when the JFrame is closing
		running = false;
	}

	// ----------------------------------------------

	private void testPress(int x, int y) {
		// Check to see where the user clicked
/*		if (!isPaused && !gameOver) {
			if (fred.nearHead(x,y)) {		// was mouse press near the head?
				gameOver = true;
				score =	100;	// For now, hack together a score
			} else {	 // add an obstacle if possible
				if (!fred.touchedAt(x,y))		// was the worm's body untouched?
					obs.add(x,y);
			}
		// We don't do anything here yet
		} */
	}

	public void run() {
		long beforeTime, afterTime, timeDiff, sleepTime;
		long overSleepTime = 0L;
		int noDelays = 0;
		long excess = 0L;

		gameStartTime = System.nanoTime();
		prevStatsTime = gameStartTime;
		beforeTime = gameStartTime;

		running = true;

		while(running) {
			gameUpdate();
			gameRender();
			paintScreen();

			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;
			sleepTime = (period - timeDiff) - overSleepTime;	

			if (sleepTime > 0) {	 // some time left in this cycle
				try {
					Thread.sleep(sleepTime/1000000L);	 // nano -> ms
				}
				catch(InterruptedException ex){}
				overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
			} else {		// sleepTime <= 0; the frame took longer than the period
				excess -= sleepTime;	// store excess time value
				overSleepTime = 0L;

				if (++noDelays >= TIME_TO_YIELD) {
					Thread.yield();		// give another thread a chance to run
					noDelays = 0;
				}
			}

			beforeTime = System.nanoTime();

			/* If frame animation is taking too long, update the game state
				 without rendering it, to get the updates/sec nearer to
				 the required FPS. */
			int skips = 0;

			while((excess > period) && (skips < MAX_FRAME_SKIPS)) {
				excess -= period;
				gameUpdate();		 // update state but don't render
				skips++;
			}

			framesSkipped += skips;
			storeStats();
		}

		printStats();
		System.exit(0);
	}


	private void gameUpdate() {
		if (!isPaused && !gameOver) {
//			double ts = System.nanoTime();
			theFishA.move();
			theFishB.move();
			theFishC.move();
			theFishD.move();
			theWater.move();
			pads.move();
			bob.move();
			joe.move();
			bill.move();
//			System.out.println("Move: " + (System.nanoTime() - ts) / 1000000.0 + "ms.");
		}
	}

	private void gameRender() {
		// Time to draw everything. First we'll setup the double-buffering image if needed.
		
		if (dbImage == null) {
			dbImage = createImage(PANEL_WIDTH, PANEL_HEIGHT);

			if (dbImage == null) {
				System.out.println("dbImage is null");
				return;
			} else {
				dbg = dbImage.getGraphics();
			}
		}

		// Draw stuff

		double nt;
		double ts = System.nanoTime();

//		theWater.draw(dbg);
		theWater.drawFast(dbg);

//		System.out.println("");

//		nt = System.nanoTime();
//		System.out.println("Water:  " + (nt - ts) / 1000000.0 + "ms.");
//		ts = nt;

		theFishA.draw(dbg);
		theFishB.draw(dbg);
		theFishC.draw(dbg);
		theFishD.draw(dbg);

//		nt = System.nanoTime();
//		System.out.println("Fish:   " + (nt - ts) / 1000000.0 + "ms.");
//		ts = nt;

		bill.draw(dbg);

//		nt = System.nanoTime();
//		System.out.println("Turtle: " + (nt - ts) / 1000000.0 + "ms.");
//		ts = nt;

		pads.draw(dbg);
//		pads.quickDraw(dbg);

//		nt = System.nanoTime();
//		System.out.println("Pads:   " + (nt - ts) / 1000000.0 + "ms.");
//		ts = nt;

		joe.draw(dbg);

//		nt = System.nanoTime();
//		System.out.println("Fly:    " + (nt - ts) / 1000000.0 + "ms.");
//		ts = nt;

		bob.draw(dbg);

//		nt = System.nanoTime();
//		System.out.println("Frog:   " + (nt - ts) / 1000000.0 + "ms.");
//		ts = nt;

//		System.out.println("Draw: " + (System.nanoTime() - ts) / 1000000.0 + "ms.");

		if (isPaused) {
			drawPaused(dbg);
		}

		if (gameOver) {
			gameOverMessage(dbg);
		}
	}

	private void gameOverMessage(Graphics g) {
		// center the game-over message in the panel

		if (score < 0)
			score = 0;		// Prevent a negative score.

		String msg = "Game Over. Your Score: " + score;

		int x = (PANEL_WIDTH - metrics.stringWidth(msg))/2; 
		int y = (PANEL_HEIGHT - metrics.getHeight())/2;

		g.setColor(Color.red);
		g.setFont(font);
		g.drawString(msg, x, y);
	}

	private void drawPaused(Graphics g) {
		String msg = "Game Paused";

		int x = (PANEL_WIDTH - metrics.stringWidth(msg))/2; 
		int y = (PANEL_HEIGHT - metrics.getHeight())/2;

		g.setColor(Color.red);
		g.setFont(font);
		g.drawString(msg, x, y);
	}

	private void paintScreen() {
		// Use active rendering to put the buffered image on-screen

		Graphics g;

		try {
			g = this.getGraphics();

			if ((g != null) && (dbImage != null))
				g.drawImage(dbImage, 0, 0, null);

			g.dispose();
		} catch (Exception e) {
			System.out.println("Graphics context error: " + e);
		}
	}

	private void storeStats() {
		/* The statistics:
				 - the summed periods for all the iterations in this interval
					 (period is the amount of time a single frame iteration should take), 
					 the actual elapsed time in this interval, 
					 the error between these two numbers;

				 - the total frame count, which is the total number of calls to run();

				 - the frames skipped in this interval, the total number of frames
					 skipped. A frame skip is a game update without a corresponding render;

				 - the FPS (frames/sec) and UPS (updates/sec) for this interval, 
					 the average FPS & UPS over the last NUM_FPSs intervals.

			 The data is collected every MAX_STATS_INTERVAL	 (1 sec).
		*/

		frameCount++;
		statsInterval += period;

		if (statsInterval >= MAX_STATS_INTERVAL) {		 // record stats every MAX_STATS_INTERVAL
			long timeNow = System.nanoTime();
			timeSpentInGame = (int) ((timeNow - gameStartTime)/1000000000L);	// ns --> secs

			long realElapsedTime = timeNow - prevStatsTime;		// time since last stats collection
			totalElapsedTime += realElapsedTime;

			double timingError = 
				 ((double)(realElapsedTime - statsInterval) / statsInterval) * 100.0;

			totalFramesSkipped += framesSkipped;

			double actualFPS = 0;			// calculate the latest FPS and UPS
			double actualUPS = 0;
			if (totalElapsedTime > 0) {
				actualFPS = (((double)frameCount / totalElapsedTime) * 1000000000L);
				actualUPS = (((double)(frameCount + totalFramesSkipped) / totalElapsedTime) 
																														 * 1000000000L);
			}

			// store the latest FPS and UPS
			fpsStore[ (int)statsCount%NUM_FPS ] = actualFPS;
			upsStore[ (int)statsCount%NUM_FPS ] = actualUPS;
			statsCount = statsCount+1;

			double totalFPS = 0.0;		 // total the stored FPSs and UPSs
			double totalUPS = 0.0;
			for (int i=0; i < NUM_FPS; i++) {
				totalFPS += fpsStore[i];
				totalUPS += upsStore[i];
			}

			if (statsCount < NUM_FPS) { // obtain the average FPS and UPS
				averageFPS = totalFPS/statsCount;
				averageUPS = totalUPS/statsCount;
			}
			else {
				averageFPS = totalFPS/NUM_FPS;
				averageUPS = totalUPS/NUM_FPS;
			}
/*
			System.out.println(timedf.format( (double) statsInterval/1000000000L) + " " + 
										timedf.format((double) realElapsedTime/1000000000L) + "s " + 
							df.format(timingError) + "% " + 
										frameCount + "c " +
										framesSkipped + "/" + totalFramesSkipped + " skip; " +
										df.format(actualFPS) + " " + df.format(averageFPS) + " afps; " + 
										df.format(actualUPS) + " " + df.format(averageUPS) + " aups" );
*/
			framesSkipped = 0;
			prevStatsTime = timeNow;
			statsInterval = 0L;		// reset
		}
	}

	private void printStats() {
		System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
		System.out.println("Average FPS: " + df.format(averageFPS));
		System.out.println("Average UPS: " + df.format(averageUPS));
		System.out.println("Time Spent: " + timeSpentInGame + " secs");
	}
}
