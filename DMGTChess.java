import java.util.HashSet;
import java.util.Stack;

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

abstract class Piece{
    Color color;
    String symbol;
    boolean hasMoved = false;
    public Color getColor(){
        return color;
    }
    public String getSymbol(){
        return symbol;
    }
    public abstract HashSet<Square> calculateLegalEdges(BoardGraph board, Square current);
    protected void addSlidingEdges(BoardGraph board, Square current, HashSet<Square> edges, int[][] dirs){
        for(int[]dir : dirs){
            int cx = current.getX() + dir[0];
            int cy = current.getY() + dir[1];
            while(true){
                Square target = board.getSquare(cx, cy);
                if(target == null){
                    break;
                }
                if(target.isEmpty()){
                    edges.add(target);
                }
                else{
                    if(target.getPiece().getColor() != this.color){
                        edges.add(target);
                        break;
                    }
                }
                cx += dir[0];
                cy += dir[1];
            }
        }
    }
}

class Pawn extends Piece{
    // PAWN Class Constructor
    public Pawn(Color color){
        super(color, color == Color.WHITE ? "♙" : "♟");
    }
    public HashSet<Square> calculateEdges(BoardGraph board, Square current){
        HashSet<Square> edges = new HashSet<>();
        int dir = (color == Color.WHITE) ? 1 : -1;
        int startRow = (color == Color.WHITE) ? 1 : 6;

        Square forward1 = board.getSquare(current.getX(), current.getY() + (dir * 2));
        if(forward1 != null && forward1.isEmpty()){
            edges.add(forward1);
            Square forward2 = board.getSquare(current.getX(), current.getY() + (dir * 2));
            if(current.getY() == startRow && forward2 != null && forward2.isEmpty()){
                edges.add(forward2);
            }
        }

        Square[] diags = {
            board.getSquare(current.getX() - 1, current.getY() + dir),
            board.getSquare(current.getX() + 1, current.getY() + dir)
        };
        for(Square diag : diags){
            if(diag != null && !diag.isEmpty() && diag.getPiece().getColor() != this.color){
                edges.add(diag);
            }
        }

        if(!board.moveHistory.isEmpty()){
            MoveRecord lastMove = board.moveHistory.peek();
            if(lastMove.moved instanceof Pawn && Math.abs(lastMove.startY - lastMove.endY) == 2){
                if(lastMove.endY == current.getY() && Math.abs(lastMove.endX - current.getX()) == 1){
                    edges.add(board.getSquare(lastMove.endX, current.getY() + dir));
                }
            }
        }
        return edges;
    }
}

class Knight extends Piece {
    public Knight(Color color) { 
        super(color, color == Color.WHITE ? "♘" : "♞"); 
    }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        int[][] jumps = {{2,1},{1,2},{-1,2},{-2,1},{-2,-1},{-1,-2},{1,-2},{2,-1}};
        for (int[] jump : jumps) {
            Square target = board.getSquare(current.getX() + jump[0], current.getY() + jump[1]);
            if (target != null && (target.isEmpty() || target.getPiece().getColor() != this.color)) {
                edges.add(target);
            }
        }
        return edges;
    }
}

class Rook extends Piece {
    public Rook(Color color) { 
        super(color, color == Color.WHITE ? "♖" : "♜"); 
    }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        addSlidingEdges(board, current, edges, new int[][]{{0,1},{1,0},{0,-1},{-1,0}});
        return edges;
    }
}

class Bishop extends Piece {
    public Bishop(Color color) { 
        super(color, color == Color.WHITE ? "♗" : "♝"); 
    }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        addSlidingEdges(board, current, edges, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
        return edges;
    }
}

class Queen extends Piece {
    public Queen(Color color) { 
        super(color, color == Color.WHITE ? "♕" : "♛"); 
    }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        addSlidingEdges(board, current, edges, new int[][]{{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}});
        return edges;
    }
}

class King extends Piece {
    public King(Color color) { 
        super(color, color == Color.WHITE ? "♔" : "♚"); 
    }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        int[][] moves = {{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] m : moves) {
            Square target = board.getSquare(current.getX() + m[0], current.getY() + m[1]);
            if (target != null && (target.isEmpty() || target.getPiece().getColor() != this.color)) {
                edges.add(target);
            }
        }

        // CASTLING LOGIC (DMGT Subset Generation)
        if (!this.hasMoved) {
            int y = current.getY();
            
            // Kingside (O-O)
            Square hRookSq = board.getSquare(7, y);
            if (hRookSq != null && !hRookSq.isEmpty() && hRookSq.getPiece() instanceof Rook && !hRookSq.getPiece().hasMoved) {
                if (board.getSquare(5, y).isEmpty() && board.getSquare(6, y).isEmpty()) {
                    edges.add(board.getSquare(6, y)); // Add g1/g8
                }
            }
            // Queenside (O-O-O)
            Square aRookSq = board.getSquare(0, y);
            if (aRookSq != null && !aRookSq.isEmpty() && aRookSq.getPiece() instanceof Rook && !aRookSq.getPiece().hasMoved) {
                if (board.getSquare(1, y).isEmpty() && board.getSquare(2, y).isEmpty() && board.getSquare(3, y).isEmpty()) {
                    edges.add(board.getSquare(2, y)); // Add c1/c8
                }
            }
        }
        return edges;
    }
}

public class DMGTChess {
    
}