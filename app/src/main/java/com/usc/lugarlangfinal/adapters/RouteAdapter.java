package com.usc.lugarlangfinal.adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Route;

import java.util.List;

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
        holder.txtCode.setText("Route Code: " + route.getCode());
        holder.txtTerminal1.setText("Terminal 1: " + route.getTerminal1());
        holder.txtTerminal2.setText("Terminal 2: " + route.getTerminal2());
        holder.txtDistnce.setText("Distance: " + route.getDistance());
        holder.txtStatus.setText("Status: " + route.getStatus());
    }


    @Override
    public int getItemCount() {
        return routeList.size();
    }

    public class RouteViewHolder extends RecyclerView.ViewHolder {

        TextView txtCode, txtTerminal1, txtTerminal2, txtDistnce, txtStatus;
        ImageButton imgEdit;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCode = itemView.findViewById(R.id.txtcode);
            txtTerminal1 = itemView.findViewById(R.id.txtterminal1);
            txtTerminal2 = itemView.findViewById(R.id.txtterminal2);
            txtDistnce = itemView.findViewById(R.id.txtdistnce);
            txtStatus = itemView.findViewById(R.id.txtstatus);
            imgEdit = itemView.findViewById(R.id.imgedit);
        }
    }


}
