package com.alphatica.genotick.reversal;

import com.alphatica.genotick.data.Column;
import com.alphatica.genotick.data.DataLines;
import com.alphatica.genotick.data.DataSet;
import com.alphatica.genotick.data.DataSetName;
import com.alphatica.genotick.data.MainAppData;

import java.io.File;

public class Reversal {

    private final DataSet originalSet;
    private DataSetName reversedName;
    private DataSet reversedSet;

    public Reversal(DataSet dataSet) {
        this.originalSet = dataSet;
        this.reversedName = null;
        this.reversedSet = null;
    }

    public boolean isReversed() {
        return originalSet.getName().isReversed();
    }
    
    public DataSetName getReversedName() {
        if (reversedName == null) {
            final DataSetName original = originalSet.getName();
            reversedName = createReversedDataSetName(original);
        }
        return reversedName;
    }

    public DataSet getReversedDataSet() {
        if (reversedSet == null) {
            final DataLines dataLines = originalSet.getDataLinesCopy();
            reverseDataLines(dataLines);
            reversedSet = new DataSet(getReversedName(), dataLines);
        }
        return reversedSet;
    }

    public boolean addReversedDataSetTo(MainAppData data) {
        if (!isReversed()) {
            if (!data.containsDataSet(getReversedName())) {
                data.put(getReversedDataSet());
                return true;
            }
        }
        return false;
    }
    
    private static DataSetName createReversedDataSetName(DataSetName name) {
        final StringBuilder newName = new StringBuilder(name.getPath());
        final int index = newName.lastIndexOf(File.separator) + 1;
        if (name.isReversed()) {
            final int len = DataSetName.REVERSE_DATA_IDENTIFIER.length();
            newName.delete(index, index + len);
        }
        else {
            newName.insert(index, DataSetName.REVERSE_DATA_IDENTIFIER);
        }
        return new DataSetName(newName.toString());
    }
    
    private static void reverseDataLines(DataLines dataLines) {
        final int lineCount = dataLines.lineCount();
        double[] lastOriginal = dataLines.getOhlcValuesCopy(0);
        double[] lastReversed = dataLines.getOhlcValuesCopy(0);
        for (int line = 0; line < lineCount; ++line) {
            double[] currentOriginal = dataLines.getOhlcValuesCopy(line);
            double[] currentReversed = createReversedOHLC(lastOriginal, currentOriginal, lastReversed);
            dataLines.setOhlcValues(line, currentReversed);
            lastOriginal = currentOriginal;
            lastReversed = currentReversed;
        }        
        normalize(dataLines);
    }
    
    private static double[] createReversedOHLC(double[] lastOriginal, double[] original, double[] lastReversed) {
        final double[] reversed = new double[original.length];
        final double openDiff = original[Column.OHLC.OPEN] / lastOriginal[Column.OHLC.CLOSE];
        final double highDiff = original[Column.OHLC.HIGH] / original[Column.OHLC.OPEN];
        final double lowDiff = original[Column.OHLC.LOW] / original[Column.OHLC.OPEN];
        final double closeDiff = original[Column.OHLC.CLOSE] / original[Column.OHLC.OPEN];
        reversed[Column.OHLC.OPEN] = lastReversed[Column.OHLC.CLOSE] * (2 - openDiff);
        reversed[Column.OHLC.HIGH] = reversed[Column.OHLC.OPEN] * (2 - lowDiff);
        reversed[Column.OHLC.LOW] = reversed[Column.OHLC.OPEN] * ( 2 - highDiff);
        reversed[Column.OHLC.CLOSE] = reversed[Column.OHLC.OPEN] * (2 - closeDiff);
        return reversed;
    }
    
    private static void normalize(DataLines dataLines) {
        final int lineCount = dataLines.lineCount();
        final double threshold = 0.01;
        double lowest = threshold;
        for (int line = 0; line < lineCount; ++line) {
            final double low = dataLines.getOhlcValue(line, Column.OHLC.LOW);
            if (low < lowest)
                lowest = low;
        }
        if (lowest < threshold) {
            final double negOffset = lowest - threshold;
            for (int line = 0; line < lineCount; ++line) {
                for (int column : Column.OHLCArray.OHLC) {
                    final double value = dataLines.getOhlcValue(line, column);
                    dataLines.setOhlcValue(line, column, value - negOffset);
                }
            }
        }
    }
}
