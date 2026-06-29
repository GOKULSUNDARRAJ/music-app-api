package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.Saalai.SalaiMusicApp.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class CountryDialogAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private JSONArray countries;
    private JSONArray filteredCountries;
    private final OnCountrySelectListener listener;

    public interface OnCountrySelectListener {
        void onCountrySelected(int position, JSONArray jsonArray);
    }

    public CountryDialogAdapter(Context context, JSONArray countries, OnCountrySelectListener listener) {
        this.context = context;
        this.countries = countries;
        this.filteredCountries = countries;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return filteredCountries.length();
    }

    @Override
    public Object getItem(int position) {
        return filteredCountries.optJSONObject(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_country, parent, false);
            holder = new ViewHolder();
            holder.countryName = convertView.findViewById(R.id.txtCountryName);
            holder.countryId = convertView.findViewById(R.id.txtCountryId);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        JSONObject obj = filteredCountries.optJSONObject(position);
        if (obj != null) {
            String name = obj.optString("CountryName");
            String id = obj.optString("CountryId");

            holder.countryName.setText(name);
            holder.countryId.setText("+" +id);
        }

        convertView.setOnClickListener(v -> listener.onCountrySelected(position, filteredCountries));

        return convertView;
    }

    static class ViewHolder {
        TextView countryName;
        TextView countryId;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                JSONArray filteredArray = new JSONArray();

                if (constraint == null || constraint.length() == 0) {
                    results.values = countries;
                    results.count = countries.length();
                } else {
                    String filterString = constraint.toString().toLowerCase();

                    for (int i = 0; i < countries.length(); i++) {
                        JSONObject obj = countries.optJSONObject(i);
                        if (obj != null) {
                            String name = obj.optString("CountryName").toLowerCase();
                            if (name.contains(filterString)) {
                                filteredArray.put(obj);
                            }
                        }
                    }
                    results.values = filteredArray;
                    results.count = filteredArray.length();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredCountries = (JSONArray) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
