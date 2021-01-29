import java.util.Date;
import java.util.Scanner;
import java.io.File; 
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.io.FileNotFoundException;

public class PreAssignment {
		
		public static void main(String [] args) {
			String username;
			String message = " ";
			Pre_assignment info = new Pre_assignment();
			Scanner lukija = new Scanner(System.in);
			
			
			//pyydetään käyttäjännimi
			System.out.println("Username >");
			username = lukija.nextLine();
			info.setUsername(username);
			
			while (!message.equals("0")) {
					
					//syöttämällä nollan ohjelma tulostaa viimeksi syötetyn viestin
					System.out.println("To display your last message type 0");
					System.out.println("Type your message >");
					message = lukija.nextLine();
					
					//kirjoittaa viestit tiedostoon niin kauan kun syötetty viesti ei ole nolla
					if(!message.equals("0")) {
						
						info.setMessage(message);
						
						//luodaan aikaleima
						String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
						info.setTime(time); 
			
						//kirjoitetaan tiedostoon
						try {
							FileWriter myWriter = new FileWriter("user.txt", true);
							myWriter.write(info.getTime() +" <" +info.getUsername() +"> " +info.getMessage() +"\n");
							myWriter.close();
						} catch (IOException e2) {
							System.out.println("An error occurred.");
							e2.printStackTrace();
					}
				}
			} 
			
			//tulostetaan viimeisin viesti aikaleiman ja käyttäjänimen kanssa
			System.out.println("(" +info.getTime() + ")" +" <" +info.getUsername() +"> " +info.getMessage() +"\n");
			
			lukija.close();
		}
	
}
