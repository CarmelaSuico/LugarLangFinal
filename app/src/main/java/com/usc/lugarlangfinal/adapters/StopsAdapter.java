package com.usc.lugarlangfinal.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.usc.lugarlangfinal.R;
import java.util.List;

public class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.ViewHolder> {

    private final List<String> stops;
    private final OnStartDragListener mDragStartListener;

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public StopsAdapter(List<String> stops, OnStartDragListener dragStartListener) {
        this.stops = stops;
        this.mDragStartListener = dragStartListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stop_input, parent, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtStopName.setText(stops.get(position));

        if (mDragStartListener != null) {
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            });
        }

        holder.btnRemove.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                stops.remove(currentPos);
                notifyItemRemoved(currentPos);
                notifyItemRangeChanged(currentPos, stops.size());
            }
        });
    }

    @Override public int getItemCount() { return stops != null ? stops.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView txtStopName;
        public final View btnRemove;
        public final ImageView dragHandle;

        public ViewHolder(View v) {
            super(v);
            txtStopName = v.findViewById(R.id.txtStopName);
            btnRemove = v.findViewById(R.id.btnRemoveStop);
            dragHandle = v.findViewById(R.id.ivDragHandle);
        }
    }
}