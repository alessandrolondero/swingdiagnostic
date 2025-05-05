import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;


public class MainWindow extends JFrame {
    private JPanel panel1;
    private JScrollPane scrollPane1;
    private JTextArea textArea; // Area di testo per visualizzare i messaggi

    private JButton PA_PTTButton;
    private JButton D_INButton;
    private JButton CABIN_PTTButton;
    private JButton HANDSET_PTTButton;
    private JButton HANDSET_HOOKButton;

    private JButton HANDSET_MICButton;
    private JButton CABIN_MICButton;
    private JButton SPARE_MICButton;

    private JButton PA_TESTButton;
    private JButton CABIN_TESTButton;
    private JButton SPARE_TESTButton;
    private JButton OUT3_AMP_TESTButton;
    private JButton HANDSET_TESTButton;

    private JButton D_OUT1Button;
    private JButton D_OUT0Button;

    private JButton LAN_TESTButton;
    private JSlider volumeslider;
    private JLabel volumeLabel;
    private JButton clearDisplayButton;
    private JButton HANDSET_BENCH_Button;
    private JButton CABIN_BENCH_Button;
    private JButton IN2OUT2_BENCH_Button;
    private JButton BLINKButton;


    public MainWindow() {

        final boolean[] isBlinkActive = {false};
        final Process[] blinkProcess = {null}; // Array per memorizzare il processo in esecuzione private Process[] processHolder = new Process[1]; // Array per memorizzare il processo in esecuzione

        //Utils.setTheme( "javax.swing.plaf.nimbus.NimbusLookAndFeel", this);
        // Imposta il contenuto della finestra
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(800, 400);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        //autoscroll della textarea
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        setTitle("Diagnostic V1.1");


        volumeslider.setMajorTickSpacing(10);
        volumeslider.setMinorTickSpacing(5);
        volumeslider.setPaintTicks(true);

        PA_TESTButton.setToolTipText("Selezionare la posizione '2' sul commutatore rotativo. L'esito del test è ascoltabile dallo speaker della BCNK.");
        CABIN_TESTButton.setToolTipText("Selezionare la posizione '1' sul commutatore rotativo. L'esito del test è ascoltabile soltanto dallo speaker della cornetta.");
        SPARE_TESTButton.setToolTipText("Selezionare la posizione '3' sul commutatore rotativo. L'esito del test è ascoltabile dallo speaker della BCNK.");
        OUT3_AMP_TESTButton.setToolTipText("Selezionare la posizione '4' sul commutatore rotativo. L'esito del test è ascoltabile dallo speaker della BCNK.");
        HANDSET_TESTButton.setToolTipText("Selezionare la posizione '4' sul commutatore rotativo. L'esito del test è ascoltabile dallo speaker della BCNK e da quello della cornetta.");

        Utils.setDefaultTextArea(textArea);

        // Aggiungi i listener ai pulsanti, ognuno con la propria azione
        PA_PTTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.append("premere e poi rilasciare il pulsante 'PA_PTT' per terminare il test.\n");
                Utils.executeGpioTest("gpiochip3", "24");
            }
        });

        D_INButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.append("premere e poi rilasciare il pulsante 'D_IN' per terminare il test.\n");
                Utils.executeGpioTest("gpiochip3", "26");
            }
        });

        CABIN_PTTButton.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  textArea.append("Premere e poi rilasciare il pulsante 'CABIN_PTT' per terminare il test.\n");
                  Utils.executeGpioTest("gpiochip3", "21");
              }
        });

        HANDSET_PTTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.append("premere e poi rilasciare il pulsante 'HANDSET_PTT' per terminare il test.\n");
                Utils.executeGpioTest("gpiochip3", "28");
            }
        });

        HANDSET_HOOKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.append("premere e poi rilasciare il pulsante 'HANDSET_HOOK' per terminare il test.\n");
                Utils.executeGpioTest("gpiochip3", "23");
            }
        });

        D_OUT1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Esegui il comando
                        String command = "gpioset gpiochip3 30=1";
                        Utils.execProcess(command);

                        // Aggiungi il risultato alla textArea
                        textArea.append("D_OUT=1 (ACCESO)\n");
                    }
                }).start();
            }
        });

        D_OUT0Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Esegui il comando
                        String command = "gpioset gpiochip3 30=0";
                        Utils.execProcess(command);

                        // Aggiungi il risultato alla textArea
                        textArea.append("D_OUT=0 (SPENTO)\n");
                    }
                }).start();
            }
        });
        HANDSET_MICButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Esegui il comando
                        Utils.execProcess("amixer -c 0 set \"INPGAL IN1L\" off");
                        Utils.execProcess("amixer -c 0 set \"INPGAL IN1L\" on");
                        Utils.execProcess("amixer -c 0 set \"INPGAR IN1R\" off");
                        Utils.execProcess( "amixer -c 0 set \"INPGAR IN3R\" on");
                        Utils.execProcess( "amixer -c 0 set \"MIXINR IN3R\" on");
                        textArea.append("Inizio registrazione di 5 secondi dal microfono HANDSET (cornetta)...\n");
                        Utils.execProcess("arecord -f S16_LE -d 5 -r 16000 --device=\"hw:0,0\" /tmp/test-mic.wav");
                        textArea.append("Fine registrazione.\n");
                        Utils.execProcess("gpioset gpiochip4 5=1");
                        textArea.append("Inizio riproduzione file audio registrato...\n");
                        Utils.execProcess("aplay /tmp/test-mic.wav");
                        textArea.append("Fine riproduzione file audio.\n");
                    }
                }).start();
            }
        });

        CABIN_MICButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //settaggio microfoni
                        Utils.execProcess("amixer -c 0 set \"INPGAR IN1R\" off");
                        Utils.execProcess("amixer -c 0 set \"INPGAR IN3R\" off");
                        Utils.execProcess( "amixer -c 0 set \"MIXINR IN3R\" off");
                        Utils.execProcess( "amixer -c 0 set \"INPGAL IN1L\" on");
                        textArea.append("Inizio registrazione di 5 secondi dal microfono CABIN...\n");
                        Utils.execProcess("arecord -f S16_LE -d 5 -r 16000 --device=\"hw:0,0\" /tmp/test-mic.wav");
                        textArea.append("Fine registrazione.\n");
                        Utils.execProcess("gpioset gpiochip4 5=1");
                        textArea.append("Inizio riproduzione file audio registrato...\n");
                        Utils.execProcess("aplay /tmp/test-mic.wav");
                        textArea.append("Fine riproduzione file audio.\n");
                    }
                }).start();
            }
        });

        SPARE_MICButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Esegui il comando
                        Utils.execProcess("amixer -c 0 set \"INPGAR IN3R\" off");
                        Utils.execProcess("amixer -c 0 set \"MIXINR IN3R\"\" off");
                        Utils.execProcess( "amixer -c 0 set \"INPGAL IN1L\" off");
                        Utils.execProcess( "amixer -c 0 set \"INPGAR IN1R\" on");
                        Utils.execProcess( "amixer set \"ADC L/R Swap\" on");
                        textArea.append("Inizio registrazione di 5 secondi dal microfono SPARE...\n");
                        Utils.execProcess("arecord -f S16_LE -d 5 -r 16000 --device=\"hw:0,0\" /tmp/test-mic.wav");
                        textArea.append("Fine registrazione.\n");
                        Utils.execProcess("gpioset gpiochip4 5=0");
                        textArea.append("Inizio riproduzione file audio registrato...\n");
                        Utils.execProcess("aplay /tmp/test-mic.wav");
                        textArea.append("Fine riproduzione file audio.\n");
                        Utils.execProcess( "amixer set \"ADC L/R Swap\" off");
                    }
                }).start();
            }
        });
        LAN_TESTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                    textArea.append("Eseguo test di rete...\n");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Esegui il comando
                            Utils.execProcess("ifconfig");
                            Utils.execProcess("ping 10.0.0.1 -c 6");
                        }
                    }).start();
            }
        });


        PA_TESTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Crea e avvia un thread per eseguire i comandi in sequenza
                textArea.append("\nInizio 'PA_TEST' ...\nSelezionare la posizione '2' sul commutatore rotativo. \nL'esito del test è ascoltabile dallo speaker della BCNK.\n");
                Utils.playSampleAudio();
            }
        });

        CABIN_TESTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Crea e avvia un thread per eseguire i comandi in sequenza
                textArea.append("\nInizio 'CABIN_TEST' ...\nSelezionare la posizione '1' sul commutatore rotativo. \n" +
                        "L'esito del test è ascoltabile soltanto dallo speaker della cornetta.\n");
                Utils.setMuxCtrl(1);
                Utils.playSampleAudio();
            }
        });

        SPARE_TESTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Crea e avvia un thread per eseguire i comandi in sequenza
                textArea.append("\nInizio 'SPARE_TEST' ...\nSelezionare la posizione '3' sul commutatore rotativo.\nL'esito del test è ascoltabile dallo speaker della BCNK.\n");
                Utils.setMuxCtrl(0);
                Utils.playSampleAudio();
            }
        });

        OUT3_AMP_TESTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Crea e avvia un thread per eseguire i comandi in sequenza
                textArea.append("\nInizio 'OUT3_AMP_TEST' ...\nSelezionare la posizione '4' sul commutatore rotativo, ascoltare l'esito del test dallo speaker della BCNK.\n");
                Utils.setMuxCtrl(1);
                Utils.playSampleAudio();
            }
        });

        HANDSET_TESTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Crea e avvia un thread per eseguire i comandi in sequenza
                textArea.append("\nInizio 'HANDSET_TEST' ...\nSelezionare la posizione '4' sul commutatore rotativo," +
                        "\nL'esito del test è ascoltabile dallo speaker della BCNK e da quello della cornetta.\n");
                Utils.setMuxCtrl(1);
                Utils.playSampleAudio();
            }
        });

        clearDisplayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               textArea.setText("");
            }
        });

        // Aggiungi un listener per il rilascio del mouse
        volumeslider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // Quando l'utente rilascia il mouse, esegui il comando amixer con il valore dello slider
                int value = volumeslider.getValue();
                String volume = value + "%";
                String command = "amixer set Headphone " + volume;
                // Esegui il comando amixer
                Utils.execProcess(command);
                volumeLabel.setText("Volume: "+volume);
            }
        });

        HANDSET_BENCH_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.execProcess("/usr/bin/handset_mic_test");
                    }
                }).start();
            }
        });

        CABIN_BENCH_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.execProcess("/usr/bin/cabin_mic_test");
                    }
                }).start();
            }
        });

        IN2OUT2_BENCH_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.execProcess("/usr/bin/in2out2_mic_test");
                    }
                }).start();
            }
        });


        BLINKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Se il blinkProcesso è attivo, fermalo, altrimenti avvialo
                if (!isBlinkActive[0]) {
                    // Attiva il blinkProcesso
                    isBlinkActive[0] = true; // blinkProcesso attivo
                    BLINKButton.setText("DISATTIVA BLINK");
                    BLINKButton.setBackground(Color.LIGHT_GRAY); // Cambia colore del pulsante
                    textArea.append("BLINK ogni secondo...\n");

                    // Usa ProcessBuilder per avviare il processo blink_led
                    ProcessBuilder builder = new ProcessBuilder("/usr/bin/blink_led");
                    builder.redirectErrorStream(true); // Unisci gli errori alla normale uscita
                    Process process = null;
                    try {
                        process = builder.start();
                        blinkProcess[0] = process; // Memorizza il processo per poterlo terminare
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        textArea.append("Errore nell'avvio del processo.\n");
                    }

                } else {
                    // Disattiva il blinkProcesso
                    isBlinkActive[0] = false; // blinkProcesso disattivato
                    BLINKButton.setText("ATTIVA BLINK");
                    BLINKButton.setBackground(null); // Ripristina il colore del pulsante

                    // Verifica se il processo è stato avviato correttamente prima di tentare di fermarlo
                    if (blinkProcess[0] != null && blinkProcess[0].isAlive()) {
                        blinkProcess[0].destroy(); // Termina il processo blink_led
                        textArea.append("Test BLINK terminato.\n");
                    } else {
                        textArea.append("Nessun processo attivo da terminare.\n");
                    }
                }
            }
        });

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainWindow().setVisible(true);
            }
        });
    }


}
