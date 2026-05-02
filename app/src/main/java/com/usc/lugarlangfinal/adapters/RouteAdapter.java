package com.usc.lugarlangfinal.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Route;
import com.usc.lugarlangfinal.route.RouteMoreDetails;

import java.util.List;
import java.util.Locale;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<Route> routeList;

    public RouteAdapter(List<Route> routeList) {
        this.routeList = routeList;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);

        holder.txtCode.setText(route.getRouteCode());
        holder.txtTerminal1.setText(route.getTerminal1());
        holder.txtTerminal2.setText(route.getTerminal2());
        holder.txtStatus.setText(route.getStatus());
        holder.txtDistance.setText(String.format(Locale.getDefault(), "%.2f KM", route.getDistance()));

        holder.btnMoreDetails.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RouteMoreDetails.class);
            intent.putExtra("ROUTE_CODE", route.getRouteCode());
            intent.putExtra("COMPANY", route.getCompany()); // Uses @PropertyName("Company") from model
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return routeList != null ? routeList.size() : 0;
    }

    public static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView txtCode, txtTerminal1, txtTerminal2, txtDistance, txtStatus;
        MaterialButton btnMoreDetails;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCode = itemView.findViewById(R.id.txtcode);
            txtStatus = itemView.findViewById(R.id.txtstatus);
            txtTerminal1 = itemView.findViewById(R.id.txtterminal1);
            txtTerminal2 = itemView.findViewById(R.id.txtterminal2);
            txtDistance = itemView.findViewById(R.id.txtdistance);
            btnMoreDetails = itemView.findViewById(R.id.btnmoredetails);
        }
    }
}
