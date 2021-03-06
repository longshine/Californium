package ch.ethz.inf.vs.californium.plugtests.tests;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_21: Perform GET transaction containing the ETag option (CON
 * mode)
 * 
 * @author Matthias Kovatsch
 */
public class CC21 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/validate";
	public final ResponseCode EXPECTED_RESPONSE_CODE_A = ResponseCode.CONTENT;
	public final ResponseCode EXPECTED_RESPONSE_CODE_B = ResponseCode.VALID;
	public final ResponseCode EXPECTED_RESPONSE_CODE_C = ResponseCode.CONTENT;

	private byte[] etagStep3;

	public CC21(String serverURI) {
		super(CC21.class.getSimpleName());

		Request request = new Request(Code.GET, Type.CON);
		executeRequest(request, serverURI, RESOURCE_URI);

	}

	@Override
	protected synchronized void executeRequest(Request request, String serverURI, String resourceUri) {

		// defensive check for slash
		if (!serverURI.endsWith("/") && !resourceUri.startsWith("/")) {
			resourceUri = "/" + resourceUri;
		}

		URI uri = null;
		try {
			uri = new URI(serverURI + resourceUri);
		} catch (URISyntaxException use) {
			throw new IllegalArgumentException("Invalid URI: "
					+ use.getMessage());
		}

		request.setURI(uri);

		// print request info
		if (verbose) {
			System.out.println("Request for test " + this.testName
					+ " sent");
			Utils.prettyPrint(request);
		}

		// execute the request
		try {
			Response response = null;
			boolean success = true;

			System.out.println();
			System.out.println("**** TEST: " + testName + " ****");
			System.out.println("**** BEGIN CHECK ****");

			// Part A
			request.send();
			response = request.waitForResponse(6000);

			// checking the response
			if (response != null) {

				// print response info
				if (verbose) {
					System.out.println("Response received");
					System.out.println("Time elapsed (ms): "
							+ response.getRTT());
					Utils.prettyPrint(response);
				}

				success &= checkType(Type.ACK, response.getType());
				success &= checkInt(EXPECTED_RESPONSE_CODE_A.value,
						response.getCode().value, "code");
				success &= hasEtag(response);
				success &= hasNonEmptyPalyoad(response);
				etagStep3 = response.getOptions().getETags().get(0);

				// Part B
				request = new Request(Code.GET, Type.CON);
				request.getOptions().addETag(etagStep3);

				request.setURI(uri);

				request.send();
				response = request.waitForResponse(6000);

				// checking the response
				if (response != null) {

					// print response info
					if (verbose) {
						System.out.println("Response received");
						System.out.println("Time elapsed (ms): "
								+ response.getRTT());
						Utils.prettyPrint(response);
					}

					success &= checkType(Type.ACK, response.getType());
					success &= checkInt(EXPECTED_RESPONSE_CODE_B.value,
							response.getCode().value, "code");
					success &= hasEtag(response);
					success &= checkOption(etagStep3, response.getOptions()
							.getETags().get(0), "ETag");

					request = new Request(Code.PUT, Type.CON);
					request.setURI(uri);
					request.setPayload("It should change",
							MediaTypeRegistry.TEXT_PLAIN);
					request.send();

					Thread.sleep(1000);

					// Part C
					request = new Request(Code.GET, Type.CON);
					request.getOptions().addETag(etagStep3);

					request.setURI(uri);

					request.send();
					response = request.waitForResponse(6000);

					// checking the response
					if (response != null) {

						// print response info
						if (verbose) {
							System.out.println("Response received");
							System.out.println("Time elapsed (ms): "
									+ response.getRTT());
							Utils.prettyPrint(response);
						}

						success &= checkType(Type.ACK, response.getType());
						success &= checkInt(EXPECTED_RESPONSE_CODE_C.value,
								response.getCode().value, "code");
						success &= hasEtag(response);
						success &= checkDifferentOption(etagStep3, response
								.getOptions().getETags().get(0), "ETag");
					}
				}
			}

			if (success) {
				System.out.println("**** TEST PASSED ****");
				addSummaryEntry(testName + ": PASSED");
			} else {
				System.out.println("**** TEST FAILED ****");
				addSummaryEntry(testName + ": --FAILED--");
			}

			tickOffTest();

		} catch (InterruptedException e) {
			System.err.println("Interupted during receive: "
					+ e.getMessage());
			System.exit(-1);
		}
	}

	protected boolean checkResponse(Request request, Response response) {
		return false;
	}

}