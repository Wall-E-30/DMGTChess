import java.util.HashSet;
import java.util.Stack;
import javax.swing.*;
import java.awt.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

// STATE TRACKERS
enum Color {WHITE, BLACK}
enum GameState {ACTIVE, WHITE_WINS, BLACK_WINS, STALEMATE}

class MoveRecord{ // THE HISTORICAL EDGE
    // VARIABLES
    int startX, startY, endX, endY; //Coordinates for the primary piece movement
    int capX, capY; //Coordinates of the captured piece
    Piece moved, captured; //Object references to what was moved and what was taken
    boolean wasPawnPromotion, wasCastling, movedHadMoved;

    //MoveRecord Class Constructor
    public MoveRecord(int sx, int sy, int ex, int ey, Piece m, Piece c, int cx, int cy, boolean promo, boolean castle, boolean hadMoved){
        startX = sx;
        startY = sy;
        endX = ex;
        endY = ey;
        moved = m;
        captured = c;
        capX = cx;
        capY = cy;
        wasPawnPromotion = promo;
        wasCastling = castle;
        movedHadMoved = hadMoved;
    }
}

class Square{ // THE GRAPH VERTEX
    // VARIABLES
    private int x, y; //The fixed grid coordinates
    private Piece piece; //The object currently occupying this vertex

    // METHODS
    // Square Class Constructor
    public Square(int x, int y){
        this.x = x;
        this.y = y;
    }
    // Getters for co-ordinates
    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }
    // Getter / Setter for the occupying piece
    public Piece getPiece(){
        return piece;
    }
    public void setPiece(Piece piece){
        this.piece = piece;
    }
    public boolean isEmpty(){
        return piece == null; // returns true if piece == null
    }
}

//THE ADJACENCY MATRIX
class BoardGraph{
    // VARIABLES
    Square[][] grid; //The 2D array holding the 64 Square vertices
    Stack<MoveRecord> moveHistory = new Stack<>(); //Tracks executed moves
    Stack<MoveRecord> redoStack = new Stack<>(); //Tracks undone moves

    //METHODS
    //BoardGraph Class Constructor
    public BoardGraph(){ //Must initialize the grid and fill it with 64 empty Square objects
        grid = new Square[8][8];
        for(int x = 0; x < 8; x++){
            for(int y = 0; y < 8; y++){
                grid[x][y] = new Square(x, y);
            }
        }
    }
    public Square getSquare(int x, int y){ //Returns the vertex at the given coordinates
        if(x < 0 || x > 7 || y < 0 || y > 7){
            return null;
        }
        return grid[x][y];
    }
    public void setupStandardBoard(){
        moveHistory.clear();
        redoStack.clear();
        for(int x = 0; x < 8; x++){
            for(int y = 0; y < 8; y++){
                grid[x][y].setPiece(null);
            }
        }
    }
}

public class DMGTChess {
    
}
