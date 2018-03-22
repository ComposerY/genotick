package com.alphatica.genotick.genotick;

import com.alphatica.genotick.data.DataFactory;
import com.alphatica.genotick.data.DataLoader;
import com.alphatica.genotick.data.DataSaver;
import com.alphatica.genotick.data.DataSet;
import com.alphatica.genotick.data.FileSystemDataLoader;
import com.alphatica.genotick.data.FileSystemDataSaver;
import com.alphatica.genotick.data.MainAppData;
import com.alphatica.genotick.data.YahooFixer;
import com.alphatica.genotick.reversal.Reversal;
import com.alphatica.genotick.timepoint.TimePoint;
import com.alphatica.genotick.timepoint.TimePoints;
import com.alphatica.genotick.ui.ConsoleOutput;
import com.alphatica.genotick.ui.Parameters;
import com.alphatica.genotick.ui.UserInput;
import com.alphatica.genotick.ui.UserInputOutputFactory;
import com.alphatica.genotick.ui.UserOutput;
import com.alphatica.genotick.utility.TimeCounter;
import com.alphatica.genotick.utility.ParallelTasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Main {
    public static final String DEFAULT_DATA_DIR = "data";
    private static final String VERSION_STRING = "Genotick version 0.11-SNAPSHOT (copyleft 2018)";
    private ErrorCode error = ErrorCode.NO_ERROR;
    private boolean canContinue = true;
    private UserInput input;
    private UserOutput output;
    private MainInterface.Session session;

    public static void main(String[] args) throws IOException, IllegalAccessException {
        Main main = new Main();
        main.init(args, null);
    }

    public ErrorCode init(String[] args, MainInterface.Session session) throws IOException, IllegalAccessException {
        ParallelTasks.prepareDefaultThreadPool();
        TimeCounter totalRunTime = new TimeCounter("Total Run Time", false);
        this.session = session;
        Parameters parameters = new Parameters(args);
        if (canContinue) {
            initHelp(parameters);
        }
        if (canContinue) {
            initVersionRequest(parameters);
        }
        if (canContinue) {
            initUserIO(parameters);
        }
        if (canContinue) {
            initDrawData(parameters);
        }
        if (canContinue) {
            initShowPopulation(parameters);
        }
        if (canContinue) {
            initShowRobot(parameters);
        }
        if (canContinue) {
            initMerge(parameters);
        }
        if (canContinue) {
            initReverse(parameters);
        }
        if (canContinue) {
            initYahoo(parameters);
        }
        if (canContinue) {
            initSimulation(parameters);
        }
        printError(error, totalRunTime.stop(TimeUnit.SECONDS));
        return error;
    }
    
    private void setError(ErrorCode error) {
        this.error = error;
        this.canContinue = false;
    }

    private void printError(final ErrorCode error, long elapsedSeconds) {
        System.out.println(format("Program finished with error code %s(%d) in %d seconds", error.toString(), error.getValue(), elapsedSeconds));
    }

    private void initHelp(Parameters parameters) {
        if(parameters.getValue("help") != null
                || parameters.getValue("--help") != null
                || parameters.getValue("-h") != null) {
            System.out.print("Displaying version: ");
            System.out.println("    java -jar genotick.jar showVersion");
            System.out.print("Reversing data: ");
            System.out.println("    java -jar genotick.jar reverse=MY_DATA_DIR");
            System.out.print("Inputs from a file: ");
            System.out.println("    java -jar genotick.jar input=file:MY_CONFIG_FILE iterations=X|auto:X (optional) maxIterations=Y (optional) maxTime=Z (seconds optional trainingEndTime=A (optional");
            System.out.println("        iterations=X repeats training X times.  Outputs into seperate directories");
            System.out.println("        iterations=auto:X repeats training while population improves with up to X retries,  Merges outputs into one directory");
            System.out.println("        maxIterations=Y limits iterative training to Y total attempts");
            System.out.println("        maxTime=Z limits iterative training to Z seconds");
            System.out.println("        trainingEndTime=A limits training to end at A.  A can be a time point or if a negative value will stop at that many time points from then end. E.g. -30 to stop 30 time points back");
            System.out.print("Output to a file: ");
            System.out.println("    java -jar genotick.jar output=csv");
            System.out.print("Custom output directory for generated files (log, charts, population): ");
            System.out.println("    java -jar genotick.jar outdir=MY_RESULT_DIR");
            System.out.print("Show population: ");
            System.out.println("    java -jar genotick.jar showPopulation=MY_POPULATION_DIR");
            System.out.print("Show robot info: ");
            System.out.println("    java -jar genotick.jar showRobot=MY_POPULATION_DIR\\ROBOT_ID.prg");
            System.out.print("Merge robots: ");
            System.out.println("    java -jar genotick.jar mergeRobots=MY_TARGET_POPULATION_DIR candidateRobots=MY_SOURCE_POPULATIONS_DIR");
            System.out.print("Draw price curves for asset data ");
            System.out.println("    java -jar genotick.jar drawData=MY_DATA_DIR begin=TIMEPOINT end=TIMEPOINT");
            System.out.println("contact:        lukasz.wojtow@gmail.com");
            System.out.println("more info:      genotick.com");

            setError(ErrorCode.NO_ERROR);
        }
    }
    
    private void initVersionRequest(Parameters parameters) {
        if(parameters.getValue("showVersion") != null) {
            System.out.println(Main.VERSION_STRING);
            setError(ErrorCode.NO_ERROR);
        }
    }

    private void initUserIO(Parameters parameters) throws IOException {
        output = UserInputOutputFactory.createUserOutput(parameters);
        if (output == null) {
            setError(ErrorCode.NO_OUTPUT);
            return;
        }
        input = UserInputOutputFactory.createUserInput(parameters, output, session);
        if (input == null) {
            setError(ErrorCode.NO_INPUT);
            return;
        }
    }

    private void initDrawData(Parameters parameters) {
        String dataDirectory = parameters.getValue("drawData");
        if (dataDirectory != null) {
            String beginString = parameters.getValue("begin");
            String endString = parameters.getValue("end");
            DataPrinter.DrawData(output, dataDirectory, beginString, endString);
            setError(ErrorCode.NO_ERROR);
        }
    }
    
    private void initShowRobot(Parameters parameters) {
        String path = parameters.getValue("showRobot");
        if(path != null) {
            try {
                RobotPrinter.printRobot(path);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
            setError(ErrorCode.NO_ERROR);
        }
    }

    private void initShowPopulation(Parameters parameters) {
        String path = parameters.getValue("showPopulation");
        if(path != null) {
            try {
                PopulationPrinter.printPopulation(path, false, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
            setError(ErrorCode.NO_ERROR);
        }
    }

    private void initMerge(Parameters parameters) {
        String destination = parameters.getValue("mergeRobots");
        if(destination != null) {
            String source = parameters.getValue("candidateRobots");
            if(source != null) {
                ErrorCode errorCode = ErrorCode.NO_OUTPUT;
                try {
                    Merge merge = new Merge(null, output);
                    errorCode = merge.mergePopulations(destination, source);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    output.errorMessage(e.getMessage());
                }
                setError(errorCode);
            } else {
                output.errorMessage("mergeRobots command found but candidateRobots argument is missing.");
                setError(ErrorCode.MISSING_ARGUMENT);
                return;
            }
        }
    }

    private void initYahoo(Parameters parameters) {
        String path = parameters.getValue("fixYahoo");
        if(path != null) {
            YahooFixer yahooFixer = new YahooFixer(path, output);
            yahooFixer.fixFiles();
            setError(ErrorCode.NO_ERROR);
        }
    }

    private void initReverse(Parameters parameters) {
        String dataDirectory = parameters.getValue("reverse");
        if(dataDirectory != null) {
            DataLoader loader = new FileSystemDataLoader(output);
            DataSaver saver = new FileSystemDataSaver(output);
            MainAppData data = loader.loadAll(dataDirectory);
            for (DataSet loadedSet : data.getDataSets()) {
                Reversal reversal = new Reversal(loadedSet);
                if (!reversal.isReversed()) {
                    if (!data.containsDataSet(reversal.getReversedName())) {
                        DataSet reversedSet = reversal.getReversedDataSet();
                        saver.save(reversedSet);
                    }
                }
            }
            setError(ErrorCode.NO_ERROR);
        }
    }

    private void initSimulation(Parameters parameters) throws IllegalAccessException {
        TimeCounter totalSimulationRunTime = new TimeCounter("Total Simulation Run Time", false);
        
        int requestedIterations = 1;
        boolean automaticIterations = false;
        String iterationsString = parameters.getAndRemoveValue("iterations");
        if (iterationsString != null && !iterationsString.isEmpty()) {
            if(iterationsString.toLowerCase().startsWith("auto")) {
                automaticIterations = true;
                if(iterationsString.toLowerCase().startsWith("auto:")) {
                    requestedIterations = Math.max(1, Integer.parseInt(iterationsString.substring("auto:".length())));
                }
                if(output instanceof ConsoleOutput) {
                    ((ConsoleOutput)output).setVerbose(false);
                }
            } else {
                requestedIterations = Math.max(1, Integer.parseInt(iterationsString));
            }
        }
        int maximumIterations = Integer.MAX_VALUE;
        String maximumIterationsString = parameters.getAndRemoveValue("maxIterations");
        if (maximumIterationsString != null && !maximumIterationsString.isEmpty()) {
            maximumIterations = Math.max(1, Integer.parseInt(maximumIterationsString));
        }
        int maximumTime = Integer.MAX_VALUE;
        String maximumTimeString = parameters.getAndRemoveValue("maxTime");
        if (maximumTimeString != null && !maximumTimeString.isEmpty()) {
            maximumTime = Math.max(1, Integer.parseInt(maximumTimeString));
        }
        long trainingEndTimePoint = Long.MIN_VALUE;
        String trainingTimePoint = parameters.getAndRemoveValue("trainingEndTime");
        if (trainingTimePoint != null && !trainingTimePoint.isEmpty()) {
            long newTrainingTimePoint = Long.parseLong(trainingTimePoint);
            if (newTrainingTimePoint != 0) trainingEndTimePoint = newTrainingTimePoint;
        }
        if(!parameters.allConsumed()) {
            output.errorMessage("Not all arguments processed: " + parameters.getUnconsumed());
            setError(ErrorCode.UNKNOWN_ARGUMENT);
            return;
        }
        MainSettings settings = input.getSettings();
        MainAppData data = input.getData(settings.dataDirectory);
        generateMissingData(settings, data);
        List<EngineResult> engineResults = new ArrayList<EngineResult>();

        TimePoint originalEndTimePoint = settings.endTimePoint;
        Double originalMinimumScoreToSaveToDisk = settings.minimumScoreToSaveToDisk;
        if(automaticIterations == true) {
            if(settings.minimumScoreToSaveToDisk == 0.0) settings.minimumScoreToSaveToDisk = 0.01;
            if(trainingEndTimePoint != Long.MIN_VALUE) {
                if(trainingEndTimePoint > 0) {
                    settings.endTimePoint = data.getTimePoint(data.getNearestBar(new TimePoint(trainingEndTimePoint)));
                } else if(trainingEndTimePoint == 0 || Math.abs(trainingEndTimePoint) > data.getTimePointCount()) {
                    output.errorMessage(String.format("Invalid ending time point %d specified, ignoring", trainingEndTimePoint));
                } else {
                    TimePoints timePoints = data.createTimePointsCopy(data.getFirstTimePoint(),  data.getLastTimePoint());
                    settings.endTimePoint = timePoints.get(timePoints.size() + (int)trainingEndTimePoint);
                }
            }
        }
        
        int totalActiveRobotCount = 0;
        int simulationIteration = 0;
        int simulationIterationWithRobotsProduced=0;
        int iterationsWithImprovements = 0;
        int iterations = requestedIterations + 1;
        boolean isTransistionFromBuildingToImproving = false;
        while (true) {
            // Check exit conditions.
            if(--iterations == 0) {
                if(automaticIterations == true) output.infoMessage("Iterative training ending due to no progress improving population");
                break;
            }
            if(--maximumIterations < 0) {
                output.infoMessage("Iterative training ending due to maximum iterations reached");
                break;
            }
            if(TimeUnit.SECONDS.convert(totalSimulationRunTime.getElapsedNanoSeconds(), TimeUnit.NANOSECONDS) >= maximumTime) {
                output.infoMessage("Iterative training ending due to maximum runtime reached");
                break;
            }
            
            Simulation simulation = new Simulation(output);
            MainInterface.SessionResult sessionResult = (session != null) ? session.result : null;
            EngineResult engineResult = simulation.start(settings, data, sessionResult, ++simulationIteration);
            if(engineResult == null) {
                setError(ErrorCode.ENGINE_FAILURE);
                return;
            }
            engineResults.add(engineResult);
            
            if(automaticIterations == true) {
                if(engineResult.getActivePopulationSize() > 0) {
                    simulationIterationWithRobotsProduced++;
                }
                
                if(simulationIterationWithRobotsProduced > 1) {
                    if(totalActiveRobotCount < settings.populationDesiredSize && totalActiveRobotCount + engineResult.getActivePopulationSize() >= settings.populationDesiredSize) {
                        isTransistionFromBuildingToImproving = true;
                        output.infoMessage("Core population built. Tranisitioning from building population to improving population.");
                    } else {
                        isTransistionFromBuildingToImproving = false;
                    }
                    // Only consider robots after our first iterations for the final set.  The first iteration just creates a baseline.
                    totalActiveRobotCount += engineResult.getActivePopulationSize();
                }

                // While we have not created enough robots to fill a single population iterate until we do or we expire our max iterations.
                if(totalActiveRobotCount < settings.populationDesiredSize) {
                    output.infoMessage(String.format("Automatic iteration %d complete. Building population. Minimum score required: %f Active robots: %d Best score: %f Worst score: %f Population score: %f Population Size: %d", 
                            simulationIteration, 
                            settings.minimumScoreToSaveToDisk, 
                            engineResult.getActivePopulationSize(), 
                            engineResult.getBestRobotScore(), 
                            engineResult.getWorstRobotScore(),
                            engineResult.getPopulationScore(),
                            totalActiveRobotCount));

                    if(simulationIterationWithRobotsProduced == 1 || engineResult.getActivePopulationSize() < settings.populationDesiredSize * 0.10) {
                        // Update the difficulty to the average of the runs overall if no progress is being made.
                        settings.minimumScoreToSaveToDisk = Math.max(0.1, calculateAverageEngineResultScore(engineResults, 0.1) );
                    } else if (engineResult.getActivePopulationSize() > 0){
                        // Add at least a nominal 5% increase to each additional run or the higher mid average of the runs.
                        settings.minimumScoreToSaveToDisk = Math.max(settings.minimumScoreToSaveToDisk * 1.05, calculateAverageEngineResultScore(engineResults, settings.minimumScoreToSaveToDisk / 2));
                    }
                    
                    if(engineResult.getActivePopulationSize() > 0) {
                        iterations = requestedIterations;
                    } else if(iterations == 1) {
                        // If we have not built a full population but have expired our attempts, lower the difficulty back down.
                        settings.minimumScoreToSaveToDisk = Math.max(0.1, calculateAverageEngineResultScore(engineResults, 0.0));
                        iterations = requestedIterations;
                    }
                    continue;
                } else {
                    output.infoMessage(String.format("Automatic iteration %d complete. Improving population. Minimum score required: %f Active robots: %d Best score: %f Worst score: %f Population score: %f Population Size: %d", 
                            simulationIteration, 
                            settings.minimumScoreToSaveToDisk, 
                            engineResult.getActivePopulationSize(), 
                            engineResult.getBestRobotScore(), 
                            engineResult.getWorstRobotScore(),
                            engineResult.getPopulationScore(),
                            totalActiveRobotCount));
                }
                
                // If this iteration doesn't have an active robots or the score doesn't meet our minimum, try again.  This counts
                // against our max iterations.
                if(engineResult.getActivePopulationSize() == 0 || engineResult.getPopulationScore() <= settings.minimumScoreToSaveToDisk) {
                    output.infoMessage(String.format("Iteration does not improve population, retrying. Iterations attempts remaining: %d", iterations));
                    continue;
                } else {
                    iterationsWithImprovements++;
                }
                
                if(isTransistionFromBuildingToImproving || (iterationsWithImprovements % Math.max(1, requestedIterations/2)) == 0) {
                    double averageEngineResultScore = calculateAverageEngineResultScore(engineResults, calculateAverageEngineResultScore(engineResults, 0.0));
                    // Minimum raise of 1% and maximum raise of 20%
                    double newMinimumScoreToSaveToDisk = Math.max(settings.minimumScoreToSaveToDisk * 1.01, Math.min(averageEngineResultScore, settings.minimumScoreToSaveToDisk * 1.20));
                    output.infoMessage(String.format("Automatic iterations raising minimumScoreToSaveToDisk to: %f", newMinimumScoreToSaveToDisk));
                    settings.minimumScoreToSaveToDisk = newMinimumScoreToSaveToDisk;
                }
                iterations = requestedIterations;
                output.infoMessage(String.format("Continuing to improve population. Iterations attempts remaining: %d", iterations));
                continue;
            }
        }
        output.infoMessage(String.format("Final populations average score: %f. Total iterations: %d", calculateAverageEngineResultScore(engineResults, Double.MIN_VALUE), simulationIteration));
        
        if(automaticIterations) {
            ErrorCode mergeResult = autoMergePopulations(settings, engineResults);
            if(mergeResult != ErrorCode.NO_ERROR && mergeResult != ErrorCode.NO_OUTPUT) {
                return;
            }
            
            if(output instanceof ConsoleOutput) {
                ((ConsoleOutput)output).setVerbose(true);
            }

            PopulationPrinter.printPopulation(engineResults.get(0).getPopulationDirectory(), true, true);
            
            if(trainingEndTimePoint != Long.MIN_VALUE && trainingEndTimePoint < originalEndTimePoint.getValue()) {
                output.infoMessage(String.format("Automatic iterations running final population against remaining data"));
                settings.startTimePoint = new TimePoint(settings.endTimePoint.getValue()+1);
                settings.endTimePoint = originalEndTimePoint;
                settings.minimumScoreToSaveToDisk = originalMinimumScoreToSaveToDisk;
                settings.performTraining = false;
                settings.populationDAO = "ram:" + engineResults.get(0).getPopulationDirectory();
                Simulation simulation = new Simulation(output);
                MainInterface.SessionResult sessionResult = (session != null) ? session.result : null;
                EngineResult engineResult = simulation.start(settings, data, sessionResult, ++simulationIteration);
                if(engineResult == null) {
                    setError(ErrorCode.ENGINE_FAILURE);
                    return;
                }
                engineResults.add(engineResult);
            }
            
        }
        setError(ErrorCode.NO_ERROR);
    }
    
    private double calculateAverageEngineResultScore(final List<EngineResult> engineResults, final double minimumScoreToConsider) {
        return engineResults.stream()
            .filter(engineResult -> engineResult.getActivePopulationSize() > 0)
            .filter(engineResult -> engineResult.getPopulationScore() >= minimumScoreToConsider)
            .mapToDouble(EngineResult::getPopulationScore).average().orElse(0);
    }
    
    private ErrorCode autoMergePopulations(MainSettings settings, final List<EngineResult> engineResults) {
        List<String> populationDirectories = engineResults.stream()
                .filter(engineResult -> engineResult.getActivePopulationSize() > 0)
                .map(EngineResult::getPopulationDirectory).collect(Collectors.toList());
        if(populationDirectories.isEmpty()) {
            return ErrorCode.ENGINE_FAILURE;
        }
        String finalPopulationDirectory = engineResults.get(0).getPopulationDirectory();
        populationDirectories.remove(finalPopulationDirectory);

        ErrorCode errorCode = ErrorCode.NO_OUTPUT;
        try {
            Merge merge = new Merge(settings, output);
            errorCode = merge.mergePopulations(finalPopulationDirectory, populationDirectories);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            output.errorMessage(e.getMessage());
        }
        setError(errorCode);
        return errorCode;
    }
    
    private void generateMissingData(MainSettings settings, MainAppData data) {
        if (settings.requireSymmetricalRobots) {
            Collection<DataSet> loadedSets = data.getDataSets();
            DataSet[] loadedSetsCopy = loadedSets.toArray(new DataSet[data.getDataSets().size()]);
            for (DataSet loadedSet : loadedSetsCopy) {
                Reversal reversal = new Reversal(loadedSet);
                if (reversal.addReversedDataSetTo(data)) {
                    if (!settings.dataDirectory.isEmpty()) {
                        DataSaver saver = DataFactory.getDefaultSaver(output);
                        saver.save(reversal.getReversedDataSet());
                    }
                }
            }
        }
    }
}
