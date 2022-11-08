package uniresolver.driver.did.bid;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import foundation.identity.did.VerificationMethod;
import foundation.identity.did.jsonld.DIDKeywords;
import foundation.identity.jsonld.JsonLDKeywords;
import foundation.identity.jsonld.JsonLDUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import uniresolver.ResolutionException;
import uniresolver.driver.Driver;
import uniresolver.result.ResolveDataModelResult;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class DidBidDriver implements Driver {

	public static final String DEFAULT_BID_URL = "https://bidresolver.bitfactory.cn";

	public static final HttpClient DEFAULT_HTTP_CLIENT = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

	private String bidUrl = DEFAULT_BID_URL;
	private HttpClient httpClient = DEFAULT_HTTP_CLIENT;

	public DidBidDriver() {

	}



	@Override
	public ResolveDataModelResult resolve(DID did, Map<String, Object> resolveOptions) throws ResolutionException {
		Gson gson=new Gson();
		// isAddress
		Boolean isAddress=UtilsTool.encAddressValid(did.getDidString());
		if(!isAddress) {
		//	return null;
            throw new ResolutionException("Invalid BID " + did.getDidString());
		}
		log.info("isAddress", isAddress,did.getDidString());
		// fetch data from BID
		String resolveUrl = this.getBidUrl() + "/" + did.getDidString();
		HttpGet httpGet = new HttpGet(resolveUrl);
		// find the DDO
		JsonObject jsonResponse;
		try {
			CloseableHttpResponse httpResponse = (CloseableHttpResponse) this.getHttpClient().execute(httpGet);
			if(httpResponse.getStatusLine().getStatusCode() != 200) {
				throw new ResolutionException("Cannot retrieve DDO for `" + did.getDidString() + "` from `" + this.getBidUrl() + ": " + httpResponse.getStatusLine());
			}

			// extract payload
			HttpEntity httpEntity = httpResponse.getEntity();
			String entityString = EntityUtils.toString(httpEntity);

			EntityUtils.consume(httpEntity);
			// check if exist identifier
			if (entityString == null) {
				throw new ResolutionException("docuement not exist");
			}
			jsonResponse = gson.fromJson(entityString, JsonObject.class);
			log.info("jsonDataElement："+jsonResponse.toString());
			if(jsonResponse.get("errorCode").getAsInt() != 0){
				throw new ResolutionException("docuement not exist");
			}
		} catch (IOException ex) {
			throw new ResolutionException("Cannot retrieve DDO info for `" + did.getDidString() + "` from `" + this.getBidUrl() + "`: " + ex.getMessage(), ex);
		} catch (JSONException jex) {
			throw new ResolutionException("Cannot parse JSON response from `" + this.getBidUrl() + "`: " + jex.getMessage(), jex);
		}

		JsonObject jsonDataObject=jsonResponse.getAsJsonObject("data");
		JsonObject didDocumentObject= jsonDataObject == null ? null : jsonDataObject.getAsJsonObject("didDocument");

		Set<URI> contexts = new HashSet<>(Arrays.asList(DIDDocument.DEFAULT_JSONLD_CONTEXTS));
		List<VerificationMethod>  allVerificationMethods = new ArrayList<>();
		JsonArray publicKey = didDocumentObject == null ? null : didDocumentObject.getAsJsonArray("publicKey");
		if (publicKey != null) {
			for (JsonElement publicKeyElement : publicKey) {
				JsonObject jsonObject = publicKeyElement == null ? null : publicKeyElement.getAsJsonObject();
				if(jsonObject !=null){

					VerificationMethod verificationMethodKey = VerificationMethod.builder().build();
					if (jsonObject.get("id") != null) {
						JsonLDUtils.jsonLdAddAll(verificationMethodKey, Map.of(
								"id", jsonObject.get("id").getAsString()
						));
					}
					if (jsonObject.get("controller") != null) {
						JsonLDUtils.jsonLdAddAll(verificationMethodKey, Map.of(
								"controller", jsonObject.get("controller").getAsString()
						));
					}
					if (jsonObject.get("type") != null) {
						JsonLDUtils.jsonLdAddAll(verificationMethodKey, Map.of(
								"type", jsonObject.get("type").getAsString()
						));
					}
					if (jsonObject.get("publicKeyHex") != null) {
						JsonLDUtils.jsonLdAddAll(verificationMethodKey, Map.of(
								"publicKeyHex", jsonObject.get("publicKeyHex").getAsString()
						));
					}
					allVerificationMethods.add(verificationMethodKey);
				}
			}
		}

		List<VerificationMethod>  authenticationVerificationMethods = new ArrayList<>();
		JsonArray authentication = didDocumentObject == null ? null : didDocumentObject.getAsJsonArray("authentication");
		if (authentication != null) {
			for (JsonElement element : authentication) {
				VerificationMethod verificationMethodKey = VerificationMethod.builder()
						.id(URI.create(element.getAsString()))
						.build();
				authenticationVerificationMethods.add(verificationMethodKey);
			}
		}
		List<Service> services = new ArrayList<>();
		JsonArray service = didDocumentObject == null ? null : didDocumentObject.getAsJsonArray("service");
		if (service != null) {
			for (JsonElement serviceElement : service) {
				JsonObject jsonObject = serviceElement == null ? null : serviceElement.getAsJsonObject();
				if(jsonObject !=null){
				Service service2 = Service.builder()
						.id(jsonObject.get("id") == null ? null : URI.create(jsonObject.get("id").getAsString()))
						.type(jsonObject.get("type") == null ? null : jsonObject.get("type").getAsString())
						.serviceEndpoint(jsonObject.get("serviceEndpoint") == null ? null : jsonObject.get("serviceEndpoint").getAsString())
						.build();

				String serverType = jsonObject.get("serverType") == null ? null : jsonObject.get("serverType").getAsString();
				if (jsonObject.get("serverType") != null) {
					JsonLDUtils.jsonLdAddAll(service2, Map.of(
							"serverType", jsonObject.get("serverType").getAsInt()
					));
				}
				if (jsonObject.get("protocol") != null) {
					JsonLDUtils.jsonLdAddAll(service2, Map.of(
							"protocol", jsonObject.get("protocol").getAsInt()
					));
				}

				if (jsonObject.get("serverType") != null && serverType.equals("1") && (jsonObject.get("port") != null)) {
					JsonLDUtils.jsonLdAddAll(service2, Map.of(
							"port", jsonObject.get("port").getAsBigInteger()
					));
				}
				if (jsonObject.get("version") != null) {
					JsonLDUtils.jsonLdAddAll(service2, Map.of(
							"version", jsonObject.get("version").getAsString()
					));
				}
				services.add(service2);
			  }
			}
		}
		// create Method METADATA
		Map<String, Object> methodMetadata = new LinkedHashMap<>();
        JsonObject proof = didDocumentObject == null ? null : didDocumentObject.getAsJsonObject("proof");
		if(proof!=null)methodMetadata.put("proof", gson.fromJson(proof, Map.class));

		DIDDocument didDocument = DIDDocument.builder()
				.defaultContexts(false)
				.verificationMethods(allVerificationMethods)
				.authenticationVerificationMethods(authenticationVerificationMethods)
				.contexts(new ArrayList<>(contexts))
				.id(did.toUri())
				.services(services)
				.build();

		//alsoKnownAs
		JsonArray alsoKnownAs = didDocumentObject==null?null: didDocumentObject.getAsJsonArray("alsoKnownAs");
		if (alsoKnownAs != null) {
            List<Map<String, Object>> alsoKnownAsL = new ArrayList<>();
			for (JsonElement element : alsoKnownAs) {
				JsonObject jsonObject = element == null ? null : element.getAsJsonObject();
				if(jsonObject !=null){
					Map<String, Object> map = new LinkedHashMap<>();
					if(jsonObject.get("id")!=null)map.put("id",jsonObject.get("id").getAsString());
					if(jsonObject.get("type")!=null)map.put("type",jsonObject.get("type").getAsInt());
					if(!map.isEmpty())alsoKnownAsL.add(map);
				}
			}
            JsonLDUtils.jsonLdAddAsJsonArray(didDocument, "alsoKnownAs",alsoKnownAsL);
		}

		JsonObject extensionObject= didDocumentObject==null?null: didDocumentObject.getAsJsonObject("extension");
		//acsms
		List<String> acsnsL = new ArrayList<>();
		JsonArray acsns = extensionObject == null ? null : extensionObject.getAsJsonArray("acsns");
		if (acsns != null) {
			for (JsonElement element : acsns) {
				String  val=element.getAsString();
				if(val!=null)acsnsL.add(val);
			}
		}
		//attributes
		List<Map<String, Object>> attributesList = new ArrayList<>();
		JsonArray attributes = extensionObject == null ? null : extensionObject.getAsJsonArray("attributes");
		if (attributes != null) {
			for (JsonElement element : attributes) {
				JsonObject jsonObject = element == null ? null : element.getAsJsonObject();
				if(jsonObject !=null){
					Map<String, Object> map = new LinkedHashMap<>();
					if(jsonObject.get("encrypt") !=null ) map.put("encrypt",jsonObject.get("encrypt").getAsInt());
					if(jsonObject.get("format") !=null ) map.put("format",jsonObject.get("format").getAsString());
					if(jsonObject.get("value") !=null ) map.put("value",jsonObject.get("value").getAsString());
					if(jsonObject.get("key") !=null ) map.put("key",jsonObject.get("key").getAsString());
					if(jsonObject.get("desc") !=null ) map.put("desc",jsonObject.get("desc").getAsString());
					if(!map.isEmpty())attributesList.add(map);
				}
			}
		}

		//delegateSign
		Map<String, Object> delegateSignMap = new LinkedHashMap<>();
		JsonObject delegateSign= extensionObject == null ? null : extensionObject.getAsJsonObject("delegateSign");
		if (delegateSign != null) {
			String signatureValue=delegateSign.get("signatureValue")==null?null:delegateSign.get("signatureValue").getAsString();
			String signer=delegateSign.get("signer")==null?null:delegateSign.get("signer").getAsString();
			if(signatureValue!=null)delegateSignMap.put("signatureValue",signatureValue);
            if(signer!=null)delegateSignMap.put("signer",signer);
		}
		//recovery
		List<String> recoveryList = new ArrayList<>();
		JsonArray recovery = extensionObject == null ? null : extensionObject.getAsJsonArray("recovery");
		if (recovery != null) {
			for (JsonElement element : recovery) {
				String  val=element.getAsString();
				if(val !=null)recoveryList.add(val);
			}
		}
		String ttl= extensionObject == null ? null : extensionObject.get("ttl") == null ? null :extensionObject.get("ttl").getAsString();
		String type= extensionObject == null ? null : extensionObject.get("type") == null ? null :extensionObject.get("type").getAsString();
		//verifiableCredentialsList
		List<Map<String, Object>> verifiableCredentialsList = new ArrayList<>();
		JsonArray verifiableCredentials = extensionObject == null ? null : extensionObject.getAsJsonArray("verifiableCredentials");
		if (verifiableCredentials != null) {
			for (JsonElement element : verifiableCredentials) {
				JsonObject jsonObject = element == null ? null : element.getAsJsonObject();
				if(jsonObject!=null){
					Map<String, Object> map = new LinkedHashMap<>();
					if(jsonObject.get("id")!=null)map.put("id",jsonObject.get("id").getAsString());
					if(jsonObject.get("type")!=null)map.put("type",jsonObject.get("type").getAsString());
					verifiableCredentialsList.add(map);
				}
			}
		}

		Map<String, Object> extensionMap = new LinkedHashMap<>();
		if(acsnsL.size()>0)extensionMap.put("acsns",acsnsL);
		if(attributesList.size()>0)extensionMap.put("attributes",attributesList);
		if(delegateSignMap.size()>0)extensionMap.put("delegateSign",delegateSignMap);
		if(recoveryList.size()>0)extensionMap.put("recovery",recoveryList);
		if(ttl!=null)extensionMap.put("ttl",extensionObject.get("ttl").getAsLong());
		if(type!=null)extensionMap.put("type",extensionObject.get("type").getAsInt());
		if(verifiableCredentialsList.size()>0)extensionMap.put("verifiableCredentials",verifiableCredentialsList);
		if(!extensionMap.isEmpty()){
            JsonLDUtils.jsonLdAddAll(didDocument, Map.of(
                    "extension", extensionMap
            ));
        }

       if(didDocumentObject!=null){
          if(didDocumentObject.get("version")!=null) JsonLDUtils.jsonLdAddAll(didDocument, Map.of(
               "version", didDocumentObject.get("version").getAsString()
             ));
		   if(didDocumentObject.get("created")!=null) methodMetadata.put("created", didDocumentObject.get("created").getAsString());
		   if(didDocumentObject.get("updated")!=null) methodMetadata.put("updated", didDocumentObject.get("updated").getAsString());

       }

		ResolveDataModelResult  resolveResult = ResolveDataModelResult.build(null, didDocument, methodMetadata);
		// done
		return resolveResult;
	}

	@Override
	public Map<String, Object> properties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("BidUrl", this.getBidUrl());

		return properties;
	}

	/*
	 * Getters and setters
	 */

	public String getBidUrl() {
		return bidUrl;
	}

	public void setBidUrl(String bidUrl) {
		this.bidUrl = bidUrl;
	}

	public HttpClient getHttpClient() {

		return this.httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {

		this.httpClient = httpClient;
	}


}
