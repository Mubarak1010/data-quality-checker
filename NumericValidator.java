public class NumericValidator implements Validator {

    public boolean validate(String value) {
        if (value == null || value.trim().isEmpty()){
			return false;
		}

        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}