public class EmailValidator implements Validator{
	
	public boolean validate(String value){
		return value != null && value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}
}