package Tris;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TrisClient {

    static final String INDIRIZZO_SERVER = "127.0.0.1";
    static final int PORT = 12345;
    static final int TENTATIVI_MASSIMI = 3;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Socket connessione = null;

        // Tentativo di connessione con retry esponenziale
        for (int i = 0; i < TENTATIVI_MASSIMI && connessione == null; i++) {
            try {
                connessione = new Socket(INDIRIZZO_SERVER, PORT);
            } catch (IOException e) {
                System.out.println("Connessione fallita. Riprovando tra " + (int)(10 * Math.pow(2, i)) + " secondi...");
                try { Thread.sleep((long)(1000 * Math.pow(2, i))); } catch (InterruptedException ex) {}
            }
        }

        if (connessione == null) {
            System.out.println("Impossibile collegarsi al server. Uscita.");
            scanner.close();
            return;
        }

        try {
            PrintWriter scrittore = new PrintWriter(connessione.getOutputStream(), true);
            BufferedReader lettore = new BufferedReader(new InputStreamReader(connessione.getInputStream()));

            // Menu iniziale
            System.out.println("=============================");
            System.out.println("         GIOCO DEL TRIS      ");
            System.out.println("=============================");
            System.out.println("1. Gioca");
            System.out.println("2. Esci");
            System.out.print("Scelta: ");
            String scelta = scanner.nextLine().trim();

            if (!"1".equals(scelta)) {
                System.out.println("Arrivederci!");
                scrittore.close();
                lettore.close();
                connessione.close();
                scanner.close();
                return;
            }

            // Invia "ricerca" al server
            scrittore.println("ricerca");
            System.out.println("Matchmaking in corso...");

            // Ricevi la squadra assegnata
            String squadraRicevuta = lettore.readLine();
            if (squadraRicevuta == null || squadraRicevuta.isEmpty()) {
                System.out.println("Errore: squadra non ricevuta. Uscita.");
                connessione.close();
                scanner.close();
                return;
            }
            char squadra = squadraRicevuta.charAt(0);
            System.out.println("Matchmaking trovato!");
            System.out.println("Sei il giocatore: " + squadra);
            System.out.println();

            // Ciclo di gioco
            while (true) {
                // Leggi il primo messaggio: "BOARD" oppure esito finale
                String etichetta = lettore.readLine();

                if (etichetta == null || !"BOARD".equals(etichetta)) {
                    // Non è BOARD -> è l'esito della partita
                    gestisciEsito(etichetta);
                    break;
                }

                // Leggi le 3 righe della matrice
                char[][] matrice = new char[3][3];
                for (int i = 0; i < 3; i++) {
                    String riga = lettore.readLine();
                    if (riga == null || riga.isEmpty()) {
                        System.out.println("Errore: stato ricevuto non valido. Uscita.");
                        break;
                    }
                    String[] celle = riga.split(" ");
                    for (int j = 0; j < 3; j++) {
                        matrice[i][j] = celle[j].charAt(0);
                    }
                }

                // Leggi il turno
                String turnoRicevuto = lettore.readLine();
                boolean mioTurno = "true".equals(turnoRicevuto);

                // Visualizza la matrice
                System.out.println();
                stampaMatrice(matrice);

                if (!mioTurno) {
                    System.out.println("Aspettando l'avversario...");
                    continue; // Salta tutto il codice che sotto nel ciclo while e torna subito all'inizio di esso.
                }

                // È il mio turno
                System.out.print("Inserisci la tua mossa (a-i): ");
                String mossa = scanner.nextLine().trim().toLowerCase(); // Testo in miniscolo e senza spazi

                // Invia la mossa al server
                scrittore.println(mossa);
                System.out.println("Mossa inviata: " + mossa);
            }

        } catch (IOException e) {
            System.err.println("Errore di comunicazione: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { connessione.close(); } catch (IOException e) {}
            scanner.close();
        }
    }

    // Gestisce l'esito finale
    static void gestisciEsito(String risultato) { // Essendo static non appartiene ad un oggetto e non ritorna nulla, quello che fa è stampare una linea
        System.out.println();
        System.out.println("=============================");
        if ("1".equals(risultato)) {
            System.out.println("  CONGRATULAZIONI! HAI VINTO!");
        } else if ("0".equals(risultato)) {
            System.out.println("  PECCATO... HAI PERSO.");
        } else if ("2".equals(risultato)) {
            System.out.println("  PAREGGIO!");
        } else {
            System.out.println("  Esito non riconosciuto: " + risultato);
        }
        System.out.println("=============================");
    }

    // Stampa la matrice del Tris
    static void stampaMatrice(char[][] matrice) {
        System.out.println(" " + matrice[0][0] + " | " + matrice[0][1] + " | " + matrice[0][2]);
        System.out.println("-----------");
        System.out.println(" " + matrice[1][0] + " | " + matrice[1][1] + " | " + matrice[1][2]);
        System.out.println("-----------");
        System.out.println(" " + matrice[2][0] + " | " + matrice[2][1] + " | " + matrice[2][2]);
    }
}