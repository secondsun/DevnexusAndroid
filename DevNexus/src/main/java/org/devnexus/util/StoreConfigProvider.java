package org.devnexus.util;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.devnexus.vo.Schedule;
import org.devnexus.vo.UserCalendar;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.impl.datamanager.StoreConfig;
import org.jboss.aerogear.android.impl.datamanager.StoreTypes;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Created by summers on 2/8/14.
 */
public class StoreConfigProvider implements Provider<StoreConfig> {

    private static final GsonBuilder BUILDER;

    static {
        BUILDER = new GsonBuilder();

        BUILDER.registerTypeAdapter(Date.class, new JsonDeserializer() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        BUILDER.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                    context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        });

    }

    public static StoreConfig getUserStoreConfig(Context context) {
        return new StoreConfigProvider().get(context, BUILDER, UserCalendar.class);
    }

    public static StoreConfig getScheduleStoreConfig(Context context) {
        return new StoreConfigProvider().get(context, BUILDER, Schedule.class);
    }

    @Override
    public StoreConfig get(Object... objects) {
        Context context = (Context) objects[0];
        GsonBuilder builder = (GsonBuilder) objects[1];
        Class klass = (Class) objects[2];
        StoreConfig userCalendarStoreConfig = new StoreConfig();
        userCalendarStoreConfig.setType(StoreTypes.SQL);
        userCalendarStoreConfig.setKlass(klass);
        userCalendarStoreConfig.setContext(context);
        userCalendarStoreConfig.setBuilder(builder);
        return userCalendarStoreConfig;
    }
}
