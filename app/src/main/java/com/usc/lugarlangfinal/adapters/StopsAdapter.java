package com.usc.lugarlangfinal.adapters;

import android.annotation.SuppressLint;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.usc.lugarlangfinal.R;
import java.util.List;

public class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.ViewHolder> {
    private List<String> stops;
    private OnStartDragListener mDragStartListener; // Interface reference

    // 1. Create the Interface
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public StopsAdapter(List<String> stops, OnStartDragListener dragStartListener) {
        this.stops = stops;
        this.mDragStartListener = dragStartListener;
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_stop_input, p, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        h.editStop.setText(stops.get(pos));

        // Update list as user types
        h.editStop.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                int currentPos = h.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    stops.set(currentPos, s.toString());
                }
            }
        });

        // 2. Trigger drag when the ivDragHandle is touched
        h.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(h);
            }
            return false;
        });

        h.btnRemove.setOnClickListener(v -> {
            int currentPos = h.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                stops.remove(currentPos);
                notifyItemRemoved(currentPos);
            }
        });
    }

    @Override public int getItemCount() { return stops.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        EditText editStop;
        ImageButton btnRemove;
        ImageView dragHandle; // Reference to your drag icon

        public ViewHolder(View v) {
            super(v);
            editStop = v.findViewById(R.id.editStopName);
            btnRemove = v.findViewById(R.id.btnRemoveStop);
            dragHandle = v.findViewById(R.id.ivDragHandle); // Ensure this ID matches your XML
        }
    }
}