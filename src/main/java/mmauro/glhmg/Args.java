package mmauro.glhmg;

import org.jetbrains.annotations.NotNull;

public class Args {

    @NotNull
    private final String[] args;
    private int index = 0;

    public Args(@NotNull String[] args) {
        this.args = args;
    }

    public String next() {
        if (args.length <= index) {
            return null;
        } else {
            return args[index++];
        }
    }

    public boolean hasNext() {
        return args.length > index;
    }

}
