package ch.logixisland.anuto.engine.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import ch.logixisland.anuto.engine.logic.map.MapPath;
import ch.logixisland.anuto.util.math.Vector2;
import android.util.Log;

import java.util.List;

public class PathDrawable implements Drawable {

    private final List<MapPath> mPaths;
    private final Paint mPathPaint;
    private final Paint mPointPaint;
    private final Paint mStartPointPaint; // 起始点画笔（蓝色填充）
    private final Paint mStartCirclePaint; // 起始点外圈画笔（白色边框）
    private boolean mVisible;

    public PathDrawable(List<MapPath> paths) {
        mPaths = paths;

        // 路径线条的画笔 - 半透明红色
        mPathPaint = new Paint();
        mPathPaint.setColor(0x80FF0000); // 半透明红色，50%透明度
        mPathPaint.setStrokeWidth(0.2f); // 路径线宽
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setAntiAlias(true);

        // 路径点的画笔 - 亮绿色
        mPointPaint = new Paint();
        mPointPaint.setColor(0xFF00FF00); // 亮绿色，完全不透明
        mPointPaint.setStrokeWidth(0.3f);
        mPointPaint.setAntiAlias(true);

        // 起始点内圈画笔 - 更明显的蓝色
        mStartPointPaint = new Paint();
        mStartPointPaint.setColor(0xFFFF0000); // 改为亮红色，更容易看到
        mStartPointPaint.setStyle(Paint.Style.FILL);
        mStartPointPaint.setAntiAlias(true);

        // 起始点外圈画笔 - 更粗的白色边框
        mStartCirclePaint = new Paint();
        mStartCirclePaint.setColor(0xFFFFFFFF); // 白色
        mStartCirclePaint.setStrokeWidth(0.3f); // 增加边框宽度
        mStartCirclePaint.setStyle(Paint.Style.STROKE);
        mStartCirclePaint.setAntiAlias(true);

        mVisible = true;

        // 添加初始化日志
        Log.d("PathDrawable", "PathDrawable initialized with " + (paths != null ? paths.size() : 0) + " paths");
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
        Log.d("PathDrawable", "Visibility set to: " + visible);
    }

    public boolean isVisible() {
        return mVisible;
    }

    @Override
    public int getLayer() {
        return -10; // 较低图层，确保在背景之上但在其他实体之下
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mVisible) {
            Log.d("PathDrawable", "Not visible, skipping draw");
            return;
        }

        if (mPaths == null) {
            Log.d("PathDrawable", "Paths are null, skipping draw");
            return;
        }

        Log.d("PathDrawable", "Starting draw with " + mPaths.size() + " paths");

        int pathCount = 0;
        int pointCount = 0;

        for (MapPath path : mPaths) {
            pathCount++;
            List<Vector2> wayPoints = path.getWayPoints();
            if (wayPoints == null) {
                Log.d("PathDrawable", "WayPoints is null for path " + pathCount);
                continue;
            }

            if (wayPoints.size() < 2) {
                Log.d("PathDrawable", "Way points less than 2, skipping path. Count: " + wayPoints.size());
                continue;
            }

            Log.d("PathDrawable", "Drawing path " + pathCount + " with " + wayPoints.size() + " way points");

            // 绘制路径线段
            for (int i = 0; i < wayPoints.size() - 1; i++) {
                Vector2 start = wayPoints.get(i);
                Vector2 end = wayPoints.get(i + 1);
                canvas.drawLine(start.x(), start.y(), end.x(), end.y(), mPathPaint);
            }

            // 绘制路径点（绿色）
            for (int i = 0; i < wayPoints.size(); i++) {
                Vector2 point = wayPoints.get(i);
                canvas.drawPoint(point.x(), point.y(), mPointPaint);
                pointCount++;
            }

            // 在起始位置（敌人生成位置）绘制明显的标记
            if (!wayPoints.isEmpty()) {
                Vector2 startPoint = wayPoints.get(0); // 第一个点就是敌人生成位置

                Log.d("PathDrawable", "Start point coordinates: x=" + startPoint.x() + ", y=" + startPoint.y());

                // 大幅增加标记尺寸
                float outerRadius = 1.5f; // 外圈半径 - 大幅增加
                float innerRadius = 1.2f; // 内圈半径 - 大幅增加

                // 绘制白色外圈（边框）
                canvas.drawCircle(startPoint.x(), startPoint.y(), outerRadius, mStartCirclePaint);

                // 绘制红色内圈（填充）- 改为红色更容易看到
                canvas.drawCircle(startPoint.x(), startPoint.y(), innerRadius, mStartPointPaint);

                Log.d("PathDrawable", "Drawn enhanced start marker at: " + startPoint.x() + ", " + startPoint.y() +
                        " with radii: outer=" + outerRadius + ", inner=" + innerRadius);
            }
        }

        Log.d("PathDrawable", "Finished drawing. Total paths: " + pathCount + ", total points: " + pointCount);
    }
}
