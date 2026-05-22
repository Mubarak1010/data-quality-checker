public class LengthValidator implements Validator{
	private int validLength;
	
	public boolean validate(String value){
		int length = value.length();
		
		if (length != validLength){
			return false;
		}
		else{
			return true;
		}
	}
	
	public void setValidLength(int validLength){
		this.validLength = validLength;
	}
}