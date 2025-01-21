import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utils {

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

        public static void execProcess(String command, JTextArea textArea) {
            try {
                // Creazione del processo con il comando
                ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Lettura dello standard output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        textArea.append(line+"\n"); // Stampa l'output del comando
                    }
                }

                // Aspetta il termine del processo
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        //aggiungere controllo che il file sample sia presente
        static public void playSampleAudio(JTextArea textArea){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        textArea.append("Avvio riproduzione audio...\n");
                        String[] aplayCommand = {"aplay", "/home/root/Audio/Front_Center.wav"};
                        Process aplayProcess = new ProcessBuilder(aplayCommand).start();
                        aplayProcess.waitFor(); // Aspetta che aplay termini
                        textArea.append("Comando aplay completato.\n");

                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace();
                        textArea.append("Si è verificato un errore durante l'esecuzione dei comandi.\n");

                    }
                }
            }).start();
        }

        static public void setMuxCtrl(int value, JTextArea textArea){

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String gpioValue= "5="+value;
                        String[] gpioCommand = {"gpioset", "gpiochip4", gpioValue}; // Imposta AUDIO_MUX_CTRL=value
                        Process gpioSetProcess = new ProcessBuilder(gpioCommand).start();
                        textArea.append("AUDIO_MUX_CTRL=" + value+ "\n");
                        gpioSetProcess.waitFor(); // Aspetta che gpioSet termini

                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace();
                        textArea.append("Si è verificato un errore durante l'esecuzione dei comandi.\n");

                    }
                }
            }).start();
        }



        /**
         * Esegue un test su una specifica linea GPIO utilizzando gpiomon e aggiorna la textArea in tempo reale.
         *
         * @param gpioChip Il nome del GPIO chip (es: "gpiochip3")
         * @param line     La linea GPIO da monitorare (es: "21")
         * @param textArea La textArea dove verrà scritto l'output
         */
        public static void executeGpioTest(String gpioChip, String line, JTextArea textArea) {
            new Thread(() -> {
                try {
                    // Comando da eseguire
                    String[] command = {"gpiomon", "-f", "-n", "1", gpioChip, line};
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    Process process = processBuilder.start();

                    // Leggi l'output del processo in tempo reale
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String outputLine;

                    while ((outputLine = reader.readLine()) != null) {
                        final String lineToAppend = outputLine;

                        // Aggiorna la textArea sull'EDT (Event Dispatch Thread)
                        SwingUtilities.invokeLater(() -> textArea.append(lineToAppend + "\n"));
                    }

                    // Aspetta che il processo termini
                    process.waitFor();
                    SwingUtilities.invokeLater(() -> textArea.append("Test terminato.\n"));

                } catch (IOException | InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> textArea.append("Errore durante l'esecuzione del test.\n"));
                    ex.printStackTrace();
                }
            }).start();
        }



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


