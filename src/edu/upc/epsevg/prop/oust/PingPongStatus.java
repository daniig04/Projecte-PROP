package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.PlayerType;
import java.awt.Point;

/**
 * Subclasse optimitzada de GameStatus per a la gestió d'estats i heurístiques.
 * <p>
 * Aquesta classe afegeix funcionalitats específiques per a l'avaluació del tauler,
 * incloent la pre-inicialització de pesos posicionals i una funció d'avaluació
 * personalitzada que combina material i mobilitat.
 * </p>
 * * @author Equip PingPong
 */
public class PingPongStatus extends GameStatus {

    /**
     * Matriu estàtica per guardar els pesos de cada casella.
     * Les caselles centrals tenen més valor que les perifèriques.
     */
    private static int[][] centerWeights;

    /**
     * Flag per indicar si la matriu de pesos ja ha estat inicialitzada.
     * Evita recàlculs innecessaris a cada instanciació.
     */
    private static boolean weightsInitialized = false;

    /**
     * Constructor que crea una còpia de l'estat actual.
     * Necessari per a la generació de l'arbre de cerca Minimax.
     * * @param gs L'estat del joc (GameStatus) original a copiar.
     */
    public PingPongStatus(GameStatus gs) {
        super(gs);
        initializeWeightsIfNecessary(gs.getSize());
    }

    /**
     * Inicialitza la matriu de pesos estàtica si encara no existeix.
     * <p>
     * Calcula la distància de cada casella al centre del tauler i assigna
     * una puntuació inversament proporcional a aquesta distància (més a prop = més punts).
     * </p>
     * * @param n La mida (radi) del tauler hexagonal.
     */
    private static void initializeWeightsIfNecessary(int n) {
        if (weightsInitialized) return;

        int dim = n * 2 + 1; // Mida màxima aproximada de la matriu interna
        centerWeights = new int[dim][dim];
        Point center = new Point(n, n); // Punt central aproximat en coordenades hex

        // Recorrem una matriu imaginària per pre-calcular distàncies
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                double dist = new Point(i, j).distance(center);
                // Com més a prop, més valor (Invertim la distància)
                // 100 punts al centre, 0 als marges
                int weight = (int) Math.max(0, 100 - (dist * 15));
                centerWeights[i][j] = weight;
            }
        }
        weightsInitialized = true;
    }

    /**
     * Funció d'avaluació heurística optimitzada.
     * <p>
     * Avalua com de favorable és l'estat actual per al jugador especificat.
     * Es basa en dos criteris principals:
     * <ul>
     * <li><b>Material:</b> Diferència de peces entre el jugador i el rival (Prioritat molt alta).</li>
     * <li><b>Mobilitat:</b> Nombre de moviments disponibles (Prioritat mitjana/tàctica).</li>
     * </ul>
     * </p>
     * * @param myColor El color del jugador que està avaluant el tauler.
     * @return Un valor double que representa la puntuació de l'estat (valors positius favorables a myColor).
     */
    public double getHeuristicValue(PlayerType myColor) {
        // 1. MATERIAL (Diferència de peces)
        int diff = this.diff(); 
        if (myColor == PlayerType.PLAYER2) diff = -diff;
        double materialScore = diff * 1000.0; // Pesa molt

        // 2. MOBILITAT
        // Nota: getMoves() és costós, però necessari per evitar quedar ofegat.
        int movesAvailable = this.getMoves().size();
        double mobilityScore = (this.getCurrentPlayer() == myColor) ? movesAvailable * 10.0 : -movesAvailable * 10.0;

        // 3. CONTROL POSICIONAL (NOU I RÀPID)
        // Actualment confiem en Material + Mobilitat + Move Ordering del Player.
        // Si s'habilités l'accés als pesos posicionals, se sumarien aquí.
        
        return materialScore + mobilityScore;
    }
}