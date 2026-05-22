import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateValidator implements Validator{
	
	DateTimeFormatter f1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	DateTimeFormatter f3 = DateTimeFormatter.ofPattern("MM/dd/yyyy");
	DateTimeFormatter f4 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	
	public boolean validate(String value){
		if (value == null || value.trim().isEmpty()){
			return false;
		}
		
		DateTimeFormatter[] formatter = {f1, f2, f3, f4};
		for (int i = 0; i < formatter.length; i++){
			try{
				LocalDate date = LocalDate.parse(value.trim(), formatter[i]);
				return true;
			}
			catch (DateTimeParseException e){}
		}
		
		return false;
	}
}