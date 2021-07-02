# keycloak-phone-authenticator

### Keycloak 13 is supported. If you want to use this with a lower version, you may need to use tag v9.0.2

To install the SMS Authenticator one has to:

* Build and package the project:
  * `$ mvn package`

* Add the jar to the Keycloak server:
  * `$ cp target/keycloak-phone-authenticator-*.jar _KEYCLOAK_HOME_/providers/`

## Log in via SMS

![image](https://user-images.githubusercontent.com/81133379/118950667-fd3b1600-b98c-11eb-9877-b84b38677ddf.png)

To enable this you need to:

1. copy the 'Browser' flow to 'Copy Of Browser' flow (Under Authentication > Flows)
2. Using the `Actions` menu on the right-hand side of the 'Copy Of Browser Forms' subflow, select 'Add execution', using the drop-down select 'Username Password Or Phone Form'. Move it up to be the top. (Optional: if you want to enable Google Recaptcha, using the `Actions` menu on the right-hand side of the 'Username Password Or Phone Form', select 'Config' and enter in the site key and secret, see more: [Google Recaptcha](https://wjw465150.gitbooks.io/keycloak-documentation/content/server_admin/topics/users/recaptcha.html))
3. Remove 'Username Password Form'
4. In the 'Bindings' menu, change the browser flow from 'Browser' to 'Copy Of Browser'

'Copy Of Reset Credentials' flow shoule be like this:
![image](https://user-images.githubusercontent.com/81133379/118952761-cb2ab380-b98e-11eb-880f-9bee98461390.png)

## Reset credentials via SMS

![WeChat13144f2eccd289afc7df3120fdb82abb](https://user-images.githubusercontent.com/6838487/67840807-a7481880-fb31-11e9-9fa8-7e165832b2aa.png)

To enable this you need to:

1. copy the 'Reset Credentials' flow to 'Copy Of Reset Credentials' flow (Under Authentication > Flows)
2. Click 'Add execution' to add provider 'Reset Credential With Phone' and move it to be the top (Optional: if you want to enable Google Recaptcha, using the `Actions` menu on the right-hand side of the 'Reset Credential With Phone', select 'Config' and enter in the site key and secret, see more: [Google Recaptcha](https://wjw465150.gitbooks.io/keycloak-documentation/content/server_admin/topics/users/recaptcha.html))
3. Click 'Add execution' to add provider 'Send Reset Email If Not Phone' and move it to be the second
4. Remove 'Choose User' and 'Send Reset Email'
5. In the 'Bindings' menu, change the reset credential flow from 'reset credentials' to 'Copy Of Reset Credential'

'Copy Of Reset Credentials' flow shoule be like this:
![image](https://user-images.githubusercontent.com/81133379/118950355-b3eac680-b98c-11eb-82c7-eb0ee1346e8a.png)

## Phone theme

![image](.github/images/phone_theme.png)

To enable this you need to Select phone theme on the "Realm Settings > Themes" as below:

![image](.github/images/select-phone-theme.png)

## Build into keycloak

[An example shows how to build this into keycloak using docker](examples/README.md)

## Installation

This needs an SMS implementation to enable sending verification codes. There are some implementations below (thanks in advance if you'd like to add your implementation here):  

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
