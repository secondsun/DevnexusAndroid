package org.devnexus.vo;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.internal.cr;

import java.io.Serializable;
import java.util.Date;

public class Presentation implements Serializable, Parcelable {
    public int id;
    public Date createdDate;
    public Date updatedDate;
    public int version;
    public String audioLink;
    public String description;
    public String presentationLink;
    public Speaker speaker;
    public String title;
    public String presentationType;
    public String skillLevel;

    public Presentation() {

    }

    public Presentation(Parcel source) {
        id = source.readInt();
        createdDate = new Date(source.readLong());
        updatedDate = new Date(source.readLong());
        version = source.readInt();
        audioLink = source.readString();
        description = source.readString();
        presentationLink = source.readString();
        speaker = (Speaker) source.readParcelable(Speaker.class.getClassLoader());
        title = source.readString();
        presentationLink = source.readString();
        skillLevel = source.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || (((Object)this).getClass()) != o.getClass()) return false;

        Presentation that = (Presentation) o;

        if (id != that.id) return false;
        if (version != that.version) return false;
        if (audioLink != null ? !audioLink.equals(that.audioLink) : that.audioLink != null)
            return false;
        if (createdDate != null ? !createdDate.equals(that.createdDate) : that.createdDate != null)
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        if (presentationLink != null ? !presentationLink.equals(that.presentationLink) : that.presentationLink != null)
            return false;
        if (presentationType != null ? !presentationType.equals(that.presentationType) : that.presentationType != null)
            return false;
        if (skillLevel != null ? !skillLevel.equals(that.skillLevel) : that.skillLevel != null)
            return false;
        if (speaker != null ? !speaker.equals(that.speaker) : that.speaker != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
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
        result = 31 * result + (audioLink != null ? audioLink.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (presentationLink != null ? presentationLink.hashCode() : 0);
        result = 31 * result + (speaker != null ? speaker.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (presentationType != null ? presentationType.hashCode() : 0);
        result = 31 * result + (skillLevel != null ? skillLevel.hashCode() : 0);
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
        dest.writeString(audioLink);
        dest.writeString(description);
        dest.writeString(presentationLink);
        dest.writeParcelable(speaker, flags);
        dest.writeString(title);
        dest.writeString(presentationLink );
        dest.writeString(skillLevel );
    }

    public static final Creator<Presentation> CREATOR = new Creator<Presentation>() {
        @Override
        public Presentation createFromParcel(Parcel source) {
            return new Presentation(source);
        }

        @Override
        public Presentation[] newArray(int size) {
            return new Presentation[size];
        }
    };

}
