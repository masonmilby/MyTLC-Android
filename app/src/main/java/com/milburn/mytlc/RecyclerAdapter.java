package com.milburn.mytlc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private List<Shift> mShiftArray = new ArrayList<>();
    private List<String> mExpandPos = new ArrayList<>();
    private Context context;
    private LayoutInflater layoutInflater;
    private SharedPreferences sharedPreferences;

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

        private TextView mTextViewSpan;
        private TextView mTextViewWeekHours;
        private TextView mTextViewWeekPay;

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

            mTextViewSpan = (TextView)itemView.findViewById(R.id.textview_dates);
            mTextViewWeekHours = (TextView)itemView.findViewById(R.id.textview_hours);
            mTextViewWeekPay = (TextView)itemView.findViewById(R.id.textview_pay);
        }
    }

    public RecyclerAdapter(List<Shift> itemArray, Context con) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(con);
        if (sharedPreferences.getBoolean("display_past", false)) {
            Credentials credentials = new Credentials(con);
            mShiftArray.addAll(credentials.getPastSchedule());
        }
        mShiftArray.addAll(itemArray);
        addDividers();
        context = con;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        if (mShiftArray.get(position) == null) {
            return R.layout.item_cardview_totalweek;
        }
        return R.layout.item_cardview_single;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        if (mShiftArray.get(position) == null) {
            String[] calcs = getTotals(position);
            holder.mTextViewSpan.setText(calcs[1]);
            holder.mTextViewWeekHours.setText(calcs[0]);
            holder.mTextViewWeekPay.setText(calcs[2]);
            return;
        } else {
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
    }

    @Override
    public int getItemCount() {
        return mShiftArray.size();
    }

    private void addDividers() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);

        calendar.clear(Calendar.HOUR);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.add(Calendar.DATE, 7);

        int i = -1;
        List<Integer> posList = new ArrayList<>();
        for (Shift shift : mShiftArray) {
            i++;
            if (shift.getStartTime().getTime() > calendar.getTime().getTime()) {
                posList.add(i);
                calendar.add(Calendar.DATE, 7);
            }
        }
        for (Integer pos : posList) {
            mShiftArray.add(pos, null);
        }
        if (mShiftArray.get(mShiftArray.size()-1) != null) {
            mShiftArray.add(mShiftArray.size(), null);
        }
    }

    private String[] getTotals(Integer position) {
        int i = -1;
        List<Shift> list = new ArrayList<>();
        for (Shift shift : mShiftArray) {
            i++;
            if (i < position) {
                list.add(shift);
            } else {
                break;
            }
        }

        float hours = 0;
        for (Shift shift : list) {
            if (shift != null) {
                hours+= shift.getTotalHours();
            }
        }

        Date dayInWeek = list.get(list.size()-1).getStartTime();

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.setTime(dayInWeek);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        Date first = calendar.getTime();
        calendar.add(Calendar.DATE, 6);
        Date last = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("M'/'d");
        String dates = dateFormat.format(first) + "–" + dateFormat.format(last);
        String totalHours = String.valueOf(hours) + " Hours";

        PrefManager pm = new PrefManager(context, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                //
            }
        });
        Float pay = Float.valueOf(sharedPreferences.getString(pm.key_pay, ""));
        Float tax = Float.valueOf(sharedPreferences.getString(pm.key_tax, ""));
        DecimalFormat df = new DecimalFormat("0.00");
        String finalPay = "$" + String.valueOf(df.format((pay*hours)-((pay*hours)*(tax/100.0))));

        return new String[]{totalHours, dates, finalPay};
    }
}
