package org.devnexus.util;

import org.devnexus.R;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by summers on 11/13/13.
 */
public class ResourceUtils {

    private static Map<String, Integer> cassValueMap = new HashMap<String, Integer>();


    private static final int BALLROOM_A = "Ballroom A".hashCode();
    private static final int BALLROOM_B = "Ballroom B".hashCode();
    private static final int BALLROOM_C = "Ballroom C".hashCode();
    private static final int BALLROOM_D = "Ballroom D".hashCode();
    private static final int BALLROOM_E = "Ballroom E".hashCode();
    private static final int BALLROOM_F = "Ballroom F".hashCode();
    private static final int BALLROOM_G = "Ballroom G".hashCode();
    private static final int ROOM_102 = "Room 102".hashCode();
    private static final int ROOM_103 = "Room 103".hashCode();
    private static final int ROOM_104 = "Room 104".hashCode();
    private static final int ROOM_105 = "Room 105".hashCode();


    public static int trackCSSToColor(String trackCSS) {
        if (trackCSS == null) {
            return R.color.dn_orange_red;
        }
        String trackId = trackCSS.replace("-", "_");
        if (cassValueMap.get(trackId) == null) {
            for (Field field : R.color.class.getFields()) {
                if (field.getName().toLowerCase().equals(trackId)) {
                    try {
                        cassValueMap.put(trackId, field.getInt(null));
                    } catch (IllegalAccessException e) {
                        return 0;
                    }
                }
            }
        }

        return cassValueMap.get(trackId);

    }

    public static int roomCSSToColor(String roomName) {
        return trackCSSToColor(RoomName.room(roomName).trackName);
    }

}
