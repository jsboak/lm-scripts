import base64
import hashlib
import hmac
import time
import json
import requests

# Logicmonitor Variables
# Account Info
AccessId =''
AccessKey =''
Company = ''

# Request Info
httpVerb = 'GET'
resourcePath = '/setting/datasources/89130725'
queryParameters = ''
data = ''

#Construct URL 
url = 'https://'+ Company +'.logicmonitor.com/santaba/rest' + resourcePath + queryParameters

#Get current time in milliseconds
epoch = str(int(time.time() * 1000))

#Concatenate Request details
requestVars = httpVerb + epoch + data + resourcePath

#Construct signature
signature = base64.b64encode(hmac.new(AccessKey,msg=requestVars,digestmod=hashlib.sha256).hexdigest())

#Construct headers
auth = 'LMv1 ' + AccessId + ':' + signature + ':' + epoch
headers = {'Content-Type':'application/json','Authorization':auth}

#Make request
response = requests.get(url, data=data, headers=headers)
response_json = json.loads(response.content)

# print(response.content)
my_new_datapoint = json.loads(json.dumps(response_json['data']['dataPoints'][-1]))
my_new_datapoint['name'] = "keyValue5"
my_new_datapoint['postProcessorParam'] = "##WILDVALUE##.keyValue5"
del my_new_datapoint['id']
response_json['data']['dataPoints'].append(my_new_datapoint)

data = json.dumps(response_json['data'])
# print(data)

# Request Info
httpVerb = 'PUT'
resourcePath = '/setting/datasources/89130725'
queryParameters = ''

#Construct URL 
url = 'https://'+ Company +'.logicmonitor.com/santaba/rest' + resourcePath + queryParameters

#Get current time in milliseconds
epoch = str(int(time.time() * 1000))

#Concatenate Request details
requestVars = httpVerb + epoch + data + resourcePath

#Construct signature
signature = base64.b64encode(hmac.new(AccessKey,msg=requestVars,digestmod=hashlib.sha256).hexdigest())

#Construct headers
auth = 'LMv1 ' + AccessId + ':' + signature + ':' + epoch
headers = {'Content-Type':'application/json','Authorization':auth}

#Make request
put_response = requests.put(url, data=data, headers=headers)
# put_response_json = json.loads(response.content)

print(put_response.content)