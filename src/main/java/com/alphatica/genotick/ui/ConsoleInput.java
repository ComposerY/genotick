package com.alphatica.genotick.ui;

import com.alphatica.genotick.data.FilterOption;
import com.alphatica.genotick.data.MainAppData;
import com.alphatica.genotick.genotick.Main;
import com.alphatica.genotick.genotick.MainSettings;
import com.alphatica.genotick.genotick.WeightMode;
import com.alphatica.genotick.timepoint.TimePoint;
import com.alphatica.genotick.breeder.InheritedWeightMode;
import com.alphatica.genotick.chart.GenoChartMode;

import java.io.Console;

class ConsoleInput extends BasicUserInput {
    private final Console console;

    ConsoleInput(UserOutput output) {
        super(output);
        console = System.console();
        if(console == null) {
            throw new RuntimeException("Unable to create system console");
        }
    }

    @Override
    public MainSettings getSettings() {
        MainSettings settings = getMainSettings();
        if (settings == null) {
            String dataDirectory = getString("Data directory", Main.DEFAULT_DATA_DIR);
            MainAppData data = getData(dataDirectory);
            settings = MainSettings.getSettings();
            settings.dataDirectory = dataDirectory;
            settings.performTraining = getBoolean("Perform training", settings.performTraining);
            settings.startTimePoint = new TimePoint(getLong("Start time point",data.getFirstTimePoint().getValue()));
            settings.endTimePoint = new TimePoint(getLong("End time point", data.getLastTimePoint().getValue()));
            settings.populationDAO = getString("Population storage", settings.populationDAO);
            settings.maximumProcessorInstructionFactor = getInteger("Maximum Processor instruction factor", settings.maximumProcessorInstructionFactor);
            settings.resultThreshold = getDouble("Result threshold",settings.resultThreshold);
            settings.requireSymmetricalRobots = getBoolean("Require symmetrical robots", settings.performTraining); // If 'perform training' is false then there is no need for symmetrical robots
            settings.chartMode = getEnumValue(GenoChartMode.class, "Chart mode", settings.chartMode);
            if (settings.performTraining) {
                settings.maximumDataOffset = getInteger("Maximum data offset", settings.maximumDataOffset);
                settings.populationDesiredSize = getInteger("Population desired size", settings.populationDesiredSize);
                settings.minimumRobotInstructions = getInteger("Minimum robot instructions", settings.minimumRobotInstructions);
                settings.maximumRobotInstructions = getInteger("Maximum robot instructions", settings.maximumRobotInstructions);
                settings.minimumRobotVariables = getInteger("Minimum robot variables", settings.minimumRobotVariables);
                settings.maximumRobotVariables = getInteger("Maximum robot variables", settings.maximumRobotVariables);
                settings.maximumDeathByAge = getDouble("Maximum death rate by age", settings.maximumDeathByAge);
                settings.maximumDeathByWeight = getDouble("Maximum death rate by weight", settings.maximumDeathByWeight);
                settings.probabilityOfDeathByAge = getDouble("Probability of death by age", settings.probabilityOfDeathByAge);
                settings.probabilityOfDeathByWeight = getDouble("Probability of death by weight", settings.probabilityOfDeathByWeight);
                settings.weightMode = getEnumValue(WeightMode.class, "Weight mode", settings.weightMode);
                settings.weightExponent = getDouble("Weight exponent", settings.weightExponent);
                settings.inheritedChildWeight = getDouble("Inherited child's weight", settings.inheritedChildWeight);
                settings.inheritedChildWeightMode = getEnumValue(InheritedWeightMode.class, "Inherited child's weight mode", settings.inheritedChildWeightMode);
                settings.protectRobotsUntilOutcomes = getInteger("Protect robots until outcomes", settings.protectRobotsUntilOutcomes);
                settings.newInstructionProbability = getDouble("Probability of new instruction", settings.newInstructionProbability);
                // This looks like an error but it's not. Default value for 'skipInstruction...' is same as 'newInstruction'
                // to keep robots more or less constant size.
                settings.skipInstructionProbability = getDouble("Probability of skipping instruction", settings.newInstructionProbability);
                settings.instructionMutationProbability = getDouble("Instruction mutation probability", settings.instructionMutationProbability);
                settings.minimumOutcomesToAllowBreeding = getInteger("Minimum outcomes to allow breeding", settings.minimumOutcomesToAllowBreeding);
                settings.minimumOutcomesBetweenBreeding = getInteger("Minimum outcomes between breeding", settings.minimumOutcomesBetweenBreeding);
                settings.killNonPredictingRobots = getInteger("Kill non-predicting robots", settings.killNonPredictingRobots);
                settings.minimumScoreToSaveToDisk = getDouble("Minimum score to save robot to disk", settings.minimumScoreToSaveToDisk);
                settings.randomRobotsAtEachUpdate = getDouble("Random robots at each update", settings.randomRobotsAtEachUpdate);
                settings.protectBestRobots = getDouble("Protect best robots", settings.protectBestRobots);
                settings.ignoreColumns = getInteger("Ignore columns for training", settings.ignoreColumns);
                settings.randomSeed = getLong("Random seed", settings.randomSeed);
                settings.profitReinvestFactor = getDouble("Profit reinvest factor", settings.profitReinvestFactor);
                settings.filterOption = getEnumValue(FilterOption.class, "Filter option", settings.filterOption);
            }
            setMainSettings(settings);
        }
        return settings;
    }

    private <E extends Enum<E>> E getEnumValue(Class<E> enumType, String message, E def) {
        String str = def.name();
        displayMessage(message,str);
        String response = getString(null,str);
        final E[] enumValues = enumType.getEnumConstants();
        for (E enumValue : enumValues) {
            if (enumValue.name().equals(response)) {
                return enumValue;
            }
        }
        return def;
    }
    
    private double getDouble(String message, double def) {
        String str = String.valueOf(def);
        displayMessage(message,str);
        String response = getString(null,str);
        return Double.parseDouble(response);
    }

    private int getInteger(String message, int def) {
        String str = String.valueOf(def);
        displayMessage(message,str);
        String response = getString(null,str);
        return Integer.parseInt(response);
    }

    private boolean getBoolean(String message, boolean def) {
        String str = String.valueOf(def);
        displayMessage(message,str);
        String response = getString(null,str);
        return Boolean.valueOf(response);
    }

    private long getLong(String message, long def) {
        String str = String.valueOf(def);
        displayMessage(message,str);
        String response = getString(null,str);
        return Long.parseLong(response);
    }

    private String getString(String message, String def) {
        displayMessage(message, def);
        String response = console.readLine();
        if(response.equals(""))
            return def;
        else
            return response;
    }

    private void displayMessage(String message, String def) {
        if(message != null)
            System.out.print(String.format("%s [%s]: ",message,def));
    }
}
