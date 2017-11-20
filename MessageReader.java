import java.io.BufferedReader;
import java.io.IOException;


public class MessageReader implements Runnable{
	
	private BufferedReader in;
	private String sender;
	private boolean printing;

	MessageReader(BufferedReader in){
		this.in = in;
		printing = true;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		String inputLine;
		//System.out.println("In mr");
		try {
			while(printing && (inputLine = in.readLine()) != null){
				System.out.println(inputLine);
				if(inputLine.equals("CLOSE")){ //If other person says CLOSE, then this will close this message reader.
					System.out.println("Chat ended. Press enter to continue.");
					this.setPrinting(false);
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Chat closed.");
			this.setPrinting(false);
		} catch (NullPointerException e){
			System.out.println("mr2");
		}
		
	}
	
	public void setPrinting(boolean status){
		this.printing = status;
	}
	
	public boolean getPrinting(){
		return printing;
	}
	
	

}
