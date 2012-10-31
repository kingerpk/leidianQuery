package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;

public class IOUtil {
	
	public static String getStringFromInputStream(InputStream in,String charEncoding){
		
		InputStreamReader inputRead=null;
		try {
			inputRead = new InputStreamReader(in,charEncoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		BufferedReader bufReader=new BufferedReader(inputRead);
		StringBuilder resultBuilder=new StringBuilder();
		try {
			for(String line=bufReader.readLine();line!=null;line=bufReader.readLine())
			{
				resultBuilder.append("\n"+line);
			}
			bufReader.close();
			inputRead.close();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return resultBuilder.toString();
	}
	
	public static void pushData(HttpServletResponse respone, String data)
	{
		OutputStream outputStream;
		try 
		{
			outputStream = respone.getOutputStream();
			OutputStreamWriter outputWriter=new OutputStreamWriter(outputStream, "utf-8");
			outputWriter.write(data);
			outputWriter.flush();
			outputWriter.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
}
