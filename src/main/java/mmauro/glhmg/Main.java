package mmauro.glhmg;

import com.github.fcannizzaro.material.Colors;
import mmauro.glhmg.datastruct.Corrections;
import mmauro.glhmg.datastruct.MapSize;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    @NotNull
    public static final String DATE_TIME_FORMAT = "an ISO-8601 zoned/offset date time (e.g. 2011-12-03T10:15:30+01:00[Europe/Paris]), an ISO-8608 local date time (e.g. 2011-12-03T10:15:30) or an epoch in milliseconds (e.g. 1322903730000). If you pass a local date time your system timezone will be used";

    @NotNull
    public static final Opt<File> OPTION_LOCATION_HISTORY = Opt.<File>builder()
            .longOpt("location-history")
            .desc("The JSON file that contains the Google Location History information")
            .parser(File::new)
            .paramProvider(x -> x.locationHistoryJson)
            .build();

    @NotNull
    public static final Opt<File> OPTION_OUTPUT_DIRECTORY = Opt.<File>builder()
            .longOpt("output-directory")
            .desc("The directory that will contain the output files")
            .parser(File::new)
            .paramProvider(x -> x.outputDirectory)
            .build();

    @NotNull
    public static final Opt<String> OPTION_API_KEY = Opt.<String>builder()
            .longOpt("api-key")
            .desc("The Google Static Map API Key. You can get one from https://developers.google.com/maps/documentation/static-maps/get-api-key")
            .parser(key -> key)
            .paramProvider(x -> x.googleStaticMapsApiKey)
            .build();

    @NotNull
    public static final Opt<Instant> OPTION_START_TIME = Opt.<Instant>builder()
            .longOpt("start-time")
            .desc("The date time from which the software will start to take location data\nFormat: " + DATE_TIME_FORMAT)
            .defValue(null)
            .parser(Utils::parseDateTime)
            .paramProvider(x -> x.startTime)
            .build();

    @NotNull
    public static final Opt<Instant> OPTION_END_TIME = Opt.<Instant>builder()
            .longOpt("end-time")
            .desc("The date time from which the software will end to take location data\nFormat: " + DATE_TIME_FORMAT)
            .defValue(null)
            .parser(Utils::parseDateTime)
            .paramProvider(x -> x.endTime)
            .build();

    @NotNull
    public static final Opt<Duration> OPTION_INTERPOLATION = Opt.<Duration>builder()
            .longOpt("interpolation")
            .desc("An interval at which the data will be interpolated. Format: an ISO-8601 duration format: PnDTnHnMn.nS (e.g. PT10S, PT0.750S)")
            .defValue(null)
            .parser(Duration::parse)
            .paramProvider(x -> x.interpolation)
            .build();

    @NotNull
    public static final Opt<Integer> OPTION_MAP_ZOOM = Opt.<Integer>builder()
            .longOpt("map-zoom")
            .desc("The map zoom level. 1=world, 5=continent, 10=city, 15=streets, 20=buildings")
            .defValue(15)
            .parser(Integer::parseInt)
            .paramProvider(x -> x.mapZoom)
            .build();

    @NotNull
    public static final Opt<MapSize> OPTION_MAP_SIZE = Opt.<MapSize>builder()
            .longOpt("map-size")
            .desc("The map resolution (in pixels)")
            .defValue(new MapSize(512, 512))
            .parser(MapSize::parse)
            .paramProvider(x -> x.mapSize)
            .build();

    @NotNull
    public static final Opt<Integer> OPTION_MAP_SCALE = Opt.<Integer>builder()
            .longOpt("map-scale")
            .desc("A multiplier for the map-size value (e.g. a scale of 2 with a map-size of 512x512 will output a 1024x1024 image)")
            .defValue(1)
            .parser(Integer::parseInt)
            .paramProvider(x -> x.mapScale)
            .build();

    @NotNull
    public static final Opt<Color> OPTION_PATH_COLOR = Opt.<Color>builder()
            .longOpt("path-color")
            .desc("The color of the path")
            .defValue(new Color(0, 0, 0xFF, 0x80))
            .parser(Main::parseColor)
            .paramProvider(x -> x.pathColor)
            .build();

    @NotNull
    public static final Opt<Integer> OPTION_PATH_WEIGHT = Opt.<Integer>builder()
            .longOpt("path-weight")
            .desc("The thickness of the path")
            .defValue(5)
            .parser(Integer::parseInt)
            .paramProvider(x -> x.pathWeight)
            .build();

    @NotNull
    public static final Opt<Corrections> OPTION_COORDINATE_CORRECTIONS = Opt.<Corrections>builder()
            .longOpt("coordinate-corrections")
            .desc("Removes the given comma separated coordinates times from the path (useful if there are some outsider coordinates)")
            .defValue(Corrections.EMPTY)
            .parser(Corrections::parse)
            .paramProvider(x -> x.coordinateCorrections)
            .build();

    @NotNull
    private static final Opt<?>[] OPTIONS = new Opt<?>[]{
            OPTION_LOCATION_HISTORY,
            OPTION_OUTPUT_DIRECTORY,
            OPTION_API_KEY,
            OPTION_START_TIME,
            OPTION_END_TIME,
            OPTION_INTERPOLATION,
            OPTION_MAP_ZOOM,
            OPTION_MAP_SIZE,
            OPTION_MAP_SCALE,
            OPTION_PATH_COLOR,
            OPTION_PATH_WEIGHT,
            OPTION_COORDINATE_CORRECTIONS
    };

    public static class ExitCodes {
        public static final int ARGS_PARSE_ERROR = 1;
        public static final int INVALID_PARAM = 2;
        public static final int MISSING_PARAM = 3;
    }

    public interface Parser<R> {
        R parse(@NotNull String string) throws ParseException;
    }

    public static void main(String[] args) {
        final Options options = getOptions();
        final CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            OutUtils.err("Error parsing arguments: " + e.getMessage(), ExitCodes.ARGS_PARSE_ERROR);
            return;
        }

        if (commandLine.hasOption('h')) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(150, "glhmg", "", options, "", true);
        } else {

            //Verbosity
            if (commandLine.hasOption('v')) {
                OutUtils.setVerbosity(OutUtils.Verbosity.VERBOSE);
            } else if (commandLine.hasOption('s')) {
                OutUtils.setVerbosity(OutUtils.Verbosity.SILENT);
            } else if (commandLine.hasOption("verbosity")) {
                final String value = commandLine.getOptionValue("verbosity");
                OutUtils.Verbosity verbosity = OutUtils.Verbosity.parse(value);
                if (verbosity == null) {
                    OutUtils.err("Invalid verbosity " + value, ExitCodes.INVALID_PARAM);
                } else {
                    OutUtils.setVerbosity(verbosity);
                }
            }
            final Executor executor = new Executor();
            final boolean interactive = commandLine.hasOption('i');
            final Main main = new Main(commandLine, interactive);

            for (Opt<?> opt : OPTIONS) {
                main.setOrAsk(opt, executor);
            }

            executor.execute();
        }
    }

    @NotNull
    private final CommandLine commandLine;
    @Nullable
    private final Scanner inputScanner;

    private Main(@NotNull CommandLine commandLine, boolean interactive) {
        this.commandLine = commandLine;
        this.inputScanner = interactive ? new Scanner(System.in) : null;
    }

    private <T> void setOrAsk(@NotNull Opt<T> opt, @NotNull Executor executor) {
        final Option option = opt.getOption();
        final String name = option.hasLongOpt() ? option.getLongOpt() : option.getOpt();
        final Param<T> param = opt.getParam(executor);
        if (commandLine.hasOption(name)) {
            try {
                param.setValue(opt.getParser().parse(commandLine.getOptionValue(name)));
            } catch (ParseException | IllegalArgumentException e) {
                OutUtils.err("Invalid param " + name + ": " + e.getMessage(), ExitCodes.INVALID_PARAM);
            }
        } else if (inputScanner != null) {
            System.out.println(option.getDescription());
            if(option.hasArgs()) {
                System.out.println("You can pass how many values you want. Empty to end");
            }
            while (true) {
                try {
                    final String defStr;
                    if (opt.hasDefaultValue()) {
                        defStr = "";
                    } else {
                        defStr = opt.getDefaultValueString();
                    }
                    System.out.print(name + defStr + ": ");
                    if (parseValue(opt, param)) {

                    } else if(!option.hasArgs() && opt.hasDefaultValue()) {
                        param.setValue(opt.getDefaultValue().value);
                    }
                } catch (ParseException ex) {
                    System.out.println("Unparsable input: " + ex.getMessage());
                }
            }
        } else if (opt.hasDefaultValue()) {
            param.setValue(opt.getDefaultValue().value);
        } else {
            OutUtils.err("Missing param " + name, ExitCodes.MISSING_PARAM);
        }
    }

    private <T> boolean parseValue(@NotNull Opt<T> opt, Param<T> param) throws ParseException {
        Objects.requireNonNull(inputScanner);

        final String line = inputScanner.nextLine();
        System.out.println();
        if (line.isEmpty()) {
            return false;
        } else {
            final T parsed = opt.getParser().parse(line);
            try {
                param.setValue(parsed);
                return true;
            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid input: " + ex.getMessage());
            }
        }
        return false;
    }

    @NotNull
    private static Color parseColor(@NotNull String str) {
        try {
            return Colors.valueOf(str.replace('-', '_')).asColor();
        } catch (IllegalArgumentException ignored) {
        }
        if (str.matches("^#[0-9A-Fa-f]+$")) {
            final String hex = str.substring(1);
            final boolean hasAlpha, single;

            switch (hex.length()) {
                case 3:
                    hasAlpha = false;
                    single = true;
                    break;
                case 4:
                    hasAlpha = true;
                    single = true;
                    break;
                case 6:
                    hasAlpha = false;
                    single = false;
                    break;
                case 8:
                    hasAlpha = true;
                    single = false;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid length for color: " + str);
            }
            final int[] components = new int[4]; //ARGB components
            int i;
            if (hasAlpha) {
                i = 0;
            } else {
                components[0] = 255;
                i = 1;
            }
            final int increment = single ? 1 : 2;
            for (int strIndex = 0; i < 4; i++, strIndex += increment) {
                if (single) {
                    final int val = Integer.valueOf(String.valueOf(hex.charAt(strIndex)), 16);
                    components[i] = val * 16 + val;
                } else {
                    components[i] = Integer.valueOf(hex.substring(strIndex, strIndex + 2), 16);
                }
            }
            return new Color(components[1], components[2], components[3], components[0]);
        }

        throw new IllegalArgumentException("Invalid color: " + str);
    }

    @NotNull
    private static Options getOptions() {
        final Options options = new Options();

        //HELP
        final OptionGroup helpGroup = new OptionGroup();

        helpGroup.addOption(Option.builder("h").longOpt("help").desc("Prints the help message").build());
        options.addOptionGroup(helpGroup);

        //VERBOSITY
        final OptionGroup verbosityGroup = new OptionGroup();
        verbosityGroup.addOption(Option.builder().longOpt("verbosity").hasArg(true).argName("verbosity").type(OutUtils.Verbosity.class).desc("Can be either SILENT, STANDARD or VERBOSE").build());
        verbosityGroup.addOption(Option.builder("v").longOpt("verbose").desc("Sets the verbosity to VERBOSE").build());
        verbosityGroup.addOption(Option.builder("s").longOpt("silent").desc("Sets the verbosity to SILENT").build());
        options.addOptionGroup(verbosityGroup);


        options.addOption(Option.builder("i").longOpt("interactive").desc("Allows to insert parameters interactively. Parameters passed by argument will NOT be asked").build());
        for (Opt<?> opt : OPTIONS) {
            options.addOption(opt.getOption());
        }

        return options;
    }

}
