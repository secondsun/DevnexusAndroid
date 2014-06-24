package org.devnexus.vo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

public class Room implements Serializable, Parcelable {
    public int id;
    public Date createdDate;
    public Date updatedDate;
    public int version;

    public String name;
    public String track;
    public String cssStyleName;
    public int capacity;
    public String description;
    public int roomOrder;

    public Room(Parcel source) {
        id = source.readInt();
        createdDate = new Date(source.readLong());
        updatedDate = new Date(source.readLong());
        version = source.readInt();

        name = source.readString();
        track = source.readString();
        cssStyleName = source.readString();
        capacity = source.readInt();
        description = source.readString();;
        roomOrder = source.readInt();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object)this).getClass() != o.getClass()) return false;

        Room room = (Room) o;

        if (capacity != room.capacity) return false;
        if (id != room.id) return false;
        if (roomOrder != room.roomOrder) return false;
        if (version != room.version) return false;
        if (createdDate != null ? !createdDate.equals(room.createdDate) : room.createdDate != null)
            return false;
        if (cssStyleName != null ? !cssStyleName.equals(room.cssStyleName) : room.cssStyleName != null)
            return false;
        if (description != null ? !description.equals(room.description) : room.description != null)
            return false;
        if (name != null ? !name.equals(room.name) : room.name != null) return false;
        if (track != null ? !track.equals(room.track) : room.track != null) return false;
        if (updatedDate != null ? !updatedDate.equals(room.updatedDate) : room.updatedDate != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        result = 31 * result + (updatedDate != null ? updatedDate.hashCode() : 0);
        result = 31 * result + version;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (track != null ? track.hashCode() : 0);
        result = 31 * result + (cssStyleName != null ? cssStyleName.hashCode() : 0);
        result = 31 * result + capacity;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + roomOrder;
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeLong(createdDate.getTime());
        dest.writeLong(updatedDate.getTime());
        dest.writeInt(version);

        dest.writeString(name);
        dest.writeString(track);
        dest.writeString(cssStyleName);
        dest.writeInt(capacity);
        dest.writeString(description);
        dest.writeInt(roomOrder);

    }

    public static Creator<Room> CREATOR = new Creator<Room>() {
        @Override
        public Room createFromParcel(Parcel source) {
            return new Room(source);
        }

        @Override
        public Room[] newArray(int size) {
            return new Room[size];
        }
    };
}
