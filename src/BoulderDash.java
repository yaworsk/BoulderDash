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
    //whether we should print the board
    private boolean isReady = false;
    
	//gamefield Variables
	private BDLevelReader blr;
	private int numLevels;
	private int level = 1;
    private int moves = 0;
    private int time = 0;
    
    //Timers
    private Timer levelTimer;
    private Timer levelActionsTimer;
    private static final int SECONDDELAY = 1000;
    private static final int LEVELACTIONDELAY = 200;

	//gamefield's rectangles
	private final int WIDTH = 40;
    private final int HEIGHT = 22;
	private BDTile[][] gamefield = new BDTile[WIDTH][HEIGHT];
	
	//level variables
	private int diamondsCollected = 0;
    private JLabel levelLabel = new JLabel ("Level: " + level);
    private JLabel levelDiamondsCollected = new JLabel ("Diamonds Collected: " + (diamondsCollected));
    private JLabel levelMovesLabel = new JLabel ("Moves: " + (moves));
    private JLabel levelTimerLabel = new JLabel("Time: " + time);
	
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
            
            //loop through the gamefield to find the player and move them
            for (int x = 0; x <= gamefield.length - 1; x++) {
                for (int y = 0; y <= gamefield[x].length - 1; y++) {
                    if (gamefield[x][y].toString() == "PLAYER") {
                        if (isMoveable(gamefield[x + dx][y + dy])) {
                            //move the player
                            movePlayer(x, y, dx, dy);
                            //bc the gamefield is 2d, we need a boolean to check so we can get out
                            exit = true;
                            break;
                        } //end can move
                    } //end if player
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
	
    /**
     * ActionListener for timer level actions
     */
    ActionListener levelActions = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		//loop through all level tiles
    	}
    };
    
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
        
        //check if the next tile is a box
        if (isRock(nextTile, gamefield[x + dx *2][y + dy *2]) && !moved) {
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
     * Check to see if the player can move to the next tile
     * 
     * @param nextTile The next tile the player is to move on to
     * @return
     */
    public boolean isMoveable (BDTile nextTile) {
        return (nextTile.toString() == "EMPTY" || nextTile.toString() == "ROCK" || nextTile.toString() == "DIRT" || nextTile.toString() == "DIAMOND") ? true : false;
    }
    
    /**
     * Check to see if the next tile is an empty
     * 
     * @param nextTile 
     * 	The next tile the player is to move on to
     * @return 
     * 	True if the next tile is an empty tile
     */
    public boolean isE (BDTile nextTile) {
        return (nextTile.toString() == "EMPTY") ? true : false;
    }
    
    /**
     * Check to see if the next tile is a dirt tile
     * 
     * @param nextTile 
     * 	The next tile the player is to move on to
     * @return 
     * 	True if the next tile is a dirt tile
     */
    public boolean isDirt (BDTile nextTile) {
        return (nextTile.toString() == "DIRT") ? true : false;
    }
    
    /**
     * Check to see if the next tile is a diamond tile
     * 
     * @param nextTile 
     * 	The next tile the player is to move on to
     * @return 
     * 	True if the next tile is a diamond tile
     */
    public boolean isDiamond (BDTile nextTile) {
        return (nextTile.toString() == "DIAMOND") ? true : false;
    }
    
    /**
     * Check to see if the next tile is a rock and can be moved
     * 
     * @param nextTile The next tile the player is to move on to
     * @param doubleNextTile The tile the next tile is to be moved to
     * @return True if the next tile can be moved to the subsequent tile
     */
    public boolean isRock (BDTile nextTile, BDTile doubleNextTile) {
        return (nextTile.toString() == "ROCK"  && doubleNextTile.toString() == "EMPTY") ? true : false;
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
                if (gamefield[x][y].toString() == "FIREFLY")  g2.setPaint(Color.ORANGE);
                if (gamefield[x][y].toString() == "BUTTERFLY") g2.setPaint(Color.CYAN);
                if (gamefield[x][y].toString() == "EXIT")  g2.setPaint(Color.BLUE);
                if (gamefield[x][y].toString() == "PLAYER") g2.setPaint(Color.RED);;
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
