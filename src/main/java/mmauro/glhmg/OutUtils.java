package mmauro.glhmg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OutUtils {

    public enum Verbosity {
        SILENT,
        STANDARD,
        VERBOSE;

        public boolean canPrint(@NotNull Verbosity verbosity) {
            return ordinal() >= verbosity.ordinal();
        }

        @Nullable
        public static Verbosity parse(@Nullable String str) {
            for (Verbosity verb : values()) {
                if (verb.name().equalsIgnoreCase(str)) {
                    return verb;
                }
            }
            return null;
        }
    }

    private static Verbosity verbosity = Verbosity.STANDARD;

    public static void setVerbosity(Verbosity verbosity) {
        if (OutUtils.verbosity.canPrint(verbosity)) {
            OutUtils.verbosity = verbosity;
        }
    }

    public static void println(@NotNull Verbosity verbosity, String str) {
        if (OutUtils.verbosity.canPrint(verbosity)) {
            System.out.println(str);
        }
    }
    public static void standard(String str) {
        println(Verbosity.STANDARD, str);
    }

    public static void verbose(String str) {
        println(Verbosity.VERBOSE, str);
    }

    public static void err(String str, int status) {
        err(str, status, null);
    }

    public static void err(String str, int status, @Nullable Throwable throwable) {
        System.out.println("ERROR: " + str);
        if (throwable != null) {
            throwable.printStackTrace();
        }
        System.exit(status);
    }
}
