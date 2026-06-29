package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.ContactUsResponse;

import java.util.List;

public class ContactUsAdapter extends RecyclerView.Adapter<ContactUsAdapter.ViewHolder> {

    private Context context;
    private List<ContactUsResponse.SupportContact> contactList;
    private static final String TAG = "ContactUsAdapter";

    public ContactUsAdapter(Context context, List<ContactUsResponse.SupportContact> contactList) {
        this.context = context;
        this.contactList = contactList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_support, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactUsResponse.SupportContact contact = contactList.get(position);

        // Handle null country safely
        String country = contact.getCountry();
        if (country != null && !country.trim().isEmpty()) {
            holder.tvCountryName.setText(country.toUpperCase());
        } else {
            holder.tvCountryName.setText("UNKNOWN");
            Log.w(TAG, "Country is null or empty at position: " + position);
        }

        // Handle null phone number safely
        String phoneNumber = contact.getContactNo();
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            holder.tvPhoneNumber.setText(phoneNumber);
        } else {
            holder.tvPhoneNumber.setText("N/A");
            Log.w(TAG, "Phone number is null or empty at position: " + position);
        }

        holder.itemView.setOnClickListener(v -> {
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                makePhoneCall(phoneNumber);
            } else {
                Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Cannot make call: phone number is null or empty");
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

            // Validate phone number
            if (cleanNumber.isEmpty()) {
                Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + cleanNumber));

            // Check if there's an app to handle the intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No dialer app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Cannot make call", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error making phone call: " + e.getMessage(), e);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCountryName;
        TextView tvPhoneNumber;
        ImageView ivPhoneIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCountryName = itemView.findViewById(R.id.tvCountryName);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            ivPhoneIcon = itemView.findViewById(R.id.ivPhoneIcon);
        }
    }
}