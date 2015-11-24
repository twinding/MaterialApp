package dk.tw.opencvtest;

/**
 * This class contains a simple String
 * Used for returning values from anonymous inner classes such as the onClick
 * listeners in dialogs.
 */
public class StringContainer {
    private String string;

    StringContainer(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
