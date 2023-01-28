package com.example.adi.mausam.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.lang.Integer.max
import java.lang.Integer.min

class MausamLineGraphView(context: Context, attr: AttributeSet): View(context, attr) {

    private var minTemp = Float.MAX_VALUE
    private var maxTemp = Float.MIN_VALUE
    private var midTemp = 0.0f
    private var unit = 0.0f
    private var minMaxTempList = mutableListOf<DataPoint>()

    private val dataPointPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 7f
        style = Paint.Style.STROKE
    }

    private val dataPointFillPaint = Paint().apply {
        color = Color.BLACK
    }

    private val dataPointLinePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 7f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        minMaxTempList.forEachIndexed { index, dataPoint ->
            val startTempMin = getPointPosition(dataPoint.minTemp)
            val startTempMax = getPointPosition(dataPoint.maxTemp)

            val startXPosition: Float = (width.toFloat()/10) * (index*2 + 1)
            if (index != minMaxTempList.size - 1) {
                val endTempMin = getPointPosition(minMaxTempList[index + 1].minTemp)
                val endTempMax = getPointPosition(minMaxTempList[index + 1].maxTemp)

                val endXPosition = (width.toFloat()/10) * ((index + 1)*2 + 1)

                canvas.drawLine(startXPosition, startTempMin, endXPosition, endTempMin, dataPointLinePaint)
                canvas.drawLine(startXPosition, startTempMax, endXPosition, endTempMax, dataPointLinePaint)
            }

            canvas.drawCircle(startXPosition, startTempMin, 7f, dataPointFillPaint)
            canvas.drawCircle(startXPosition, startTempMin, 7f, dataPointPaint)

            canvas.drawCircle(startXPosition, startTempMax, 7f, dataPointFillPaint)
            canvas.drawCircle(startXPosition, startTempMax, 7f, dataPointPaint)
        }
    }

    private fun getPointPosition(temp: Int): Float {
        val midLine: Float = height/2.0f
        return if (temp.toFloat() < midTemp) {
            midLine + ((midTemp - temp) * unit)
        } else if (temp.toFloat() > midTemp) {
            midLine - ((temp - midTemp) * unit)
        } else {
            midLine
        }
    }

    fun setData(minMaxTemp: ArrayList<DataPoint>) {
        minTemp = min(minMaxTemp.minOf { it.maxTemp }, minMaxTemp.minOf { it.minTemp }).toFloat()
        maxTemp = max(minMaxTemp.maxOf { it.maxTemp }, minMaxTemp.maxOf { it.minTemp }).toFloat()
        midTemp = (minTemp + maxTemp)/2
        unit = height/(maxTemp - minTemp + 10)
        minMaxTempList.clear()
        minMaxTempList.addAll(minMaxTemp)
        invalidate()
    }
}

data class DataPoint(
    val minTemp: Int,
    val maxTemp: Int
)