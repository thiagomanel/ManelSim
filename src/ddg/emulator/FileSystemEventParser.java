/**
 * Copyright (C) 2009 Universidade Federal de Campina Grande
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ddg.emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import ddg.emulator.events.filesystem.CloseEvent;
import ddg.emulator.events.filesystem.ReadEvent;
import ddg.emulator.events.filesystem.UnlinkEvent;
import ddg.emulator.events.filesystem.WriteEvent;
import ddg.kernel.Event;
import ddg.kernel.Time;
import ddg.model.DDGClient;

/**
 * A parser for the trace of calls to the file system.
 * 
 * @author thiago - thiago@lsd.ufcg.edu.br
 * @author Patrick Maia - patrickjem@lsd.ufcg.edu.br
 */
public class FileSystemEventParser implements EventParser {

	private final BufferedReader bufferedReader;

	private final DDGClient client;

	/**
	 * @param traceStream
	 * @throws IOException
	 */
	public FileSystemEventParser(InputStream traceStream, DDGClient client) {
		this.bufferedReader = new BufferedReader(new InputStreamReader(traceStream));
		this.client = client;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Event getNextEvent() {

		String traceLine;

		try {
			traceLine = readNextLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (traceLine == null)
			return null;

		StringTokenizer tokenizer = new StringTokenizer(traceLine);
		String operation = tokenizer.nextToken();

		if (operation.equals("read")) {
			return parseReadEvent(tokenizer);
		} else if (operation.equals("write")) {
			return parseWriteEvent(tokenizer);
		} else if (operation.equals("close")) {
			return parseCloseEvent(tokenizer);
		} else if (operation.equals("unlink")) {
			return parseUnlinkEvent(tokenizer);
		} else {
			return getNextEvent();
		}

	}

	private String readNextLine() throws IOException {

		String readLine = null;

		while ((readLine = bufferedReader.readLine()) != null) {

			if (!readLine.trim().equals("") && !readLine.startsWith("#")) {
				return readLine;
			}

		}

		return readLine;
	}
	
	private UnlinkEvent parseUnlinkEvent(StringTokenizer tokenizer) {
		// unlink  begin-elapsed   fullpath
		
		Time time = parseTime(tokenizer.nextToken());
		String targetPath = tokenizer.nextToken();
		
		return new UnlinkEvent(client, time, targetPath);
	}

	private CloseEvent parseCloseEvent(StringTokenizer tokenizer) {
		// close   begin-elapsed   fullpath

		Time time = parseTime(tokenizer.nextToken());
		String targetPath = tokenizer.nextToken();

		return new CloseEvent(client, time, targetPath);
	}

	private ReadEvent parseReadEvent(StringTokenizer tokenizer) {
		// read    begin-elapsed   fullpath        length
		
		Time time = parseTime(tokenizer.nextToken());
		String filePath = tokenizer.nextToken();
		long length = Long.parseLong(tokenizer.nextToken());

		return new ReadEvent(client, time, filePath, length);
	}

	private Event parseWriteEvent(StringTokenizer tokenizer) {
		// write   begin-elapsed   fullpath        length
		
		Time time = parseTime(tokenizer.nextToken());
		String filePath = tokenizer.nextToken();
		long length = Long.parseLong(tokenizer.nextToken());

		return new WriteEvent(client, time, length, filePath); 
	}
	
	private Time parseTime(String traceTimestamp) {
		long timeInMilli = 
				Long.parseLong(traceTimestamp.split("-")[0]) / 1000;
		
		return new Time(timeInMilli);
	}

}