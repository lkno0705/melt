package de.uni_mannheim.informatik.dws.melt.examples.transformers;

import de.uni_mannheim.informatik.dws.melt.examples.transformers.recallmatcher.RecallMatcherAnatomy;
import de.uni_mannheim.informatik.dws.melt.examples.transformers.recallmatcher.RecallMatcherKgTrack;
import de.uni_mannheim.informatik.dws.melt.matching_data.TestCase;
import de.uni_mannheim.informatik.dws.melt.matching_data.Track;
import de.uni_mannheim.informatik.dws.melt.matching_data.TrackRepository;
import de.uni_mannheim.informatik.dws.melt.matching_eval.ExecutionResultSet;
import de.uni_mannheim.informatik.dws.melt.matching_eval.Executor;
import de.uni_mannheim.informatik.dws.melt.matching_eval.evaluator.EvaluatorCSV;
import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherPipelineYAAAJena;
import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.external.matcher.SimpleStringMatcher;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.textExtractors.TextExtractorAllAnnotationProperties;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.textExtractors.TextExtractorFallback;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.textExtractors.TextExtractorUrlFragment;
import de.uni_mannheim.informatik.dws.melt.matching_ml.python.PythonServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.uni_mannheim.informatik.dws.melt.matching_ml.python.nlptransformers.TransformersFineTuner;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is also the main class that will be run when executing the JAR.
 */
public class Main {


    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        //CLI setup:
        Options options = new Options();

        options.addOption(Option.builder("g")
                .longOpt("gpu")
                .hasArg()
                .desc("Which GPUs to use. This can be comma separated. Eg. 0,1 which uses GPU zero and one.")
                .build());

        options.addOption(Option.builder("tc")
                .longOpt("transformerscache")
                .hasArg()
                .desc("The file path to the transformers cache.")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("python")
                .hasArg()
                .desc("The python command to use.")
                .build());

        options.addOption(Option.builder("c")
                .longOpt("cache")
                .hasArg()
                .argName("path")
                .desc("The path to the cache folder for ontologies.")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Print this help message.")
                .build());

        options.addOption(Option.builder("tm")
                .longOpt("transformermodels")
                .hasArgs()
                .required()
                .valueSeparator(' ')
                .desc("The transformer models to be used, separated by space.")
                .build());

        options.addOption(Option.builder("tracks")
                .longOpt("tracks")
                .hasArgs()
                .valueSeparator(' ')
                .desc("The tracks to be used, separated by spaces.")
                .build()
        );

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar ", options);
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("java -jar ", options);
            System.exit(1);
        }
        if (cmd.hasOption("p")) {
            String p = cmd.getOptionValue("p");
            LOGGER.info("Setting python command to {}", p);
            PythonServer.setPythonCommandBackup(p);
        }

        if (cmd.hasOption("c")) {
            File cacheFile = new File(cmd.getOptionValue("c"));
            Track.setCacheFolder(cacheFile);
        }

        String gpu = cmd.getOptionValue("g", "");
        File transformersCache = null;
        if (cmd.hasOption("tc")) {
            transformersCache = new File(cmd.getOptionValue("tc"));
        }

        if (cmd.hasOption("tm")) {
            transformerModels = cmd.getOptionValues("tm");
        }

        String[] trackStrings;
        if (cmd.hasOption("tracks")) {
            trackStrings = cmd.getOptionValues("tracks");
            for (String trackString : trackStrings) {
                trackString = trackString.toLowerCase(Locale.ROOT).trim();
                switch (trackString) {
                    case "conference":
                        tracks.add(TrackRepository.Conference.V1);
                        break;
                    case "anatomy":
                        tracks.add(TrackRepository.Anatomy.Default);
                        break;
                    case "kg":
                    case "knowledge-graphs":
                    case "knowledgegraphs":
                    case "knowledgegraph":
                        tracks.add(TrackRepository.Knowledgegraph.V4);
                        break;
                    default:
                        System.out.println("Could not map track: " + trackString);
                }
            }
        }

        if (tracks.size() == 0) {
            System.out.println("No tracks specified. Using anatomy...");
            tracks.add(TrackRepository.Anatomy.Default);
        }

        zeroShotEvaluation(gpu, transformerModels, transformersCache, tracks);
    }

    // just used for testing!
    static List<Track> tracks = new ArrayList<>();
    static String[] transformerModels;


    static void fineTunedPerTestCase(String gpu, List<Track> tracks, Float[] fractions, String[] transformerModels,
                                     File transformersCache, File targetDir) {
        if(!isOk(transformerModels, tracks)){
            return;
        }

        List<TestCase> trainingTrack =
                TrackRepository.generateTrackWithSampledReferenceAlignment(TrackRepository.Knowledgegraph.V4, 0.5,
                41, false);

        TextExtractorFallback extractorFallback = new TextExtractorFallback(
                new TextExtractorAllAnnotationProperties(),
                new TextExtractorUrlFragment()
        );

        ExecutionResultSet ers = new ExecutionResultSet();
        for (String model : transformerModels) {
            for(TestCase tc : trainingTrack) {
                // Step 1: Training
                // ----------------

                File finetunedModelFile = new File(targetDir, model + "_" + tc.getName());

                // Step 1.1.: Running the test case
                TransformersFineTuner fineTuner = new TransformersFineTuner(extractorFallback, model,
                        finetunedModelFile);
                MatcherPipelineYAAAJena trainingPipelineMatcher = new MatcherPipelineYAAAJena() {
                    @Override
                    protected List<MatcherYAAAJena> initializeMatchers() {
                        List<MatcherYAAAJena> result = new ArrayList<>();
                        if(tc.getTrack().equals(TrackRepository.Knowledgegraph.V4)){
                            result.add(new RecallMatcherKgTrack());
                        } else {
                            result.add(new RecallMatcherAnatomy());
                        }
                        result.add(fineTuner);
                        return result;
                    }
                };
                Executor.run(tc, trainingPipelineMatcher);

                // Step 1.2: Fine-Tuning the Model
                try {
                    fineTuner.finetuneModel();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Step 2: Apply Model
                // -------------------
                MatcherYAAAJena matcher;
                if(tc.getTrack().equals(TrackRepository.Knowledgegraph.V4)){
                    matcher = new KnowledgeGraphMatchingPipeline(gpu,
                        finetunedModelFile.getAbsolutePath(), transformersCache);
                } else {
                    matcher = new AnatomyMatchingPipeline(gpu,
                            finetunedModelFile.getAbsolutePath(), transformersCache);
                }
                ers.addAll(Executor.run(tc, matcher, model + "(fine-tuned per TestCase)"));
            }
        }
        EvaluatorCSV evaluator = new EvaluatorCSV(ers);
        evaluator.writeToDirectory();
    }


    /**
     * Performs a zero shot evaluation on the given models using the provided tracks.
     * @param gpu The GPU to be used.
     * @param transformerModels Models (Strings) to be used.
     * @param transformersCache Cache for transformers.
     * @param tracks Tracks to be evaluated.
     * @throws Exception General exception.
     */
    static void zeroShotEvaluation(String gpu, String[] transformerModels, File transformersCache,
                                   List<Track> tracks) throws Exception {
        if(!isOk(transformerModels, tracks)){
            return;
        }

        List<TestCase> testCasesNoKG = new ArrayList<>();
        List<TestCase> testCasesKG = new ArrayList<>();
        for (Track track : tracks) {
            if(track != TrackRepository.Knowledgegraph.V4) {
                testCasesNoKG.addAll(track.getTestCases());
            } else {
                testCasesKG.addAll(track.getTestCases());
            }
        }

        ExecutionResultSet ers = new ExecutionResultSet();

        SimpleStringMatcher ssm = new SimpleStringMatcher();
        ssm.setVerboseLoggingOutput(false);

        // just adding some baseline matchers below:
        if(testCasesNoKG.size() > 0) {
            ers.addAll(Executor.run(testCasesNoKG, new RecallMatcherKgTrack()));
            ers.addAll(Executor.run(testCasesNoKG, new RecallMatcherAnatomy()));
            ers.addAll(Executor.run(testCasesNoKG, ssm));
        }
        if(testCasesKG.size() > 0){
            ers.addAll(Executor.run(testCasesKG, new RecallMatcherKgTrack()));
            ers.addAll(Executor.run(testCasesKG, new RecallMatcherAnatomy()));
            ers.addAll(Executor.run(testCasesKG, ssm));
        }

        for (String transformerModel : transformerModels) {
            System.out.println("Processing transformer model: " + transformerModel);
            try {
                if(testCasesNoKG.size() > 0) {
                    ers.addAll(Executor.run(testCasesNoKG, new AnatomyMatchingPipeline(gpu,
                            transformerModel, transformersCache), transformerModel));
                }
                if(testCasesKG.size() > 0){
                    ers.addAll(Executor.run(testCasesKG, new KnowledgeGraphMatchingPipeline(gpu,
                            transformerModel, transformersCache), transformerModel));
                }
            } catch (Exception e){
                System.out.println("A problem occurred with transformer: '" + transformerModel + "'.\n" +
                        "Continuing process...");
                e.printStackTrace();
            }
        }
        EvaluatorCSV evaluator = new EvaluatorCSV(ers);
        evaluator.writeToDirectory();
    }


    /***
     * Very quick parameter check.
     * @param transformerModels Transformer models to be checked.
     * @param tracks Tracks to be checked.
     * @return True if OK, else false.
     */
    private static boolean isOk(String[] transformerModels, List<Track> tracks){
        if (tracks == null || tracks.size() == 0) {
            System.out.println("No tracks specified. ABORTING program.");
            return false;
        }
        if (transformerModels == null || transformerModels.length == 0) {
            System.out.println("No transformer model specified. ABORTING program.");
            return false;
        }
        return true;
    }
}
