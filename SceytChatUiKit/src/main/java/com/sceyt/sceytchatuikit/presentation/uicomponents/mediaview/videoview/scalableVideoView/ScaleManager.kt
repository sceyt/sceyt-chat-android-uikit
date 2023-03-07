package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.videoview.scalableVideoView

import android.graphics.Matrix

class ScaleManager(private val mViewSize: Size, private val mVideoSize: Size) {
    fun getScaleMatrix(scalableType: ScalableType?): Matrix? {
        return when (scalableType) {
            ScalableType.NONE -> noScale
            ScalableType.FIT_XY -> fitXY()
            ScalableType.FIT_CENTER -> fitCenter()
            ScalableType.FIT_START -> fitStart()
            ScalableType.FIT_END -> fitEnd()
            ScalableType.LEFT_TOP -> getOriginalScale(PivotPoint.LEFT_TOP)
            ScalableType.LEFT_CENTER -> getOriginalScale(PivotPoint.LEFT_CENTER)
            ScalableType.LEFT_BOTTOM -> getOriginalScale(PivotPoint.LEFT_BOTTOM)
            ScalableType.CENTER_TOP -> getOriginalScale(PivotPoint.CENTER_TOP)
            ScalableType.CENTER -> getOriginalScale(PivotPoint.CENTER)
            ScalableType.CENTER_BOTTOM -> getOriginalScale(PivotPoint.CENTER_BOTTOM)
            ScalableType.RIGHT_TOP -> getOriginalScale(PivotPoint.RIGHT_TOP)
            ScalableType.RIGHT_CENTER -> getOriginalScale(PivotPoint.RIGHT_CENTER)
            ScalableType.RIGHT_BOTTOM -> getOriginalScale(PivotPoint.RIGHT_BOTTOM)
            ScalableType.LEFT_TOP_CROP -> getCropScale(PivotPoint.LEFT_TOP)
            ScalableType.LEFT_CENTER_CROP -> getCropScale(PivotPoint.LEFT_CENTER)
            ScalableType.LEFT_BOTTOM_CROP -> getCropScale(PivotPoint.LEFT_BOTTOM)
            ScalableType.CENTER_TOP_CROP -> getCropScale(PivotPoint.CENTER_TOP)
            ScalableType.CENTER_CROP -> getCropScale(PivotPoint.CENTER)
            ScalableType.CENTER_BOTTOM_CROP -> getCropScale(PivotPoint.CENTER_BOTTOM)
            ScalableType.RIGHT_TOP_CROP -> getCropScale(PivotPoint.RIGHT_TOP)
            ScalableType.RIGHT_CENTER_CROP -> getCropScale(PivotPoint.RIGHT_CENTER)
            ScalableType.RIGHT_BOTTOM_CROP -> getCropScale(PivotPoint.RIGHT_BOTTOM)
            ScalableType.START_INSIDE -> startInside()
            ScalableType.CENTER_INSIDE -> centerInside()
            ScalableType.END_INSIDE -> endInside()
            else -> null
        }
    }

    private fun getMatrix(sx: Float, sy: Float, px: Float, py: Float): Matrix {
        val matrix = Matrix()
        matrix.setScale(sx, sy, px, py)
        return matrix
    }

    private fun getMatrix(sx: Float, sy: Float, pivotPoint: PivotPoint): Matrix {
        return when (pivotPoint) {
            PivotPoint.LEFT_TOP -> getMatrix(sx, sy, 0f, 0f)
            PivotPoint.LEFT_CENTER -> getMatrix(sx, sy, 0f, mViewSize.height / 2f)
            PivotPoint.LEFT_BOTTOM -> getMatrix(sx, sy, 0f, mViewSize.height.toFloat())
            PivotPoint.CENTER_TOP -> getMatrix(sx, sy, mViewSize.width / 2f, 0f)
            PivotPoint.CENTER -> getMatrix(sx, sy, mViewSize.width / 2f, mViewSize.height / 2f)
            PivotPoint.CENTER_BOTTOM -> getMatrix(
                sx,
                sy,
                mViewSize.width / 2f,
                mViewSize.height.toFloat()
            )
            PivotPoint.RIGHT_TOP -> getMatrix(sx, sy, mViewSize.width.toFloat(), 0f)
            PivotPoint.RIGHT_CENTER -> getMatrix(
                sx,
                sy,
                mViewSize.width.toFloat(),
                mViewSize.height / 2f
            )
            PivotPoint.RIGHT_BOTTOM -> getMatrix(
                sx,
                sy,
                mViewSize.width.toFloat(),
                mViewSize.height.toFloat()
            )
        }
    }

    private val noScale: Matrix
        get() {
            val sx = mVideoSize.width / mViewSize.width.toFloat()
            val sy = mVideoSize.height / mViewSize.height.toFloat()
            return getMatrix(sx, sy, PivotPoint.LEFT_TOP)
        }

    private fun getFitScale(pivotPoint: PivotPoint): Matrix {
        var sx = mViewSize.width.toFloat() / mVideoSize.width
        var sy = mViewSize.height.toFloat() / mVideoSize.height
        val minScale = sx.coerceAtMost(sy)
        sx = minScale / sx
        sy = minScale / sy
        return getMatrix(sx, sy, pivotPoint)
    }

    private fun fitXY(): Matrix {
        return getMatrix(1f, 1f, PivotPoint.LEFT_TOP)
    }

    private fun fitStart(): Matrix {
        return getFitScale(PivotPoint.LEFT_TOP)
    }

    private fun fitCenter(): Matrix {
        return getFitScale(PivotPoint.CENTER)
    }

    private fun fitEnd(): Matrix {
        return getFitScale(PivotPoint.RIGHT_BOTTOM)
    }

    private fun getOriginalScale(pivotPoint: PivotPoint): Matrix {
        val sx = mVideoSize.width / mViewSize.width.toFloat()
        val sy = mVideoSize.height / mViewSize.height.toFloat()
        return getMatrix(sx, sy, pivotPoint)
    }

    private fun getCropScale(pivotPoint: PivotPoint): Matrix {
        var sx = mViewSize.width.toFloat() / mVideoSize.width
        var sy = mViewSize.height.toFloat() / mVideoSize.height
        val maxScale = sx.coerceAtLeast(sy)
        sx = maxScale / sx
        sy = maxScale / sy
        return getMatrix(sx, sy, pivotPoint)
    }

    private fun startInside(): Matrix {
        return if (mVideoSize.height <= mViewSize.width
                && mVideoSize.height <= mViewSize.height
        ) {
            // video is smaller than view size
            getOriginalScale(PivotPoint.LEFT_TOP)
        } else {
            // either of width or height of the video is larger than view size
            fitStart()
        }
    }

    private fun centerInside(): Matrix {
        return if (mVideoSize.height <= mViewSize.width
                && mVideoSize.height <= mViewSize.height
        ) {
            // video is smaller than view size
            getOriginalScale(PivotPoint.CENTER)
        } else {
            // either of width or height of the video is larger than view size
            fitCenter()
        }
    }

    private fun endInside(): Matrix {
        return if (mVideoSize.height <= mViewSize.width
                && mVideoSize.height <= mViewSize.height
        ) {
            // video is smaller than view size
            getOriginalScale(PivotPoint.RIGHT_BOTTOM)
        } else {
            // either of width or height of the video is larger than view size
            fitEnd()
        }
    }
}