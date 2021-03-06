package io.seldon.engine.api.rest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.kubernetes.client.proto.IntStr.IntOrString;
import io.kubernetes.client.proto.Meta.Time;
import io.kubernetes.client.proto.Meta.Timestamp;
import io.kubernetes.client.proto.Resource.Quantity;
import io.seldon.engine.pb.IntOrStringUtils;
import io.seldon.engine.pb.JsonFormat;
import io.seldon.engine.pb.QuantityUtils;
import io.seldon.engine.pb.TimeUtils;
import io.seldon.engine.predictors.EnginePredictor;
import io.seldon.engine.service.InternalPredictionService;
import io.seldon.protos.DeploymentProtos.PredictorSpec;
import io.seldon.protos.PredictionProtos.SeldonMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	    "management.security.enabled=false",
	})
public class TestRandomABTest {
	
	protected String readFile(String path, Charset encoding) 
			  throws IOException 
	 {
		 byte[] encoded = Files.readAllBytes(Paths.get(path));
		 return new String(encoded, encoding);
	 }	
	
	private <T extends Message.Builder> void updateMessageBuilderFromJson(T messageBuilder, String json) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields()
        .usingTypeParser(IntOrString.getDescriptor().getFullName(), new IntOrStringUtils.IntOrStringParser())
        .usingTypeParser(Quantity.getDescriptor().getFullName(), new QuantityUtils.QuantityParser())
        .usingTypeParser(Time.getDescriptor().getFullName(), new TimeUtils.TimeParser())
        .usingTypeParser(Timestamp.getDescriptor().getFullName(), new TimeUtils.TimeParser()) 
        .merge(json, messageBuilder);
    }
	
	@Autowired
	private WebApplicationContext context;
	
	@Autowired
	EnginePredictor enginePredictor;
	
	
    //@Autowired
    private MockMvc mvc;
    
    @Autowired
    RestClientController restController;
    
    @Before
	public void setup() throws Exception {
    	mvc = MockMvcBuilders
				.webAppContextSetup(context)
				.build();
	}
    
    @LocalServerPort
    private int port;
    
    @Mock
    private RestTemplate restTemplate;

    @Autowired
    private InternalPredictionService internalPredictionService;

   

    @Test
    public void testModelMetrics() throws Exception
    {
    	String jsonStr = readFile("src/test/resources/abtest.json",StandardCharsets.UTF_8);
    	String responseStr = readFile("src/test/resources/response_with_metrics.json",StandardCharsets.UTF_8);
    	PredictorSpec.Builder PredictorSpecBuilder = PredictorSpec.newBuilder();
    	updateMessageBuilderFromJson(PredictorSpecBuilder, jsonStr);
    	PredictorSpec predictorSpec = PredictorSpecBuilder.build();
    	final String predictJson = "{" +
         	    "\"request\": {" + 
         	    "\"ndarray\": [[1.0]]}" +
         		"}";
    	enginePredictor.setPredictorSpec(predictorSpec);
    	
    	
    	ResponseEntity<String> httpResponse = new ResponseEntity<String>(responseStr, null, HttpStatus.OK);
    	Mockito.when(restTemplate.postForEntity(Matchers.<URI>any(), Matchers.<HttpEntity<MultiValueMap<String, String>>>any(), Matchers.<Class<String>>any()))
    		.thenReturn(httpResponse);
    	internalPredictionService.setRestTemplate(restTemplate);
    	
    	int routeACount = 0;
    	int routeBCount = 1;
    			
    	for(int i=0;i<100;i++)
    	{
    		MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
        			.accept(MediaType.APPLICATION_JSON_UTF8)
        			.content(predictJson)
        			.contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
        	String response = res.getResponse().getContentAsString();
        	System.out.println(response);
        	Assert.assertEquals(200, res.getResponse().getStatus());
        	
        	SeldonMessage.Builder builder = SeldonMessage.newBuilder();
    	    JsonFormat.parser().ignoringUnknownFields().merge(response, builder);
    	    SeldonMessage seldonMessage = builder.build();
    	    
    	    Assert.assertTrue(seldonMessage.getMeta().getRoutingMap().get("abtest") >= 0);
    	    if (seldonMessage.getMeta().getRoutingMap().get("abtest") == 0)
    	    	routeACount++;
    	    else
    	    	routeBCount++;
    	}
    	double split = routeACount /(double)(routeACount + routeBCount);
 	    System.out.println(routeACount);
 	    System.out.println(routeBCount);
 	    Assert.assertEquals(0.5, split,0.2);
	    
	    
	  
    }
    
}
