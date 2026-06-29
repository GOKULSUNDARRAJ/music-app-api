package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Response.ContactUsResponse;

import java.util.List;

public class SocialContactAdapter extends RecyclerView.Adapter<SocialContactAdapter.ViewHolder> {

    private static final String TAG = "SocialContactAdapter";
    private Context context;
    private List<ContactUsResponse.SocialContact> socialList;

    public SocialContactAdapter(Context context, List<ContactUsResponse.SocialContact> socialList) {
        this.context = context;
        this.socialList = socialList;
        Log.d(TAG, "Adapter created with " + (socialList != null ? socialList.size() : 0) + " items");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called for viewType: " + viewType);
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_social_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (socialList == null || position >= socialList.size()) {
            Log.e(TAG, "Invalid position or null list. Position: " + position);
            return;
        }

        ContactUsResponse.SocialContact socialContact = socialList.get(position);
        Log.d(TAG, "Binding position: " + position +
                ", Type: " + socialContact.getType() +
                ", URL: " + socialContact.getUrl() +
                ", ContactNo: " + socialContact.getContactNo());

        // Set icon based on type
        switch (socialContact.getType().toLowerCase()) {
            case "whatsapp":
                holder.ivSocialIcon.setImageResource(R.drawable.whatsapp);
                holder.tvSocialLabel.setText("WhatsApp");
                holder.tvPhoneNumber.setText(socialContact.getContactNo());
                holder.cardSocial.setCardBackgroundColor(context.getResources().getColor(R.color.green1));
                Log.d(TAG, "WhatsApp item configured");
                break;
            case "viber":
                holder.ivSocialIcon.setImageResource(R.drawable.viber);
                holder.tvSocialLabel.setText("Viber");
                holder.tvPhoneNumber.setText(socialContact.getContactNo());
                holder.cardSocial.setCardBackgroundColor(context.getResources().getColor(R.color.blue1));
                Log.d(TAG, "Viber item configured");
                break;
            default:
                Log.w(TAG, "Unknown social type: " + socialContact.getType());
        }

        // Set click listener with detailed logging
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked at position: " + position);
            Log.d(TAG, "Opening " + socialContact.getType() + " with URL: " + socialContact.getUrl());
            openSocialApp(socialContact.getUrl(), socialContact.getType());
        });

        // Also set click listener on the CardView itself
        holder.cardSocial.setOnClickListener(v -> {
            Log.d(TAG, "CardView clicked at position: " + position);
            Log.d(TAG, "Opening " + socialContact.getType() + " with URL: " + socialContact.getUrl());
            openSocialApp(socialContact.getUrl(), socialContact.getType());
        });

        // Set click listener on phoneLayout
        holder.phoneLayout.setOnClickListener(v -> {
            Log.d(TAG, "PhoneLayout clicked at position: " + position);
            Log.d(TAG, "Opening " + socialContact.getType() + " with URL: " + socialContact.getUrl());
            openSocialApp(socialContact.getUrl(), socialContact.getType());
        });

        // Log view hierarchy for debugging
        holder.itemView.setOnLongClickListener(v -> {
            Log.d(TAG, "View hierarchy for position " + position + ":");
            logViewHierarchy(holder.itemView, 0);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        int count = socialList != null ? socialList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    private void openSocialApp(String url, String type) {
        try {
            Log.d(TAG, "openSocialApp called for type: " + type + ", URL: " + url);
            debugWhatsAppIntent();
            if (url == null || url.isEmpty()) {
                Log.e(TAG, "URL is null or empty for type: " + type);
                Toast.makeText(context, type + " link is not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Remove TEST_MODE - use real detection
            boolean isAppInstalled = false;
            String packageName = "";

            if (type.equalsIgnoreCase("whatsapp")) {
                // Try multiple package names
                if (isAppInstalled("com.whatsapp")) {
                    packageName = "com.whatsapp";
                    isAppInstalled = true;
                } else if (isAppInstalled("com.whatsapp.w4b")) {
                    packageName = "com.whatsapp.w4b";
                    isAppInstalled = true;
                }
            } else if (type.equalsIgnoreCase("viber")) {
                if (isAppInstalled("com.viber.voip")) {
                    packageName = "com.viber.voip";
                    isAppInstalled = true;
                }
            }

            Log.d(TAG, type + " installed: " + isAppInstalled + ", package: " + packageName);

            if (isAppInstalled) {
                // Try to open in WhatsApp app
                Log.d(TAG, "Attempting to open in " + type + " app");

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setPackage(packageName);

                // Debug: Check what activities can handle this
                PackageManager pm = context.getPackageManager();
                List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
                Log.d(TAG, "Activities that can handle intent: " + activities.size());
                for (ResolveInfo info : activities) {
                    Log.d(TAG, "  Package: " + info.activityInfo.packageName);
                }

                // Try with package restriction
                if (intent.resolveActivity(pm) != null) {
                    Log.d(TAG, "Opening " + type + " with package restriction");
                    context.startActivity(intent);
                } else {
                    // Try without package restriction
                    Log.d(TAG, "Trying without package restriction");
                    intent.setPackage(null);
                    if (intent.resolveActivity(pm) != null) {
                        context.startActivity(intent);
                    } else {
                        Log.w(TAG, "No activity can handle this URL");
                        openWhatsAppWeb(url);
                    }
                }
            } else {
                Log.d(TAG, type + " not installed, opening web version");
                openWhatsAppWeb(url);
            }

        } catch (Exception e) {
            String errorMessage = "Unable to open " + type;
            Log.e(TAG, errorMessage, e);
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        }

    }

    private void debugWhatsAppIntent() {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage("com.whatsapp");

        if (intent != null) {
            Log.d(TAG, "WhatsApp launch intent FOUND. App is likely installed.");
            // Also, try to get the application label
            try {
                ApplicationInfo ai = pm.getApplicationInfo("com.whatsapp", 0);
                String appName = pm.getApplicationLabel(ai).toString();
                Log.d(TAG, "WhatsApp app name: " + appName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "Could not get app info.");
            }
        } else {
            Log.d(TAG, "WhatsApp launch intent NOT FOUND.");
        }
    }

    // Add this method
    private boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            Log.d(TAG, packageName + " is INSTALLED");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, packageName + " is NOT installed");
            return false;
        }
    }



    private void openInMobileApp(String url, String packageName, String type) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(packageName);

            PackageManager pm = context.getPackageManager();

            // Check if the app can handle this intent
            if (intent.resolveActivity(pm) != null) {
                Log.d(TAG, "Opening " + type + " in mobile app");
                context.startActivity(intent);
            } else {
                // Try without package restriction
                Log.d(TAG, "Trying without package restriction");
                intent.setPackage(null);
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent);
                } else {
                    Log.w(TAG, "No activity found to handle " + type + " URL");
                    Toast.makeText(context, "Cannot open " + type, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening " + type + " in mobile app", e);
            Toast.makeText(context, "Cannot open " + type, Toast.LENGTH_SHORT).show();
        }
    }

    private void showInstallDialog(String appName, String packageName) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(appName + " Not Installed");
        builder.setMessage(appName + " app is not installed on your device. Would you like to install it from Play Store?");

        builder.setPositiveButton("Install", (dialog, which) -> {
            openPlayStore(packageName);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // If Play Store not available, open in browser
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Play Store", e);
        }
    }

    private void openWhatsAppWeb(String originalUrl) {
        try {
            Log.d(TAG, "Opening WhatsApp Web");

            // Use the original WhatsApp URL but change domain to web.whatsapp.com
            String webUrl = originalUrl;

            if (originalUrl.contains("wa.me/")) {
                // Convert wa.me URL to web.whatsapp.com format
                // Example: https://wa.me/442031373917?text=Hello
                // Becomes: https://web.whatsapp.com/send?phone=442031373917&text=Hello
                String phoneAndQuery = originalUrl.substring(originalUrl.indexOf("wa.me/") + 6);
                String phoneNumber = "";
                String message = "";

                if (phoneAndQuery.contains("?")) {
                    phoneNumber = phoneAndQuery.substring(0, phoneAndQuery.indexOf("?"));
                    String query = phoneAndQuery.substring(phoneAndQuery.indexOf("?") + 1);
                    if (query.contains("text=")) {
                        message = query.substring(query.indexOf("text=") + 5);
                    }
                } else {
                    phoneNumber = phoneAndQuery;
                }

                webUrl = "https://web.whatsapp.com/send?phone=" + phoneNumber;
                if (!message.isEmpty()) {
                    webUrl += "&text=" + message;
                }
            } else if (originalUrl.contains("api.whatsapp.com/")) {
                // Convert api.whatsapp.com URL to web.whatsapp.com format
                webUrl = originalUrl.replace("api.whatsapp.com/", "web.whatsapp.com/");
            }

            Log.d(TAG, "WhatsApp Web URL: " + webUrl);

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error opening WhatsApp Web", e);
            // Fallback to simple WhatsApp Web
            String webUrl = "https://web.whatsapp.com/";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);
        }
    }



    private void logViewHierarchy(View view, int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }

        Log.d(TAG, indent.toString() + view.getClass().getSimpleName() +
                " [id=" + view.getId() +
                ", clickable=" + view.isClickable() +
                ", enabled=" + view.isEnabled() +
                ", visible=" + (view.getVisibility() == View.VISIBLE) + "]");

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                logViewHierarchy(viewGroup.getChildAt(i), depth + 1);
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardSocial;
        ImageView ivSocialIcon;
        TextView tvSocialLabel, tvPhoneNumber;
        View phoneLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSocial = itemView.findViewById(R.id.cardSocial);
            ivSocialIcon = itemView.findViewById(R.id.ivSocialIcon);
            tvSocialLabel = itemView.findViewById(R.id.tvSocialLabel);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            phoneLayout = itemView.findViewById(R.id.phoneLayout);

            Log.d(TAG, "ViewHolder created - Views found: " +
                    (cardSocial != null) + ", " +
                    (ivSocialIcon != null) + ", " +
                    (tvSocialLabel != null) + ", " +
                    (tvPhoneNumber != null) + ", " +
                    (phoneLayout != null));
        }
    }
}