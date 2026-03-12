package com.example.mpa_booknookapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private Marker tempMarker;
    private boolean isPickingLocation = false; //track state for when adding new nook

    private ExtendedFloatingActionButton fab;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //inflate layout for map fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //get the map ready
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_inside_the_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        //initialise fused loc client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        //buttons:
        //logic for settings icon/button
        ImageView settingsIcon = view.findViewById(R.id.settingsButton);
        if (settingsIcon != null) {
            settingsIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        //FAB is now ExtendedFloatingActionButton from user testing 1
        fab = view.findViewById(R.id.fabAddNook);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                if (!isPickingLocation) {
                    //if not already in pin placement mode enter it now. change flag to true
                    isPickingLocation = true;
                    fab.setText("Tap the map to choose a location");
                    fab.setIconResource(R.drawable.ic_location_pin);

                } else {
                    //then go to the form if loc is chosen
                    if (tempMarker == null) { //still gotta pick a place. tell user.
                        Toast.makeText(getActivity(), "Please tap the map first to choose where to put your Nook!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //go to add nook form and add location coords in.
                    Intent intent = new Intent(getActivity(), AddNookActivity.class);
                    intent.putExtra("lat", tempMarker.getPosition().latitude);
                    intent.putExtra("lng", tempMarker.getPosition().longitude);
                    startActivity(intent);

                    //reset the FAB for when user goes back to map
                    resetFabState();
                }
            });
        }
        return view;
    }//end of oncreateview

    @Override
    public void onMapReady(GoogleMap googleMap) {

        //set up stuffs
        mMap = googleMap;
        mMap.setPadding(0, 214, 24, 0);
        loadNooksFromFirebase();
        getUserLocation();

        //for clickys on pre-existing nook pins to open the nook popup
        mMap.setOnMarkerClickListener(marker -> {
            String nookName = marker.getTitle();
            String nookDescription = marker.getSnippet();
            String distance = "Nearby"; //placeholder sort later

            //check its not a temp one
            if (nookName != null && !nookName.equals("New Nook Spot")) { //so if there is a name there and its not "new nook spot
                showNookPopup(nookName, nookDescription, distance);
                return true;
            }
            return false;
        });

        //NEW NOOK
        //to create new nook (after tapping fab
        mMap.setOnMapClickListener(latLng -> {
            if (isPickingLocation) { //in create nook mode.
                if (tempMarker != null) tempMarker.remove(); //only one at a time

                //drop pin on map
                tempMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Confirm Location?")
                        .snippet("Tap the pin to create your Nook!") //the smaller text
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                //makes the bubble pop up automatically
                tempMarker.showInfoWindow();
            }
        });

        //clicky for the tap to "Confirm Location" bubble. should go to add nook form
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTitle().equals("Confirm Location?")) {
                Intent intent = new Intent(getActivity(), AddNookActivity.class);
                intent.putExtra("lat", marker.getPosition().latitude);
                intent.putExtra("lng", marker.getPosition().longitude);
                startActivity(intent);
                resetFabState(); //map cleanup for when forms done
            }
        });


    }//end of onMapReady


    private void resetFabState() {
        isPickingLocation = false;
        if (fab != null) {
            fab.setText("Add a Nook");
            fab.setIconResource(R.drawable.ic_add);
        }
        if (tempMarker != null) tempMarker.remove();
        tempMarker = null;
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
                }
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(requireContext(), "Location needed to find nearby Nooks!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addNookPin(LatLng position, String title, String snippet, float color) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(color));
        mMap.addMarker(markerOptions);
    }

    private void loadNooksFromFirebase() {
        db.collection("nooks").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("FirebaseError", "Error: " + error.getMessage());
                return;
            }

            if (value != null) {
                mMap.clear();
                for (QueryDocumentSnapshot doc : value) {
                    String name = doc.getString("name");
                    String desc = doc.getString("description");
                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");

                    float color = doc.contains("colour") ?
                            doc.getDouble("colour").floatValue() : BitmapDescriptorFactory.HUE_RED;

                    if (lat != null && lng != null) {
                        LatLng pos = new LatLng(lat, lng);
                        addNookPin(pos, name, desc, color);
                    }
                }
            }
        });
    }

    private void showNookPopup(String name, String type, String dist) {
        if (!isPickingLocation) { //in create nook mode.
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_nook_popup, null);

        ((TextView) sheetView.findViewById(R.id.tvNookName)).setText(name);
        ((TextView) sheetView.findViewById(R.id.tvNookType)).setText(type);
        ((TextView) sheetView.findViewById(R.id.tvDistance)).setText(dist);

        sheetView.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(getActivity(), NookDetailsActivity.class);
            startActivity(intent);
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }}
}