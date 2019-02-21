package de.xenyria.splatoon.game.sound;

import com.xxmicloxx.NoteBlockAPI.model.Song;

import java.io.File;

public class MusicTrack {

    private String name;
    public String getName() { return name; }

    private File file;
    public File getFile() { return file; }

    public enum MusicType {

        BACKGROUND_MUSIC,
        LAST_MINUTE;

    }

    private Song song;
    public Song getSong() { return song; }

    private MusicType type;
    public MusicType getType() { return type; }

    private int id;
    public int getID() { return id; }

    public MusicTrack(int id, String name, File file, MusicType type, Song song) {

        this.id = id;
        this.name = name;
        this.file = file;
        this.type = type;
        this.song = song;

    }

}
