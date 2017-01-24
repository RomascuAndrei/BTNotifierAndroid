/*
 * Copyright (C) 2015 - Holy Lobster
 *
 * Nuntius is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Nuntius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.²
 *
 * You should have received a copy of the GNU General Public License
 * along with Nuntius. If not, see <http://www.gnu.org/licenses/>.
 */

package org.holylobster.nuntius.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.holylobster.nuntius.R;

import java.util.ArrayList;
import java.util.List;


public class AppBlacklistAdapter extends RecyclerView.Adapter<AppBlacklistAdapter.ViewHolder> {
    private ArrayList<ApplicationInfo> appInfos;
    private PackageManager pm;
    private boolean deleteButton;

    public OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        public void onItemClick(View view , int position);
    }

    public void SetOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public AppBlacklistAdapter(Context c, List<ApplicationInfo> packages, boolean deleteButton) {
        this.pm = c.getPackageManager();
        this.appInfos = new ArrayList<>(packages);
        this.deleteButton = deleteButton;
    }

    public void refresh(List<ApplicationInfo> packages){
        this.appInfos = new ArrayList<>(packages);
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AppBlacklistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.application_item, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(pm.getApplicationLabel(appInfos.get(position)));
        holder.imageView.setImageDrawable(pm.getApplicationIcon(appInfos.get(position)));
        if (deleteButton) {
            holder.deleteButtonView.setVisibility(View.VISIBLE);
        }else{
            holder.deleteButtonView.setVisibility(View.GONE);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return appInfos.size();
    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // each data item is just a string in this case
        public TextView textView;
        public ImageView imageView;
        private ViewGroup deleteButtonView;

        public ViewHolder(View v) {
            super(v);
            textView = (TextView) itemView.findViewById(R.id.title);
            imageView = (ImageView) itemView.findViewById(R.id.icon);
            deleteButtonView = (ViewGroup) itemView.findViewById(R.id.deleteButton);
            if (deleteButton){
                deleteButtonView.setOnClickListener(this);
            } else {
                v.setOnClickListener(this);
            }

        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(v, getPosition());
            }
        }
    }

}
