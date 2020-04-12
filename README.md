# keycloak-phone-authenticator

To install the SMS Authenticator one has to:

* Build and package the project:
  * `$ mvn package`

* Add the jar to the Keycloak server:
  * `$ cp target/keycloak-phone-authenticator-*.jar _KEYCLOAK_HOME_/providers/`
  
## Reset credentials with phone

![WeChat13144f2eccd289afc7df3120fdb82abb](https://user-images.githubusercontent.com/6838487/67840807-a7481880-fb31-11e9-9fa8-7e165832b2aa.png)

![image](https://user-images.githubusercontent.com/6838487/67840943-f2fac200-fb31-11e9-97ca-6d1c19628070.png)

## Build into keycloak

[An example shows how to build into keycloak using docker](examples/README.md)

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

And then use `Verification Code` authentication flow with the code to obtain an access code.

```
curl -X POST \
  http://localhost:8081/auth/realms/$YOUR_REALM/protocol/openid-connect/token \
  -H 'authorization: Basic c2h1bmRhby1hZG1pbjoxODc3MGYxMi02NjE4LTQwOTctYThmYi1kMjA0Mzg0Mzg4OTk=' \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&phone_number=$PHONE_NUMBER&code=$VERIFICATION_CODE'
```

## Configuration

Configure your REALM to use the phone number and verification code Authentication.
First create a new REALM (or select a previously created REALM).

Under Authentication > Flows:
* Copy the 'Direct Grant' flow to 'Direct grant with phone' flow
* Click on 'Actions > Add execution' on the 'Provide Phone Number' line
* Click on 'Actions > Add execution' on the 'Provide Verification Code' line
* Set both of 'Provide Phone Number' and 'Provide Verification Code' to 'REQUIRED'
