package ch.logixisland.anuto.business.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaderboardRepository {

    private static final String TAG = LeaderboardRepository.class.getSimpleName();
    private static final String PREFS_NAME = "leaderboard";
    private static final String KEY_ENTRIES = "leaderboard_entries";
    private static final int MAX_ENTRIES_PER_MAP = 10;

    private final SharedPreferences mPreferences;
    private final List<LeaderboardEntry> mEntries;

    public LeaderboardRepository(Context context) {
        mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mEntries = new ArrayList<>();
        loadEntries();
    }

    public void addEntry(String mapId, int score, int wave, int lives) {
        LeaderboardEntry newEntry = new LeaderboardEntry(mapId, score, new Date(), wave, lives);
        mEntries.add(newEntry);

        // 按分数排序并保留每个地图的前10名
        filterAndSortEntries();
        saveEntries();
    }

    public List<LeaderboardEntry> getEntriesForMap(String mapId) {
        List<LeaderboardEntry> mapEntries = new ArrayList<>();
        for (LeaderboardEntry entry : mEntries) {
            if (entry.getMapId().equals(mapId)) {
                mapEntries.add(entry);
            }
        }

        // 按分数降序排列
        Collections.sort(mapEntries, new Comparator<LeaderboardEntry>() {
            @Override
            public int compare(LeaderboardEntry e1, LeaderboardEntry e2) {
                return Integer.compare(e2.getScore(), e1.getScore());
            }
        });

        return mapEntries;
    }

    public List<LeaderboardEntry> getAllEntries() {
        return new ArrayList<>(mEntries);
    }

    public void clearLeaderboard() {
        mEntries.clear();
        saveEntries();
    }

    private void filterAndSortEntries() {
        // 按地图分组
        List<String> mapIds = new ArrayList<>();
        for (LeaderboardEntry entry : mEntries) {
            if (!mapIds.contains(entry.getMapId())) {
                mapIds.add(entry.getMapId());
            }
        }

        List<LeaderboardEntry> filteredEntries = new ArrayList<>();

        // 对每个地图保留前10名
        for (String mapId : mapIds) {
            List<LeaderboardEntry> mapEntries = new ArrayList<>();
            for (LeaderboardEntry entry : mEntries) {
                if (entry.getMapId().equals(mapId)) {
                    mapEntries.add(entry);
                }
            }

            // 按分数降序排序
            Collections.sort(mapEntries, new Comparator<LeaderboardEntry>() {
                @Override
                public int compare(LeaderboardEntry e1, LeaderboardEntry e2) {
                    return Integer.compare(e2.getScore(), e1.getScore());
                }
            });

            // 只保留前10名
            for (int i = 0; i < Math.min(mapEntries.size(), MAX_ENTRIES_PER_MAP); i++) {
                filteredEntries.add(mapEntries.get(i));
            }
        }

        mEntries.clear();
        mEntries.addAll(filteredEntries);
    }

    private void loadEntries() {
        String jsonString = mPreferences.getString(KEY_ENTRIES, "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String mapId = jsonObject.getString("mapId");
                int score = jsonObject.getInt("score");
                long date = jsonObject.getLong("date");
                int wave = jsonObject.getInt("wave");
                int lives = jsonObject.getInt("lives");

                mEntries.add(new LeaderboardEntry(mapId, score, new Date(date), wave, lives));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading leaderboard entries", e);
        }
    }

    private void saveEntries() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (LeaderboardEntry entry : mEntries) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("mapId", entry.getMapId());
                jsonObject.put("score", entry.getScore());
                jsonObject.put("date", entry.getDate().getTime());
                jsonObject.put("wave", entry.getWave());
                jsonObject.put("lives", entry.getLives());
                jsonArray.put(jsonObject);
            }
            mPreferences.edit().putString(KEY_ENTRIES, jsonArray.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving leaderboard entries", e);
        }
    }
}
