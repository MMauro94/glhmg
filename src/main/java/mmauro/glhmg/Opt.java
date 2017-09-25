package mmauro.glhmg;

import org.apache.commons.cli.Option;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.ws.Holder;
import java.awt.*;
import java.util.function.Function;

public final class Opt<T> {

    @NotNull
    private final Option option;
    @NotNull
    private final Main.Parser<T> parser;
    @Nullable
    private final Holder<T> defaultValue;
    @NotNull
    private final Function<Executor, Param<T>> paramProvider;

    private Opt(@NotNull Option.Builder option, @NotNull Main.Parser<T> parser, @Nullable Holder<T> defaultValue, @NotNull Function<Executor, Param<T>> paramProvider) {
        this.option = option.build();
        this.parser = parser;
        this.defaultValue = defaultValue;
        this.paramProvider = paramProvider;
    }

    @Contract(pure = true)
    @NotNull
    public Option getOption() {
        return option;
    }

    @Contract(pure = true)
    @NotNull
    public Main.Parser<T> getParser() {
        return parser;
    }

    @Contract(pure = true)
    @NotNull
    public Holder<T> getDefaultValue() {
        if (defaultValue == null) {
            throw new IllegalStateException("no default value");
        } else {
            return defaultValue;
        }
    }

    @Contract(pure = true)
    @Nullable
    public Holder<T> optDefaultValue() {
        return defaultValue;
    }

    @Contract(pure = true)
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    @Contract(pure = true)
    @NotNull
    public String getDefaultValueString() {
        return getDefaultValueString(getDefaultValue());
    }

    @NotNull
    public static String getDefaultValueString(@NotNull Holder<?> defaultValue) {
        if (defaultValue.value == null) {
            return "no value";
        } else if (defaultValue.value instanceof Color) {
            return "#" + Integer.toHexString(((Color) defaultValue.value).getRGB());
        } else {
            return defaultValue.value.toString();
        }
    }

    @Contract(pure = true)
    @NotNull
    public Function<Executor, Param<T>> getParamProvider() {
        return paramProvider;
    }

    @NotNull
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Contract(pure = true)
    public Param<T> getParam(@NotNull Executor executor) {
        return paramProvider.apply(executor);
    }

    public static final class Builder<T> {

        private Builder() {
        }

        private String longOpt, desc;
        private Main.Parser<T> parser;
        private Holder<T> defaultValue;
        private Function<Executor, Param<T>> paramProvider;

        @NotNull
        public Builder<T> longOpt(@NotNull String longOpt) {
            this.longOpt = longOpt;
            return this;
        }

        @NotNull
        public Builder<T> desc(@NotNull String desc) {
            this.desc = desc;
            return this;
        }


        @NotNull
        public Builder<T> parser(@NotNull Main.Parser<T> parser) {
            this.parser = parser;
            return this;
        }

        @NotNull
        public Builder<T> defValue(@Nullable T defaultValue) {
            this.defaultValue = new Holder<>(defaultValue);
            return this;
        }

        @NotNull
        public Builder<T> paramProvider(@NotNull Function<Executor, Param<T>> paramProvider) {
            this.paramProvider = paramProvider;
            return this;
        }

        @NotNull
        public Opt<T> build() {
            final String desc;
            if (defaultValue != null) {
                desc = "[Optional] " + this.desc + (defaultValue.value == null ? "" : ". Default value is " + Opt.getDefaultValueString(defaultValue));
            } else {
                desc = this.desc;
            }
            return new Opt<>(Option.builder().longOpt(longOpt).hasArg(true).desc(desc), parser, defaultValue, paramProvider);
        }
    }
}
