package com.example.mpa_booknookapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

import java.util.List;

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
            settingsIcon.setOnClickListener(v -> { //if settings is tapped
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
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

                // 1. THIS IS THE FIX: Read the sticky note (the Tag) from the marker
                List<String> accessData = (List<String>) marker.getTag();

                // 2. Pass 'accessData' instead of 'null'
                showNookPopup(nookName, marker.getSnippet(), marker.getPosition(), accessData);

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

    private void addNookPin(LatLng position, String title, String snippet, int colorInt, List<String> accessibility) {
        BitmapDescriptor customIcon = getCustomOutlinedPin(colorInt);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(title)
                .snippet(snippet)
                .icon(customIcon);

        //create map pin and save it to a variable
        com.google.android.gms.maps.model.Marker marker = mMap.addMarker(markerOptions);

        //stick accessibility list
        if (marker != null) {
            marker.setTag(accessibility);
        }
    }

    private void loadNooksFromFirebase() {
        db.collection("nooks").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("FirebaseError", "Error: " + error.getMessage());
                return;
            }

            if (value != null) {
                mMap.clear();
                //if in middle of adding a new nook, put the blue map pin back in
                if (isPickingLocation && tempMarker != null) {
                    tempMarker = mMap.addMarker(new MarkerOptions()
                            .position(tempMarker.getPosition())
                            .title("Confirm Location?")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }

                //look at each nook document in nooks collection in firebase and grab the info
                for (QueryDocumentSnapshot doc : value) {
                    String name = doc.getString("name");
                    String desc = doc.getString("description");
                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");

                    // NEW WAY: Fetch the colour as a String (Text)
                    String hexColorStr = doc.getString("colour");

                    // Set a default colour (Material Blue) just in case a Nook doesn't have one
                    int colorInt = android.graphics.Color.parseColor("#1E88E5");

                    // Safely try to convert the text ("#E53935") into a real Android Color number
                    if (hexColorStr != null) {
                        try {
                            colorInt = android.graphics.Color.parseColor(hexColorStr);
                        } catch (IllegalArgumentException e) {
                            Log.e("MapFragment", "Couldn't parse colour: " + hexColorStr);
                        }
                    }

                    // Add the pin to the map
                    if (lat != null && lng != null) {
                        LatLng pos = new LatLng(lat, lng);

                        // 1. THIS IS NEW: Grab the accessibility list from the doc
                        List<String> accessibilityList = (List<String>) doc.get("accessibility");

                        // 2. THIS WAS MISSING: Actually add the pin and pass the list in!
                        addNookPin(pos, name, desc, colorInt, accessibilityList);
                    }
                }
            }
        });
    }


    private void showNookPopup(String name, String type, LatLng nookLatLng, List<String> accessibility) {

        // 1. SAFETY CHECK FIRST! Only do this if we are NOT adding a new nook.
        if (!isPickingLocation) {

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

            // 2. INFLATE THE VIEW FIRST! (Now the app knows where 'miniMapContainer' is)
            View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_nook_popup, null);

            // 3. SET BASIC INFO
            ((TextView) sheetView.findViewById(R.id.tvNookName)).setText(name);
            ((TextView) sheetView.findViewById(R.id.tvNookType)).setText(type);

            // 4. DISTANCE CALCULATION
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

            // 5. ACCESSIBILITY PROMPT
            TextView tvPrompt = sheetView.findViewById(R.id.tvAccessPrompt);
            if (accessibility != null && accessibility.contains("Open 24/7")) {
                tvPrompt.setText("This nook is open 24/7!");
                tvPrompt.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
            } else {
                tvPrompt.setText("Hours unconfirmed by Nook creator - check details before visiting!");
                tvPrompt.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary));
            }

            // 6. THE NEW MAPVIEW LOGIC (No Fragment Manager needed!)
            com.google.android.gms.maps.MapView miniMap = sheetView.findViewById(R.id.miniMapContainer);
            if (miniMap != null) {
                // We have to tell the MapView to "wake up" manually
                miniMap.onCreate(null);
                miniMap.onResume();

                miniMap.getMapAsync(googleMap -> {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nookLatLng, 15f));
                    googleMap.addMarker(new MarkerOptions().position(nookLatLng));
                    googleMap.getUiSettings().setAllGesturesEnabled(false);
                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                });
            }

            // 7. CLICK TO OPEN FULL DETAILS
            sheetView.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(getActivity(), NookDetailsActivity.class);
                intent.putExtra("nookName", name);
                startActivity(intent);
            });

            // 8. SHOW THE POPUP
            bottomSheetDialog.setContentView(sheetView);
            bottomSheetDialog.show();
        }
    }

    private BitmapDescriptor getCustomOutlinedPin(int colorInt) {
        // 1. Load the new Layered XML
        android.graphics.drawable.LayerDrawable layerDrawable =
                (android.graphics.drawable.LayerDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.bg_pin_outline);

        if (layerDrawable == null) return BitmapDescriptorFactory.defaultMarker();

        // 2. Find ONLY the background layer and tint it
        Drawable background = layerDrawable.findDrawableByLayerId(R.id.pin_background);
        DrawableCompat.setTint(DrawableCompat.wrap(background).mutate(), colorInt);

        // 3. Set the size and draw it into a Bitmap
        int targetWidth = 130;
        int targetHeight = 180;
        layerDrawable.setBounds(0, 0, targetWidth, targetHeight);

        // 4. Create the Bitmap at the new larger size
        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}