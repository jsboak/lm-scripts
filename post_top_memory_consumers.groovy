import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPatch
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import groovy.json.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

TotalVisibleMemorySize = output['TOTALVISIBLEMEMORYSIZE'].toFloat()
FreePhysicalMemory = output['FREEPHYSICALMEMORY'].toFloat()

MemUtilizationPercent = ((TotalVisibleMemorySize-FreePhysicalMemory)/TotalVisibleMemorySize)*100

def host_id = hostProps.get("system.deviceId")
def hostname = hostProps.get("system.sysname")
String[] command = ["powershell",
                    "-Command",
                    "Invoke-Command -Session (New-PSSession -ComputerName ${hostname}) -ScriptBlock {ps | sort -des WS | select ProcessName -f 10 | ft -a; sleep 1; cls}"
                    ];

if( MemUtilizationPercent > 56 ) { 
    
    Process process = new ProcessBuilder(command).start();
    process.waitFor()
    InputStream inputStream = process.getInputStream();
    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    
    String output = bufferedReader.readLines().toString()
    
    processes = (output =~ /(\w+)/)
    
    process_list = []
    
    processes.each {
        
        if(it[0] != "ProcessName") {
            process_list.add( it[0] )
        }
    }
    
    Runtime rt = Runtime.getRuntime();
    Process proc = rt.exec("taskkill /F /IM powershell.exe");
    Process proc2 = rt.exec("taskkill /F /IM cmd.exe");
    
    patch_request = lm_put_request(
        "/device/devices/${host_id}",
        "?v=2&patchFields=customProperties&opType=add",
        '{"customProperties":[{"name":"top_memory_processes","value":"' + process_list + '"}]}'
        )
        
    if (patch_request[1] == 200) {
        
        return 200
        
    } else {
        
        return 400
        
    }
} else {
    
    return 600
    
}
    
def lm_put_request(resourcePath,queryParams,data) {
    
    accessId = hostProps.get('lmaccessid.key'); //This is your LM API Access ID
    accessKey = hostProps.get('lmaccesskey.key'); //This is your LM API Access Key
    account = '';

    File file = new File('../conf/agent.conf');
    file.text.eachLine
    {
            if (it =~ /company=/)
            {
                    account = it.split('=')[1].trim();
            }
    }
    
    CloseableHttpClient httpclient = HttpClients.createDefault();
    epoch = System.currentTimeMillis();
    
    httpVerb = 'PATCH'
    url = "https://" + account + ".logicmonitor.com" + "/santaba/rest" + resourcePath + queryParams;
    StringEntity params = new StringEntity(data,ContentType.APPLICATION_JSON);
    
    //calculate signature
    requestVars = httpVerb + epoch + data + resourcePath;
    
    hmac = Mac.getInstance('HmacSHA256');
    secret = new SecretKeySpec(accessKey.getBytes(), 'HmacSHA256');
    hmac.init(secret);
    hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
    signature = hmac_signed.bytes.encodeBase64();
    
    // Execute the POST = putting seconary devices into SDT
    httpPost = new HttpPatch(url);
    httpPost.addHeader('Authorization' , 'LMv1 ' + accessId + ':' + signature + ':' + epoch);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setEntity(params);
    response = httpclient.execute(httpPost);
    responseBody = EntityUtils.toString(response.getEntity());
    code = response.getStatusLine().getStatusCode();
    allResponse = new JsonSlurper().parseText(responseBody);

    return [allResponse,code]

}

def calcSignature(verb,resourcePath,accessKey,accessId,data) {
	epoch = System.currentTimeMillis();
	requestVars = verb + epoch + data + resourcePath;
	hmac = Mac.getInstance("HmacSHA256");
	secret = new SecretKeySpec(accessKey.getBytes(), "HmacSHA256");
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();
	headers = [:]
	headers.put("Authorization" , "LMv1 " + accessId + ":" + signature + ":" + epoch)
	return headers;
}