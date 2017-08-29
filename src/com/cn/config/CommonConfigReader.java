package com.cn.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Properties;

import com.cn.res.IConstants;

public class CommonConfigReader extends Properties {

	private static final long serialVersionUID = 1L;

	private String fileName;

	public static void main(String[] args) throws IOException {
		CommonConfigReader properties = new CommonConfigReader();
		properties.loadProperties();
		System.out.println(properties.get(CommonConfigEnum.NumberOfPreferredNeighbors.toString()));
	}

	public CommonConfigReader() {
		this.fileName = IConstants.COMMON_FILENAME;
	}

	public void loadProperties() throws IOException {
		FileReader commonReader = new FileReader(this.fileName);
		this.load(commonReader);
	}

	@Override
	public synchronized void load(Reader reader) {

		BufferedReader in = new BufferedReader(reader);
		int i = 0;
		try {
			for (String line; (line = in.readLine()) != null; i++) {
				line = line.trim();
				if ((line.length() <= 0) || (line.startsWith(IConstants.COMMENT_CHAR))) {
					continue;
				}
				String[] tokens = line.split("\\s+");
				if (tokens.length != 2) {
					throw new IOException(new ParseException(line, i));

				}
				setProperty(tokens[0].trim(), tokens[1].trim());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
