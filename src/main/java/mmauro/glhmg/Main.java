package mmauro.glhmg;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {

    public static final String GOOGLE_MAPS_API_KEY = "AIzaSyAA3PAwW6LvsS61UBcRTb97wuiIQLZuS70";
    public static final ZonedDateTime START_TIME = ZonedDateTime.of(2017, 7, 23, 5, 18, 0, 0, ZoneId.systemDefault());
    public static final ZonedDateTime END_TIME = ZonedDateTime.of(2017, 7, 23, 6, 0, 0, 0, ZoneId.systemDefault());
    public static final Duration GRAB_INTERVAL = Duration.ofSeconds(1);
    public static final File LOCATION_DATA = new File("C:\\Users\\Mauro\\Desktop\\Cronologia delle posizioni.json");
    //public static final File LOCATION_DATA = new File("C:\\Users\\Mauro\\Desktop\\posi.txt");
    public static final File IMAGES_OUTPUT = new File("C:\\Users\\Mauro\\Desktop\\Positions");


    public static final String DATE_TIME_FORMAT = "Format: an ISO-8601 zoned/offset date time (e.g. 2011-12-03T10:15:30+01:00[Europe/Paris]), an ISO-8608 local date time (e.g. 2011-12-03T10:15:30) or an epoch in milliseconds (e.g. 1322903730000). If you pass a local date time your system timezone will be used";


    public static class ExitCodes {
        public static final int ARGS_PARSE_ERROR = 1;

        public static final int INVALID_VERBOSITY = 101;
        public static final int INVALID_API_KEY = 102;
        public static final int INVALID_DATETIME = 103;


        public static final int MISSING_API_KEY = 201;
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
                    OutUtils.err("Invalid verbosity " + value, ExitCodes.INVALID_VERBOSITY);
                } else {
                    OutUtils.setVerbosity(verbosity);
                }
            }

            boolean interactive = commandLine.hasOption('i');
            final Executor executor = new Executor();
            //Params
            if (commandLine.hasOption("api-key")) {
                try {
                    executor.googleStaticMapsApiKey.setValue(commandLine.getOptionValue("api-key"));
                } catch (IllegalArgumentException e) {
                    OutUtils.err(e.getMessage(), ExitCodes.INVALID_API_KEY);
                }
            } else if (!interactive) {
                OutUtils.err("You must provide a Google Static Map API Key", ExitCodes.MISSING_API_KEY);
            }

            //Interactive
            if (interactive) {
                final Scanner inputScanner = new Scanner(System.in);

                //Location history JSON
                if (!executor.locationHistoryJson.hasValue()) {
                    ask(
                            inputScanner,
                            File::new,
                            executor.locationHistoryJson,
                            "Location history JSON file",
                            "A file location (e.g. C:\\Users\\JD\\Desktop\\locations.json",
                            "For starters, I need to know where the Google Location History JSON is located"
                    );
                }

                //Location history JSON
                if (!executor.outputDirectory.hasValue()) {
                    ask(
                            inputScanner,
                            File::new,
                            executor.outputDirectory,
                            "Output directory",
                            "A directory location (e.g. C:\\Users\\JD\\Desktop\\Output",
                            "I need to know where to put all the output images"
                    );
                }

                //Api key
                if (!executor.googleStaticMapsApiKey.hasValue()) {
                    ask(
                            inputScanner,
                            key -> key,
                            executor.googleStaticMapsApiKey,
                            "Google Static Map API Key",
                            "<alphanumeric_string>",
                            "To download maps, I need a Google Static Map API Key. You can get one from https://developers.google.com/maps/documentation/static-maps/get-api-key"
                    );
                }

                //Start date time
                if (!executor.startTime.hasValue()) {
                    ask(
                            inputScanner,
                            Main::parseDateTime,
                            executor.startTime,
                            "Start time",
                            DATE_TIME_FORMAT,
                            "If you want, you can specify the date time from which starting to take location data. Hit enter to skip it"
                    );
                }

                //End date time
                if (!executor.endTime.hasValue()) {
                    ask(
                            inputScanner,
                            Main::parseDateTime,
                            executor.endTime,
                            "End time",
                            DATE_TIME_FORMAT,
                            "If you want, you can specify the ending date time. Hit enter to skip it"
                    );
                }
            }

            executor.execute();
        }
    }

    private static <T> T ask(@NotNull Scanner inputScanner, @NotNull Parser<T> parser, @NotNull Param<T> param, @NotNull String name, @NotNull String format, @NotNull String description) {
        System.out.println(description);
        System.out.println("Format: " + format);
        while (true) {
            try {
                System.out.print(name + ": ");
                final T parsed = parser.parse(inputScanner.nextLine());
                System.out.println();
                try {
                    param.setValue(parsed);
                    return parsed;
                } catch (IllegalArgumentException ex) {
                    System.out.println("Invalid input: " + ex.getMessage());
                }
            } catch (ParseException ex) {
                System.out.println("Unparsable input: " + ex.getMessage());
            }
        }
    }

    @Nullable
    private static Instant parseDateTime(@NotNull String dateTime) throws ParseException {
        if (dateTime.trim().isEmpty()) {
            return null;
        } else {
            try {
                return ZonedDateTime.parse(dateTime).toInstant();
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDateTime.parse(dateTime).toInstant(OffsetDateTime.now().getOffset());
            } catch (DateTimeParseException ignored) {
            }
            try {
                return Instant.ofEpochMilli(Long.parseLong(dateTime));
            } catch (NumberFormatException ignored) {
            }
        }
        throw new ParseException("Invalid date time: " + dateTime);
    }


    /*
     HELP("-h", "--help", "Prints this help message"),
            INTERACTIVE("-i", "--interactive", "Allows to insert parameters interactively. Parameters passed by argument will NOT be asked."),
            VERBOSITY(null, "--verbosity", "Can be either SILENT, STANDARD or VERBOSE "),
            SILENT("-s", "--silent", "Sets the verbosity to SILENT"),
            VERBOSE("-v", "--verbose", "Sets the verbosity to VERBOSE"),
            API_KEY(null, "--api-key", "Sets the Google Static Map API Key. You can get one from https://developers.google.com/maps/documentation/static-maps/get-api-key"),;
     */
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
        options.addOption(Option.builder().longOpt("api-key").hasArg(true).argName("key").desc("Sets the Google Static Map API Key. You can get one from https://developers.google.com/maps/documentation/static-maps/get-api-key").build());

        return options;
    }

}
