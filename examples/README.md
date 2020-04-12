## A docker demo shows how to build this into keycloak

Make sure you have docker installed before executing

```bash
git clone https://github.com/FX-HAO/keycloak-phone-authenticator.git
cd keycloak-phone-authenticator
git clone https://github.com/FX-HAO/keycloak-phone-authenticator-yuntongxun-sms.git
git clone https://github.com/FX-HAO/yuntongxun4j.git
docker build -t keycloak-phone-authenticator-demo -f examples/Dockerfile .
docker run keycloak-phone-authenticator-demo
```
