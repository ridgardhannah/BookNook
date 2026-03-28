package com.example.mpa_booknookapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private Marker tempMarker;
    private boolean isPickingLocation = false; //track state for when adding new nook
    private ExtendedFloatingActionButton fab;

    //fir the search filter
    private String currentSearchQuery = ""; //starts empty
    private List<QueryDocumentSnapshot> allNooksList = new ArrayList<>();

    @Nullable

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
            settingsIcon.setOnClickListener(v -> { //if settings is tapped
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        //search for nooks logic
        EditText etSearch = view.findViewById(R.id.etMapSearch);
        ImageView btnSearch = view.findViewById(R.id.btnMapSearch);

        if (etSearch != null && btnSearch != null) {
            //listen for magnifying glass icon tap
            btnSearch.setOnClickListener(v -> {
                currentSearchQuery = etSearch.getText().toString().trim();
                refreshMapPins(); //to redraw the map with right pins
            });

            //also listen for the enter/search button on keyboard
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    currentSearchQuery = etSearch.getText().toString().trim();
                    refreshMapPins(); //to redraw the map with right pins

                    //hide keyboard after presing search
                    InputMethodManager manager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            });

        }


        //FAB is now ExtendedFloatingActionButton from user testing 1
        fab = view.findViewById(R.id.fabAddNook);
        if (fab != null) {
            fab.setOnClickListener(v -> { //when user taps the fab
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

        //when user taps somewhere on the map to put a pin there
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

        //when user taps any map pin. could be an existing nook pin or a temp pin for creating new nook
        mMap.setOnMarkerClickListener(marker -> {
            //check if its a temp pin for adding new nook or existing nook first
            if (isPickingLocation && tempMarker != null && marker.getId().equals(tempMarker.getId())) {

                //its a temp pin so set up to go to add nook form. pre fil with lat and long
                Intent intent = new Intent(getActivity(), AddNookActivity.class);
                intent.putExtra("lat", marker.getPosition().latitude);
                intent.putExtra("lng", marker.getPosition().longitude);
                startActivity(intent);

                resetFabState(); //clean up map
                return true; //click handled now. dont show the bubble
            }

            //if here, the user clicked a pre existing nook pin
            String nookName = marker.getTitle();

            if (nookName != null && !nookName.equals("Confirm Location?")) {

                //get the nook id from the marker
                String id = "";
                boolean isOpen247 = false; //default to closed just in case

                Object rawTag = marker.getTag();

                //tag has to be a pair
                if (rawTag instanceof android.util.Pair) {
                    android.util.Pair<String, Boolean> markerData = (android.util.Pair<String, Boolean>) rawTag;

                    if (markerData.first != null) {
                        id = markerData.first; //nooks unique id
                    }
                    if (markerData.second != null) {
                        isOpen247 = markerData.second; //24/7 status
                    }
                }

                //now show popup using the safely unpacked data
                showNookPopup(id, nookName, marker.getSnippet(), marker.getPosition(), isOpen247);

                return true;
            }

            return false;
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

    private void addNookPin(String id, LatLng position, String title, String snippet, int colorInt, boolean isOpen247) {
        BitmapDescriptor customIcon = getCustomOutlinedPin(colorInt);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(title)
                .snippet(snippet)
                .icon(customIcon);

        //create the map pin and save it to a variable called marker
        Marker marker = mMap.addMarker(markerOptions);

        //stick 24/7 bool and nook id to pin
        if (marker != null) {
            marker.setTag(new Pair<>(id, isOpen247)); //true or false stuck to map pin. true means open
        }
    }

    private void loadNooksFromFirebase() {
        db.collection("nooks").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("FirebaseError", "Error: " + error.getMessage());
                return;
            }

            if (value != null) {
                //save all nooks to local cache
                allNooksList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    allNooksList.add(doc);
                }

                //tell map to draw them in and apply the search filter if active
                refreshMapPins();
            }
        });
    }

    //method that draws and filters the pins
    private void refreshMapPins() {
        if (mMap == null) return;
        mMap.clear(); //clear off all pins from map. nice clean slate

        //if in middle of adding a new nook, put the blue map pin back in
        if (isPickingLocation && tempMarker != null) {
            tempMarker = mMap.addMarker(new MarkerOptions()
                    .position(tempMarker.getPosition())
                    .title("Confirm Location?")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        //now look at local cached list of nooks and look at each nook in it
        for (QueryDocumentSnapshot nook : allNooksList) {
            //checking by nook name
            String name = nook.getString("name");
            if (name == null) name = "";

            //filter logic: if there is a search query, skip this Nook if it doesn't match the search
            if (!currentSearchQuery.isEmpty() && !name.toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                continue; //skips to the next nook in the loop
            }

            //if it meets the search. or if theres no search at all and we want them all, draw it in
            //get its details
            String id = nook.getId();
            String nookType = nook.getString("nookType");
            Double lat = nook.getDouble("lat");
            Double lng = nook.getDouble("lng");
            Boolean isOpen247 = nook.getBoolean("isOpen247");
            if (isOpen247 == null) isOpen247 = false;

            String hexColourStr = nook.getString("colour");
            int colourInt = Color.parseColor("#1E88E5"); //default blue

            if (hexColourStr != null) {
                try {
                    colourInt = Color.parseColor(hexColourStr);
                } catch (IllegalArgumentException e) {
                    Log.e("MapFragment", "Couldn't get colour: " + hexColourStr);
                }
            }

            //make it if we have coords and put in right place on map
            if (lat != null && lng != null) {
                LatLng pos = new LatLng(lat, lng);
                addNookPin(id, pos, name, nookType, colourInt, isOpen247);
            }
        }
    }


    private void showNookPopup(String id, String name, String type, LatLng nookLatLng, boolean isOpen247) {

        //safety check. only do this if NOT adding new nook
        if (!isPickingLocation) {

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

            //inflate the bottom sheet as we know where it is now
            View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_nook_popup, null);

            //basic info in popup
            ((TextView) sheetView.findViewById(R.id.tvNookName)).setText(name);
            ((TextView) sheetView.findViewById(R.id.tvNookType)).setText(type);

            //distance cal
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null && sheetView != null) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                nookLatLng.latitude, nookLatLng.longitude, results);

                        float metres = results[0];
                        String distance = (metres < 1000) ? (int) metres + "m away" : String.format("%.1f km away", metres / 1000f);

                        TextView tvDistance = sheetView.findViewById(R.id.tvDistance);
                        if (tvDistance != null) tvDistance.setText(distance);
                    }
                });
            }

            //tell user if its likely to be open
            TextView tvPrompt = sheetView.findViewById(R.id.tvAccessPrompt);
            if (isOpen247) {
                tvPrompt.setText("This Nook is open 24/7!");
                tvPrompt.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
            } else {
                tvPrompt.setText("Hours unconfirmed by Nook creator - check details before visiting!");
                tvPrompt.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary));
            }

            //logic to see the teeny map
            com.google.android.gms.maps.MapView miniMap = sheetView.findViewById(R.id.miniMapContainer);
            if (miniMap != null) {
                //tell it to wakey shakey if it can find the mini map container space
                miniMap.onCreate(null);
                miniMap.onResume();

                //user cant do anything with it like clicking/moving about
                miniMap.getMapAsync(googleMap -> {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nookLatLng, 15f));
                    googleMap.addMarker(new MarkerOptions().position(nookLatLng));
                    googleMap.getUiSettings().setAllGesturesEnabled(false);
                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                });
            }

            //click on it to open the full nook details activity
            sheetView.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(getActivity(), NookDetailsActivity.class);
                intent.putExtra("nookName", name);
                intent.putExtra("NOOK_ID", id);
                startActivity(intent);
            });

            //show
            bottomSheetDialog.setContentView(sheetView);
            bottomSheetDialog.show();
        }
    }

    private BitmapDescriptor getCustomOutlinedPin(int colorInt) {
        //load in new layaered xml
        android.graphics.drawable.LayerDrawable layerDrawable =
                (android.graphics.drawable.LayerDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.bg_pin_outline);

        if (layerDrawable == null) return BitmapDescriptorFactory.defaultMarker();

        //get background layer and tint it
        Drawable background = layerDrawable.findDrawableByLayerId(R.id.pin_background);
        DrawableCompat.setTint(DrawableCompat.wrap(background).mutate(), colorInt);

        //set size. draw it into a bitmap
        int targetWidth = 130;
        int targetHeight = 180;
        layerDrawable.setBounds(0, 0, targetWidth, targetHeight);

        //create bitmap at new larger size
        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}