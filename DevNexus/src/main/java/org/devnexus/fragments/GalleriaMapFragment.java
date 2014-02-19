package org.devnexus.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.devnexus.R;
import org.devnexus.util.ResourceUtils;

/**
 * Created by summers on 11/13/13.
 */
public class GalleriaMapFragment extends Fragment implements
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener {

    private static final LatLng GALLERIA = new LatLng(33.88346, -84.46695);

    private static final LatLng BALLROOM_A = new LatLng(toDec(33, 53.003, 0), toDec(-84, 28.033, 0));
    private static final LatLng BALLROOM_B = new LatLng(toDec(33, 52.996, 0), toDec(-84, 28.030, 0));
    private static final LatLng BALLROOM_C = new LatLng(toDec(33, 52.984, 0), toDec(-84, 28.025, 0));
    private static final LatLng BALLROOM_D = new LatLng(toDec(33, 52.977, 0), toDec(-84, 28.022, 0));
    private static final LatLng BALLROOM_E = new LatLng(toDec(33, 53.008, 0), toDec(-84, 28.014, 0));
    private static final LatLng BALLROOM_F = new LatLng(toDec(33, 52.984, 0), toDec(-84, 28.005, 0));
    private static final LatLng BALLROOM_G = new LatLng(toDec(33, 52.990, 0), toDec(-84, 28.028, 0));

    private static final LatLng ROOM_102 = new LatLng(toDec(33, 53.069, 0), toDec(-84, 27.970, 0));
    private static final LatLng ROOM_103 = new LatLng(toDec(33, 53.067, 0), toDec(-84, 27.975, 0));
    private static final LatLng ROOM_104 = new LatLng(toDec(33, 53.065, 0), toDec(-84, 27.982, 0));
    private static final LatLng ROOM_105 = new LatLng(toDec(33, 53.063, 0), toDec(-84, 27.988, 0));

    // Initial camera position
    private static final LatLng CAMERA_GALLERIA = new LatLng(33.88346, -84.46695);
    private static final float CAMERA_ZOOM = 17.75f;
    private static final String TAG = GalleriaMapFragment.class.getSimpleName();
    private GoogleMap mMap;
    private View view;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.galleria_map_fragment, null);
            setupMap(true);
        }
        return view;
    }

    private void setupMap(boolean resetCamera) {

        if (mMap == null) {
            mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        }

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                View v = View.inflate(getActivity(), R.layout.map_info_window, null);
                v.setBackgroundResource(ResourceUtils.roomCSSToColor(marker.getTitle()));
                TextView titleText = (TextView) v.findViewById(R.id.title);
                titleText.setBackgroundResource(ResourceUtils.roomCSSToColor(marker.getTitle()));
                titleText.setText(marker.getTitle());
                return v;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = View.inflate(getActivity(), R.layout.map_info_window, null);
                v.setBackgroundResource(ResourceUtils.roomCSSToColor(marker.getTitle()));
                TextView titleText = (TextView) v.findViewById(R.id.title);
                titleText.setText(marker.getTitle());
                titleText.setBackgroundResource(ResourceUtils.roomCSSToColor(marker.getTitle()));
                return v;
            }
        });

        mMap.addMarker(new MarkerOptions().position(BALLROOM_A).title("Ballroom A"));
        mMap.addMarker(new MarkerOptions().position(BALLROOM_B).title("Ballroom B"));
        mMap.addMarker(new MarkerOptions().position(BALLROOM_C).title("Ballroom C"));
        mMap.addMarker(new MarkerOptions().position(BALLROOM_D).title("Ballroom D"));
        mMap.addMarker(new MarkerOptions().position(BALLROOM_E).title("Ballroom E"));
        mMap.addMarker(new MarkerOptions().position(BALLROOM_F).title("Ballroom F"));
        mMap.addMarker(new MarkerOptions().position(BALLROOM_G).title("Ballroom G"));

        mMap.addMarker(new MarkerOptions().position(ROOM_102).title("Room 102"));
        mMap.addMarker(new MarkerOptions().position(ROOM_103).title("Room 103"));
        mMap.addMarker(new MarkerOptions().position(ROOM_104).title("Room 104"));
        mMap.addMarker(new MarkerOptions().position(ROOM_105).title("Room 105"));


        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnCameraChangeListener(this);

        if (resetCamera) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(
                    CAMERA_GALLERIA, CAMERA_ZOOM)));
        }

        mMap.setIndoorEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setZoomControlsEnabled(false);


    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        TrackViewFragment.newInstance(marker.getTitle()).show(getChildFragmentManager(), TAG);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private static double toDec(double deg, double min, double sec) {
        double sign = deg < 0 ? -1 : 1;
        deg = deg * sign;

        double result = sign * (deg + min / 60d + sec / 3600d);

        Log.d(TAG, String.format("%f° %2.0f.%3.0f = %.6f", deg, min, sec, result));
        return result;
    }

    public void onDestroyView() {
        super.onDestroyView();
//        Fragment fragment = (getFragmentManager().findFragmentById(R.id.map));
//        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
//        ft.remove(fragment);
//        ft.commit();
    }

}

