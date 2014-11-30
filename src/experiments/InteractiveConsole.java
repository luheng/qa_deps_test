package experiments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * 
 * From:
 * http://stackoverflow.com/questions/104254/java-io-console-support-in-eclipse-ide
 *
 */
public class InteractiveConsole {
    BufferedReader br;
    PrintStream ps;

    public InteractiveConsole() {
        br = new BufferedReader(new InputStreamReader(System.in));
        ps = System.out;
    }

    public String readLine(String out) {
        ps.format(out);
        try {
            return br.readLine();
        } catch(IOException e) {
            return null;
        }
    }
    
    public PrintStream format(String format, Object...objects) {
        return ps.format(format, objects);
    }
    
    public static void main(String[] args) {
    	// Test ...
    	InteractiveConsole console = new InteractiveConsole();
    	for (int i = 0; i < 5; i++) {
    		String input = console.readLine("test:\t");
    		System.out.println("you said:\t" + input);
    	}
    }
}
