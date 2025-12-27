package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.IAuto;
import edu.upc.epsevg.prop.oust.PlayerMove;
import edu.upc.epsevg.prop.oust.PlayerType;
import edu.upc.epsevg.prop.oust.SearchType;
import java.util.ArrayList;

/**
 * Implementació del jugador amb Iterative Deepening Search (IDS).
 * <p>
 * Aquesta classe hereta de {@link PlayerMiniMax} i reutilitza la seva lògica d'Alpha-Beta.
 * Afegeix el control de temps (Timeout) i un bucle iteratiu que incrementa la profunditat
 * progressivament fins que s'esgota el temps disponible.
 * </p>
 * * @author Equip PingPong
 */
public class PlayerMiniMaxIDS extends PlayerMiniMax implements IAuto {

    /**
     * Constructor buit obligatori.
     * <p>
     * Crida al constructor de la classe pare amb profunditat 0, ja que
     * la profunditat serà gestionada dinàmicament pel bucle IDS.
     * </p>
     */
    public PlayerMiniMaxIDS() {
        super(0); 
    }

    /**
     * Retorna el nom del jugador.
     * @return "MiniMaxIDS".
     */
    @Override
    public String getName() {
        return "MiniMaxIDS";
    }

    /**
     * Gestiona el senyal de temps esgotat.
     * <p>
     * A diferència de la classe pare, aquí activem el flag {@code timeoutTriggered}
     * a {@code true}. Com que aquest camp és {@code protected} a la classe pare,
     * l'algorisme {@code alphaBeta} (que estem reutilitzant) detectarà el canvi
     * i aturarà la recursivitat immediatament.
     * </p>
     */
    @Override
    public void timeout() {
        this.timeoutTriggered = true;
    }

    /**
     * Calcula el millor moviment utilitzant IDS.
     * <p>
     * Executa un bucle que comença a profunditat 1 i va incrementant la profunditat.
     * Si el temps s'esgota durant el càlcul d'un nivell, es descarta aquell nivell
     * i es retorna el millor resultat obtingut al nivell anterior completat.
     * </p>
     * * @param status L'estat actual del tauler.
     * @return El millor moviment trobat dins del límit de temps.
     */
    @Override
    public PlayerMove move(GameStatus status) {
        this.timeoutTriggered = false;
        this.nodesExplored = 0;
        int maxDepthReached = 0;
        
        // Convertim a PingPongStatus per a l'heurística
        PingPongStatus myStatus = new PingPongStatus(status);
        PlayerType myColor = myStatus.getCurrentPlayer();
        ScoredPath bestMoveOverall = null;
        
        // --- BUCLE ITERATIVE DEEPENING SEARCH (IDS) ---
        int currentDepth = 1;
        try {
            while (!timeoutTriggered) {
                // REUTILITZACIÓ: Cridem l'alphaBeta heretat de la classe pare (PlayerMiniMax)
                ScoredPath result = alphaBeta(myStatus, currentDepth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, myColor);
                
                // Només acceptem el resultat si s'ha completat tot el nivell sense timeout
                if (!timeoutTriggered) {
                    bestMoveOverall = result;
                    maxDepthReached = currentDepth;
                    
                    // Optimització: Si trobem una victòria segura, no cal seguir cercant
                    if (result.score >= WIN_SCORE) break;
                    
                    currentDepth++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Seguretat: Si el timeout salta instantàniament o hi ha error
        if (bestMoveOverall == null || bestMoveOverall.path.isEmpty()) {
            return new RandomPlayer("Emergency").move(status);
        }

        return new PlayerMove(bestMoveOverall.path, nodesExplored, maxDepthReached, SearchType.MINIMAX);
    }
}