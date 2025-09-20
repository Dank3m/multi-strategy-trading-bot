package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.HistoricalTrade;
import com.kinduberre.multistrategytradingbot.model.MLPrediction;
import com.kinduberre.multistrategytradingbot.model.MarketFeatures;
import com.kinduberre.multistrategytradingbot.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MLSignalFilter {

    private MultiLayerNetwork model;

    @Value("${ml.min-confidence}")
    private double minConfidence;

    public MLSignalFilter() {
        initializeModel();
    }

    private void initializeModel() {
        NeuralNetConfiguration.ListBuilder config = new NeuralNetConfiguration.Builder()
                .seed(123)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(0.001))
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(20) // Input features
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(64)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(32)
                        .nOut(16)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                        .nIn(16)
                        .nOut(3) // Buy, Sell, Hold
                        .activation(Activation.SOFTMAX)
                        .build());

        model = new MultiLayerNetwork(config.build());
        model.init();
    }

    public MLPrediction filterSignal(Signal signal, MarketFeatures features) {
        // Convert features to INDArray
        INDArray input = Nd4j.create(features.toArray());

        // Get prediction
        INDArray output = model.output(input);

        double[] predictions = output.toDoubleVector();

        MLPrediction prediction = new MLPrediction();
        prediction.setBuyProbability(predictions[0]);
        prediction.setSellProbability(predictions[1]);
        prediction.setHoldProbability(predictions[2]);

        // Determine action
        int maxIndex = getMaxIndex(predictions);
        if (maxIndex == 0 && predictions[0] > minConfidence) {
            prediction.setAction("BUY");
        } else if (maxIndex == 1 && predictions[1] > minConfidence) {
            prediction.setAction("SELL");
        } else {
            prediction.setAction("HOLD");
        }

        prediction.setConfidence(predictions[maxIndex]);
        prediction.setOriginalSignal(signal);

        return prediction;
    }

    public void trainModel(List<HistoricalTrade> historicalTrades) {
        log.info("Training ML model with {} trades", historicalTrades.size());

        // Prepare training data
        List<DataSet> dataSets = new ArrayList<>();

        for (HistoricalTrade trade : historicalTrades) {
            INDArray features = Nd4j.create(trade.getFeatures());
            INDArray label = createLabel(trade.getOutcome());
            dataSets.add(new DataSet(features, label));
        }

        // Train model
        for (int i = 0; i < 100; i++) {
            for (DataSet dataSet : dataSets) {
                model.fit(dataSet);
            }

            if (i % 10 == 0) {
                double score = model.score();
                log.info("Training epoch {} - Score: {}", i, score);
            }
        }

        log.info("Model training complete");
    }

    private INDArray createLabel(String outcome) {
        double[] label = new double[3];
        switch (outcome) {
            case "PROFITABLE":
                label[0] = 1.0; // Buy was correct
                break;
            case "LOSS":
                label[1] = 1.0; // Should have sold
                break;
            default:
                label[2] = 1.0; // Should have held
        }
        return Nd4j.create(label);
    }

    private int getMaxIndex(double[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}