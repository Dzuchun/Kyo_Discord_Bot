import java.io.*;
import java.util.Scanner;
public class startbot {
	public static void main(String[] args) throws IOException, InterruptedException{
		Scanner sc=  new Scanner(new File("startscript.txt"));
		Runtime runtime = Runtime.getRuntime();
		while(sc.hasNext()){
			String command = sc.nextLine();
			if (args.length > 0 && command.startsWith("java")) command += " " + args[0];
			System.out.println("Executing " + command);
			Process proc = runtime.exec(command);

			InputStream stdIn = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdIn);
			BufferedReader br = new BufferedReader(isr);

			String line = null;
			System.out.println("<OUTPUT>");

			while ((line = br.readLine()) != null)
    			System.out.println(line);

			System.out.println("</OUTPUT>");
			int exitVal = proc.waitFor();
			System.out.println("Process exitValue: " + exitVal);
	}
}
}