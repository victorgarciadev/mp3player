/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package prac1.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.util.Callback;
import javax.sound.sampled.UnsupportedAudioFileException;
import prac1.exceptions.NoDurationException;
import prac1.exceptions.DuplicatedItemException;
import prac1.Model.Song;
import prac1.main.SongListViewCell;

/**
 * FXML Controller class
 *
 * @author GrupD
 * @author Txell Llanas
 * @author Izan Jimenez
 * @author Victor García
 * @author Pablo Morante
 */
public class MainScreenController implements Initializable {

    private final FileChooser fileChooser = new FileChooser();                  // Obrir fitxers
    private final long MAX_FILE_SIZE = (20480L * 1024L);                        // 20.971.520 Bytes = 20MB 
    private Song song = null;                                                   // Variable per reproduir

    //classes per reproduir media(mp3 en aquest cas)
    private Media media;
    private MediaPlayer mediaPlayer;

    private int songNumber = 0;                                                 // Índex numèric de la llista de cançons

    //control del temps de la cançó
    private Timer timer;
    private TimerTask task;
    private boolean running;

    private boolean random = false;

    @FXML
    private Text actualTime;

    @FXML
    private Text songTime;

    @FXML
    private Slider sliderBar;

    @FXML
    private ListView<Song> listView;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Label TempsTotalLlista;

    private final ObservableList<Song> songObservableList = FXCollections.observableArrayList();

    /**
     * Inicialitza el controlador
     *
     * @param url The location used to resolve relative paths for the root
     * object, or null if the location is not known.
     * @param rb The resources used to localize the root object, or null if the
     * root object was not localized.
     *
     * @author GrupD
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        openBtn.setTooltip(openTooltip);
        playBtn.setTooltip(playTooltip);
        stopBtn.setTooltip(stopTooltip);
        btnNextSong.setTooltip(nextTooltip);
        btnPrevSong.setTooltip(prevTooltip);
        fwdBtn.setTooltip(fwdTooltip);
        rwdBtn.setTooltip(rwdTooltip);
        randomSong.setTooltip(randomTooltip);
        FileChooser.ExtensionFilter extension;                                  // Filtre: Limitar tipus d'arxiu a MP3
        extension = new FileChooser.ExtensionFilter("MP3-File", "*.mp3");
        fileChooser.getExtensionFilters().add(extension);

        Label placeholder = new Label("Afegeix una cançó.");                    // Especifico un texte d'ajuda per quan el llistat està buit
        listView.setPlaceholder(placeholder);

        listView.setItems(songObservableList);                                  // Actualitzo el llistat amb els elements disponibles (openFile())             
        listView.setCellFactory(new Callback<ListView<Song>, ListCell<Song>>() {// Carrego un layout a cada fila del llistat, on carregar-hi les dades de la cançó afegida
            @Override
            public ListCell<Song> call(ListView<Song> songListView) {
                return new SongListViewCell();
            }
        });
        listView.setPrefHeight(Screen.getPrimary().getBounds().getHeight());    // Fem la llista de cancons adaptabele al monitor de la pantalla
        sliderBar.setPrefWidth(Screen.getPrimary().getBounds().getHeight());

        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {

                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(volumeSlider.getValue() / 100);
                }
            }
        });

        currentSongTitle.maxWidth(currentSongTitle.getParent().getScaleX());
        currentSongTitle.prefWidth(Screen.getPrimary().getBounds().getHeight());
        
        playBtn.getStyleClass().add("play");                                    // Assigno classe CSS per mostrar icona PLAY
        randomSong.getStyleClass().add("random");                               // Assigno classe CSS per mostrar icona RANDOM

    }

    @FXML
    private Button openBtn;
    Tooltip openTooltip = new Tooltip("Carregar cançó (Màx. 20MB)");

    /**
     * (RF01): Permet seleccionar un arxiu *.mp3 dins el Sistema Operatiu i el
     * llista dins una 'listView' connectada a una 'ObservableList'.
     *
     * @author Txell Llanas
     */
    @FXML
    private void openFile() throws UnsupportedAudioFileException, NoDurationException {

        try {

            File file = fileChooser.showOpenDialog(null);                       // Obrir 'Dialog' per seleccionar l'arxiu d'àudio
            song = new Song(file);                                              // Crear nou objecte de tipus cançó
            song.setPath(file);
            listView.refresh();
            if (file.canRead()) {                                               //  Filtre 1: Si l'arxiu existeix i té permissos de lectura...

                fileChooser.setInitialDirectory(file.getParentFile());          // Si s'ha obert un arxiu prèviament, recorda/obre l'últim directori visitat

                String index = String.format("%02d", listView.getItems().size()); // Genero un índex de 2 dígits per a cada element a llistar
                song.setIndex(index);

                long fileSize = file.length();
                if (fileSize <= MAX_FILE_SIZE) {                                // Filtre 2: limitar pes arxiu (MAX_FILE_SIZE)

                    if (!song.getDuration().equals("null")) {                   // Filtre 3: Si l'arxiu té una duració major a 00:00, afegir-la al llistat                       

                        if (!comprovarTitol(songObservableList, song.getTitle())) {

                            songObservableList.add(song);
                            listView.setItems(songObservableList);
                            String totalDuration = getPlaylistDuration(songObservableList);
                            TempsTotalLlista.setText("Temps total: " + totalDuration);
                        } else {
                            throw new DuplicatedItemException("Són elements duplicats!");
                        }

                    } else {
                        throw new NoDurationException("Arxiu sense duració!");  // Error personalitzat per indicar que l'arxiu no té duració i no es pot reproduir
                    }

                } else {
                    throw new RuntimeException("Arxiu massa gran!");            // Error personalitzat per limitar tamany d'arxiu (MAX_FILE_SIZE)
                }

            }

        } catch (NullPointerException e) {                                      // Mostra un AVÍS si no se selecciona cap fitxer

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Missatge informatiu");
            alert.setHeaderText("No es pot carregar cap cançó");
            alert.setContentText("No s'ha seleccionat cap arxiu.");
            alert.show();
            System.out.println("S'ha clicat CANCEL·LAR. No s'ha obert cap arxiu");

        } catch (RuntimeException e) {                                          // Mostra un AVÍS si el fitxer supera el tamany màxim d'arxiu (MAX_FILE_SIZE)

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Missatge informatiu");
            alert.setHeaderText("Arxiu massa gran!");
            alert.setContentText("El tamany de l'arxiu excedeix del límit (20MB).");
            alert.show();
            System.out.println("Tamany superior a 20MB");

        } catch (DuplicatedItemException e) {                                   // Mostra un AVÍS quan se selecciona una cançó que ja existeix al llistat

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Missatge d'error");
            alert.setHeaderText("La cançó no es pot afegir");
            alert.setContentText("La Cançó seleccionada ja existeix al llistat.");
            alert.show();
            System.out.println("Cançó repetida");

        } catch (NoDurationException e) {                                       // Mostra un AVÍS quan se selecciona una cançó que no té duració (00:00)

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Missatge d'error");
            alert.setHeaderText("La cançó no es pot reproduir ");
            alert.setContentText("Cançó sense duració (00:00): "
                    + e.getLocalizedMessage());
            alert.show();
            System.out.println("Cançó sense duració");

        } catch (IOException e) {                                               // Mostra un AVÍS quan no es troba l'arxiu

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Avís important");
            alert.setHeaderText("La cançó no es pot reproduir ");
            alert.setContentText("Comprova que la cançó no s'hagi esborrat, "
                    + "canviat d'ubicació o renombrat: " + e.getLocalizedMessage());
            alert.show();
            System.out.println("Arxiu no trobat, IOException: " + e.getMessage());

        } catch (UnsupportedAudioFileException e) {                             // Mostra un AVÍS quan l'arxiu no està realment codificat amb format (*.mp3)

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Avís important");
            alert.setHeaderText("La cançó no és un arxiu mp3 vàlid ");
            alert.setContentText("L'arxiu no està correctament codificat o no "
                    + "es tracta d'ún arxiu MP3 (" + e.getLocalizedMessage() + ")");
            alert.show();
            System.out.println("L'arxiu no és un MP3: " + e.getMessage());

        }

    }

    @FXML
    private Button playBtn;
    Tooltip playTooltip = new Tooltip("Reproduir cançó");
    Tooltip pauseTooltip = new Tooltip("Pausar cançó");

    @FXML
    private Label currentSongTitle;

    /**
     * (RF01): Mètode per reproduir la llista de cançons des del principi,
     * seleccionant un tema de la llista o després de fer nextSong(), prevSong()
     * o pause.
     * (RF07): Mètode per pausar la cançó tenint en compte que s'ha de guardar
     * el minut on es pausa quan s'està reproduint
     *
     * @author Víctor García
     * @author Pablo Morante
     */
    @FXML
    private void playSong() {
        songNumber = listView.getSelectionModel().getSelectedIndex();
        if (songNumber == -1) {
            songNumber = 0;
        }
        if (!running == true) {
            try {
                if (!songObservableList.isEmpty()) {                            //Si la llista de reproducció no es buida
                    if (mediaPlayer != null) {                                  //Si mediaPlayer ja està inicialitzat
                        stylePlayBtn("pause");
                        controlPlay(true);                                 
                    } else {                                                    //Si mediaPlayer no està inicialitzat
                        song = songObservableList.get(songNumber);
                        media = new Media(song.getPath());
                        mediaPlayer = new MediaPlayer(media);

                        stylePlayBtn("pause");
                        controlPlay(true);
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);           //Mostra avís si la llista de reproducció no té cançons
                    alert.setTitle("Avís");
                    alert.setHeaderText("La cançó no es pot reproduir.");
                    alert.setContentText("La llista de reproducció es buida.");
                    alert.show();
                    System.out.println("CANCEL");
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);               //Mostra avís si no es pot reproduir la llista
                alert.setTitle("Avís important");
                alert.setHeaderText("La cançó no es pot reproduir.");
                alert.setContentText("Comprova que la cançó no s'hagi esborrat, "
                        + "canviat d'ubicació o renombrat: " + e.getLocalizedMessage());
                alert.show();
                System.out.println("Arxiu no trobat, Exception: " + e.getMessage());
            }
        } else {
            try {
                mediaPlayer.pause();                                            
                Status currentStatus = mediaPlayer.getStatus();                 //Guardem l'estat del mediaPlayer
                if (currentStatus == Status.PLAYING) {                          //Comprovem si s'està reproduint música
                    stylePlayBtn("play");
                    controlPlay(false);
                } else if (currentStatus == Status.PAUSED || currentStatus == Status.STOPPED) {     //Comprovem si la música està pausada o aturada
                    song = songObservableList.get(songNumber);
                    media = new Media(song.getPath());
                    mediaPlayer = new MediaPlayer(media);
                    currentSongTitle.setText(songObservableList.get(songNumber).getTitle());

                    stylePlayBtn("pause");
                    controlPlay(true);                                         //Continuem la reproducció al punt on l'havíem aturat
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);               //Mostra avís si la cançó no es pot pausar
                alert.setTitle("Avís important");
                alert.setHeaderText("La cançó no es pot pausar.");
                alert.setContentText("Comprova que la cançó no s'hagi esborrat, "
                        + "canviat d'ubicació o renombrat: " + e.getLocalizedMessage());
                alert.show();
                System.out.println("Arxiu no trobat, Exception: " + e.getMessage());
            }
        }
    }

    @FXML
    private Button stopBtn;
    Tooltip stopTooltip = new Tooltip("Parar cançó");

    /**
     * (RF07): Mètode per aturar la cançó. En aquest cas la cançó atura la
     * reproducció sense guardar a quin minut es troba.
     *
     * @author Pablo Morante
     */
    @FXML
    private void stopSong() {
        try {
            openBtn.setDisable(false);                                          // Habilita el botó d'afegir cançons
            stylePlayBtn("play");
            running = false;
            listView.setDisable(false);
            mediaPlayer.stop();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Avís important");
            alert.setHeaderText("La cançó no es pot aturar.");
            alert.setContentText("Comprova que la cançó no s'hagi esborrat, "
                    + "canviat d'ubicació o renombrat: " + e.getLocalizedMessage());
            alert.show();
            System.out.println("Arxiu no trobat, Exception: " + e.getMessage());
        }
    }

    @FXML
    private Button btnNextSong;
    Tooltip nextTooltip = new Tooltip("Cançó següent");

    /**
     * (RF09): Mètode per canviar a la següent cançó de la llista de
     * reproducció.
     *
     * @author Izan Jimenez
     */
    @FXML
    public void nextSong() {
        if (songObservableList.size() > 0) {
            if (songNumber < songObservableList.size() - 1) {                   //si no es la última cançó

                songNumber++;
                mediaPlayer.stop();
                if (running) {
                    cancelTimer();
                }
                song = songObservableList.get(songNumber);
                media = new Media(song.getPath());
                mediaPlayer = new MediaPlayer(media);
                listView.getSelectionModel().select(songNumber);
                playSong();
            } else {                                                            //si es la ultima
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Avís");
                alert.setContentText("No hi ha més cançons a la llista de reproducció");
                alert.show();
                System.out.println("No hi ha més cançons");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Avís");
            alert.setContentText("No s'ha seleccionat cap arxiu.");
            alert.show();
            System.out.println("CANCEL");
        }
        refreshVolume();
    }

    @FXML
    private Button btnPrevSong;
    Tooltip prevTooltip = new Tooltip("Cançó anterior");

    /**
     * (RF09): Mètode per canviar a la cançó anterior de la llista de
     * reproducció.
     *
     * @author Izan Jimenez
     */
    @FXML
    void prevSong() {
        if (songObservableList.size() > 0) {
            if (songNumber >= 1) {                                              //si no es la última cançó

                songNumber--;
                mediaPlayer.stop();
                if (running) {
                    cancelTimer();
                }
                song = songObservableList.get(songNumber);
                media = new Media(song.getPath());
                mediaPlayer = new MediaPlayer(media);
                listView.getSelectionModel().select(songNumber);
                playSong();
            } else {                                                            //si es la ultima
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Avís");
                alert.setContentText("No hi ha més cançons a la llista de reproducció");
                alert.show();
                System.out.println("No hi ha més cançons");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Avís");
            alert.setContentText("No s'ha seleccionat cap arxiu.");
            alert.show();
            System.out.println("CANCEL");
        }
        refreshVolume();

    }

    /**
     * Mètode que manté el nivell de volum actualitzat entre les cançons.
     * 
     * @author Izan Jimenez
     */
    private void refreshVolume() {
    double i = volumeSlider.getValue();
    volumeSlider.setValue(0);
    volumeSlider.setValue(i);
    }
    
    @FXML
    private Button fwdBtn;
    Tooltip fwdTooltip = new Tooltip("Avançar 10 seg.");

    /**
     * (RF09): Durant la reproducció, ens permet avançar la cançó en blocs de 10 segons.
     *
     * @author Izan Jimenez
     */
    @FXML
    void fwdTime() {
        if (mediaPlayer != null) {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(javafx.util.Duration.seconds(10)));
        }
    }

    @FXML
    private Button rwdBtn;
    Tooltip rwdTooltip = new Tooltip("Retrocedir 10 seg.");

    /**
     * (RF09): Durant la reproducció, ens permet retrocedir la cançó en blocs de 10 segons.
     *
     * @author Izan Jimenez
     */
    @FXML
    void rwdTime() {
        if (mediaPlayer != null) {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(javafx.util.Duration.seconds(-10)));
        }
    }

    @FXML
    private Button randomSong;
    Tooltip randomTooltip = new Tooltip("Reproducció aleatòria");

    /**
     * (FA01): Permet reproduir cançons en mode aleatori si el tenim seleccionat.
     * Quan no està seleccionat es torna a la llista original.
     *
     * @author Víctor García
     */
    @FXML
    void randomSong() {
        if (random == false) {                                                  //Si seleccionem mode Random
            for (int i = 0; i < songObservableList.size(); ++i) {               //Creem una llista de reproducció aleatòria
                Random rand = new Random();
                int temp = rand.nextInt(songObservableList.size() - i) + i;
                Collections.swap(songObservableList, i, temp);
                random = true;
                randomSong.getStyleClass().clear();                             // Netejo classe CSS per assignar-li una de nova 
                randomSong.getStyleClass().add("random-active");                // Assigno classe CSS per mostrar icona RANDOM ON
            }
            if(running) stopSong();
            running = false;   
            songNumber = 0;       
            listView.getSelectionModel().select(songNumber);
            listView.refresh();                                          
            playSong();
            randomSong.setDisable(false);
        } else {                                                                //Per tornar a la llista original ordenem la llista de reproducció pel seu títol
            Comparator<Song> songComparator = Comparator.comparing(Song::getTitle);
            songObservableList.sort(songComparator);
            SortedList<Song> sortedSongs = new SortedList<>(songObservableList, songComparator);
            random = false;
            stopSong();
            running = false;
            randomSong.getStyleClass().clear();                                 // Netejo classe CSS per assignar-li una de nova 
            randomSong.getStyleClass().add("random");                           // Assigno classe CSS per mostrar icona RANDOM OFF;
            stylePlayBtn("play");
            songNumber = songObservableList.indexOf(song);
            listView.getSelectionModel().select(songNumber);
            listView.refresh();
            playSong();
        }        
    }

    /**
     * Inicialitza la barra de temps quan reproduïm música.
     *
     * @author Izan Jimenez
     */
    public void beginTimer() {

        timer = new Timer();

        task = new TimerTask() {

            public void run() {

                running = true;
                double current = mediaPlayer.getCurrentTime().toSeconds();
                double end = media.getDuration().toSeconds();
                sliderBar.setMajorTickUnit(end);
                sliderBar.setValue((current / end) * 100);

                songTime.setText(String.format("%02.0f:%02.0f", Math.floor(end / 60), end % 60));
                actualTime.setText(String.format("%02.0f:%02.0f", Math.floor(current / 60), current % 60));
            }
        };

        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    /**
     * Atura la barra de temps quan aturem la reproducció.
     *
     * @author Izan Jimenez
     */
    public void cancelTimer() {

        running = false;
        timer.cancel();
    }

    /**
     * (RF05): Creació de mètode per facilitar l'eliminació de cançons aquest
     * mètode comprova Song per Song si el títol es repeteix aixó fa que no
     * calgui una array abans creada 'títols'
     *
     * @param songs llista amb totes les cançons actual
     * @param nouTitol títol de la nova canço
     * @return Boolean si el títol es repeteix o no
     *
     * @author Pablo Morante
     */
    public boolean comprovarTitol(ObservableList<Song> songs, String nouTitol) {
        for (Song song : songs) {
            if (song.getTitle().equals(nouTitol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * (FA02): Càlcul de temps total de la llista de reproducció: s'envia la
     * llista de cançons i es suma la duració de totes
     *
     * @param songs llista de cançons
     * @return String duració total de la playlist
     * @throws UnsupportedAudioFileException, IOException, NoDurationException
     *
     * @author Pablo Morante
     */
    public String getPlaylistDuration(ObservableList<Song> songs) throws UnsupportedAudioFileException, IOException, NoDurationException {
        String totalDuration = "00:00";
        int minutes = 0;
        int seconds = 0;
        int hours = 0;
        for (Song song : songs) {
            String duration = song.getDuration();
            String[] parts = duration.split(":");
            minutes = minutes + Integer.parseInt(parts[0]);
            seconds = seconds + Integer.parseInt(parts[1]);

            int temp = seconds % 60;
            minutes = minutes + (seconds / 60);
            seconds = temp;
            temp = minutes % 60;
            hours = minutes / 60;
            minutes = temp;
        }

        if (hours == 0) {
            totalDuration = String.format("%02d:%02d", minutes, seconds);
        } else {
            totalDuration = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return totalDuration;
    }

    /**
     * Mètode per simplificar codi que es repeteix: start/stop una cançó
     *
     * @param accio Booleà per saber si es vol començar a reproduir o pausar
     *
     * @author Pablo Morante
     * @author Victor Garcia
     */
    public void controlPlay(Boolean accio) {
        if (!openBtn.isDisabled()) {                            //Comprovem si el botó per afegir cançons està activat
            openBtn.setDisable(accio);                           //Desactivem el botó per afegir cançons
        }
        listView.refresh();
        listView.getSelectionModel().select(songNumber);

        currentSongTitle.setText(songObservableList.get(songNumber).getTitle());
        beginTimer();
        if (accio) {
            mediaPlayer.play();
        }
        listView.setDisable(accio);                              //Desactivem la llista mentre reproduïm música per no tenir accés
        running = accio;
    }

    /**
     * Mètode per simplificar codi que es repeteix: canviar estils del botó play/pause
     *
     * @param accio String per saber si el botò ha de ser per reproduir o parar
     *
     * @author Pablo Morante
     * @author Victor Garcia
     */
    public void stylePlayBtn(String accio) {
        playBtn.getStyleClass().clear();                                    // Netejo classe CSS per assignar-li una de nova 
        playBtn.getStyleClass().add(accio);                                // Assigno classe CSS per mostrar icona PLAY
        if (accio.equals("play")) {
            playBtn.setTooltip(playTooltip);
        } else {
            playBtn.setTooltip(stopTooltip);
        }
    }
}
