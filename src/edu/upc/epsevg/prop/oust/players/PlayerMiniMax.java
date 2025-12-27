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
 * Implementació del jugador Minimax amb profunditat limitada (fixa).
 * <p>
 * Aquesta classe implementa l'algorisme Minimax amb poda Alfa-Beta i Ordenació de Moviments (Move Ordering).
 * Està dissenyada per explorar l'arbre de joc fins a una profunditat fixa especificada al constructor,
 * ignorant les restriccions de temps.
 * </p>
 * <p>
 * Implementa la interfície {@link IAuto} perquè el tauler (Game.java) executi el moviment automàticament,
 * tot i que el mètode {@link #timeout()} es deixa buit intencionadament en aquesta versió base.
 * </p>
 * Utilitza {@link PingPongStatus} per a l'avaluació heurística.
 * * @author Equip PingPong
 */
public class PlayerMiniMax implements IPlayer, IAuto {

    /** Profunditat fixa de cerca. */
    protected int fixedDepth;
    
    /** Nom del jugador per a la interfície. */
    protected String name = "MiniMax";
    
    /** * Flag per indicar si s'ha esgotat el temps. 
     * Protegit perquè la classe filla (PlayerMiniMaxIDS) el pugui activar.
     */
    protected boolean timeoutTriggered = false;
    
    /** Comptador de nodes explorats durant la cerca. */
    protected long nodesExplored = 0;
    
    // Constants de puntuació per victòria i derrota
    protected static final int WIN_SCORE = 1000000;
    protected static final int LOSS_SCORE = -1000000;

    /**
     * Constructor obligatori.
     * Inicialitza el jugador amb una profunditat de cerca fixa.
     * * @param profunditatMaxima Nombre de nivells a explorar a l'arbre de joc.
     */
    public PlayerMiniMax(int profunditatMaxima) {
        this.fixedDepth = profunditatMaxima;
    }

    /**
     * Notificació de temps esgotat.
     * <p>
     * En aquesta implementació (PlayerMiniMax), aquest mètode <b>no fa res</b>.
     * L'estratègia de profunditat fixa prioritza acabar el càlcul del nivell demanat
     * encara que s'excedeixi el temps recomanat pel framework.
     * </p>
     */
    @Override
    public void timeout() {
        // IMPORTANT: Està buit expressament.
        // Com que és la versió de profunditat fixa, ignorem el senyal de timeout.
    }

    /**
     * Retorna el nom del jugador.
     * @return Una cadena amb el nom i la profunditat configurada.
     */
    @Override
    public String getName() {
        return "MiniMax(" + fixedDepth + ")";
    }

    /**
     * Calcula el millor moviment per a l'estat actual del joc.
     * Executa l'algorisme Minimax a la profunditat fixa configurada.
     * * @param status L'estat actual del tauler (GameStatus).
     * @return El moviment triat (PlayerMove) amb estadístiques de la cerca.
     */
    @Override
    public PlayerMove move(GameStatus status) {
        // Resetegem variables
        timeoutTriggered = false;
        nodesExplored = 0;
        
        // 1. Convertim l'estat genèric a PingPongStatus per tenir accés a l'heurística optimitzada
        PingPongStatus myStatus = new PingPongStatus(status);
        PlayerType myColor = myStatus.getCurrentPlayer();
        
        // 2. Cridem l'algorisme a la profunditat fixada
        ScoredPath best = alphaBeta(myStatus, fixedDepth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, myColor);
        
        // 3. Gestió d'errors (si no troba res, moviment aleatori d'emergència)
        if (best == null || best.path.isEmpty()) {
            return new RandomPlayer("Emergency").move(status);
        }
        
        return new PlayerMove(best.path, nodesExplored, fixedDepth, SearchType.MINIMAX);
    }

    /**
     * Algorisme Minimax amb poda Alfa-Beta (Protegit).
     * <p>
     * Aquest mètode és {@code protected} per permetre que la classe filla {@link PlayerMiniMaxIDS}
     * el reutilitzi dins del seu bucle iteratiu.
     * </p>
     * Inclou lògica de:
     * <ul>
     * <li><b>Poda Alfa-Beta:</b> Per descartar branques inútils.</li>
     * <li><b>Move Ordering:</b> Ordena els moviments pel centre per millorar l'eficiència de la poda.</li>
     * <li><b>Gestió de Captures:</b> No redueix la profunditat si el torn continua (captures encadenades).</li>
     * </ul>
     * * @param s Estat del joc (PingPongStatus) amb accés a l'heurística.
     * @param depth Profunditat restant a explorar.
     * @param alpha Valor Alfa (millor opció per MAX).
     * @param beta Valor Beta (millor opció per MIN).
     * @param myColor Color del nostre jugador.
     * @return Un objecte {@link ScoredPath} amb la puntuació i el camí de moviments.
     */
    protected ScoredPath alphaBeta(PingPongStatus s, int depth, double alpha, double beta, PlayerType myColor) {
        
        // Comprovació de timeout (utilitzat principalment pel fill IDS). 
        if (timeoutTriggered) return new ScoredPath(0, new ArrayList<>()); 

        // Cas base: Final de joc
        if (s.isGameOver()) {
            nodesExplored++;
            if (s.GetWinner() == myColor) return new ScoredPath(WIN_SCORE + depth, new ArrayList<>());
            else if (s.GetWinner() == null) return new ScoredPath(0, new ArrayList<>());
            else return new ScoredPath(LOSS_SCORE - depth, new ArrayList<>());
        }

        // Cas base: Profunditat límit assolida
        if (depth <= 0) {
            nodesExplored++;
            return new ScoredPath(s.getHeuristicValue(myColor), new ArrayList<>());
        }

        List<Point> moves = s.getMoves();
        
        // Optimització: Ordenació de moviments (Prioritzem el centre del tauler)
        Point center = new Point(s.getSize(), s.getSize());
        moves.sort((p1, p2) -> Double.compare(p1.distance(center), p2.distance(center)));

        boolean isMax = (s.getCurrentPlayer() == myColor);
        ScoredPath bestPath = null;

        if (isMax) { // Node MAX (El nostre torn)
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Point p : moves) {
                // IMPORTANT: Creem fills de tipus PingPongStatus
                PingPongStatus child = new PingPongStatus(s);
                child.placeStone(p);
                
                // Lògica de torn continu (Captura): Si capturem, tornem a tirar sense gastar profunditat
                boolean sameTurn = (child.getCurrentPlayer() == myColor);
                int nextDepth = sameTurn ? Math.max(depth, 1) : depth - 1;

                ScoredPath eval = alphaBeta(child, nextDepth, alpha, beta, myColor);
                
                if (eval.score > maxEval) {
                    maxEval = eval.score;
                    bestPath = new ScoredPath(maxEval, new ArrayList<>());
                    bestPath.path.add(p);
                    if (sameTurn) bestPath.path.addAll(eval.path);
                }
                alpha = Math.max(alpha, eval.score);
                if (beta <= alpha) break; // Poda Beta
            }
            return bestPath != null ? bestPath : new ScoredPath(LOSS_SCORE, new ArrayList<>());
            
        } else { // Node MIN (Torn del rival)
            double minEval = Double.POSITIVE_INFINITY;
            for (Point p : moves) {
                PingPongStatus child = new PingPongStatus(s);
                child.placeStone(p);
                
                boolean sameTurn = (child.getCurrentPlayer() != myColor);
                int nextDepth = sameTurn ? Math.max(depth, 1) : depth - 1;

                ScoredPath eval = alphaBeta(child, nextDepth, alpha, beta, myColor);
                
                if (eval.score < minEval) {
                    minEval = eval.score;
                    bestPath = new ScoredPath(minEval, new ArrayList<>());
                }
                beta = Math.min(beta, eval.score);
                if (beta <= alpha) break; // Poda Alfa
            }
            return bestPath != null ? bestPath : new ScoredPath(WIN_SCORE, new ArrayList<>());
        }
    }

    /**
     * Classe auxiliar interna per emmagatzemar la puntuació i el camí associat.
     */
    protected class ScoredPath {
        double score;
        List<Point> path;
        public ScoredPath(double score, List<Point> path) { this.score = score; this.path = path; }
    }
}