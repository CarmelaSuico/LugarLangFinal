package com.usc.lugarlangfinal.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.employee.EditEmployee;
import com.usc.lugarlangfinal.models.Employee;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<Employee> employeeList;
    private Context context;

    public EmployeeAdapter(List<Employee> employeeList) {
        this.employeeList = employeeList;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeList.get(position);

        // Binding basic info
        holder.txtName.setText(employee.getName());
        holder.txtRole.setText(employee.getRole());
        holder.txtId.setText("ID: " + employee.getId());
        holder.txtFranchise.setText(employee.getFranchise());

        // Handling assigned unit (Checking for null/empty)
        if (employee.getAssignedUnit() != null && !employee.getAssignedUnit().isEmpty()) {
            holder.txtAssignedUnit.setText("Unit: " + employee.getAssignedUnit());
            holder.txtAssignedUnit.setVisibility(View.VISIBLE);
        } else {
            holder.txtAssignedUnit.setText("Unit: Not Assigned");
        }

        // Icon Logic: Person icon for everyone, but you can swap based on role if you have more drawables
        if (employee.getRole() != null && employee.getRole().equalsIgnoreCase("Driver")) {
            holder.ivEmployeeIcon.setImageResource(R.drawable.person_24px);
        } else {
            // If you have a different icon for conductors, use it here
            holder.ivEmployeeIcon.setImageResource(R.drawable.person_24px);
        }

        // Edit Button Click Logic
        holder.btnEdit.setOnClickListener(v -> {
             Intent intent = new Intent(context, EditEmployee.class);
             intent.putExtra("EMPLOYEE_ID", employee.getId());
             context.startActivity(intent);

            android.widget.Toast.makeText(context, "Editing " + employee.getName(), android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtRole, txtId, txtFranchise, txtAssignedUnit;
        ShapeableImageView ivEmployeeIcon;
        MaterialButton btnEdit;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtname);
            txtRole = itemView.findViewById(R.id.txtrole);
            txtId = itemView.findViewById(R.id.txtid);
            txtFranchise = itemView.findViewById(R.id.txtfranchise);
            txtAssignedUnit = itemView.findViewById(R.id.txtassignedunit);
            ivEmployeeIcon = itemView.findViewById(R.id.employee_icon);
            btnEdit = itemView.findViewById(R.id.imgedit);
        }
    }
}