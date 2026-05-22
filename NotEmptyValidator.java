import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NotEmptyValidator implements Validator {

    // Common "empty-like" values found in real-world data
    private static final Set<String> EMPTY_LIKE = new HashSet<>(Arrays.asList(
        "n/a", "na", "null", "none", "nil", "undefined",
        "-", "--", "---", "0000", "00/00/0000", "unknown", "?"
    ));

    public boolean validate(String value) {
        if (value == null) return false;

        String trimmed = value.trim();

        if (trimmed.isEmpty()) return false;

        if (EMPTY_LIKE.contains(trimmed.toLowerCase())) return false;

        return true;
    }
}