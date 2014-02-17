package org.devnexus.sync;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.devnexus.util.GsonUtils;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.contract.ScheduleContract;
import org.devnexus.vo.contract.SingleColumnJsonArrayList;
import org.devnexus.vo.contract.UserCalendarContract;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.impl.datamanager.DefaultIdGenerator;
import org.jboss.aerogear.android.impl.datamanager.SQLStore;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by summers on 2/8/14.
 */
public class DevNexusContentProvider extends ContentProvider {


    private static final String TAG = DevNexusContentProvider.class.getSimpleName();
    private static final Gson GSON = GsonUtils.GSON;

    private static ContentResolver resolver;

    @Override
    public boolean onCreate() {
        resolver = getContext().getContentResolver();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri.equals(UserCalendarContract.URI)) {
            return execute(uri, null, null, null, new CalendarQuery());
        } else if (uri.equals(ScheduleContract.URI)) {
            return execute(uri, null, null, null, new ScheduleQuery());
        } else
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));
    }

    @Override
    public String getType(Uri uri) {
        if (uri.equals(UserCalendarContract.URI)) {
            return uri.toString();
        } else if (uri.equals(ScheduleContract.URI)) {
            return uri.toString();
        } else
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        if (uri.equals(UserCalendarContract.URI)) {
            return execute(uri, new ContentValues[]{values}, null, null, new CalendarInsert());
        } else if (uri.equals(ScheduleContract.URI)) {
            return execute(uri, new ContentValues[]{values}, null, null, new ScheduleInsert());
        } else
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));
    }

    @Override
    public int bulkInsert(final Uri uri, final ContentValues[] values) {
        if (uri.equals(UserCalendarContract.URI)) {
            return execute(uri, values, "", null, new CalendarBulkInsert());
        } else if (uri.equals(ScheduleContract.URI)) {
            return execute(uri, values, "", null, new ScheduleBulkInsert());
        } else
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));

    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        if (uri.equals(UserCalendarContract.URI)) {
            return execute(uri, null, selection, selectionArgs, new CalendarDelete());
        } else if (uri.equals(ScheduleContract.URI)) {
            return execute(uri, null, selection, selectionArgs, new ScheduleDelete());
        } else
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));

    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        if (uri.equals(UserCalendarContract.URI)) {
            if (values == null) {
                return execute(uri, new ContentValues[]{null}, selection, selectionArgs, new CalendarUpdate());
            } else {
                return execute(uri, new ContentValues[]{values}, selection, selectionArgs, new CalendarUpdate());
            }
        } else if (uri.equals(ScheduleContract.URI)) {
            if (values == null) {
                return execute(uri, new ContentValues[]{null}, selection, selectionArgs, new ScheduleUpdate());
            } else {
                return execute(uri, new ContentValues[]{values}, selection, selectionArgs, new ScheduleUpdate());
            }
        } else
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));


    }

    private <T> T execute(final Uri uri, final ContentValues[] values, final String selection, final String[] selectionArgs, final Operation<T> op) {
        final AtomicReference<T> returnRef = new AtomicReference<T>();

        SQLStore tempStore;
        if (uri.equals(UserCalendarContract.URI)) {
            tempStore = new SQLStore<UserCalendar>(UserCalendar.class, getContext(), GsonUtils.builder(), new DefaultIdGenerator());
        } else if (uri.equals(ScheduleContract.URI)) {
            tempStore = new SQLStore<Schedule>(Schedule.class, getContext(), GsonUtils.builder(), new DefaultIdGenerator());
        } else {
            throw new IllegalArgumentException(String.format("%s not supported", uri.toString()));
        }

        final SQLStore store = tempStore;

        final CountDownLatch latch = new CountDownLatch(1);
        synchronized (TAG) {
            store.open(new Callback<SQLStore<UserCalendar>>() {
                @Override
                public void onSuccess(SQLStore<UserCalendar> userCalendarSQLStore) {


                    try {
                        returnRef.set(op.exec(GSON, store, uri, values, selection, selectionArgs));
                    } finally {
                        try {
                            store.close();
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        } finally {
                            latch.countDown();
                        }
                    }


                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    try {
                        store.close();
                    } catch (Exception ignore) {
                        Log.e(TAG, e.getMessage(), e);

                    }
                    latch.countDown();
                }
            });

            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
                //ignore?
            }
        }
        return returnRef.get();
    }

    private interface Operation<T> {
        T exec(Gson gson, SQLStore calendarStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs);
    }

    private static class CalendarInsert implements Operation<Uri> {

        @Override
        public Uri exec(Gson gson, SQLStore calendarStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            UserCalendar calendar = gson.fromJson(values[0].getAsString(UserCalendarContract.DATA), UserCalendar.class);
            calendarStore.save(calendar);
            if (values[0].getAsBoolean(ScheduleContract.NOTIFY) != null && values[0].getAsBoolean(ScheduleContract.NOTIFY)) {
                resolver.notifyChange(UserCalendarContract.URI, null);
            }
            return UserCalendarContract.URI;
        }
    }

    private static class CalendarBulkInsert implements Operation<Integer> {

        @Override
        public Integer exec(Gson gson, SQLStore calendarStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            for (ContentValues value : values) {
                UserCalendar calendar = gson.fromJson(value.getAsString(UserCalendarContract.DATA), UserCalendar.class);
                calendarStore.save(calendar);
            }
            resolver.notifyChange(UserCalendarContract.URI, null);
            return values.length;
        }
    }


    private static class CalendarDelete implements Operation<Integer> {

        @Override
        public Integer exec(Gson gson, SQLStore calendarStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            if (selectionArgs == null || selectionArgs[0] == null) {
                calendarStore.reset();
            } else {
                Long id = Long.getLong(selectionArgs[0]);
                calendarStore.remove(id);
            }
            resolver.notifyChange(UserCalendarContract.URI, null);
            return 1;
        }
    }

    private static class CalendarUpdate implements Operation<Integer> {

        @Override
        public Integer exec(Gson gson, SQLStore calendarStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            if (selectionArgs == null || selectionArgs[0] == null) {
                calendarStore.reset();
            } else {
                Long id = Long.getLong(selectionArgs[0]);
                calendarStore.remove(id);
            }
            UserCalendar calendar = gson.fromJson(values[0].getAsString(UserCalendarContract.DATA), UserCalendar.class);
            calendarStore.save(calendar);
            if (values[0].getAsBoolean(UserCalendarContract.NOTIFY) != null && values[0].getAsBoolean(UserCalendarContract.NOTIFY)) {
                resolver.notifyChange(UserCalendarContract.URI, null);
            }
            return 1;
        }
    }

    private static class CalendarQuery implements Operation<SingleColumnJsonArrayList> {

        @Override
        public SingleColumnJsonArrayList exec(Gson gson, SQLStore calendarStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {

            return new SingleColumnJsonArrayList(new ArrayList<UserCalendar>(calendarStore.readAll()));
        }
    }

    private static class ScheduleQuery implements Operation<Cursor> {

        @Override
        public SingleColumnJsonArrayList exec(Gson gson, SQLStore scheduleStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            return new SingleColumnJsonArrayList(new ArrayList<UserCalendar>(scheduleStore.readAll()));
        }
    }

    private static class ScheduleUpdate implements Operation<Integer> {

        @Override
        public Integer exec(Gson gson, SQLStore scheduleStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            if (selectionArgs == null || selectionArgs[0] == null) {
                scheduleStore.reset();
            } else {
                Long id = Long.getLong(selectionArgs[0]);
                scheduleStore.remove(id);
            }
            Schedule schedule = gson.fromJson(values[0].getAsString(ScheduleContract.DATA), Schedule.class);
            scheduleStore.save(schedule);
            if (values[0].getAsBoolean(ScheduleContract.NOTIFY) != null && values[0].getAsBoolean(ScheduleContract.NOTIFY)) {
                resolver.notifyChange(ScheduleContract.URI, null);
            }
            return 1;
        }
    }

    private static class ScheduleInsert implements Operation<Uri> {

        @Override
        public Uri exec(Gson gson, SQLStore scheduleStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            Schedule calendar = gson.fromJson(values[0].getAsString(ScheduleContract.DATA), Schedule.class);
            scheduleStore.save(calendar);
            if (values[0].getAsBoolean(ScheduleContract.NOTIFY) != null && values[0].getAsBoolean(ScheduleContract.NOTIFY)) {
                resolver.notifyChange(ScheduleContract.URI, null);
            }
            return ScheduleContract.URI;
        }
    }

    private static class ScheduleBulkInsert implements Operation<Integer> {

        @Override
        public Integer exec(Gson gson, SQLStore scheduleStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {
            for (ContentValues value : values) {
                Schedule calendar = gson.fromJson(value.getAsString(ScheduleContract.DATA), Schedule.class);
                scheduleStore.save(calendar);
            }
            resolver.notifyChange(ScheduleContract.URI, null);
            return values.length;
        }
    }


    private static class ScheduleDelete implements Operation<Integer> {

        @Override
        public Integer exec(Gson gson, SQLStore scheduleStore, Uri uri, ContentValues[] values, String selection, String[] selectionArgs) {

            if (selectionArgs == null || selectionArgs[0] == null) {
                scheduleStore.reset();
            } else {
                Long id = Long.getLong(selectionArgs[0]);
                scheduleStore.remove(id);
            }

            resolver.notifyChange(ScheduleContract.URI, null);
            return 1;
        }
    }


}
