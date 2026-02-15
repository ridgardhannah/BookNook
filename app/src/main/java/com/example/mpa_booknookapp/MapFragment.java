package com.example.mpa_booknookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MapFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //find all the pins we have for now
        View pinLib = view.findViewById(R.id.pin_library);
        View pinCastle = view.findViewById(R.id.pin_castle_street);
        View pinPhone = view.findViewById(R.id.pin_phonebox);

        //click listeners with specific details
        pinLib.setOnClickListener(v -> showNookPopup("Derby Central Library", "Public Library", "0.2 km"));
        pinCastle.setOnClickListener(v -> showNookPopup("Castle Street Nook", "Book Nook Box", "0.4 km"));
        pinPhone.setOnClickListener(v -> showNookPopup("The Old Phonebox", "Converted Nook", "1.1 km"));

        return view;
    }

    //update the method to take more details
    private void showNookPopup(String name, String type, String dist) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_nook_popup, null);

        //set the specific data
        ((TextView) sheetView.findViewById(R.id.tvNookName)).setText(name);
        ((TextView) sheetView.findViewById(R.id.tvNookType)).setText(type);
        ((TextView) sheetView.findViewById(R.id.tvDistance)).setText(dist + " away");

        //find view details button inside nook details popup.
        //tell it to open the new screen
        sheetView.findViewById(R.id.btnViewDetails).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the popup so its gone when we come back to this
                bottomSheetDialog.dismiss();

                //now launch the full details screen
                //map fragment is inside mainactivity so cant use "this"
                //have to use getActivity() because the fragment needs permission from the activity to start the new screen)
                Intent intent = new Intent(getActivity(), NookDetailsActivity.class);
                startActivity(intent);
            }
        });

        //show popup to user
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

}