import requests

url = "http://localhost:8080/hello"
response = requests.get(url)
print(response.text)