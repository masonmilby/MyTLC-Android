package com.milburn.mytlc;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private List<Shift> mShiftArray;
    private List<String> mExpandPos = new ArrayList<>();
    private Context context;
    private LayoutInflater layoutInflater;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextViewDay;
        private TextView mTextViewDayNum;
        private TextView mTextViewHours;
        private TextView mTextViewTime1;
        private TextView mTextViewDept1;
        private TextView mTextViewActivity;

        private ImageView imageArrow;
        private ConstraintLayout constraintExtra;
        private LinearLayout linearLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextViewDay = (TextView)itemView.findViewById(R.id.textview_day_name);
            mTextViewDayNum = (TextView)itemView.findViewById(R.id.textview_day_num);
            mTextViewHours = (TextView)itemView.findViewById(R.id.textview_hours_num);
            mTextViewTime1 = (TextView)itemView.findViewById(R.id.textview_time);
            mTextViewDept1 = (TextView)itemView.findViewById(R.id.textview_dept_1);
            mTextViewActivity = (TextView)itemView.findViewById(R.id.textview_activity);

            imageArrow = (ImageView)itemView.findViewById(R.id.image_drop_down);
            constraintExtra = (ConstraintLayout)itemView.findViewById(R.id.constraintlayout_extra);
            linearLayout = (LinearLayout)itemView.findViewById(R.id.linearlayout);
        }
    }

    public RecyclerAdapter(List<Shift> itemArray, Context con) {
        mShiftArray = itemArray;
        context = con;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cardview_single, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String pos = String.valueOf(position);
        final boolean deptDiff = mShiftArray.get(position).getDeptDiff();
        final boolean actDiff = mShiftArray.get(position).getActDiff();

        holder.mTextViewDay.setText(mShiftArray.get(position).getStartTime("E"));
        holder.mTextViewDayNum.setText(mShiftArray.get(position).getStartTime("d"));
        holder.mTextViewHours.setText(mShiftArray.get(position).getTotalHours().toString());
        holder.mTextViewTime1.setText(mShiftArray.get(position).getCombinedTime());
        holder.mTextViewDept1.setText(mShiftArray.get(position).getDept(0));
        holder.mTextViewActivity.setText(mShiftArray.get(position).getActivity(0));

        holder.constraintExtra.setVisibility(View.GONE);
        holder.imageArrow.setVisibility(View.GONE);

        if (deptDiff || actDiff) {
            holder.linearLayout.removeAllViews();
            int i = -1;
            for (String deptName : mShiftArray.get(position).getDepts()) {
                i++;
                View extraFrag = layoutInflater.inflate(R.layout.extra_time_frag, null);

                TextView time = (TextView)extraFrag.findViewById(R.id.textview_time);
                time.setText(mShiftArray.get(position).getCombinedTime(i));

                TextView dept = (TextView)extraFrag.findViewById(R.id.textview_dept);
                dept.setText(deptName);

                TextView act = (TextView)extraFrag.findViewById(R.id.textview_activity);
                act.setText(mShiftArray.get(position).getActivity(i));

                holder.linearLayout.addView(extraFrag);
            }
        }

        if (mExpandPos.contains(pos) && (deptDiff || actDiff)) {
            holder.constraintExtra.setVisibility(View.VISIBLE);
            holder.imageArrow.setVisibility(View.VISIBLE);
            holder.imageArrow.setRotation(180);
        } else if (deptDiff || actDiff) {
            holder.constraintExtra.setVisibility(View.GONE);
            holder.imageArrow.setVisibility(View.VISIBLE);
            holder.imageArrow.setRotation(0);
        } else {
            holder.imageArrow.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deptDiff || actDiff) {
                    ConstraintLayout constraintExtra = (ConstraintLayout)v.findViewById(R.id.constraintlayout_extra);
                    if (constraintExtra.getVisibility() == View.VISIBLE) {
                        if (mExpandPos.contains(pos)) {
                            mExpandPos.remove(pos);
                        }
                        constraintExtra.setVisibility(View.GONE);
                    } else {
                        mExpandPos.add(pos);
                        constraintExtra.setVisibility(View.VISIBLE);
                    }
                    notifyItemChanged(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mShiftArray.size();
    }
}
