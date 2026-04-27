package com.usc.lugarlangfinal.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.employee.EditEmployee;
import com.usc.lugarlangfinal.models.Employee;

import org.w3c.dom.Text;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<Employee> employeeList;

    public EmployeeAdapter(List<Employee> employeeList) {
        this.employeeList = employeeList;
    }

    public void setEmployeeList(List<Employee> employeeList) {
        this.employeeList = employeeList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeList.get(position);
        holder.txtId.setText("Employee ID: " + employee.getId());
        holder.txtName.setText("Name: " + employee.getName());
        holder.txtRole.setText("Role: " + employee.getRole());
        holder.txtAssignedUnit.setText("Assigned Unit: " + employee.getAssignedUnit());
        holder.txtFranchise.setText("Franchise: " + employee.getFranchise());
        holder.imgEdit.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EditEmployee.class);
            intent.putExtra("EMPLOYEE_ID", employee.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public class EmployeeViewHolder extends RecyclerView.ViewHolder {

        TextView txtId, txtName, txtRole, txtAssignedUnit, txtFranchise;
        ImageButton imgEdit;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            txtId = itemView.findViewById(R.id.txtid);
            txtName = itemView.findViewById(R.id.txtname);
            txtRole = itemView.findViewById(R.id.txtrole);
            txtAssignedUnit = itemView.findViewById(R.id.txtassignedunit);
            txtFranchise = itemView.findViewById(R.id.txtfranchise);
            imgEdit = itemView.findViewById(R.id.imgedit);
        }
    }

}
