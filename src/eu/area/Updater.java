package eu.area;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.exit;

/**
 * Created by Meow on 2018-07-20.
 * Pas le meilleur code au monde, mais ça marche et c'est ce qui compte !
 */
public class Updater extends JFrame implements Observer {
    private JPanel mainPanel;
    private JLabel imageHeader;
    private JButton mainButton;
    public JProgressBar progressBar;
    public boolean isWorking = false;
    private int version = 1;
    private Download telechargement = null;
    private String updaterURL = "http://files.area-serveur.eu/newUpdater/";

    public Updater() {
        readVersion();
    }

    public static void main(String[] args) {
        final Updater frame = new Updater();
        frame.setTitle("Updater Area pour Mac");
        frame.setResizable(false);
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(frame.getClass().getResource("/Dofus_Icon.png")));
        frame.setContentPane(frame.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        frame.mainButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (frame.mainButton.isEnabled()) {
                    try {
                        Runtime.getRuntime().exec(new String[] {"open", "-n", "-a", "Area.app"});
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                frame.start();
                return null;
            }
        }.execute();
    }

    public void start() {
        mainButton.setEnabled(false);
        boolean complete = false;

        if (!checkFile("loader.swf")) {
            int ask = JOptionPane.showConfirmDialog(null, "Dofus ne semble pas être installé, souhaitez-vous l'installer ?", "Oh oh !", JOptionPane.YES_NO_OPTION);
            if (ask == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(null, "Nous allons installer dofus pour vous !");
                URL dofus = checkURL(updaterURL + "mac.zip");
                if (dofus == null) {
                    JOptionPane.showMessageDialog(null, "Une erreur est survenue ! (Err: 01)");
                    exit(0);
                } else {
                    mainButton.setText("Téléchargement de dofus 1.29...");
                    if (download(dofus)) {
                        mainButton.setText("Installation de dofus 1.29...");
                        unzip("mac.zip");
                        if (checkFile("Area.app/Contents/MacOS/Flash Player")) {
                            File app = new File("Area.app/Contents/MacOS/Flash Player");
                            app.setExecutable(true);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Une erreur est survenue ! (Err: 02)");
                        exit(0);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Veuillez installer Dofus manuellement et ensuite relancer l'updater !");
                exit(0);
            }
        }

        mainButton.setText("Vérification des mises à jour...");
        do {
            URL patch = checkURL(updaterURL + version + ".zip");
            if (patch != null) {
                if (version == 1) {
                    mainButton.setText("Téléchargement des fichiers de jeu, ce processus peut prendre quelques minutes...");
                } else {
                    mainButton.setText("Téléchargement de la mise à jour #" + version + "...");
                }
                if (download(patch)) {
                    mainButton.setText("Installation de la mise à jour #" + version + "...");
                    unzip(version + ".zip");
                    if (version == 1) {
                        readVersion(); // Nouveau joueurs, plusieurs majs à télécharger
                    } else {
                        version++;
                    }
                } else {
                    mainButton.setText("Une erreur s'est produite pendant le téléchargement, relancer l'updater");
                    complete = true;
                }
            } else {
                complete = true;
                updateVersion();
                mainButton.setEnabled(true);
                progressBar.setVisible(false);
                mainButton.setText("Lancer le jeu");
            }
        } while (!complete);
    }

    private void createUIComponents() {
        imageHeader = new JLabel(new ImageIcon(getClass().getResource("/launcher-mac.jpg")));
    }

    private boolean checkFile(String url) {
        boolean exist = false;
        File f = new File(url);
        if (f.exists() && !f.isDirectory()) {
            exist = true;
        }
        return exist;
    }

    private URL checkURL(String url) {
        URL u = null;
        try {
            u = new URL(url);
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("GET");
            huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
            huc.connect();
            if (huc.getResponseCode() == 404) {
                u = null;
            }
        } catch (Exception e) {
        }
        return u;
    }

    private boolean download(URL serveur) {
        boolean fileDownloaded = false;
        CountDownLatch latch = new CountDownLatch(1);
        telechargement = new Download(serveur, latch);
        telechargement.addObserver(this);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (telechargement.getStatus() == telechargement.COMPLETE) {
            fileDownloaded = true;
        }
        progressBar.setValue(0);
        return fileDownloaded;
    }

    private void unzip(String fileName) {
        File fichier = null;
        try {
            fichier = new File(fileName);
            new UnZip(fichier, this).start();
            while (isWorking) {
            }
        } catch (Exception e) {
            mainButton.setText("exception");
        }
        if (fichier != null) {
            fichier.delete();
        }
    }

    private void readVersion() {
        try (BufferedReader br = new BufferedReader(new FileReader("version.txt"))) {
            String line = br.readLine();

            if (line != null) {
                version = Integer.valueOf(line);
            }
        } catch (Exception e) {
        }
    }

    private void updateVersion() {
        try (PrintWriter writer = new PrintWriter("version.txt", "UTF-8")) {
            writer.println(version);
            writer.close();
        } catch (Exception e) {

        }
    }

    @Override
    public void update(Observable o, Object arg) {
        progressBar.setValue(telechargement.getProgress());
        if (telechargement.getStatus() != telechargement.DOWNLOADING) {
            isWorking = false;
        }
    }
}
