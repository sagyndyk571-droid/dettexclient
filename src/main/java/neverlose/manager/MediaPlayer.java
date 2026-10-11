package neverlose.manager;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

public class MediaPlayer {

    private String lastTitle = "Nothing is playing";
    private String artist = "";
    private double duration = 0.0;
    private double position = 0.0;
    private boolean playing = false;
    private Clip clip;
    private long lastPositionUpdate = 0;
    private String currentAudioUrl = null;

    /**
     * Загружает и проигрывает аудио-поток по URL.
     *
     * @param title        Название трека.
     * @param artist       Исполнитель.
     * @param audioUrl     Прямая ссылка на аудиофайл (поддерживаются форматы WAV, AIFF, AU).
     * @param durationSeconds Длительность трека в секундах.
     */
    public void loadAndPlay(String title, String artist, String audioUrl, double durationSeconds) {
        if (clip != null && clip.isRunning()) {
            stop();
        }

        this.lastTitle = title;
        this.artist = artist;
        this.duration = durationSeconds;
        this.currentAudioUrl = audioUrl;
        this.position = 0.0;

        // Загрузка аудио и воспроизведение в отдельном потоке
        new Thread(() -> {
            try {
                URL url = new URL(currentAudioUrl);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(url.openStream()));
                AudioFormat baseFormat = audioStream.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);
                clip = AudioSystem.getClip();
                clip.open(decodedStream);
                clip.start();
                playing = true;
                lastPositionUpdate = System.currentTimeMillis();
            } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                e.printStackTrace();
                playing = false;
            }
        }).start();
    }

    public void play() {
        if (clip != null && !playing) {
            clip.start();
            playing = true;
            lastPositionUpdate = System.currentTimeMillis();
        }
    }

    public void pause() {
        if (clip != null && playing) {
            clip.stop();
            playing = false;
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        playing = false;
        position = 0.0;
    }

    /**
     * Возвращает текущую позицию воспроизведения.
     */
    public double getPosition() {
        if (playing && clip != null) {
            long now = System.currentTimeMillis();
            long delta = now - lastPositionUpdate;
            position += delta / 1000.0;
            if (position > duration) position = duration;
            lastPositionUpdate = now;
        }
        return position;
    }

    public double getDuration() { return duration; }
    public String getLastTitle() { return lastTitle; }
    public String getArtist() { return artist; }

    public boolean fullNullCheck() {
        return lastTitle == null || lastTitle.isEmpty() || lastTitle.equals("Nothing is playing");
    }

    // --- Обложка ---
    public String getCoverUrl() { return null; } // Заглушка, если нужна another logic
}