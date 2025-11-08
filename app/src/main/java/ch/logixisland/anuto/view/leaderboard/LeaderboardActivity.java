package ch.logixisland.anuto.view.leaderboard;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.logixisland.anuto.AnutoApplication;
import ch.logixisland.anuto.GameFactory;
import ch.logixisland.anuto.R;
import ch.logixisland.anuto.business.game.LeaderboardEntry;
import ch.logixisland.anuto.business.game.LeaderboardRepository;
import ch.logixisland.anuto.business.game.MapRepository;
import ch.logixisland.anuto.engine.theme.ActivityType;
import ch.logixisland.anuto.view.AnutoActivity;

public class LeaderboardActivity extends AnutoActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private final LeaderboardRepository mLeaderboardRepository;
    private final MapRepository mMapRepository;

    private Spinner mSpinnerMaps;
    private ListView mListView;
    private Button mBtnClear;
    private Button mBtnClose;
    private TextView mTxtNoEntries;

    private LeaderboardAdapter mAdapter;
    private List<LeaderboardEntry> mCurrentEntries;

    public LeaderboardActivity() {
        GameFactory factory = AnutoApplication.getInstance().getGameFactory();
        mLeaderboardRepository = factory.getLeaderboardRepository();
        mMapRepository = factory.getMapRepository();
    }

    @Override
    protected ActivityType getActivityType() {
        return ActivityType.Popup;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        mSpinnerMaps = findViewById(R.id.spinner_maps);
        mListView = findViewById(R.id.list_view);
        mBtnClear = findViewById(R.id.btn_clear);
        mBtnClose = findViewById(R.id.btn_close);
        mTxtNoEntries = findViewById(R.id.txt_no_entries);

        mBtnClear.setOnClickListener(this);
        mBtnClose.setOnClickListener(this);

        setupSpinner();
        setupListView();
        updateEntries();
    }

    private void setupSpinner() {
        List<String> mapNames = new ArrayList<>();
        mapNames.add(getString(R.string.leaderboard_map_all)); // "All Maps"

        for (ch.logixisland.anuto.business.game.MapInfo mapInfo : mMapRepository.getMapInfos()) {
            mapNames.add(getString(mapInfo.getMapNameResId()));
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mapNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMaps.setAdapter(spinnerAdapter);
        mSpinnerMaps.setOnItemSelectedListener(this);
    }

    private void setupListView() {
        mCurrentEntries = new ArrayList<>();
        mAdapter = new LeaderboardAdapter(this, mCurrentEntries);
        mListView.setAdapter(mAdapter);
    }

    private void updateEntries() {
        mCurrentEntries.clear();

        int selectedPosition = mSpinnerMaps.getSelectedItemPosition();
        if (selectedPosition == 0) {
            // "All Maps" selected
            mCurrentEntries.addAll(mLeaderboardRepository.getAllEntries());
        } else {
            // Specific map selected
            String selectedMapId = mMapRepository.getMapInfos().get(selectedPosition - 1).getMapId();
            mCurrentEntries.addAll(mLeaderboardRepository.getEntriesForMap(selectedMapId));
        }

        mAdapter.notifyDataSetChanged();

        // Show/hide no entries message
        if (mCurrentEntries.isEmpty()) {
            mTxtNoEntries.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        } else {
            mTxtNoEntries.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mBtnClear) {
            mLeaderboardRepository.clearLeaderboard();
            updateEntries();
            return;
        }

        if (view == mBtnClose) {
            finish();
            return;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateEntries();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    private static class LeaderboardAdapter extends android.widget.BaseAdapter {
        private final LeaderboardActivity mActivity;
        private final List<LeaderboardEntry> mEntries;
        private final SimpleDateFormat mDateFormat;

        public LeaderboardAdapter(LeaderboardActivity activity, List<LeaderboardEntry> entries) {
            mActivity = activity;
            mEntries = entries;
            mDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public LeaderboardEntry getItem(int position) {
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = android.view.LayoutInflater.from(mActivity).inflate(R.layout.item_leaderboard, parent, false);
            }

            LeaderboardEntry entry = getItem(position);

            TextView tvRank = view.findViewById(R.id.tv_rank);
            TextView tvScore = view.findViewById(R.id.tv_score);
            TextView tvWave = view.findViewById(R.id.tv_wave);
            TextView tvLives = view.findViewById(R.id.tv_lives);
            TextView tvDate = view.findViewById(R.id.tv_date);

            tvRank.setText(mActivity.getString(R.string.leaderboard_entry_format, position + 1));
            tvScore.setText(String.valueOf(entry.getScore()));
            tvWave.setText(String.valueOf(entry.getWave()));
            tvLives.setText(String.valueOf(entry.getLives()));
            tvDate.setText(mDateFormat.format(entry.getDate()));

            return view;
        }
    }
}
