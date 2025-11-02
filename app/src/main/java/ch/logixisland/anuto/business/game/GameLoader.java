package ch.logixisland.anuto.business.game;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.EntityRegistry;
import ch.logixisland.anuto.engine.logic.loop.ErrorListener;
import ch.logixisland.anuto.engine.logic.map.GameMap;
import ch.logixisland.anuto.engine.logic.map.PlateauInfo;
import ch.logixisland.anuto.engine.logic.map.WaveInfo;
import ch.logixisland.anuto.engine.logic.persistence.GamePersister;
import ch.logixisland.anuto.engine.render.Viewport;
import ch.logixisland.anuto.entity.plateau.Plateau;
import ch.logixisland.anuto.util.container.KeyValueStore;

public class GameLoader implements ErrorListener {

    private static final String TAG = GameLoader.class.getSimpleName();

    public interface Listener {
        void gameLoaded();
    }

    private final Context mContext;
    private final GameEngine mGameEngine;
    private final GamePersister mGamePersister;
    private final Viewport mViewport;
    private final EntityRegistry mEntityRegistry;
    private final MapRepository mMapRepository;
    private final SaveGameRepository mSaveGameRepository;
    private String mCurrentMapId;

    private final SaveGameMigrator mSaveGameMigrator = new SaveGameMigrator();
    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();

    public GameLoader(Context context, GameEngine gameEngine, GamePersister gamePersister,
                      Viewport viewport, EntityRegistry entityRegistry, MapRepository mapRepository,
                      SaveGameRepository saveGameRepository) {
        mContext = context;
        mGameEngine = gameEngine;
        mGamePersister = gamePersister;
        mViewport = viewport;
        mEntityRegistry = entityRegistry;
        mMapRepository = mapRepository;
        mSaveGameRepository = saveGameRepository;

        mGameEngine.registerErrorListener(this);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public String getCurrentMapId() {
        return mCurrentMapId;
    }

    public void restart() {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(this::restart);
            return;
        }

        if (mCurrentMapId == null) {
            return;
        }

        loadMap(mCurrentMapId);
    }

    public void autoLoadGame() {
        File autoSaveStateFile = mSaveGameRepository.getAutoSaveStateFile();

        if (autoSaveStateFile.exists()) {
            loadGame(autoSaveStateFile);
        } else {
            Log.i(TAG, "No auto save game file not found.");
            loadMap(mMapRepository.getDefaultMapId());
        }
    }

    public void loadGame(final File stateFile) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(() -> loadGame(stateFile));
            return;
        }

        Log.i(TAG, "Loading game...");
        KeyValueStore gameState;

        try {
            FileInputStream inputStream = new FileInputStream(stateFile);
            gameState = KeyValueStore.fromStream(inputStream);
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not load game!", e);
        }

        if (!mSaveGameMigrator.migrate(gameState)) {
            Log.w(TAG, "Failed to migrate save game!");
            loadMap(mMapRepository.getDefaultMapId());
            return;
        }

        mCurrentMapId = gameState.getString("mapId");
        initializeGame(mCurrentMapId, gameState);
    }

    public void loadMap(final String mapId) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(() -> loadMap(mapId));
            return;
        }

        mCurrentMapId = mapId;
        initializeGame(mCurrentMapId, null);
    }

    private void initializeGame(String mapId, KeyValueStore gameState) {
        Log.d(TAG, "Initializing game...");
        mGameEngine.clear();

        MapInfo mapInfo = mMapRepository.getMapById(mapId);
        GameMap map = new GameMap(KeyValueStore.fromResources(mContext.getResources(), mapInfo.getMapDataResId()));
        mGameEngine.setGameMap(map);

        // 修改点：增强波次数据加载的错误处理和日志
        KeyValueStore waveData = null;
        try {
            waveData = KeyValueStore.fromResources(mContext.getResources(), R.raw.waves_config);
            Log.d(TAG, "Wave data loaded successfully from R.raw.waves_config");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load wave data from R.raw.waves_config", e);
            // 可以在这里添加回退逻辑，例如使用默认波次数据
            // 例如：waveData = KeyValueStore.fromResources(mContext.getResources(), R.raw.waves_fallback);
            // 但首先确保资源文件正确，所以这里直接抛出异常或处理错误
            throw new RuntimeException("Critical: Wave data resource not found or invalid!", e);
        }

        if (waveData == null) {
            Log.e(TAG, "Wave data is null after loading");
            throw new RuntimeException("Wave data is null");
        }

        List<WaveInfo> waveInfos = new ArrayList<>();
        try {
            List<KeyValueStore> waveList = waveData.getStoreList("waves");
            if (waveList == null || waveList.isEmpty()) {
                Log.e(TAG, "No 'waves' array found in wave data");
                throw new RuntimeException("Invalid wave data structure: missing 'waves' array");
            }
            for (KeyValueStore data : waveList) {
                waveInfos.add(new WaveInfo(data));
            }
            Log.d(TAG, "Wave infos parsed: " + waveInfos.size() + " waves");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing wave data", e);
            throw new RuntimeException("Failed to parse wave data", e);
        }
        mGameEngine.setWaveInfos(waveInfos);

        mViewport.setGameSize(map.getWidth(), map.getHeight());

        if (gameState != null) {
            mGamePersister.readState(gameState);
        } else {
            mGamePersister.resetState();
            initializeMap(map);
        }

        for (Listener listener : mListeners) {
            listener.gameLoaded();
        }

        Log.d(TAG, "Game loaded successfully.");
    }

    private void initializeMap(GameMap map) {
        for (PlateauInfo info : map.getPlateaus()) {
            Plateau plateau = (Plateau) mEntityRegistry.createEntity(info.getName());
            plateau.setPosition(info.getPosition());
            mGameEngine.add(plateau);
        }
    }

    @Override
    public void error(Exception e, int loopCount) {
        // avoid game not starting anymore because of a somehow corrupt saved game file
        if (loopCount < 10) {
            Log.w(TAG, "Game crashed just after loading, deleting saved game file.");

            //noinspection ResultOfMethodCallIgnored
            mSaveGameRepository.getAutoSaveStateFile().delete();
        }
    }
}
