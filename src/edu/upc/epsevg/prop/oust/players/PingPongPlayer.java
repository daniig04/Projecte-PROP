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
 * Jugador PingPong: Implementació d'un agent intel·ligent per al joc Oust.
 * <p>
 * Aquest jugador compleix els dos objectius principals del projecte:
 * <ul>
 * <li><b>Objectiu 1:</b> Permet jugar a profunditat fixa (via constructor).</li>
 * <li><b>Objectiu 2:</b> Implementa Iterative Deepening Search (IDS) amb control de temps (IAuto).</li>
 * </ul>
 * A més, inclou optimitzacions com l'ordenació de moviments (Move Ordering) i 
 * correccions per a la gestió de torns encadenats (captures).
 * </p>
 * * @author Equip PingPong
 */
public class PingPongPlayer implements IPlayer, IAuto {

    private String name;
    
    /**
     * Profunditat fixa de cerca. 
     * Si és 0, s'activa el mode automàtic (IDS). Si és > 0, cerca només a aquesta profunditat.
     */
    private int fixedDepth = 0; 
    
    private boolean timeoutTriggered = false;
    private long nodesExplored = 0;
    private int maxDepthReached = 0;

    // Constants de puntuació
    private static final int WIN_SCORE = 1000000;
    private static final int LOSS_SCORE = -1000000;

    // --- CONSTRUCTORS (PER COMPLIR L'OBJECTIU 1) ---

    /**
     * Constructor per defecte (OBJECTIU 2 - IDS / Automàtic).
     * Crea un jugador que respecta el timeout i ajusta la profunditat dinàmicament.
     */
    public PingPongPlayer() {
        this("PingPongAuto");
        this.fixedDepth = 0; // Mode automàtic
    }

    /**
     * Constructor paramètric (OBJECTIU 1 - Profunditat Fixa).
     * Crea un jugador que explora sempre a la profunditat indicada, ignorant l'estratègia IDS.
     * * @param name Nom del jugador.
     * @param fixedDepth Profunditat fixa a explorar (sense límit de temps estricte per IDS).
     */
    public PingPongPlayer(String name, int fixedDepth) {
        this.name = name;
        this.fixedDepth = fixedDepth;
    }

    /**
     * Constructor auxiliar per al mode automàtic amb nom personalitzat.
     * * @param name Nom del jugador.
     */
    public PingPongPlayer(String name) {
        this.name = name;
        this.fixedDepth = 0; 
    }

    /**
     * Mètode cridat pel framework quan s'esgota el temps de càlcul.
     * Activa el flag per aturar la recursivitat immediatament.
     */
    @Override
    public void timeout() {
        this.timeoutTriggered = true;
    }

    /**
     * Retorna el nom del jugador.
     * Inclou informació sobre si està jugant en mode de profunditat fixa o IDS.
     * * @return El nom del jugador.
     */
    @Override
    public String getName() {
        return name + (fixedDepth > 0 ? " (Depth " + fixedDepth + ")" : " (IDS)");
    }

    /**
     * Decideix el millor moviment possible donat l'estat actual del joc.
     * <p>
     * Gestiona la lògica principal de control:
     * <ul>
     * <li>Si `fixedDepth > 0`, executa Minimax una vegada a la profunditat donada.</li>
     * <li>Si `fixedDepth == 0`, executa Iterative Deepening Search (IDS) incrementant la profunditat fins al timeout.</li>
     * </ul>
     * </p>
     * * @param status L'estat actual del tauler.
     * @return L'objecte PlayerMove amb el camí de moviments i estadístiques de la cerca.
     */
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
                        // Si trobem una victòria segura, parem de cercar per estalviar temps.
                        if (result.score >= WIN_SCORE) break;
                        currentDepth++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Seguretat: Moviment d'emergència si no hem trobat res o el timeout ha saltat immediatament.
        if (bestMoveOverall == null || bestMoveOverall.path.isEmpty()) {
            return new RandomPlayer("Emergency").move(status);
        }

        return new PlayerMove(bestMoveOverall.path, nodesExplored, maxDepthReached, SearchType.MINIMAX);
    }

    /**
     * Algorisme Minimax amb poda Alfa-Beta.
     * Implementa la cerca recursiva per trobar el millor camí.
     * * @param s Estat del joc (GameStatus).
     * @param depth Profunditat restant a explorar.
     * @param alpha Valor Alfa (millor opció per MAX).
     * @param beta Valor Beta (millor opció per MIN).
     * @param myColor El color del nostre jugador (MAX).
     * @return Un objecte ScoredPath amb la puntuació i el camí de moviments.
     */
    private ScoredPath alphaBeta(GameStatus s, int depth, double alpha, double beta, PlayerType myColor) {
        
        // Comprovació de timeout per sortir de la recursivitat
        if (timeoutTriggered) return new ScoredPath(0, new ArrayList<>()); 

        // Node Terminal: Final de partida
        if (s.isGameOver()) {
            nodesExplored++;
            if (s.GetWinner() == myColor) return new ScoredPath(WIN_SCORE + depth, new ArrayList<>());
            else if (s.GetWinner() == null) return new ScoredPath(0, new ArrayList<>());
            else return new ScoredPath(LOSS_SCORE - depth, new ArrayList<>());
        }

        // Conversió a PingPongStatus per utilitzar l'heurística optimitzada
        PingPongStatus myStatus = (s instanceof PingPongStatus) ? (PingPongStatus) s : new PingPongStatus(s);

        // Node Fulla: Límit de profunditat
        if (depth <= 0) {
            nodesExplored++;
            // Cridem l'heurística encapsulada a la nova classe
            return new ScoredPath(myStatus.getHeuristicValue(myColor), new ArrayList<>());
        }

        List<Point> moves = s.getMoves();
        
        // Optimització: Move Ordering
        // Ordenem els moviments per distància al centre per millorar la poda Alfa-Beta.
        int boardSize = s.getSize();
        Point center = new Point(boardSize, boardSize); 
        moves.sort((p1, p2) -> Double.compare(p1.distance(center), p2.distance(center)));

        boolean isMax = (s.getCurrentPlayer() == myColor);
        ScoredPath bestPath = null;

        if (isMax) { // MAX (El nostre torn)
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Point p : moves) {
                // Creem fills de tipus PingPongStatus
                PingPongStatus childStatus = new PingPongStatus(s); 
                childStatus.placeStone(p);
                
                // Gestió de torns encadenats (Captures)
                boolean sameTurn = (childStatus.getCurrentPlayer() == myColor);
                // Si el torn continua, no reduïm profunditat (o mínim 1) per no tallar la seqüència
                int nextDepth = sameTurn ? Math.max(depth, 1) : depth - 1;
                
                ScoredPath eval = alphaBeta(childStatus, nextDepth, alpha, beta, myColor);
                
                if (eval.score > maxEval) {
                    maxEval = eval.score;
                    bestPath = new ScoredPath(maxEval, new ArrayList<>());
                    bestPath.path.add(p); 
                    if (sameTurn) bestPath.path.addAll(eval.path);
                }
                alpha = Math.max(alpha, eval.score);
                if (beta <= alpha) break; // Poda
            }
            return bestPath != null ? bestPath : new ScoredPath(LOSS_SCORE, new ArrayList<>());
            
        } else { // MIN (Torn del rival)
            double minEval = Double.POSITIVE_INFINITY;
            for (Point p : moves) {
                PingPongStatus childStatus = new PingPongStatus(s);
                childStatus.placeStone(p);
                
                boolean sameTurn = (childStatus.getCurrentPlayer() != myColor); 
                int nextDepth = sameTurn ? Math.max(depth, 1) : depth - 1;

                ScoredPath eval = alphaBeta(childStatus, nextDepth, alpha, beta, myColor);
                
                if (eval.score < minEval) {
                    minEval = eval.score;
                    bestPath = new ScoredPath(minEval, new ArrayList<>()); 
                }
                beta = Math.min(beta, eval.score);
                if (beta <= alpha) break; // Poda
            }
            return bestPath != null ? bestPath : new ScoredPath(WIN_SCORE, new ArrayList<>());
        }
    }

    /**
     * Mètode auxiliar d'heurística interna (Llegat).
     * Actualment s'utilitza la versió delegada a PingPongStatus.
     * * @param s Estat del joc.
     * @param myColor Color del jugador.
     * @return Puntuació heurística.
     */
    private double heuristic(GameStatus s, PlayerType myColor) {
        int diff = s.diff(); 
        if (myColor == PlayerType.PLAYER2) diff = -diff;
        double materialScore = diff * 100.0;
        
        int movesAvailable = s.getMoves().size();
        double mobilityScore = (s.getCurrentPlayer() == myColor) ? movesAvailable * 2.0 : -movesAvailable * 2.0;
        
        return materialScore + mobilityScore;
    }

    /**
     * Classe interna per emmagatzemar un camí de punts i la seva puntuació associada.
     * Necessària per retornar múltiples valors des de la recursivitat.
     */
    private class ScoredPath {
        double score;
        List<Point> path;
        public ScoredPath(double score, List<Point> path) { this.score = score; this.path = path; }
    }
}
