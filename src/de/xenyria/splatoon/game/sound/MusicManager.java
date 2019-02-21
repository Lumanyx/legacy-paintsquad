package de.xenyria.splatoon.game.sound;

import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import de.xenyria.splatoon.XenyriaSplatoon;

import java.io.File;
import java.util.ArrayList;

public class MusicManager {

    private static ArrayList<MusicTrack> tracks = new ArrayList<>();
    public static ArrayList<MusicTrack> getTracks() { return tracks; }

    public String musicPath() {

        return XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "music" + File.separator;

    }

    public MusicTrack loadFromFile(int id, String name, String fileName, MusicTrack.MusicType type) {

        return new MusicTrack(
                id, name, new File(musicPath()+fileName+".nbs"), type, NBSDecoder.parse(new File(musicPath()+fileName+".nbs"))
        );

    }

    private MusicTrack[][] getMusicTrackCombinations() {

        int i = 0;
        for(MusicTrack track : tracks) {

            if(track.getType() == MusicTrack.MusicType.BACKGROUND_MUSIC) {

                i++;

            }

        }

        MusicTrack[][] tracks = new MusicTrack[i][2];

        tracks[0][0] = getTrack(1);
        tracks[0][1] = getTrack(5);

        tracks[1][0] = getTrack(2);
        tracks[1][1] = getTrack(6);

        tracks[2][0] = getTrack(4);
        tracks[2][1] = getTrack(5);

        tracks[3][0] = getTrack(7);
        tracks[3][1] = getTrack(5);
        return tracks;

    }
    private MusicTrack[][] trackLists;
    public MusicTrack getTrack(int id) {

        for(MusicTrack track : tracks) {

            if(track.getID() == id) {

                return track;

            }

        }
        return null;

    }

    public MusicManager() {

        tracks.add(loadFromFile(1, "Splattack!", "bgm_splattack", MusicTrack.MusicType.BACKGROUND_MUSIC));
        tracks.add(loadFromFile(2, "Fly Octo Fly", "bgm_flyoctofly", MusicTrack.MusicType.BACKGROUND_MUSIC));
        tracks.add(loadFromFile(4, "Blitz It", "bgm_blitzit", MusicTrack.MusicType.BACKGROUND_MUSIC));
        tracks.add(loadFromFile(5, "Now or Never!", "lm_nowornever", MusicTrack.MusicType.LAST_MINUTE));
        tracks.add(loadFromFile(6, "Ebb and Flow (Octo)", "lm_ebbandflow_octo", MusicTrack.MusicType.LAST_MINUTE));
        // That heavenly melody...
        tracks.add(loadFromFile(7, "Calamari Inkantation", "bgm_calamariinkantation", MusicTrack.MusicType.BACKGROUND_MUSIC));

        trackLists = getMusicTrackCombinations();

        XenyriaSplatoon.getXenyriaLogger().log("§b" + tracks.size() + " Musikstücke §7geladen!");

    }

    public MusicTrack[][] getTrackLists() { return trackLists; }

    public MusicTrack[] getTrackList(int index) {

        return trackLists[index];

    }

}
