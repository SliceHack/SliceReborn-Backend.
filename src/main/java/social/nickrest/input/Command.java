package social.nickrest.input;

import lombok.Getter;

import java.util.Arrays;

@Getter
public abstract class Command {

    private final CommandInfo info = getClass().getAnnotation(CommandInfo.class);

    private final String name;
    private final String[] aliases;

    public Command() {
        if(info == null) throw new IllegalArgumentException("CommandInfo annotation not found on class " + getClass().getSimpleName());

        this.name = info.name();
        this.aliases = info.aliases();
    }

    public abstract void handle(String name, String[] args);

    public boolean check(String name) {
        return this.name.equalsIgnoreCase(name) || Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase(name));
    }
}
