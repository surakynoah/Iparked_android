package com.dsd2016.iparked_android.fragments;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import com.dsd2016.iparked_android.myClasses.AnimatorUtils;
import com.dsd2016.iparked_android.myClasses.Beacon;
import com.dsd2016.iparked_android.myClasses.ClipRevealFrame;
import com.dsd2016.iparked_android.myClasses.IparkedApp;
import com.dsd2016.iparked_android.myClasses.MyLocationProvider;
import com.dsd2016.iparked_android.myClasses.OnGotLastLocation;
import com.dsd2016.iparked_android.myClasses.OnMenuItemSelectedListener;
import com.dsd2016.iparked_android.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ogaclejapan.arclayout.ArcLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyMapFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback, OnGotLastLocation {
    private static final String TAG = "MAP_FRAGMENT";
    View myView;

    OnMenuItemSelectedListener mListener;
    protected MapView mapView;
    protected GoogleMap googleMap, map;
    MyLocationProvider myLocationProvider;
    private Map<String, MarkerOptions> markers;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        markers = new HashMap<>();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }

    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (NullPointerException e) {
                Log.e(TAG, "Error while attempting MapView.onDestroy(), ignoring exception", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        try {
            mListener = (OnMenuItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMenuItemSelectedListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_map, container, false);
        myView.findViewById(R.id.fab).setOnClickListener(this);
        IparkedApp.mMenuHandler.setElements(myView.findViewById(R.id.root_layout),getContext());
        mapView = (MapView) myView.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return myView;

    }

    public static MyMapFragment newInstance() {
        return new MyMapFragment();
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            IparkedApp.mMenuHandler.handleMenu();
            return;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        int hasLocationPermission = ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageOKCancel("You need to allow access to Location",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        modifyMap(googleMap);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this.getContext())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void modifyMap(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(true);
        MapsInitializer.initialize(this.getContext());
        myLocationProvider = new MyLocationProvider(getContext(), this);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    modifyMap(googleMap);
                } else {
                    // Permission Denied
                    Toast.makeText(this.getContext(), "Location Access Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void onGotLastLocation(Location location) {
        LatLng fer_parking = new LatLng(45.800700, 15.971215);

        GroundOverlayOptions ferParkingMap = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.iparked_garage_fer))
                .position(fer_parking, 31, 62)
                .bearing(87);
        map.addGroundOverlay(ferParkingMap);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(fer_parking, 19);
        map.animateCamera(cameraUpdate);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }

        if (map != null) {
            ArrayList<Beacon> beacons = IparkedApp.mDbHelper.getPersonalBeacons();

            /** Check if beacon list is not initialized */
            if (beacons == null) {
                return;
            }

            /** Add beacons from database to map */
            for (Beacon beacon : beacons) {

                if (beacon.getLocation() == null) {
                    continue;
                }

                LatLng latLng = new LatLng(beacon.getLocation().getLatitude(), beacon.getLocation().getLongitude());

                MarkerOptions marker = new MarkerOptions();
                marker.position(latLng);
                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.car2));
                marker.title(beacon.getName());
                marker.snippet("Parked on 12/11/2016 12:12:12");

                map.addMarker(marker);
            }

        } else {
            Log.v("iParked", "map null");
        }

        map.setMyLocationEnabled(true);
    }

    public void CheckContinue(){
        myLocationProvider.Continue();
    }
}
