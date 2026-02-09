package Tris;

import java.io.*;
import java.net.*;

public class TrisServer {

    static final int PORT = 12345;

    static char[][] matrice = new char[3][3];
    static final char[] INDICATORI_POSIZIONE = {'a','b','c','d','e','f','g','h','i'};

    static Socket connessione1 = null;
    static Socket connessione2 = null;
    static PrintWriter scrittore1 = null;
    static PrintWriter scrittore2 = null;
    static BufferedReader lettore1 = null;
    static BufferedReader lettore2 = null;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Avviato sulla porta " + PORT);

            System.out.println("[SERVER] In attesa del primo client...");
            connessione1 = serverSocket.accept();
            System.out.println("[SERVER] Primo client connesso.");

            System.out.println("[SERVER] In attesa del secondo client...");
            connessione2 = serverSocket.accept();
            System.out.println("[SERVER] Secondo client connesso.");

            scrittore1 = new PrintWriter(connessione1.getOutputStream(), true);
            lettore1 = new BufferedReader(new InputStreamReader(connessione1.getInputStream()));
            scrittore2 = new PrintWriter(connessione2.getOutputStream(), true);
            lettore2 = new BufferedReader(new InputStreamReader(connessione2.getInputStream()));

            String messaggio1 = lettore1.readLine();
            String messaggio2 = lettore2.readLine();

            if (!"ricerca".equals(messaggio1) || !"ricerca".equals(messaggio2)) {
                System.out.println("[SERVER] Uno dei client non ha inviato 'ricerca'. Chiusura.");
                connessione1.close();
                connessione2.close();
                serverSocket.close();
                return;
            }
            System.out.println("[SERVER] Entrambi i client hanno inviato 'ricerca'. Match trovato!");

            scrittore1.println("X");
            scrittore2.println("O");
            System.out.println("[SERVER] Squadre assegnate: Giocatore1=X, Giocatore2=O");

            inizializzaMatrice();

            boolean turnoGiocatore1 = true;

            while (true) {
                inviaMatrice(scrittore1, turnoGiocatore1);
                inviaMatrice(scrittore2, !turnoGiocatore1);

                BufferedReader lettoroAttivo;
                if (turnoGiocatore1) {
                    lettoroAttivo = lettore1;
                } else {
                    lettoroAttivo = lettore2;
                }

                PrintWriter scrittoreAttivo;
                if (turnoGiocatore1) {
                    scrittoreAttivo = scrittore1;
                } else {
                    scrittoreAttivo = scrittore2;
                }

                char squadraAttiva;
                if (turnoGiocatore1) {
                    squadraAttiva = 'X';
                } else {
                    squadraAttiva = 'O';
                }

                String mossaRicevuta = lettoroAttivo.readLine();
                System.out.println("[SERVER] Mossa ricevuta da " + squadraAttiva + ": " + mossaRicevuta);

                // Controllo formato
                if (mossaRicevuta == null || mossaRicevuta.length() != 1) {
                    System.out.println("[SERVER] Mossa non valida (formato). Rinvio stato.");
                    inviaMatrice(scrittoreAttivo, true); // Rinvia al client attivo la matrice non aggiornata
                    continue;
                }

                char mossa = mossaRicevuta.charAt(0);
                int indicePosizione = mossa - 'a'; // essendo che 'a' vale 97, b vale 98 e così via la sottrazione darà la posizione esatta nella matrice

                // Controllo range
                if (indicePosizione < 0 || indicePosizione > 8) {
                    System.out.println("[SERVER] Mossa fuori range. Rinvio stato.");
                    inviaMatrice(scrittoreAttivo, true); // Rinvia al client attivo la matrice non aggiornata
                    continue;
                }

                // Visto che indicePosizione contiene le coordinate ma il numero della cassella, dovremo ricavarle
                int riga = indicePosizione / 3; 
                int colonna = indicePosizione % 3;

                // Validazione cella libera
                if (matrice[riga][colonna] != INDICATORI_POSIZIONE[indicePosizione]) {
                    System.out.println("[SERVER] Cella già occupata. Rinvio stato.");
                    inviaMatrice(scrittoreAttivo, true); // Rinvia al client attivo la matrice non aggiornata
                    continue;
                }

                // Mossa valida, aggiornamento la matrice
                matrice[riga][colonna] = squadraAttiva;
                System.out.println("[SERVER] Mossa valida. Matrice aggiornata.");
                stampaMatrice();

                int risultato = controllaEsito(squadraAttiva);

                if (risultato != -1) {
                    // Partita finita, invia la matrice finale
                    inviaMatrice(scrittore1, false);
                    inviaMatrice(scrittore2, false);

                    // Invia esito: 1 vittoria, 0 sconfitta, 2 pareggio
                    if (risultato == 2) {
                        scrittore1.println("2");
                        scrittore2.println("2");
                        System.out.println("[SERVER] Pareggio!");
                    } else {
                        if (turnoGiocatore1) {
                            scrittore1.println("1");
                            scrittore2.println("0");
                        } else {
                            scrittore1.println("0");
                            scrittore2.println("1");
                        }
                        System.out.println("[SERVER] Vittoria del giocatore " + squadraAttiva + "!");
                    }
                    break;
                }

                // Cambia turno
                turnoGiocatore1 = !turnoGiocatore1;
            }

            // Chiudi tutto
            scrittore1.close();
            scrittore2.close();
            lettore1.close();
            lettore2.close();
            connessione1.close();
            connessione2.close();
            serverSocket.close();
            System.out.println("[SERVER] Partita terminata. Connessioni chiuse.");

        } catch (IOException e) {
            System.err.println("[SERVER] Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Inizializza la matrice con da a fino a i
    static void inizializzaMatrice() {
        matrice[0][0] = 'a';
        matrice[0][1] = 'b';
        matrice[0][2] = 'c';
        matrice[1][0] = 'd';
        matrice[1][1] = 'e';
        matrice[1][2] = 'f';
        matrice[2][0] = 'g';
        matrice[2][1] = 'h';
        matrice[2][2] = 'i';
    }

    // Invia matrice + turno al client
    // Formato: "BOARD" | 3 righe della matrice | "true"/"false"
    static void inviaMatrice(PrintWriter scrittore, boolean turno) {
        scrittore.println("BOARD");
        for (int i = 0; i < 3; i++) {
            scrittore.println(matrice[i][0] + " " + matrice[i][1] + " " + matrice[i][2]);
        }
        if (turno) {
            scrittore.println("true");
        } else {
            scrittore.println("false");
        }
    }

    // Controlla vittoria/pareggio
    // Ritorna: 1 se squadra ha vinto, 2 se pareggio, -1 se continua
    static int controllaEsito(char squadra) {
        // Righe
        for (int i = 0; i < 3; i++) {
            if (matrice[i][0] == squadra && matrice[i][1] == squadra && matrice[i][2] == squadra)
                return 1;
        }
        // Colonne
        for (int j = 0; j < 3; j++) {
            if (matrice[0][j] == squadra && matrice[1][j] == squadra && matrice[2][j] == squadra)
                return 1;
        }
        // Diagonali
        if (matrice[0][0] == squadra && matrice[1][1] == squadra && matrice[2][2] == squadra)
            return 1;
        if (matrice[0][2] == squadra && matrice[1][1] == squadra && matrice[2][0] == squadra)
            return 1;

        // Pareggio: nessuna cella libera
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (matrice[i][j] != 'X' && matrice[i][j] != 'O')
                    return -1;
            }
        }
        return 2;
    }

    // Stampa matrice sul terminale del server (debug)
    static void stampaMatrice() {
        System.out.println("[SERVER] Stato matrice:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  " + matrice[i][0] + " | " + matrice[i][1] + " | " + matrice[i][2]);
            if (i < 2) System.out.println("  ---------");
        }
    }
}
