POST /business-central/rest/runtime/org.acme.insurance:policyquote:1.0.0/execute HTTP/1.1
Accept-Encoding: gzip, deflate
Content-Length: 679
Content-Type: application/xml
Host: localhost:8080
Connection: Keep-Alive
User-Agent: org.kie.services.client (1 / zareason/192.168.122.1)
Authorization: Basic amJvc3M6YnJtcw==



<?xml version="1.0" encoding="UTF-8" standalone="yes"?><command-request><deployment-id>org.acme.insurance:policyquote:1.0.0</deployment-id><ver>1</ver><start-process processId="policyquote.policyquoteprocess"><parameter><item key="policy"><value xsi:type="policy" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><driver><age>23</age><creditScore>850</creditScore><driverName>azra</driverName><numberOfAccidents>0</numberOfAccidents><numberOfTickets>1</numberOfTickets></driver><price>0</price><priceDiscount>0</priceDiscount><requestDate>2013-11-25T20:59:18.930-07:00</requestDate><vehicleYear>2010</vehicleYear></value></item></parameter></start-process></command-request>
