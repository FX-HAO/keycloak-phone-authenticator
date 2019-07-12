# keycloak-phone-authenticator

To install the SMS Authenticator one has to:

* Build and package the project:
  * `$ mvn package`

* Add the jar to the Keycloak server:
  * `$ cp target/keycloak-phone-authenticator-*.jar _KEYCLOAK_HOME_/providers/`
  
## Installation

This needs an SMS implementation to enable sending verification codes. There are some implementations below:  

  * [YunTongXun SMS Implementation](https://github.com/FX-HAO/keycloak-phone-authenticator-yuntongxun-sms)
  
Or you can implement your own SPIs.

Once you add an SMS SPI implementation, you can request a verification code with
  
```
curl -X POST \
  http://127.0.0.1:8081/auth/realms/$YOUR_REALM/verification_codes/ \
  -H 'content-type: application/json' \
  -d '{
	"phoneNumber": $PHONE_NUMBER
}'
```

And then use the code to login with `Verification Code` authentication flow.

## Configuration

Configure your REALM to use the phone number and verification code Authentication.
First create a new REALM (or select a previously created REALM).

Under Authentication > Flows:
* Copy the 'Direct Grant' flow to 'Direct grant with phone' flow
* Click on 'Actions > Add execution' on the 'Provide Phone Number' line
* Click on 'Actions > Add execution' on the 'Provide Verification Code' line
* Set both of 'Provide Phone Number' and 'Provide Verification Code' to 'REQUIRED'
