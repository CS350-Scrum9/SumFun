package controller;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.FileHandler;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import model.MoveCounter;
import model.ObservableTile;
import model.Scoring;
import model.Tile;
import model.TileQueue;
import model.TimedGamemode;

import view.BoardView;
import view.HighScoreBoard;

public class SumFunController {
	
	private Scoring score;
	private TileQueue tileQ;
	private ObservableTile[][] tiles;
	private BoardView board;
	private HighScoreBoard highScoreBoard;
	private MoveCounter mc;
	private FileHandler fileHandler;
	private TimedGamemode gamemode;
	private boolean timed;
	private boolean canclick;
	private boolean clearTilesUsed;
	private int neighborCount;
	private int hintCount;
	
	public SumFunController(){}
	
	public SumFunController(Scoring score, TileQueue tileQ, 
			ObservableTile[][] tiles, MoveCounter mc, BoardView board, 
			HighScoreBoard highScoreBoard) throws SecurityException, IOException{
		this.score = score;
		this.tileQ = tileQ;
		this.tiles = tiles;
		this.board = board; 
		this.mc = mc;
		
		hintCount = 3;
		clearTilesUsed = false;
		this.canclick = true;
		this.timed = false;
		this.fileHandler = new FileHandler();
		
		this.gamemode = TimedGamemode.getGamemode();
		this.highScoreBoard = highScoreBoard;
		
		this.board.addTileButtonHandler(new TileButtonHandler());
		this.board.addRefreshButtonHandler(new RefreshButtonHandler());
		this.board.addRadioButtonListener(new RadioButtonListener());
		this.board.addMenuItemListener(new MenuItemListener());
		this.board.addHintButtonHandler(new HintButtonHandler());
		this.board.addRemoveButtonHandler(new RemoveButtonHandler());
	}

	private class TileButtonHandler implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(canclick){
				int row; 
				int column;
				Tile tile = (Tile) e.getSource();
				row = tile.getRow();
				column = tile.getColumn();
				 					
				if(!tile.isOccupied()){
				 	placeTile(tiles[row][column], tileQ.getNextValue());
				}else if(clearTilesUsed == false){
					clearAllTilesWithNumber(tile.getNumber());
					toggleTiles();
				}
			}
		}	
	}
	
	private class RefreshButtonHandler implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Timer randomizer = new Timer(10, new ActionListener(){
			private int count = 0;
		    private int maxCount = 50;
				 		    
				public void actionPerformed(ActionEvent e){
					if (count >= maxCount) {
						((Timer) e.getSource()).stop();
						tileQ.setRefreshIsEnabled(false);
					} else {
						tileQ.reset();
						count++;
					}
				}
			});
			randomizer.start();
		}
	}
	
	private class HintButtonHandler implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			useHint();
		}
	}
	
	private class RemoveButtonHandler implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e){
			toggleTiles();
		}
	}
	
	private class MenuItemListener implements ActionListener {
	
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals("New")){
				if(timed == true) {
					resetGame(1);
				} else {
					resetGame(0);
				}
			} else {
				System.exit(0);
			}
		}
	}
	
	private class RadioButtonListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals("Timed")) {
				timed = true;
			} else {
				timed = false;
			}
		}
	}
	

	/**
	 * Checks the sum of the next number with all the values of each of the neighboring tiles
	 * and compares that sum to see if sum modular 10 equals the number (qValue) in the queue.
	 * @param tile The origin tile whose neighbors are to be checked.
	 * @return Boolean saying if the move was successful.
	 */
	public boolean checkNeighbors(ObservableTile tile, int queueValue){
		int row = 0;
		int column = 0;
		int sum = 0;
		neighborCount = 0;
		row = tile.getRow();
		column = tile.getColumn();

		//Add the numbers from all surrounding tiles
		for(int i = -1; i < 2; i++){
			for(int j = -1; j < 2; j++){
				if(!(i == 0 && j == 0) && tiles[row+i][column+j].isOccupied()) {
					neighborCount++;
					sum += tiles[row+i][column+j].getNumber();
				}
			}
		}
		return sum%10 == queueValue;
	}
	
	/**
	 * Resets the values of all neighbor tiles. Only call if checkNeighbors() is true.
	 * Also sets the score from resetting neighbors if three or more neighbors are 
	 * reset.
	 * @param tile The tile whose neighbors will be reset.
	 */
	public void resetNeighbors(ObservableTile tile){
		int row = 0;
		int column = 0;
		int newScore;
		row = tile.getRow();
		column = tile.getColumn();
		for(int i = -1; i < 2; i++){
			for(int j = -1; j < 2; j++){
				tiles[row + i][column + j].setOccupied(false);
				tiles[row+i][column+j].setNumber(0);
			}
		}
		mc.setTileCount(mc.getTileCount() - neighborCount);
		if(neighborCount >= 3){
			newScore = score.getScore() + neighborCount * 10;
			score.setScore(newScore);
		}
		checkGameOver();
	}
	
	/**
	 * Called when a player attempts to place a tile on the board. 
	 * Checks neighbor values and either removes the tiles, or adds the new one.
	 * @param tile Where the player is trying to place the tile.
	 * @param qValue The value of the next tile in queue to be placed.
	 */
	public void placeTile(ObservableTile tile, int queueValue){
		canclick = false;
		if(checkNeighbors(tile, queueValue)) {
			tile.setOccupied(true);
			tile.setNumber(queueValue);
			Timer greenFlash = new Timer(200, new ActionListener(){
			 	private int count = 0;
			 	private int maxCount = 4;
			 	private boolean on = false;
			 	private int row = tile.getRow();
			 	private int col = tile.getColumn();
			 
			 	public void actionPerformed(ActionEvent e) {
			 		if (count >= maxCount) {
			 			for(int i = -1; i < 2; i++){
			 			    for(int j = -1; j < 2; j++){
			 			    	if(tiles[row+i][col+j].isOccupied()) {
			 			    		tiles[row+i][col+j].getTile().setBackground(null);
			 			        }
			 			    }
			 			}
			 			((Timer) e.getSource()).stop();
			 			resetNeighbors(tile);
			 			canclick = true;
			        } else {
			 	    	for(int i = -1; i < 2; i++){
			    			for(int j = -1; j < 2; j++){
			 			    	if(tiles[row+i][col+j].isOccupied()) {
			 						tiles[row+i][col+j].getTile().setBackground( on ? Color.GREEN : null);
			    				}
			    			}
			    		}
 			            on = !on;
			            count++;
           	        }
			 	}
			 });
			 greenFlash.start();
		} else { 
			tile.setOccupied(true);
			tile.setNumber(queueValue);
			Timer redFlash = new Timer(200, new ActionListener(){
				 private int count = 0;
				 private int maxCount = 4;
				 private boolean on = false;
				 
				 public void actionPerformed(ActionEvent e) {
				 	if (count >= maxCount) {
				 		tile.getTile().setBackground(null);
				 		((Timer) e.getSource()).stop();
				 		canclick = true;
				    } else {
				 		tile.getTile().setBackground( on ? Color.RED : null);
				 		on = !on;
				 		count++;
				 	}
				 }
			});
			redFlash.start();
			mc.setTileCount(mc.getTileCount() + 1);
			checkGameOver();
		 }
	}
	
	
	private void resetGame(int version){
		for(int i = 1; i < 10; i++){
			for(int j = 1; j < 10; j++){
				tiles[i][j].resetTile();
			}
		}
		hintCount = 3;
		tileQ.reset();
		mc.setTileCount(49);
		score.setScore(0);
		board.switchGameModeView(version);
		highScoreBoard.setVisible(false);
		clearTilesUsed = false;
		
		if (version == 1) {
			gamemode.setTime(300);
		} else {
			mc.setMoveCount(50);
		}
	}
	
	private void checkGameOver(){
		int optionNumber = 10;
		
		tileQ.dequeue();
		
		if(mc.getTileCount() >= 81 && clearTilesUsed == true){
			gameOver("Game Over! All tiles are occupied! New Game?", JOptionPane.ERROR_MESSAGE);
		} else if(mc.getTileCount() <= 0){
			if (timed) {
				gamemode.stopTimer();
			}
			/*if (( fileHandler).isHighScore(score.getScore(), timed)) {
				String name = JOptionPane.showInputDialog(null, "Congratulations! New High Score!  Please enter your name");
				if (timed) {
					fileHandler.addScore(name, gamemode.getTime(), score.getScore(), timed);
				} else {
					fileHandler.addScore(name, Integer.toString(mc.getMoveCount()) , score.getScore(), timed);
				}
				if (timed) {
					highScoreBoard.generateView("Timed");
				} else {
					highScoreBoard.generateView("Untimed");
				}
				highScoreBoard.setVisible(true);
			}*/
			
			gameOver("Congratulations! You win! New Game?", JOptionPane.PLAIN_MESSAGE);
			
		}
		
		if(timed == false){
			mc.decrementCount();
			
			if(mc.getMoveCount() <= 0){
				gameOver("Game Over! You ran out of moves! New Game?", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		reset(optionNumber);
		
	}
	
	public void gameOver(String message, int icon) {
		int optionNumber;
		
		Object[] o = {"Yes!", "No, I want to quit the game."};
		optionNumber = JOptionPane.showOptionDialog(null, message,
				"Sum Fun", JOptionPane.YES_NO_OPTION, icon, null, o, o[1]);
		
		reset(optionNumber);
	}
	
	public void reset(int optionNumber) {
		if(optionNumber == JOptionPane.YES_OPTION){
			if(timed == true){
				resetGame(1);
			} else{
				resetGame(0);
			}
		} else if(optionNumber == JOptionPane.NO_OPTION) {
			System.exit(0);	
		}
	}
	
	private void clearAllTilesWithNumber(int n){
		int count = 0;
		for(int i = 1; i < 10; i++){
			for(int j = 1; j < 10; j++){
				if(tiles[i][j].getNumber() == n){
					count++;
					tiles[i][j].setOccupied(false);
					tiles[i][j].setNumber(0);
				}
			}
		}
		
		mc.setTileCount(mc.getTileCount() - count);
		clearTilesUsed = true;
		checkGameOver();
	}
	
	private void useHint(){
		neighborCount = 0;
		int maxCount = 0;
		int row = 0;
		int column = 0;
		for(int i = 1; i < 10; i++){
			for(int j = 1; j < 10; j++){
				if(!tiles[i][j].isOccupied() && checkNeighbors(tiles[i][j], tileQ.getNextValue())){
					if(neighborCount > maxCount){
						maxCount = neighborCount;
						row = i;
						column = j;
					}
				}
			}
		}
		
		if(maxCount > 0){
			tiles[row][column].startFlash();
			tiles[row][column].stopFlash();
		}
	}
	
	private void toggleTiles(){
		for(ObservableTile[] g : tiles){
			for(ObservableTile t : g){
				t.toggleEnable(clearTilesUsed);
			}
		}
	}
}
