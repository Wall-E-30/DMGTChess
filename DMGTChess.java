import java.nio.file.WatchEvent.Kind;
import java.util.HashSet;
import java.util.Stack;

// STATE TRACKERS
enum Color {WHITE, BLACK}
enum GameState {ACTIVE, WHITE_WINS, BLACK_WINS, STALEMATE}

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
}
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
    public boolean isEmpty{
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

class MoveValidator{
    public static HashSet<Square> getAttackedVertices(BoardGraph board, Color enemyColor){
        HashSet<Square> attackedSet = new HashSet<>();
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                Square sq = board.getSquare(x, y);
                if(!sq.isEmpty && sq.getPiece().getColor() == enemyColor){
                    attackedSet.addAll(sq.getPiece().calculateLegalEdges(board, sq));
                }
            }
        }
        return attackedSet;
    }
    public static boolean isMoveStrictlyLegal(BoardGraph board, Square start, Square target, Color movingColor){
        Piece movingPiece = start.getPiece();
        Piece targetOriginalPiece = target.getPiece();

        boolean isCastling = (movingPiece instanceof King) && Math.abs(start.getX() - target.getX()) == 2;
        Square transitSquare = null;
        if(isCastling){
            int dir = (target.getX() > start.getX()) ? 1 : -1;
        }
        Square epCaptureSquare = null;
        Piece epCapturePiece = null;
        if(movingPiece instanceof Pawn && target.isEmpty() && start.getX() != target.getX()){
            epCaptureSquare = board.getSquare(target.getX(), start.getY());
            epCapturePiece = epCaptureSquare.getPiece();
            epCaptureSquare.setPiece(null);
        }
        start.setPiece(null);
        target.setPiece(movingPiece);

        Square kingVertex = null;
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                Square sq = board.getSquare(x, y);
                if(!sq.isEmpty() && sq.getPiece() instanceof King && sq.getPiece().getColor() == movingColor){
                    kingVertex = sq;
                    break;
                }
            }
        }
        Color enemyColor = (movingColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
        HashSet<Square> enemyAttacks = getAttackedVertices(board, enemyColor);
        boolean isLegal = (kingVertex != null) && !enemyAttacks.contains(kingVertex);
        
        // Strict Castling Constraint: King cannot pass through check or be in check initially
        if (isCastling && isLegal) {
            if (enemyAttacks.contains(start) || enemyAttacks.contains(transitSquare)) {
                isLegal = false;
            }
        }

        target.setPiece(targetOriginalPiece);
        start.setPiece(movingPiece);
        if (epCaptureSquare != null){
            epCaptureSquare.setPiece(epCapturedPiece); 
        }
        return isLegal;
    }

    public static GameState evaluateGameState(BoardGraph board, Color currentTurn) {
        boolean hasLegalMoves = false;
        Square kingVertex = null;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Square sq = board.getSquare(x, y);
                if (!sq.isEmpty() && sq.getPiece().getColor() == currentTurn) {
                    if (sq.getPiece() instanceof King){
                        kingVertex = sq;
                    }
                    HashSet<Square> pseudoEdges = sq.getPiece().calculateLegalEdges(board, sq);
                    for (Square target : pseudoEdges) {
                        if (isMoveStrictlyLegal(board, sq, target, currentTurn)) {
                            hasLegalMoves = true;
                            break; 
                        }
                    }
                }
            }
        }

        Color enemyColor = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        boolean inCheck = kingVertex != null && getAttackedVertices(board, enemyColor).contains(kingVertex);

        if (!hasLegalMoves) {
            if (inCheck) return (currentTurn == Color.WHITE) ? GameState.BLACK_WINS : GameState.WHITE_WINS;
            else return GameState.STALEMATE;
        }
        return GameState.ACTIVE;
    }
}

public class DMGTChess {
    
}
