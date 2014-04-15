import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class BoulderDash extends JPanel {
	//debugging
	private final boolean debug = true;
	
    //whether we should print the board
    private boolean isReady = false;
    
	//gamefield's rectangles
	private final int WIDTH = 40;
    private final int HEIGHT = 22;
	private BDTile[][] gamefield = new BDTile[WIDTH][HEIGHT];
    
	//gamefield Variables
	private boolean playerAlive = true;
	private BDLevelReader blr;
	private int numLevels;
	private int level = 1;
    private int moves = 0;
    private int time = 0;
    private int[][] rockStatus = new int[WIDTH][HEIGHT];
    private int[][] diamondStatus = new int[WIDTH][HEIGHT];
    private int[][] fireFlyStatus = new int[WIDTH][HEIGHT];
    
    //Timers
    private Timer levelTimer;
    private Timer levelActionsTimer;
    private static final int SECONDDELAY = 1000;
    private static final int LEVELACTIONDELAY = 200;
	
	//level variables
	private int diamondsCollected = 0;
    private JLabel levelLabel = new JLabel ("Level: " + level);
    private JLabel levelDiamondsCollected = new JLabel ("Diamonds Collected: " + (diamondsCollected));
    private JLabel levelMovesLabel = new JLabel ("Moves: " + (moves));
    private JLabel levelTimerLabel = new JLabel("Time: " + time);
    private JLabel levelPlayerStatus = new JLabel("");
    
    /**
     * ActionListener for levelTimer counter
     */
    ActionListener timerCounter = new ActionListener() {
        public void actionPerformed(ActionEvent e) {            
            setLevelTimerTime();
            setLevelTimerLabel();
        }
    };
    
    /**
     * Key Listener for Previous and Next Levels
     */
    private class LevelControlKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            //N (78) Pressed, Next Level
            if (e.getKeyCode() == 78) {
                nextLevel();
                initLevel(level);
            }
            //P (80) Pressed, Previous Level
            if (e.getKeyCode() == 80) {
                prevLevel();
                initLevel(level);
            }
            //R (82) Pressed, Restart Level
            if (e.getKeyCode() == 82) initLevel(level);
        }
    }
    
    /**
     * Key Listener for Directional Controls
     */
    private class ControlsKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            boolean exit = false;
            int dx = 0;
            int dy = 0;
        
            //left
            if (e.getKeyCode() == 37) dx = -1;
            //top
            if (e.getKeyCode() == 38) dy = -1;
            //right
            if (e.getKeyCode() == 39) dx = 1;
            //bottom
            if (e.getKeyCode() == 40) dy = 1;
            
            if (playerAlive) {
	            //loop through the gamefield to find the player and move them
	            for (int x = 0; x <= gamefield.length - 1; x++) {
	                for (int y = 0; y <= gamefield[x].length - 1; y++) {
	                    exit = updatePlayer(x, y, dx, dy);
	                    if (exit) break;
	                } //end loop columns
	                if (exit) break;
	            } //end loop rows
	            repaint();
	/*            if (checkWin()) {
	                nextLevel();
	                initLevel(level);
	            }*/
            }
        }
    }
    
    /**
     * ActionListener for timer level actions
     */
    ActionListener levelActions = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		if (playerAlive) {
        		updateBoard();
    		}
    	}	
    };
	
    /**
     * Act on the player dying including setting player alive to false,
     * stopping the level time and updating the status message.
     */
    public void playerDied() {
    	playerAlive = false;
    	stopLevelTimer();
    	levelPlayerStatus.setText("You Died.");
    }
      
    /**
     * React to butterflys and fireflys exploding
     */
    private void explode(int x, int y, BDTile tile) {
    	for (int i = -1; i < 2; i++) {
    		for (int h = -1; h < 2; h++) {
    			if (!isWall(gamefield[x+i][y+h])) {
    				if (isPlayer(gamefield[x+i][y+h])) playerDied();
    				if (isFireFly(tile)) gamefield[x+i][y+h] = BDTile.EMPTY;
    				if (isButterFly(tile)) gamefield[x+i][y+h] = BDTile.DIAMOND;
    				repaint();
    			}
    		}
    	}
    }
    
    /**
     * Loop through the entire board from the bottom to update it.
     */
    private synchronized void updateBoard () {
        boolean moved = false;
    	for (int y = HEIGHT - 2; y >= 0; y--) {
            for (int x = 1; x < WIDTH - 1; x++) {
                if (gamefield[x][y] == BDTile.ROCK) updateRocks(x, y);
                if (gamefield[x][y] == BDTile.DIAMOND) updateDiamonds(x, y);
                if (gamefield[x][y] == BDTile.FIREFLY) {
                	if (moved == false) {
                		moved = updateFireFlys(x, y);
                	} else {
                		moved = false;
                	}
                }
            }
            moved = false;
        }
    }
    
    /**
     * Update fireflys
     */
    private boolean updateFireFlys(int x, int y) {
    	if (
    		gamefield[x+1][y] == BDTile.PLAYER || 
    		gamefield[x][y+1] == BDTile.PLAYER ||
    		gamefield[x-1][y] == BDTile.PLAYER ||
    		gamefield[x][y-1] == BDTile.PLAYER 
    		) {
    		explode(x, y, gamefield[x][y]);
    		return false;
    	}
    	
    	if (fireFlyStatus[x][y] == 0) {
    		if (isE(gamefield[x+1][y])) {
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x+1][y] = BDTile.FIREFLY;
    			fireFlyStatus[x+1][y] = 0;
    			repaint();
    			return true;
    		} else {
    			fireFlyStatus[x][y] = 1;
    			return false;
    		}
    	}
    	
    	if (fireFlyStatus[x][y] == 1) {
    		if (isE(gamefield[x][y+1])) {
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x][y+1] = BDTile.FIREFLY;
    			fireFlyStatus[x][y+1] = 1;
    			repaint();
    			return true;
    		} else {
    			fireFlyStatus[x][y] = 2;
    			return false;
    		}
    	}
    	
    	if (fireFlyStatus[x][y] == 2) {
    		if (isE(gamefield[x-1][y])) {
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x-1][y] = BDTile.FIREFLY;
    			fireFlyStatus[x-1][y] = 2;
    			repaint();
    			return true;
    		} else {
    			fireFlyStatus[x][y] = 3;
    			return false;
    		}
    	}
    	
    	if (fireFlyStatus[x][y] == 3) {
    		if (isE(gamefield[x][y-1])) {
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x][y-1] = BDTile.FIREFLY;
    			fireFlyStatus[x][y-1] = 3;
    			repaint();
    			return true;
    		} else {
    			fireFlyStatus[x][y] = 0;
    			return false;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Update all actions for rocks to check for movements
     */
    private void updateRocks(int x, int y) {
    	if (rockStatus[x][y] == 1 && playerAlive) {
			rockStatus[x][y] = 0;
			
			//check for the player
			if (isPlayer(gamefield[x][y+1])) {
				gamefield[x][y] = BDTile.EMPTY;
				//TODO: CHANGE TO A DIFFERENT TYPE
				gamefield[x][y+1] = BDTile.ROCK;
				playerDied();
				repaint();
				return;
			}
    		
    		//check for fireflys
    		if (isFireFly(gamefield[x][y+1])){
    			explode(x, y+1, gamefield[x][y+1]);
    			repaint();
    			return;
    		}
    		
    		//check for fireflys
    		if (isButterFly(gamefield[x][y+1])){
    			explode(x, y+1, gamefield[x][y+1]);
    			return;
    		}

			//move the rock to empty spaces
    		if (isE(gamefield[x][y+1])) {
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x][y+1] = BDTile.ROCK;
    			rockStatus[x][y+1] = 1;
    			repaint();
    			return;
    		} else if (isE(gamefield[x+1][y+1])) {
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x+1][y+1] = BDTile.ROCK; 
    			rockStatus[x+1][y+1] = 1;
    			repaint();
    			return;
    		} else if (isE(gamefield[x-1][y+1])){
    			gamefield[x][y] = BDTile.EMPTY;
    			gamefield[x-1][y+1] = BDTile.ROCK;
    			rockStatus[x-1][y+1] = 1;
    			repaint();
    			return;
    		}
    		
    	}
    	
    	//update the rock to falling if the square below it is empty
    	if (
    			//is rock and below is empty
    			isRockMoveable(gamefield[x][y], gamefield[x][y+1]) ||
    			(isRockMoveable(gamefield[x][y], gamefield[x+1][y+1]) && isRockMoveable(gamefield[x][y], gamefield[x+1][y])) ||
    			(isRockMoveable(gamefield[x][y], gamefield[x-1][y+1]) && isRockMoveable(gamefield[x][y], gamefield[x-1][y]))
    		) rockStatus[x][y] = 1;
    }
    
    /**
     * Update all diamonds to check for movement
     */
    private void updateDiamonds(int x, int y) {
    	if (diamondStatus[x][y] == 1 && playerAlive) {
			diamondStatus[x][y] = 0;
			//move the rock to empty spaces
			if (isE(gamefield[x][y+1])) {
				gamefield[x][y] = BDTile.EMPTY;
				gamefield[x][y+1] = BDTile.DIAMOND;
				diamondStatus[x][y+1] = 1;
				repaint();
				return;
			} else if (isE(gamefield[x+1][y+1])) {
				gamefield[x][y] = BDTile.EMPTY;
				gamefield[x+1][y+1] = BDTile.DIAMOND; 
				diamondStatus[x+1][y+1] = 1;
				repaint();
				return;
			} else if (isE(gamefield[x-1][y+1])){
				gamefield[x][y] = BDTile.EMPTY;
				gamefield[x-1][y+1] = BDTile.DIAMOND;
				diamondStatus[x-1][y+1] = 1;
				repaint();
				return;
			}
    	}
	
	//update the diamond to falling if the square below it is empty
	if (
			isDiamondMoveable(gamefield[x][y], gamefield[x][y+1]) ||
			(isDiamondMoveable(gamefield[x][y], gamefield[x+1][y+1]) && isDiamondMoveable(gamefield[x][y], gamefield[x+1][y])) ||
			(isDiamondMoveable(gamefield[x][y], gamefield[x-1][y+1]) && isDiamondMoveable(gamefield[x][y], gamefield[x-1][y]))
		) diamondStatus[x][y] = 1;
    }
    
    public synchronized boolean updatePlayer(int x, int y, int dx, int dy) {
	    if (gamefield[x][y].toString() == "PLAYER") {
	        if (isMoveable(gamefield[x + dx][y + dy])) {
	            //move the player
	            movePlayer(x, y, dx, dy);
	            //bc the gamefield is 2d, we need a boolean to check so we can get out
	            return true;
	        } //end can move
	    } //end if player
	    return false;
    }
    
    /**
     * Move the player around the gamefield
     * @param nextTile the next tile the player is moving to
     * @param x the x value for the gamefield tile where the player currently is
     * @param y the y value for the gamefield tile where the player currently is
     * @param dx how many tiles along the x axis to move the player
     * @param dy how many tiles along the y axis to move the player
     */
    public void movePlayer (int x, int y, int dx, int dy) {
        BDTile nextTile = gamefield[x + dx][y + dy];
        BDTile prevTile = (gamefield[x][y].toString() == "PLAYER") ? BDTile.EMPTY : BDTile.DIAMOND;
        boolean moved = false;
        
        //check if the next tile is empty or a goal tile
        if ((isE(nextTile) || isDirt(nextTile)) && !moved) {
            gamefield[x + dx][y + dy] = BDTile.PLAYER;
            moved = true;
        }
        
        if (isDiamond(nextTile) && !moved) {
            gamefield[x + dx][y + dy] = BDTile.PLAYER;
            diamondsCollected += 1;
            levelDiamondsCollected.setText("Diamonds Collected: " + (diamondsCollected));
            moved = true;
        }
        
        //check if the next tile is a moveable rock
        if (isRockMoveable(nextTile, gamefield[x + dx *2][y + dy *2]) && !moved) {
        	rockStatus[x +dx][y] = 0;
            gamefield[x + dx][y + dy] = BDTile.PLAYER;
            gamefield[x + dx * 2][y + dy * 2] = BDTile.ROCK;
            moved = true;
        }
        
        //if the player moved, update the tile it moved from
        if (moved) {
            gamefield[x][y] = prevTile;
            //increase the number of moves
            incMoves();
            
        }
    }
    
    /**
     * Check to see if the player can move to a tile
     * 
     * @param Tile The tile to check if moveable
     * @return
     */
    public boolean isMoveable (BDTile tile) {
        return (tile.toString() == "EMPTY" || tile.toString() == "ROCK" || tile.toString() == "DIRT" || tile.toString() == "DIAMOND") ? true : false;
    }

    /**
     * Check to see if a tile is a wall
     * 
     * @param tile 
     * 	A tile to check if it is a wall
     * @return 
     * 	True if tile is a wall tile
     */
    public boolean isWall (BDTile tile) {
        return (tile.toString() == "WALL") ? true : false;
    }
    
    /**
     * Check to see if a tile is an empty
     * 
     * @param tile 
     * 	A tile to check if empty
     * @return 
     * 	True if the next tile is an empty tile
     */
    public boolean isE (BDTile tile) {
        return (tile.toString() == "EMPTY") ? true : false;
    }
    
    /**
     * Check to see if a tile is a dirt tile
     * 
     * @param nextTile 
     * 	A tile to check if it is dirt
     * @return 
     * 	True if the next tile is a dirt tile
     */
    public boolean isDirt (BDTile tile) {
        return (tile.toString() == "DIRT") ? true : false;
    }
    
    /**
     * Check to see if a tile is a diamond tile
     * 
     * @param tile 
     * 	A tile to check whether it is a diamond
     * @return 
     * 	True if the next tile is a diamond tile
     */
    public boolean isDiamond (BDTile tile) {
        return (tile.toString() == "DIAMOND") ? true : false;
    }
    
    /**
     * Check to see if a tile is a rock
     */
    public boolean isRock (BDTile tile) {
    	return (tile.toString() == "ROCK") ? true : false;
    }

    /**
     * Check to see if a tile is a firefly
     */
    public boolean isFireFly (BDTile tile) {
    	return (tile.toString() == "FIREFLY") ? true : false;
    }
    
    /**
     * Check to see if a tile is a butterfly
     */
    public boolean isButterFly (BDTile tile) {
    	return (tile.toString() == "BUTTERFLY") ? true : false;
    }
        
    /**
     * Check to see if a tile is a rock and can be moved
     * 
     * @param nextTile The next tile the player is to move on to
     * @param doubleNextTile The tile the next tile is to be moved to
     * @return True if the next tile can be moved to the subsequent tile
     */
    public boolean isRockMoveable (BDTile tile, BDTile nextTile) {
        return (tile.toString() == "ROCK"  && nextTile.toString() == "EMPTY") ? true : false;
    }
    
    /**
     * Check to see if a tile is a diamond and can be moved
     * 
     * @param nextTile The next tile the player is to move on to
     * @param doubleNextTile The tile the next tile is to be moved to
     * @return True if the next tile can be moved to the subsequent tile
     */
    public boolean isDiamondMoveable (BDTile tile, BDTile nextTile) {
        return (tile.toString() == "DIAMOND"  && nextTile.toString() == "EMPTY") ? true : false;
    }
    
    /**
     * Check whether a tile is the player
     * @param currentTile The current tile
     * @param nextTile
     * @return
     */
    public boolean isPlayer(BDTile tile) {
    	return (tile.toString() == "PLAYER") ? true : false;
    }
    
    /**
     * Increase the number of moves a player has taken by 1 and redraw the JLabel
     */
    private void incMoves() {
        moves += 1;
        setMovesLabel();
    }
    
    /**
     * Reset the moves to zero
     */
    private void resetMoves() {
        moves = 0;
        setMovesLabel();
    }
    
    /**
     * Set the Moves Label
     */
    private void setMovesLabel() {
        levelMovesLabel.setText("Moves: " + (moves));
    }
    
    /**
     * Go to the next level
     */
    private void nextLevel () {
        level = (level == numLevels ? 1 : level + 1);
    }
    
    /**
     * Go to the previous level
     */
    private void prevLevel() {
        level = (level == 1 ? numLevels : level - 1);
    }
    
    /**
     * Set the Level Label
     */
    private void setLevelLabel() {
        levelLabel.setText("Level: " + (level));
    }
    
    /**
     * Reset the timer time
     */
    private void resetLevelTimerTime() {
        time = 0;
        levelTimer.restart();
    }
    
    /**
     * Set the level time
     */
    private void setLevelTimerTime() {
        time += 1;
    }
    
    /**
     * Set the level Timer Label
     */
    private void setLevelTimerLabel() {
        levelTimerLabel.setText("Time: " + time);
    }
    
    /**
     * Stop the level timer
     */
    private void stopLevelTimer() {
    	levelTimer.stop();
    }
    
    /**
     * Reset the levelActionsTimer
     * @param file
     */
    private void resetLevelActionsTimer() {
    	levelActionsTimer.restart();
    }
    
    /**
     * Initialize all rocks to stationary 0 status
     */
    private void initObjects() {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < WIDTH; x++) {
                rockStatus[x][y] = 0;
                diamondStatus[x][y] = 0;
                fireFlyStatus[x][y] = 0;
            }
        }
    }
    
	public BoulderDash(String file) {	
		//set the screen
        setFocusable(true);
        this.setPreferredSize(new Dimension(1500, 800));
        
		blr = new BDLevelReader();
		
		//get the number of levels
		try {
			numLevels = blr.readLevels(file);
		} catch (Exception e) {
			System.out.println("Could not load levels.");
		}
		
        //add key listeners
        this.addKeyListener(new ControlsKeyListener());
        this.addKeyListener(new LevelControlKeyListener());
		
        //add the level and move label
        this.add(levelLabel);
        this.add(levelDiamondsCollected);
        this.add(levelMovesLabel);
        this.add(levelTimerLabel);
        this.add(levelPlayerStatus);
        
        //start the timers
        levelTimer = new Timer (SECONDDELAY, timerCounter);
        levelActionsTimer = new Timer (LEVELACTIONDELAY, levelActions);
        
		initLevel(level);
	}
    
    /**
     * Build the BoulderDash level gamefield and array of rectangles for tiles
     * 
     * @param level 
     * 	The Boulderdash Level
     */
    public void initLevel(int level) {
    	//set the current level
		try {
			blr.setCurrentLevel(level);
		} catch (Exception e) {
			System.out.println("Could not set level.");
		}
    	
		//build the gamefield array with the corresponding tile
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                this.gamefield[x][y] = blr.getTile(x, y);
            }
        }
        
        this.isReady = true;
        setLevelLabel();
        resetMoves();
        resetLevelTimerTime();
        resetLevelActionsTimer();
        initObjects();
        playerAlive = true;
        levelPlayerStatus.setText("");
        
        //writeLastLevel();
        repaint();
    }
	
	public void paintComponent(Graphics g) {
		if(!isReady) return;
		super.paintComponent(g);  
        Graphics2D g2 = (Graphics2D) g;
        for (int x = 0; x <= gamefield.length - 1; x++) {
            for (int y = 0; y <= gamefield[x].length - 1; y++) {
            	//System.out.println(x + ", " + y + ": " + gamefield[x][y].toString());
            	Rectangle2D.Double r = new Rectangle2D.Double((x+2)*30, (y+2)*30, 30, 30);
            	g2.draw(r);
                if (gamefield[x][y].toString() == "EMPTY") g2.setPaint(Color.BLACK);
                if (gamefield[x][y].toString() == "DIRT")  g2.setPaint(Color.GREEN);
                if (gamefield[x][y].toString() == "WALL")  g2.setPaint(Color.DARK_GRAY);
                if (gamefield[x][y].toString() == "ROCK")  g2.setPaint(Color.LIGHT_GRAY);
                if (gamefield[x][y].toString() == "FALLINGROCK")  g2.setPaint(Color.WHITE);
                if (gamefield[x][y].toString() == "DIAMOND")  g2.setPaint(Color.MAGENTA);
                if (gamefield[x][y].toString() == "FALLINGDIAMOND")  g2.setPaint(Color.YELLOW);
                if (gamefield[x][y].toString() == "AMOEBA")  g2.setPaint(Color.PINK);
                if (gamefield[x][y].toString() == "FIREFLY") g2.setPaint(Color.ORANGE);
                if (gamefield[x][y].toString() == "BUTTERFLY") g2.setPaint(Color.CYAN);
                if (gamefield[x][y].toString() == "EXIT")  g2.setPaint(Color.BLUE);
                if (gamefield[x][y].toString() == "PLAYER") g2.setPaint(Color.RED);
                g2.fill(r);
            }    
        }
	}
	
    /**
     * Main file execution.
     * @param args
     */
    public static void main(String args[]) {
        JFrame f = new JFrame("Boulder Dash");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setLayout(new FlowLayout());
        f.add(new BoulderDash("levels.xml"));
        f.pack();
        f.setVisible(true); 
    }
}
