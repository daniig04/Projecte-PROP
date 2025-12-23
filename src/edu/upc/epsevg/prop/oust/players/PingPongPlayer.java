package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.IAuto;
import edu.upc.epsevg.prop.oust.IPlayer;
import edu.upc.epsevg.prop.oust.PlayerMove;
import edu.upc.epsevg.prop.oust.PlayerType;
import edu.upc.epsevg.prop.oust.SearchType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Jugador PingPong: Compleix tots els objectius (1 i 2).
 * - Objectiu 1: Permet jugar a profunditat fixa via constructor.
 * - Objectiu 2: Permet jugar amb IDS i control de temps (IAuto).
 * - Optimitzacions: Ordenació de moviments i correccions de regles.
 */
public class PingPongPlayer implements IPlayer, IAuto {

    private String name;
    private int fixedDepth = 0; // Si és 0, fem IDS (Automàtic). Si és >0, profunditat fixa.
    
    private boolean timeoutTriggered = false;
    private long nodesExplored = 0;
    private int maxDepthReached = 0;

    // Constants
    private static final int WIN_SCORE = 1000000;
    private static final int LOSS_SCORE = -1000000;

    // --- CONSTRUCTORS (PER COMPLIR L'OBJECTIU 1) ---

    /**
     * Constructor per a l'OBJECTIU 2 (IDS / Automàtic).
     * Juga respectant el timeout.
     */
    public PingPongPlayer() {
        this("PingPongAuto");
        this.fixedDepth = 0; // Mode automàtic
    }

    /**
     * Constructor per a l'OBJECTIU 1 (Profunditat Fixa).
     * @param name Nom del jugador
     * @param fixedDepth Profunditat fixa a explorar (sense límit de temps estricte per IDS)
     */
    public PingPongPlayer(String name, int fixedDepth) {
        this.name = name;
        this.fixedDepth = fixedDepth;
    }

    // Constructor auxiliar pel mode automàtic amb nom
    public PingPongPlayer(String name) {
        this.name = name;
        this.fixedDepth = 0; 
    }

    @Override
    public void timeout() {
        this.timeoutTriggered = true;
    }

    @Override
    public String getName() {
        return name + (fixedDepth > 0 ? " (Depth " + fixedDepth + ")" : " (IDS)");
    }

    @Override
    public PlayerMove move(GameStatus status) {
        this.timeoutTriggered = false;
        this.nodesExplored = 0;
        this.maxDepthReached = 0;
        
        PlayerType myColor = status.getCurrentPlayer();
        ScoredPath bestMoveOverall = null;
        
        // --- SELECCIÓ DE MODE (FIX vs IDS) ---
        
        if (this.fixedDepth > 0) {
            // MODE OBJECTIU 1: PROFUNDITAT FIXA
            // Executem una sola vegada a la profunditat demanada.
            bestMoveOverall = alphaBeta(status, this.fixedDepth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, myColor);
            maxDepthReached = this.fixedDepth;
            
        } else {
            // MODE OBJECTIU 2: ITERATIVE DEEPENING SEARCH (IDS)
            int currentDepth = 1;
            try {
                while (!timeoutTriggered) {
                    ScoredPath result = alphaBeta(status, currentDepth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, myColor);
                    
                    if (!timeoutTriggered) {
                        bestMoveOverall = result;
                        maxDepthReached = currentDepth;
                        if (result.score >= WIN_SCORE) break;
                        currentDepth++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Seguretat: Moviment d'emergència
        if (bestMoveOverall == null || bestMoveOverall.path.isEmpty()) {
            return new RandomPlayer("Emergency").move(status);
        }

        return new PlayerMove(bestMoveOverall.path, nodesExplored, maxDepthReached, SearchType.MINIMAX);
    }

    private ScoredPath alphaBeta(GameStatus s, int depth, double alpha, double beta, PlayerType myColor) {
        
        // Al mode FixedDepth (Obj 1), normalment volem que acabi el càlcul encara que passi el temps,
        // però si el framework crida timeout(), parem igualment per seguretat.
        if (timeoutTriggered) return new ScoredPath(0, new ArrayList<>()); 

        if (s.isGameOver()) {
            nodesExplored++;
            if (s.GetWinner() == myColor) return new ScoredPath(WIN_SCORE + depth, new ArrayList<>());
            else if (s.GetWinner() == null) return new ScoredPath(0, new ArrayList<>());
            else return new ScoredPath(LOSS_SCORE - depth, new ArrayList<>());
        }

        if (depth <= 0) {
            nodesExplored++;
            return new ScoredPath(heuristic(s, myColor), new ArrayList<>());
        }

        List<Point> moves = s.getMoves();
        
        // Move Ordering (Optimització)
        int boardSize = s.getSize();
        Point center = new Point(boardSize, boardSize); 
        moves.sort((p1, p2) -> Double.compare(p1.distance(center), p2.distance(center)));

        boolean isMax = (s.getCurrentPlayer() == myColor);
        ScoredPath bestPath = null;

        if (isMax) { 
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Point p : moves) {
                GameStatus childStatus = new GameStatus(s);
                childStatus.placeStone(p);
                
                boolean sameTurn = (childStatus.getCurrentPlayer() == myColor);
                int nextDepth = sameTurn ? Math.max(depth, 1) : depth - 1;
                
                ScoredPath eval = alphaBeta(childStatus, nextDepth, alpha, beta, myColor);
                
                if (eval.score > maxEval) {
                    maxEval = eval.score;
                    bestPath = new ScoredPath(maxEval, new ArrayList<>());
                    bestPath.path.add(p); 
                    if (sameTurn) bestPath.path.addAll(eval.path);
                }
                alpha = Math.max(alpha, eval.score);
                if (beta <= alpha) break; 
            }
            return bestPath != null ? bestPath : new ScoredPath(LOSS_SCORE, new ArrayList<>());
        } else { // MIN
            double minEval = Double.POSITIVE_INFINITY;
            for (Point p : moves) {
                GameStatus childStatus = new GameStatus(s);
                childStatus.placeStone(p);
                
                boolean sameTurn = (childStatus.getCurrentPlayer() != myColor); 
                int nextDepth = sameTurn ? Math.max(depth, 1) : depth - 1;

                ScoredPath eval = alphaBeta(childStatus, nextDepth, alpha, beta, myColor);
                
                if (eval.score < minEval) {
                    minEval = eval.score;
                    bestPath = new ScoredPath(minEval, new ArrayList<>()); 
                }
                beta = Math.min(beta, eval.score);
                if (beta <= alpha) break; 
            }
            return bestPath != null ? bestPath : new ScoredPath(WIN_SCORE, new ArrayList<>());
        }
    }

    private double heuristic(GameStatus s, PlayerType myColor) {
        int diff = s.diff(); 
        if (myColor == PlayerType.PLAYER2) diff = -diff;
        double materialScore = diff * 100.0;
        
        int movesAvailable = s.getMoves().size();
        double mobilityScore = (s.getCurrentPlayer() == myColor) ? movesAvailable * 2.0 : -movesAvailable * 2.0;
        
        return materialScore + mobilityScore;
    }

    private class ScoredPath {
        double score;
        List<Point> path;
        public ScoredPath(double score, List<Point> path) { this.score = score; this.path = path; }
    }
}