/*******************************************************************************
 * Copyright (c) 2014, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
/**
 * 
 */
package ch.ethz.inf.vs.californium.examples;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.CaliforniumLogger;
import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.resources.Resource;

/**
 * Class container of the tests.
 * 
 * @author Francesco Corazza
 */
public class PlugtestClient {
	
	static {
		CaliforniumLogger.initialize();
		CaliforniumLogger.setLevel(Level.INFO);
	}

	public static final int PLUGTEST_BLOCK_SZX = 2; // 64 bytes

	/** The server uri. */
	private String serverURI = null;

	/** The test map. */
	private final Map<String, Class<?>> testMap = new HashMap<String, Class<?>>();

	/** The test list. */
	protected List<String> testsToRun = new ArrayList<String>();
	
	/**
	 * Default constructor. Loads with reflection each nested class that is a
	 * derived type of TestClientAbstract.
	 * 
	 * @param serverURI
	 *            the server uri
	 */
	public PlugtestClient(String serverURI) {
		if (serverURI == null || serverURI.isEmpty()) {
			throw new IllegalArgumentException("No server URI given");
		}
		
		this.serverURI = serverURI;

		// fill the map with each nested class not abstract that instantiate
		// TestClientAbstract
		for (Class<?> clientTest : this.getClass().getDeclaredClasses()) {
			if (!Modifier.isAbstract(clientTest.getModifiers())
					&& (clientTest.getSuperclass() == TestClientAbstract.class)) {

				this.testMap.put(clientTest.getSimpleName(), clientTest);
			}
		}
	}

	/**
	 * Instantiates the given testNames or if null all tests implemented.
	 * 
	 * @param testNames
	 *            the test names
	 */
	public void instantiateTests(String... testNames) {

		Catalog catalog = new Catalog();
		
		try {
			List<Report> reports = new ArrayList<Report>();

			Arrays.sort(testNames);
			List<Class<?>> tests = catalog.getTestsClasses(testNames);
			
			// iterate for each chosen test
			for (Class<?> testClass : tests) {
				System.out.println("Initialize test "+testClass); // DEBUG

				// get the unique constructor
				Constructor<?>[] constructors = testClass
						.getDeclaredConstructors();

				if (constructors.length == 0) {
					System.err.println("constructors.length == 0");
					System.exit(-1);
				}

				// inner class: first argument (this) is the enclosing instance
				TestClientAbstract testClient = (TestClientAbstract) constructors[0]
						.newInstance(serverURI);
				testClient.waitForUntilTestHasTerminated();
				reports.add(testClient.getReport());
			}
			
			System.out.println("\n==== SUMMARY ====");
			for (Report report : reports) {
				report.print();
			}

		} catch (InstantiationException e) {
			System.err.println("Reflection error");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("Reflection error");
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println("Reflection error");
			e.printStackTrace();
		} catch (SecurityException e) {
			System.err.println("Reflection error");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("Reflection error");
			e.printStackTrace();
		}
	}

	public synchronized void tickOffTest() {
		notify();
	}

	/**
	 * Main entry point.
	 * Start the program with arguments coap://localhost:5683 .* to start
	 * all tests.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {

		if (args.length == 0) {
			
			Catalog catalog = new Catalog();
			
			System.out.println("\nCalifornium (Cf) Plugtest Client");
			System.out
					.println("(c) 2014, Institute for Pervasive Computing, ETH Zurich");
			System.out.println();
			System.out.println("Usage: " + PlugtestClient.class.getSimpleName() + " [-s] URI [TESTNAMES...]");
			System.out.println("  -s        : Skip the ping in case the remote does not implement it");
			System.out.println("  URI       : The CoAP URI of the Plugtest server to test (coap://...)");
			System.out.println("  TESTNAMES : A list of specific tests to run, omit to run all");
			System.out.println();
			System.out.println("Available tests:");
			System.out.print(" ");
			
			for (String name:catalog.getAllTestNames()) {
				System.out.print(" " + name);
			}
			System.exit(-1);
		}
		
		int first = 0;
		if (args[0].equals("-s")) ++first;
		String uri = args[first++];
		
		// allow quick hostname as argument
		if (!uri.startsWith("coap://")) {
			uri = "coap://" + uri;
		}
		
		// Config used for plugtest
		NetworkConfig.getStandard()
				.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 64) 
				.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 64);
		
		if (first==1) {
			if (ping(uri)) {
				System.out.println("PASS: " + uri + " responds to ping");
			} else {
				System.out.println("FAIL: Not responding to ping");
				System.exit(-1);
			}
		}

		// create the factory with the given server URI
		PlugtestClient clientFactory = new PlugtestClient(uri);

		// instantiate the chosen tests
		clientFactory.instantiateTests(Arrays.copyOfRange(args, first, args.length));

		System.exit(0);
	}

	/**
	 * Abstract class to support various test client implementations.
	 * 
	 * @author Francesco Corazza
	 */
	public static abstract class TestClientAbstract {

		protected Report report = new Report();
		
		protected Semaphore terminated = new Semaphore(0);
		
		/** The test name. */
		protected String testName = null;

		/** The verbose. */
		protected boolean verbose = true;

		/**
		 * Use synchronous or asynchronous requests. Sync recommended due to
		 * single threaded servers and slow resources.
		 */
		protected boolean sync = true;

		/**
		 * Instantiates a new test client abstract.
		 * 
		 * @param testName
		 *            the test name
		 * @param verbose
		 *            the verbose
		 */
		public TestClientAbstract(String testName, boolean verbose,
				boolean synchronous) {
			if (testName == null || testName.isEmpty()) {
				throw new IllegalArgumentException(
						"testName == null || testName.isEmpty()");
			}

			this.testName = testName;
			this.verbose = verbose;
			this.sync = synchronous;
		}

		/**
		 * Instantiates a new test client abstract.
		 * 
		 * @param testName
		 *            the test name
		 */
		public TestClientAbstract(String testName) {
			this(testName, false, true);
		}

		/**
		 * Execute request.
		 * 
		 * @param request
		 *            the request
		 * @param serverURI
		 *            the server uri
		 * @param resourceUri
		 *            the resource uri
		 * @param payload
		 *            the payload
		 */
		protected void executeRequest(Request request, String serverURI, String resourceUri) {

			// defensive check for slash
			if (!serverURI.endsWith("/") && !resourceUri.startsWith("/")) {
				resourceUri = "/" + resourceUri;
			}

			URI uri = null;
			try {
				uri = new URI(serverURI + resourceUri);
			} catch (URISyntaxException use) {
				System.err.println("Invalid URI: " + use.getMessage());
			}

			request.setURI(uri);

			request.addMessageObserver(new TestResponseHandler(request));

			// print request info
			if (verbose) {
				System.out.println("Request for test " + this.testName
						+ " sent");
				Utils.prettyPrint(request);
			}

			// execute the request
			try {
				request.send();
				if (sync) {
					request.waitForResponse(5000);
				}
			} catch (InterruptedException e) {
				System.err.println("Interupted during receive: "
						+ e.getMessage());
				System.exit(-1);
			}
		}

		public synchronized void addSummaryEntry(String entry) {
			report.addEntry(entry);
		}
		
		public Report getReport() {
			return report;
		}

		public synchronized void tickOffTest() {
			terminated.release();
		}
		
		public void waitForUntilTestHasTerminated() {
			try {
				terminated.acquire();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * The Class TestResponseHandler.
		 */
		protected class TestResponseHandler extends MessageObserverAdapter {

			private Request request;

			public TestResponseHandler(Request request) {
				this.request = request;
			}

			@Override
			public void onResponse(Response response) {
				System.out.println();
				System.out.println("**** TEST: " + testName + " ****");

				// checking the response
				if (response != null) {

					// print response info
					if (verbose) {
						System.out.println("Response received");
						System.out.println("Time elapsed (ms): "
								+ response.getRTT());
						Utils.prettyPrint(response);
					}

					System.out.println("**** BEGIN CHECK ****");
					
					if (checkResponse(request, response)) {
						System.out.println("**** TEST PASSED ****");
						addSummaryEntry(testName + ": PASSED");
					} else {
						System.out.println("**** TEST FAILED ****");
						addSummaryEntry(testName + ": --FAILED--");
					}

					tickOffTest();
				}
			}
		}

		/**
		 * Check response.
		 * 
		 * @param request
		 *            the request
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected abstract boolean checkResponse(Request request,
				Response response);

		/**
		 * Check int.
		 * 
		 * @param expected
		 *            the expected
		 * @param actual
		 *            the actual
		 * @param fieldName
		 *            the field name
		 * @return true, if successful
		 */
		protected boolean checkInt(int expected, int actual, String fieldName) {
			boolean success = expected == actual;

			if (!success) {
				System.out.println("FAIL: Expected " + fieldName + ": "
						+ expected + ", but was: " + actual);
			} else {
				System.out.println("PASS: Correct " + fieldName
						+ String.format(" (%d)", actual));
			}

			return success;
		}

		/**
		 * Check int.
		 * 
		 * @param expected
		 *            the expected
		 * @param actual
		 *            the actual
		 * @param fieldName
		 *            the field name
		 * @return true, if successful
		 */
		protected boolean checkInts(int[] expected, int actual, String fieldName) {
			boolean success = false;
			for (int i : expected) {
				if (i == actual) {
					success = true;
					break;
				}
			}

			if (!success) {
				System.out.println("FAIL: Expected " + fieldName + ": "
						+ Arrays.toString(expected) + ", but was: " + actual);
			} else {
				System.out.println("PASS: Correct " + fieldName
						+ String.format(" (%d)", actual));
			}

			return success;
		}

		/**
		 * Check String.
		 * 
		 * @param expected
		 *            the expected
		 * @param actual
		 *            the actual
		 * @param fieldName
		 *            the field name
		 * @return true, if successful
		 */
		protected boolean checkString(String expected, String actual,
				String fieldName) {
			boolean success = expected.equals(actual);

			if (!success) {
				System.out.println("FAIL: Expected " + fieldName + ": \""
						+ expected + "\", but was: \"" + actual + "\"");
			} else {
				System.out.println("PASS: Correct " + fieldName + " \""
						+ actual + "\"");
			}

			return success;
		}

		/**
		 * Check type.
		 * 
		 * @param expectedMessageType
		 *            the expected message type
		 * @param actualMessageType
		 *            the actual message type
		 * @return true, if successful
		 */
		protected boolean checkType(Type expectedMessageType,
				Type actualMessageType) {
			boolean success = expectedMessageType.equals(actualMessageType);

			if (!success) {
				System.out.printf("FAIL: Expected type %s, but was %s\n",
						expectedMessageType, actualMessageType);
			} else {
				System.out.printf("PASS: Correct type (%s)\n",
						actualMessageType.toString());
			}

			return success;
		}

		/**
		 * Check types.
		 * 
		 * @param expectedMessageTypes
		 *            the expected message types
		 * @param actualMessageType
		 *            the actual message type
		 * @return true, if successful
		 */
		protected boolean checkTypes(Type[] expectedMessageTypes,
				Type actualMessageType) {
			boolean success = false;
			for (Type messageType : expectedMessageTypes) {
				if (messageType.equals(actualMessageType)) {
					success = true;
					break;
				}
			}

			if (!success) {
				StringBuilder sb = new StringBuilder();
				for (Type messageType : expectedMessageTypes) {
					sb.append(", " + messageType.toString());
				}
				sb.delete(0, 2); // delete the first ", "

				System.out.printf("FAIL: Expected type %s, but was %s\n", "[ "
						+ sb.toString() + " ]", actualMessageType);
			} else {
				System.out.printf("PASS: Correct type (%s)\n",
						actualMessageType.toString());
			}

			return success;
		}

		/**
		 * Checks for Content-Type option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasContentType(Response response) {
			boolean success = response.getOptions().hasContentFormat()
					|| response.getPayloadSize()==0
					|| !CoAP.ResponseCode.isSuccess(response.getCode());

			if (!success) {
				System.out.println("FAIL: Response without Content-Type");
			} else {
				System.out.printf("PASS: Content-Type (%s)\n",
						MediaTypeRegistry.toString(response.getOptions()
								.getContentFormat()));
			}

			return success;
		}

		/**
		 * Checks for Location-Path option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasLocation(Response response) {
			// boolean success =
			// response.hasOption(OptionNumberRegistry.LOCATION_PATH);
			boolean success = response.getOptions().getLocationPathCount() > 0;

			if (!success) {
				System.out.println("FAIL: Response without Location");
			} else {
				System.out.printf("PASS: Location (%s)\n", response
						.getOptions().getLocationPathString());
			}

			return success;
		}

		/**
		 * Checks for ETag option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasEtag(Response response) {
			// boolean success = response.hasOption(OptionNumberRegistry.ETAG);
			boolean success = response.getOptions().getETagCount() > 0;

			if (!success) {
				System.out.println("FAIL: Response without Etag");
			} else {
				System.out.printf(
						"PASS: Etag (%s)\n",
						Utils.toHexString(response.getOptions().getETags()
								.get(0)));
			}

			return success;
		}

		/**
		 * Checks for not empty payload.
		 * 
		 * @param response
		 *            the response
		 * @return true, if not empty payload
		 */
		protected boolean hasNonEmptyPalyoad(Response response) {
			boolean success = response.getPayload().length > 0;

			if (!success) {
				System.out.println("FAIL: Response with empty payload");
			} else {
				System.out.printf("PASS: Payload not empty \"%s\"\n",
						response.getPayloadString());
			}

			return success;
		}

		/**
		 * Checks for Max-Age option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasMaxAge(Response response) {
			// boolean success =
			// response.hasOption(OptionNumberRegistry.MAX_AGE);
			boolean success = response.getOptions().hasMaxAge();

			if (!success) {
				System.out.println("FAIL: Response without Max-Age");
			} else {
				System.out.printf("PASS: Max-Age (%s)\n", response.getOptions()
						.getMaxAge());
			}

			return success;
		}

		/**
		 * Checks for Location-Query option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasLocationQuery(Response response) {
			// boolean success =
			// response.hasOption(OptionNumberRegistry.LOCATION_QUERY);
			boolean success = response.getOptions().getLocationQueryCount() > 0;

			if (!success) {
				System.out.println("FAIL: Response without Location-Query");
			} else {
				System.out.printf("PASS: Location-Query (%s)\n", response
						.getOptions().getLocationQueryString());
			}

			return success;
		}

		/**
		 * Checks for Token option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasToken(Response response) {
			boolean success = response.getToken() != null;

			if (!success) {
				System.out.println("FAIL: Response without Token");
			} else {
				System.out.printf("PASS: Token (%s)\n",
						Utils.toHexString(response.getToken()));
			}

			return success;
		}

		/**
		 * Checks for absent Token option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasNoToken(Response response) {
			boolean success = response.hasEmptyToken();

			if (!success) {
				System.out.println("FAIL: Expected no token but had "
						+ Utils.toHexString(response.getToken()));
			} else {
				System.out.printf("PASS: No Token\n");
			}

			return success;
		}

		/**
		 * Checks for Observe option.
		 * 
		 * @param response
		 *            the response
		 * @return true, if successful
		 */
		protected boolean hasObserve(Response response, boolean invert) {
			// boolean success =
			// response.hasOption(OptionNumberRegistry.OBSERVE);
			boolean success = response.getOptions().hasObserve();

			// invert to check for not having the option
			success ^= invert;

			if (!success) {
				System.out.println("FAIL: Response without Observe");
			} else if (!invert) {
				System.out.printf("PASS: Observe (%d)\n",
				// response.getFirstOption(OptionNumberRegistry.OBSERVE).getIntValue());
						response.getOptions().getObserve().intValue());
			} else {
				System.out.println("PASS: No Observe");
			}

			return success;
		}

		protected boolean hasObserve(Response response) {
			return hasObserve(response, false);
		}

		protected boolean checkOption(Option expextedOption, Option actualOption) {
			// boolean success = actualOption!=null &&
			// expextedOption.getOptionNumber()==actualOption.getOptionNumber();
			boolean success = actualOption != null
					&& expextedOption.getNumber() == actualOption.getNumber();

			if (!success) {
				System.out.printf("FAIL: Missing option nr %d\n",
						expextedOption.getNumber());
			} else {

				// raw value byte array can be different, although value is the
				// same
				success &= expextedOption.toString().equals(
						actualOption.toString());

				if (!success) {
					System.out.printf("FAIL: Expected %s, but was %s\n",
							expextedOption.toString(), actualOption.toString());
				} else {
					System.out.printf("PASS: Correct option (%s)\n",
							actualOption.toString());
				}
			}

			return success;
		}

		protected boolean checkOption(BlockOption expected, BlockOption actual,
				String optionName) {
			boolean success = expected == null ? actual == null : expected
					.equals(actual);

			if (!success) {
				System.out.println("FAIL: option " + optionName + ": expected "
						+ expected + " but was " + actual);
			} else {
				System.out.println("PASS: Correct option " + actual);
			}

			return success;
		}

		protected boolean checkOption(byte[] expectedOption,
				byte[] actualOption, String optionName) {
			boolean success = Arrays.equals(expectedOption, actualOption);

			if (!success) {
				System.out.println("FAIL: Option " + optionName + ": expected "
						+ Utils.toHexString(expectedOption) + " but was "
						+ Utils.toHexString(actualOption));
			} else {
				System.out.printf("PASS: Correct option %s\n", optionName);
			}

			return success;
		}

		protected boolean checkOption(List<String> expected,
				List<String> actual, String optionName) {
			// boolean success = expected.size() == actual.size();
			boolean success = expected.equals(actual);

			if (!success) {
				System.out.println("FAIL: Option " + optionName + ": expected "
						+ expected + " but was " + actual);
			} else {
				System.out.printf("PASS: Correct option %s\n", optionName);
			}

			return success;
		}

		protected boolean checkOption(Integer expected, Integer actual,
				String optionName) {
			boolean success = expected == null ? actual == null : expected
					.equals(actual);

			if (!success) {
				System.out.println("FAIL: Option " + optionName + ": expected "
						+ expected + " but was " + actual);
			} else {
				System.out.printf("PASS: Correct option %s\n", optionName);
			}

			return success;
		}

		protected boolean checkDifferentOption(Option expextedOption,
				Option actualOption) {
			// boolean success = actualOption!=null &&
			// expextedOption.getOptionNumber()==actualOption.getOptionNumber();
			boolean success = actualOption != null
					&& expextedOption.getNumber() == actualOption.getNumber();

			if (!success) {
				System.out.printf("FAIL: Missing option nr %d\n",
						expextedOption.getNumber());
			} else {

				// raw value byte array can be different, although value is the
				// same
				success &= !expextedOption.toString().equals(
						actualOption.toString());

				if (!success) {
					System.out.printf(
							"FAIL: Expected difference, but was %s\n",
							actualOption.toString());
				} else {
					System.out.printf("PASS: Expected not %s and was %s\n",
							expextedOption.toString(), actualOption.toString());
				}
			}

			return success;
		}

		protected boolean checkDifferentOption(byte[] expected, byte[] actual,
				String optionName) {
			boolean success = !Arrays.equals(expected, actual);

			if (!success) {
				System.out.println("FAIL: Option " + optionName + ": expected "
						+ Utils.toHexString(expected) + " but was "
						+ Utils.toHexString(actual));
			} else {
				System.out.println("PASS: Correct option " + optionName);
			}

			return success;
		}

		/**
		 * Check token.
		 * 
		 * @param expectedToken
		 *            the expected token
		 * @param actualToken
		 *            the actual token
		 * @return true, if successful
		 */
		protected boolean checkToken(byte[] expectedToken, byte[] actualToken) {

			boolean success = true;

			if (expectedToken == null || expectedToken.length == 0) {

				success = actualToken == null || actualToken.length == 0;

				if (!success) {
					System.out.printf(
							"FAIL: Expected empty token, but was %s\n",
							Utils.toHexString(actualToken));
				} else {
					System.out.println("PASS: Correct empty token");
				}

				return success;

			} else {

				success = actualToken.length <= 8;
				success &= actualToken.length >= 1;

				// eval token length
				if (!success) {
					System.out
							.printf("FAIL: Expected token %s, but %s has illeagal length\n",
									Utils.toHexString(expectedToken),
									Utils.toHexString(actualToken));
					return success;
				}

				success &= Arrays.equals(expectedToken, actualToken);

				if (!success) {
					System.out.printf("FAIL: Expected token %s, but was %s\n",
							Utils.toHexString(expectedToken),
							Utils.toHexString(actualToken));
				} else {
					System.out.printf("PASS: Correct token (%s)\n",
							Utils.toHexString(actualToken));
				}

				return success;
			}
		}

		protected boolean checkDiscovery(String expextedResource, String actualDiscovery) {
			return actualDiscovery.contains("<"+expextedResource+">");
		}
		
		/**
		 * Check discovery.
		 * 
		 * @param expextedAttribute
		 *            the resource attribute to filter
		 * @param actualDiscovery
		 *            the reported Link Format
		 * @return true, if successful
		 */
		protected boolean checkDiscoveryAttributes(String expextedAttribute, String actualDiscovery) {

			if (actualDiscovery == "") {
				System.err.println("Empty Link Format, check manually");
				return false;
			}

			Resource res = LinkParser.parseTree(actualDiscovery);

			List<String> query = Arrays.asList(expextedAttribute);

			boolean success = true;

			for (Resource sub : res.getChildren()) {

				// goes to leaf resource -- necessary?
				while (sub.getChildren().size() > 0) {
					sub = sub.getChildren().iterator().next();
				}

				success &= LinkFormat.matches(sub, query);

				if (!success) {
					System.out.printf("FAIL: Expected %s, but was %s\n",
							expextedAttribute,
							LinkFormat.serializeResource(sub));
				}
			}

			if (success) {
				System.out.println("PASS: Correct Link Format filtering");
			}

			return success;
		}

	}
	
	private static boolean ping(String address) {
		try {
			Request request = new Request(null);
			request.setType(Type.CON);
			request.setToken(new byte[0]);
			request.setURI(address);

            System.out.println("++++++ Sending Ping ++++++");
			request.send().waitForResponse(5000);
			return request.isRejected();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String getLargeRequestPayload() {
		return new StringBuilder()
				.append("/-------------------------------------------------------------\\\n")
				.append("|                  Request BLOCK NO. 1 OF 5                   |\n")
				.append("|               [each line contains 64 bytes]                 |\n")
				.append("\\-------------------------------------------------------------/\n")
				.append("/-------------------------------------------------------------\\\n")
				.append("|                  Request BLOCK NO. 2 OF 5                   |\n")
				.append("|               [each line contains 64 bytes]                 |\n")
				.append("\\-------------------------------------------------------------/\n")
				.append("/-------------------------------------------------------------\\\n")
				.append("|                  Request BLOCK NO. 3 OF 5                   |\n")
				.append("|               [each line contains 64 bytes]                 |\n")
				.append("\\-------------------------------------------------------------/\n")
				.append("/-------------------------------------------------------------\\\n")
				.append("|                  Request BLOCK NO. 4 OF 5                   |\n")
				.append("|               [each line contains 64 bytes]                 |\n")
				.append("\\-------------------------------------------------------------/\n")
				.append("/-------------------------------------------------------------\\\n")
				.append("|                  Request BLOCK NO. 5 OF 5                   |\n")
				.append("|               [each line contains 64 bytes]                 |\n")
				.append("\\-------------------------------------------------------------/\n")
				.toString();
	}
}
