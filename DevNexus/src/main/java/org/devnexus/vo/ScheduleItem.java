package org.devnexus.vo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleItem implements Serializable, Parcelable {
    public int id;
    public Date createdDate;
    public Date updatedDate;
    public int version;
    public String scheduleItemType;
    public String title;
    public Date fromTime;
    public Date toTime;
    public Room room;
    public Presentation presentation;
    public int rowspan;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object)this).getClass() != o.getClass()) return false;

        ScheduleItem that = (ScheduleItem) o;

        if (id != that.id) return false;
        if (rowspan != that.rowspan) return false;
        if (version != that.version) return false;
        if (createdDate != null ? !createdDate.equals(that.createdDate) : that.createdDate != null)
            return false;
        if (fromTime != null ? !fromTime.equals(that.fromTime) : that.fromTime != null)
            return false;
        if (presentation != null ? !presentation.equals(that.presentation) : that.presentation != null)
            return false;
        if (room != null ? !room.equals(that.room) : that.room != null) return false;
        if (scheduleItemType != null ? !scheduleItemType.equals(that.scheduleItemType) : that.scheduleItemType != null)
            return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (toTime != null ? !toTime.equals(that.toTime) : that.toTime != null) return false;
        if (updatedDate != null ? !updatedDate.equals(that.updatedDate) : that.updatedDate != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        result = 31 * result + (updatedDate != null ? updatedDate.hashCode() : 0);
        result = 31 * result + version;
        result = 31 * result + (scheduleItemType != null ? scheduleItemType.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (fromTime != null ? fromTime.hashCode() : 0);
        result = 31 * result + (toTime != null ? toTime.hashCode() : 0);
        result = 31 * result + (room != null ? room.hashCode() : 0);
        result = 31 * result + (presentation != null ? presentation.hashCode() : 0);
        result = 31 * result + rowspan;
        return result;
    }

    // Parcelling part
    public ScheduleItem(Parcel in){

        this.id = in.readInt();
        createdDate = new Date(in.readLong());
        updatedDate = new Date(in.readLong());
        version = in.readInt();
        scheduleItemType = in.readString();
        title = in.readString();
        fromTime = new Date(in.readLong());
        toTime = new Date(in.readLong());
        room = (Room) in.readParcelable(Room.class.getClassLoader());
        presentation = (Presentation) in.readParcelable(Presentation.class.getClassLoader());
        rowspan = in.readInt();

    }

    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);

        dest.writeLong(createdDate.getTime());
        dest.writeLong(updatedDate.getTime());
        dest.writeInt(version);
        dest.writeString(scheduleItemType);
        dest.writeString(title);
        dest.writeLong(fromTime.getTime());
        dest.writeLong(toTime.getTime());
        dest.writeParcelable(room, 0);
        dest.writeParcelable(presentation, 0);
        dest.writeInt(rowspan);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ScheduleItem createFromParcel(Parcel in) {
            return new ScheduleItem(in);
        }

        public ScheduleItem[] newArray(int size) {
            return new ScheduleItem[size];
        }
    };


}
