import java.util.HashSet;
import java.util.Stack;

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
        for (int x = 0; x < 8; x++) {
            getSquare(x, 1).setPiece(new Pawn(Color.WHITE));
            getSquare(x, 6).setPiece(new Pawn(Color.BLACK));
        }
        getSquare(0, 0).setPiece(new Rook(Color.WHITE)); getSquare(7, 0).setPiece(new Rook(Color.WHITE));
        getSquare(1, 0).setPiece(new Knight(Color.WHITE)); getSquare(6, 0).setPiece(new Knight(Color.WHITE));
        getSquare(2, 0).setPiece(new Bishop(Color.WHITE)); getSquare(5, 0).setPiece(new Bishop(Color.WHITE));
        getSquare(3, 0).setPiece(new Queen(Color.WHITE)); getSquare(4, 0).setPiece(new King(Color.WHITE));
        
        getSquare(0, 7).setPiece(new Rook(Color.BLACK)); getSquare(7, 7).setPiece(new Rook(Color.BLACK));
        getSquare(1, 7).setPiece(new Knight(Color.BLACK)); getSquare(6, 7).setPiece(new Knight(Color.BLACK));
        getSquare(2, 7).setPiece(new Bishop(Color.BLACK)); getSquare(5, 7).setPiece(new Bishop(Color.BLACK));
        getSquare(3, 7).setPiece(new Queen(Color.BLACK)); getSquare(4, 7).setPiece(new King(Color.BLACK));
    }
}

abstract class Piece {
    Color color;
    String symbol;
    boolean hasMoved = false; // NEW: Crucial for Castling

    public Piece(Color color, String symbol) { this.color = color; this.symbol = symbol; }
    public Color getColor() { return color; }
    public String getSymbol() { return symbol; }
    public abstract HashSet<Square> calculateLegalEdges(BoardGraph board, Square current);

    protected void addSlidingEdges(BoardGraph board, Square current, HashSet<Square> edges, int[][] dirs) {
        for (int[] dir : dirs) {
            int cx = current.getX() + dir[0], cy = current.getY() + dir[1];
            while (true) {
                Square target = board.getSquare(cx, cy);
                if (target == null) break; 
                if (target.isEmpty()) edges.add(target);
                else {
                    if (target.getPiece().getColor() != this.color) edges.add(target);
                    break;
                }
                cx += dir[0]; cy += dir[1];
            }
        }
    }
}

class Pawn extends Piece {
    public Pawn(Color color) { super(color, color == Color.WHITE ? "♙" : "♟"); }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        int dir = (color == Color.WHITE) ? 1 : -1;
        int startRow = (color == Color.WHITE) ? 1 : 6;

        Square forward1 = board.getSquare(current.getX(), current.getY() + dir);
        if (forward1 != null && forward1.isEmpty()) {
            edges.add(forward1);
            Square forward2 = board.getSquare(current.getX(), current.getY() + (dir * 2));
            if (current.getY() == startRow && forward2 != null && forward2.isEmpty()) edges.add(forward2);
        }
        
        Square[] diags = { board.getSquare(current.getX() - 1, current.getY() + dir), 
                           board.getSquare(current.getX() + 1, current.getY() + dir) };
        for (Square diag : diags) {
            if (diag != null && !diag.isEmpty() && diag.getPiece().getColor() != this.color) edges.add(diag);
        }

        if (!board.moveHistory.isEmpty()) {
            MoveRecord lastMove = board.moveHistory.peek();
            if (lastMove.moved instanceof Pawn && Math.abs(lastMove.startY - lastMove.endY) == 2) {
                if (lastMove.endY == current.getY() && Math.abs(lastMove.endX - current.getX()) == 1) {
                    edges.add(board.getSquare(lastMove.endX, current.getY() + dir));
                }
            }
        }
        return edges;
    }
}

class Knight extends Piece {
    public Knight(Color color) { super(color, color == Color.WHITE ? "♘" : "♞"); }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        int[][] jumps = {{2,1},{1,2},{-1,2},{-2,1},{-2,-1},{-1,-2},{1,-2},{2,-1}};
        for (int[] jump : jumps) {
            Square target = board.getSquare(current.getX() + jump[0], current.getY() + jump[1]);
            if (target != null && (target.isEmpty() || target.getPiece().getColor() != this.color)) edges.add(target);
        }
        return edges;
    }
}

class Rook extends Piece {
    public Rook(Color color) { super(color, color == Color.WHITE ? "♖" : "♜"); }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        addSlidingEdges(board, current, edges, new int[][]{{0,1},{1,0},{0,-1},{-1,0}});
        return edges;
    }
}

class Bishop extends Piece {
    public Bishop(Color color) { super(color, color == Color.WHITE ? "♗" : "♝"); }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        addSlidingEdges(board, current, edges, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
        return edges;
    }
}

class Queen extends Piece {
    public Queen(Color color) { super(color, color == Color.WHITE ? "♕" : "♛"); }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        addSlidingEdges(board, current, edges, new int[][]{{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}});
        return edges;
    }
}

class King extends Piece {
    public King(Color color) { super(color, color == Color.WHITE ? "♔" : "♚"); }
    @Override
    public HashSet<Square> calculateLegalEdges(BoardGraph board, Square current) {
        HashSet<Square> edges = new HashSet<>();
        int[][] moves = {{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] m : moves) {
            Square target = board.getSquare(current.getX() + m[0], current.getY() + m[1]);
            if (target != null && (target.isEmpty() || target.getPiece().getColor() != this.color)) edges.add(target);
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

class MoveValidator{
    public static HashSet<Square> getAttackedVertices(BoardGraph board, Color enemyColor){
        HashSet<Square> attackedSet = new HashSet<>();
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                Square sq = board.getSquare(x, y);
                if(!sq.isEmpty() && sq.getPiece().getColor() == enemyColor){
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

public class DMGTChess extends JFrame {
    private BoardGraph board;
    private JButton[][] buttons;
    private Square selectedVertex = null;
    private Color currentTurn = Color.WHITE;
    private HashSet<Square> currentValidEdges = new HashSet<>();
    private GameState state = GameState.ACTIVE;
    
    private JTextArea historyArea;
    private JButton undoBtn, redoBtn;

    public DMGTChess() {
        setTitle("DMGT Chess - Complete MVP (+Castling)");
        setSize(950, 700); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        board = new BoardGraph();
        board.setupStandardBoard();
        buttons = new JButton[8][8];

        buildBoardUI();
        buildHistoryUI(); 
        refreshBoardUI();
        setVisible(true);
    }

    private void buildBoardUI() {
        JPanel boardContainer = new JPanel(new BorderLayout());
        JPanel gridPanel = new JPanel(new GridLayout(8, 8));
        
        JPanel rowLabels = new JPanel(new GridLayout(8, 1));
        for (int i = 8; i >= 1; i--) {
            JLabel lbl = new JLabel("  " + i + "  ", SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 16));
            rowLabels.add(lbl);
        }
        
        JPanel colLabels = new JPanel(new GridLayout(1, 8));
        for (char c = 'A'; c <= 'H'; c++) {
            JLabel lbl = new JLabel(String.valueOf(c), SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 16));
            colLabels.add(lbl);
        }

        Font pieceFont = new Font("SansSerif", Font.PLAIN, 45);
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 8; x++) {
                JButton btn = new JButton();
                btn.setFont(pieceFont);
                btn.setFocusPainted(false);
                final int finalX = x;
                final int finalY = y;
                btn.addActionListener(e -> handleSquareClick(finalX, finalY));
                buttons[x][y] = btn;
                gridPanel.add(btn);
            }
        }
        
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(gridPanel, BorderLayout.CENTER);
        centerWrapper.add(colLabels, BorderLayout.SOUTH);
        
        boardContainer.add(centerWrapper, BorderLayout.CENTER);
        boardContainer.add(rowLabels, BorderLayout.WEST);
        add(boardContainer, BorderLayout.CENTER);
    }

    private void buildHistoryUI() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(250, 0)); 
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controls = new JPanel(new GridLayout(1, 2, 5, 0));
        undoBtn = new JButton("Undo");
        redoBtn = new JButton("Redo");
        
        undoBtn.addActionListener(e -> performUndo());
        redoBtn.addActionListener(e -> performRedo());
        controls.add(undoBtn); controls.add(redoBtn);

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(historyArea);
        
        sidePanel.add(new JLabel("Move History", SwingConstants.CENTER), BorderLayout.NORTH);
        sidePanel.add(scrollPane, BorderLayout.CENTER);
        sidePanel.add(controls, BorderLayout.SOUTH);
        add(sidePanel, BorderLayout.EAST); 
    }

    private void performUndo() {
        if (board.moveHistory.isEmpty()) return;
        MoveRecord last = board.moveHistory.pop();
        board.redoStack.push(last);
        
        Square startSq = board.getSquare(last.startX, last.startY);
        Square endSq = board.getSquare(last.endX, last.endY);
        Square capSq = board.getSquare(last.capX, last.capY);
        
        startSq.setPiece(last.moved);
        startSq.getPiece().hasMoved = last.movedHadMoved; // Restore historical hasMoved state
        endSq.setPiece(null);
        if (last.captured != null) capSq.setPiece(last.captured);

        // Undo Castling Rook movement
        if (last.wasCastling) {
            int rookStartX = (last.endX == 6) ? 7 : 0;
            int rookEndX = (last.endX == 6) ? 5 : 3;
            Square rookStart = board.getSquare(rookStartX, last.startY);
            Square rookEnd = board.getSquare(rookEndX, last.startY);
            rookStart.setPiece(rookEnd.getPiece());
            rookEnd.setPiece(null);
            rookStart.getPiece().hasMoved = false; 
        }

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        selectedVertex = null; currentValidEdges.clear();
        state = MoveValidator.evaluateGameState(board, currentTurn);
        updateHistoryText(); refreshBoardUI();
    }

    private void performRedo() {
        if (board.redoStack.isEmpty()) return;
        MoveRecord next = board.redoStack.pop();
        board.moveHistory.push(next);
        
        Square startSq = board.getSquare(next.startX, next.startY);
        Square endSq = board.getSquare(next.endX, next.endY);
        Square capSq = board.getSquare(next.capX, next.capY);
        
        endSq.setPiece(next.wasPawnPromotion ? new Queen(next.moved.getColor()) : next.moved);
        endSq.getPiece().hasMoved = true;
        startSq.setPiece(null);
        if (capSq != endSq) capSq.setPiece(null); 

        // Redo Castling Rook movement
        if (next.wasCastling) {
            int rookStartX = (next.endX == 6) ? 7 : 0;
            int rookEndX = (next.endX == 6) ? 5 : 3;
            Square rookStart = board.getSquare(rookStartX, next.startY);
            Square rookEnd = board.getSquare(rookEndX, next.startY);
            rookEnd.setPiece(rookStart.getPiece());
            rookStart.setPiece(null);
            rookEnd.getPiece().hasMoved = true;
        }

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        selectedVertex = null; currentValidEdges.clear();
        state = MoveValidator.evaluateGameState(board, currentTurn);
        updateHistoryText(); refreshBoardUI();
    }

    private void executeMove(Square target) {
        int cx = target.getX(), cy = target.getY();
        Piece captured = target.getPiece();
        
        boolean isCastling = (selectedVertex.getPiece() instanceof King) && Math.abs(selectedVertex.getX() - target.getX()) == 2;
        boolean movedHadMoved = selectedVertex.getPiece().hasMoved;

        if (selectedVertex.getPiece() instanceof Pawn && target.isEmpty() && selectedVertex.getX() != target.getX()) {
            cy = selectedVertex.getY(); 
            captured = board.getSquare(cx, cy).getPiece();
        }

        board.moveHistory.push(new MoveRecord(
            selectedVertex.getX(), selectedVertex.getY(), 
            target.getX(), target.getY(), 
            selectedVertex.getPiece(), captured, cx, cy, false, isCastling, movedHadMoved
        ));
        board.redoStack.clear(); 

        target.setPiece(selectedVertex.getPiece());
        target.getPiece().hasMoved = true; // Lock the piece as moved
        selectedVertex.setPiece(null);
        if (cy != target.getY()) board.getSquare(cx, cy).setPiece(null); 
        
        if (target.getPiece() instanceof Pawn && (target.getY() == 0 || target.getY() == 7)) {
            target.setPiece(new Queen(currentTurn));
            board.moveHistory.peek().wasPawnPromotion = true;
        }

        // Execute Rook movement for castling
        if (isCastling) {
            int rookStartX = (target.getX() == 6) ? 7 : 0;
            int rookEndX = (target.getX() == 6) ? 5 : 3;
            Square rookStart = board.getSquare(rookStartX, target.getY());
            Square rookEnd = board.getSquare(rookEndX, target.getY());
            rookEnd.setPiece(rookStart.getPiece());
            rookStart.setPiece(null);
            rookEnd.getPiece().hasMoved = true;
        }

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        selectedVertex = null;
        currentValidEdges.clear();
        
        state = MoveValidator.evaluateGameState(board, currentTurn);
        updateHistoryText();
        refreshBoardUI();
        
        if (state != GameState.ACTIVE) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Match Concluded: " + state.toString().replace("_", " "), 
                    "Game Over", 
                    JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    private void handleSquareClick(int x, int y) {
        if (state != GameState.ACTIVE) return; 
        Square clickedSquare = board.getSquare(x, y);

        if (selectedVertex != null && currentValidEdges.contains(clickedSquare)) {
            executeMove(clickedSquare);
        } else if (!clickedSquare.isEmpty() && clickedSquare.getPiece().getColor() == currentTurn) {
            selectedVertex = clickedSquare;
            HashSet<Square> pseudoLegalEdges = selectedVertex.getPiece().calculateLegalEdges(board, selectedVertex);
            currentValidEdges.clear();
            for (Square target : pseudoLegalEdges) {
                if (MoveValidator.isMoveStrictlyLegal(board, selectedVertex, target, currentTurn)) {
                    currentValidEdges.add(target);
                }
            }
            refreshBoardUI();
        } else {
            selectedVertex = null;
            currentValidEdges.clear();
            refreshBoardUI();
        }
    }

    private void updateHistoryText() {
        StringBuilder sb = new StringBuilder();
        int turnNum = 1;
        for (int i = 0; i < board.moveHistory.size(); i++) {
            if (i % 2 == 0) sb.append(turnNum++).append(". ");
            MoveRecord m = board.moveHistory.get(i);
            
            // Render proper Castling notation
            if (m.wasCastling) {
                sb.append(m.endX == 6 ? "O-O" : "O-O-O");
            } else {
                sb.append(m.moved.getSymbol()).append(" ");
                sb.append((char)('a' + m.startX)).append(m.startY + 1).append("-");
                sb.append((char)('a' + m.endX)).append(m.endY + 1);
            }
            
            if (i % 2 == 0) sb.append("   ");
            else sb.append("\n");
        }
        historyArea.setText(sb.toString());
        undoBtn.setEnabled(!board.moveHistory.isEmpty());
        redoBtn.setEnabled(!board.redoStack.isEmpty());
    }

    private void refreshBoardUI() {
        boolean kingInCheck = false;
        Square kingSq = null;
        
        Color enemy = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        HashSet<Square> attacked = MoveValidator.getAttackedVertices(board, enemy);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Square s = board.getSquare(x, y);
                if (!s.isEmpty() && s.getPiece() instanceof King && s.getPiece().getColor() == currentTurn) {
                    kingSq = s;
                }
            }
        }
        if (kingSq != null && attacked.contains(kingSq)) {
            kingInCheck = true;
        }

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Square sq = board.getSquare(x, y);
                JButton btn = buttons[x][y];

                if ((x + y) % 2 == 0) btn.setBackground(new java.awt.Color(118, 150, 86)); 
                else btn.setBackground(new java.awt.Color(238, 238, 210)); 

                if (currentValidEdges.contains(sq)) btn.setBackground(new java.awt.Color(186, 202, 68)); 
                if (sq == selectedVertex) btn.setBackground(new java.awt.Color(246, 246, 105)); 
                if (kingInCheck && sq == kingSq) btn.setBackground(new java.awt.Color(220, 50, 50)); 

                if (!sq.isEmpty()) btn.setText(sq.getPiece().getSymbol());
                else btn.setText("");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DMGTChess());
    }
}