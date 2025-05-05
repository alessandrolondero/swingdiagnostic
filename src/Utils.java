import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utils {

        //da valutare se eliminare e sostituire definitivamente con quella successiva (execProcess)
        /*
        static public String executeProcess(String[] command) {
            String res = "";
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    res += line;
                    res += "\n";
                }

                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            return res;
        }
        */

        private static JTextArea defaultTextArea = null;

        public static void setDefaultTextArea(JTextArea textArea) {
            defaultTextArea = textArea;
        }


    //da valutare se migliorare e come
        //magari accorpare le due funzioni in una sola e aggiungere il parametro outToTextArea(boolean) per stabilire dove l'output deve essere indirizzato
        public static void execProcess(String command) {
            try {
                JTextArea outputArea = defaultTextArea;
                // Creazione del processo con il comando
                ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Lettura dello standard output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputArea.append(line+"\n"); // Stampa l'output del comando
                    }
                }

                // Aspetta il termine del processo
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    /**
     * Riproduce un file audio WAV specifico (/home/root/Audio/Front_Center.wav)
     * utilizzando il comando 'aplay' in un thread separato.
     *
     * Prima di avviare la riproduzione, verifica che il file esista.
     * L'output informativo e gli eventuali errori vengono scritti sulla
     * JTextArea predefinita 'defaultTextArea'.
     *
     */
        static public void playSampleAudio() {
            JTextArea outputArea = defaultTextArea;

            new Thread(() -> {
                File sampleFile = new File("/home/root/Audio/Front_Center.wav");

                if (!sampleFile.exists()) {
                    SwingUtilities.invokeLater(() ->
                            outputArea.append("Errore: Il file audio non esiste: " + sampleFile.getAbsolutePath() + "\n")
                    );
                    return;
                }

                try {
                    SwingUtilities.invokeLater(() ->
                            outputArea.append("Avvio riproduzione audio...\n")
                    );

                    String[] aplayCommand = {"aplay", sampleFile.getAbsolutePath()};
                    Process aplayProcess = new ProcessBuilder(aplayCommand).start();
                    aplayProcess.waitFor(); // Aspetta che aplay termini

                    SwingUtilities.invokeLater(() ->
                            outputArea.append("Comando aplay completato.\n")
                    );

                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            outputArea.append("Si è verificato un errore durante l'esecuzione dei comandi.\n")
                    );
                }
            }).start();
        }

        //da migliorare
        static public void setMuxCtrl(int value){

            JTextArea outputArea = defaultTextArea;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String gpioValue= "5="+value;
                        String[] gpioCommand = {"gpioset", "gpiochip4", gpioValue}; // Imposta AUDIO_MUX_CTRL=value
                        Process gpioSetProcess = new ProcessBuilder(gpioCommand).start();
                        outputArea.append("AUDIO_MUX_CTRL=" + value+ "\n");           //c'è un delay su questo append rispetto al resto delle operazioni
                        gpioSetProcess.waitFor(); // Aspetta che gpioSet termini

                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace();
                        outputArea.append("Si è verificato un errore durante l'esecuzione dei comandi.\n");

                    }
                }
            }).start();
        }



        /**
         * Esegue un test su una specifica linea GPIO utilizzando gpiomon e aggiorna la textArea in tempo reale.
         *
         * @param gpioChip Il nome del GPIO chip (es: "gpiochip3")
         * @param line     La linea GPIO da monitorare (es: "21")
         */
        //buona, da valutare se migliorabile
        public static void executeGpioTest(String gpioChip, String line) {

            JTextArea outputArea = defaultTextArea;

            new Thread(() -> {
                Process process = null;
                BufferedReader reader = null;
                try {
                    // Comando da eseguire
                    String[] command = {"gpiomon", "-f", "-n", "1", gpioChip, line};
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    process = processBuilder.start();

                    // Leggi l'output del processo in tempo reale
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String outputLine;

                    while ((outputLine = reader.readLine()) != null) {
                        final String lineToAppend = outputLine;

                        // Aggiorna la textArea sull'EDT (Event Dispatch Thread)
                        SwingUtilities.invokeLater(() -> outputArea.append(lineToAppend + "\n"));
                    }

                    // Aspetta che il processo termini
                    process.waitFor();
                    SwingUtilities.invokeLater(() -> outputArea.append("Test terminato.\n"));

                } catch (IOException | InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> outputArea.append("Errore durante l'esecuzione del test.\n"));
                    ex.printStackTrace();
                } finally {
                    // Chiudere le risorse
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (process != null && process.isAlive()) {
                        process.destroy();
                    }
                }
            }).start();
        }


    //mai utilizzata, si usa quello di default.
    static void setTheme(String name, MainWindow frame ) {
        try {
            // Imposta il tema Nimbus
            UIManager.setLookAndFeel(name);
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    }


