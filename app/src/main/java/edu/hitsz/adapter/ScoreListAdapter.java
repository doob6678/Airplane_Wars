package edu.hitsz.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.R;
import edu.hitsz.dao.ScoreRecord;

/**
 * 排行榜 ListView 适配器，负责数据到条目视图的绑定。
 */
public class ScoreListAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<ScoreRecord> data = new ArrayList<>();

    public ScoreListAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<ScoreRecord> scores) {
        data.clear();
        if (scores != null) {
            data.addAll(scores);
        }
        notifyDataSetChanged();
    }

    public ScoreRecord getItemRecord(int position) {
        return data.get(position);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_score_record, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ScoreRecord record = data.get(position);
        holder.rankText.setText(String.valueOf(position + 1));
        holder.playerText.setText(record.getPlayerName());
        holder.scoreText.setText(String.valueOf(record.getScore()));
        holder.timeText.setText(record.getFormattedTime());
        return convertView;
    }

    private static class ViewHolder {
        final TextView rankText;
        final TextView playerText;
        final TextView scoreText;
        final TextView timeText;

        ViewHolder(View itemView) {
            rankText = itemView.findViewById(R.id.tv_rank);
            playerText = itemView.findViewById(R.id.tv_player);
            scoreText = itemView.findViewById(R.id.tv_score);
            timeText = itemView.findViewById(R.id.tv_time);
        }
    }
}
