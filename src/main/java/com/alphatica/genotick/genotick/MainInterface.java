package com.alphatica.genotick.genotick;

import static java.lang.String.format;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alphatica.genotick.data.DataSetName;
import com.alphatica.genotick.data.MainAppData;
import com.alphatica.genotick.timepoint.TimePoint;
import com.alphatica.genotick.timepoint.TimePoints;
import com.alphatica.genotick.utility.JniExport;
import com.alphatica.genotick.utility.MethodName;

import static com.alphatica.genotick.utility.Assert.gassert;

public class MainInterface {
    
    static class SessionResult {
        final TimePoints timePoints;
        final Map<String, Predictions> predictionsMap;
        
        SessionResult(MainSettings settings, MainAppData data) {
            final Set<DataSetName> dataSetNames = data.getDataSetNames();
            this.timePoints = data.createTimePointsCopy(settings.startTimePoint, settings.endTimePoint);
            this.predictionsMap = new HashMap<String, Predictions>(dataSetNames.size());
            final int timePointCount = this.timePoints.size();
            final boolean firstTimeIsNewest = this.timePoints.firstTimeIsNewest();
            for (DataSetName dataSetName : dataSetNames) {
                final Predictions predictions = new Predictions(timePointCount, firstTimeIsNewest);
                predictions.fill(Prediction.OUT);
                this.predictionsMap.put(dataSetName.getPath(), predictions);
            }
        }
        
        private Predictions findPredictions(DataSetName dataSetName) {
            return this.predictionsMap.get(dataSetName.getPath());
        }
        
        void savePrediction(TimePoint timePoint, DataSetName dataSetName, Prediction prediction) {
            final Predictions predictions = findPredictions(dataSetName);
            if (predictions != null) {
                final int index = this.timePoints.getIndex(timePoint);
                gassert(index >= 0);
                predictions.set(index, prediction);
            }
        }
    }
    
    public static class Session {
        public final MainSettings settings;
        public final MainAppData data;
        SessionResult result;
        
        Session() {
            this.settings = MainSettings.getSettings();
            this.data = new MainAppData();
            this.result = null;
        }
        
        void prepareNewSessionResult() {
            this.result = new SessionResult(settings, data);
        }
    }
    
    private static class ThreadData {
        final Map<Integer, Session> sessions = new HashMap<Integer, Session>();
    }
    
    private static final int INTERFACE_VERSION = 1;
    private static final Map<Long, ThreadData> threads = new ConcurrentHashMap<Long, ThreadData>();
    
    @JniExport
    static int getInterfaceVersion() {
        return INTERFACE_VERSION;
    }
    
    @JniExport
    static ErrorCode start(int sessionId, String[] args) throws IOException, IllegalAccessException {
        printStart(sessionId, args);
        Session session = getSession(sessionId);
        if (session == null) {
            return printAndReturnError(ErrorCode.INVALID_SESSION);
        }
        if (session.data.isEmpty()) {
            return printAndReturnError(ErrorCode.INSUFFICIENT_DATA);
        }
        session.prepareNewSessionResult();
        Main main = new Main();
        return main.init(args, session);
    }
    
    @JniExport
    static MainSettings getSettings(int sessionId) {
        Session session = getSession(sessionId);
        return (session != null) ? session.settings : null;
    }
    
    @JniExport
    static MainAppData getData(int sessionId) {
        Session session = getSession(sessionId);
        return (session != null) ? session.data : null;
    }
    
    @JniExport
    static TimePoints getTimePoints(int sessionId) {
        SessionResult sessionResult = getSessionResult(sessionId);
        return (sessionResult != null) ? sessionResult.timePoints : null;
    }
    
    @JniExport
    static Predictions getPredictions(int sessionId, String dataSetName) {
        SessionResult sessionResult = getSessionResult(sessionId);
        return (sessionResult != null) ? sessionResult.predictionsMap.get(dataSetName) : null;
    }
    
    @JniExport
    static TimePoint getNewestTimePoint(int sessionId) {
        SessionResult sessionResult = getSessionResult(sessionId);
        return (sessionResult != null) ? sessionResult.timePoints.getNewest() : null;
    }
    
    @JniExport
    static Prediction getNewestPrediction(int sessionId, String dataSetName) {
        Prediction prediction = null;
        SessionResult sessionResult = getSessionResult(sessionId);
        if (sessionResult != null) {
            Predictions predictions = sessionResult.predictionsMap.get(dataSetName);
            if (predictions != null) {
                prediction = predictions.getNewest();
            }
        }
        return prediction;
    }
    
    @JniExport
    static ErrorCode createSession(int sessionId) {
        ThreadData thread = getCurrentThreadData();
        if (thread == null) {
            thread = new ThreadData();
            threads.put(getCurrentThreadId(), thread);
        }
        if (thread.sessions.get(sessionId) == null) {
            thread.sessions.put(sessionId, new Session());
            return printAndReturnError(ErrorCode.NO_ERROR);
        }
        return printAndReturnError(ErrorCode.DUPLICATE_SESSION);
    }
    
    @JniExport
    static ErrorCode clearSession(int sessionId) {
        long threadId = getCurrentThreadId();
        ThreadData thread = threads.get(threadId);
        if (thread != null) {
            if (thread.sessions.remove(sessionId) != null) {
                if (thread.sessions.isEmpty()) {
                    threads.remove(threadId);
                }
                return printAndReturnError(ErrorCode.NO_ERROR);
            }
        }
        return printAndReturnError(ErrorCode.INVALID_SESSION);
    }
    
    @JniExport
    static void clearSessions() {
        threads.remove(getCurrentThreadId());
    }
      
    private static Session getSession(int sessionId) {
        ThreadData thread = getCurrentThreadData();
        if (thread != null) {
            return thread.sessions.get(sessionId);
        }
        return null;
    }
    
    private static SessionResult getSessionResult(int sessionId) {
        Session session = getSession(sessionId);
        return (session != null) ? session.result : null;
    }
    
    private static ThreadData getCurrentThreadData() {
        return threads.get(getCurrentThreadId());
    }
    
    private static long getCurrentThreadId() {
        return Thread.currentThread().getId();
    }
    
    private static void printStart(int sessionId, String[] args) {
        String argsString = "";
        for (String arg : args) {
            argsString += " " + arg;
        }
        System.out.println(format("Starting session '%d' on thread '%d' with arguments:%s", sessionId, getCurrentThreadId(), argsString));
    }
    
    private static ErrorCode printAndReturnError(final ErrorCode error) {
        System.out.println(format("Method '%s' returned error code %s(%d)", MethodName.get(1), error.toString(), error.getValue()));
        return error;
    }
}
