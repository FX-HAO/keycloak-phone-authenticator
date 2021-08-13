FROM maven:3.6.2-jdk-8 AS builder
WORKDIR /app/
COPY ./ keycloak-phone-authenticator/
RUN mvn clean install -f keycloak-phone-authenticator
COPY yuntongxun4j/ yuntongxun4j/
RUN mvn clean install -f yuntongxun4j
COPY keycloak-phone-authenticator-yuntongxun-sms/ keycloak-phone-authenticator-yuntongxun-sms/
RUN mvn clean install -f keycloak-phone-authenticator-yuntongxun-sms/


FROM jboss/keycloak:13.0.1

# copy the jars into the current folder
COPY --from=builder /app/keycloak-phone-authenticator/target/keycloak-phone-authenticator-1.0.0-SNAPSHOT.jar .
COPY --from=builder /app/keycloak-phone-authenticator-yuntongxun-sms/target/keycloak-phone-authenticator-yuntongxun-sms-1.0.0-SNAPSHOT.jar .
COPY --from=builder /app/yuntongxun4j/target/yuntongxun4j-1.0-SNAPSHOT.jar .
COPY examples/cli/ cli/

# register the jars
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=cli/keycloak-phone-authenticator-yuntongxun-sms-config.cli
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=cli/module-add.cli

# copy the resource files into the current folder
COPY --from=builder /app/keycloak-phone-authenticator/target/classes/theme-resources/templates/ $JBOSS_HOME/themes/base/login/
COPY ./messages/ messages/

# fix a bug
RUN rm -rf /opt/jboss/keycloak/standalone/configuration/standalone_xml_history/current

# register the resource files
RUN cat messages/messages_en.properties >> $JBOSS_HOME/themes/base/login/messages/messages_en.properties && \
    cat messages/messages_zh_CN.properties >> $JBOSS_HOME/themes/base/login/messages/messages_zh_CN.properties && \
    cat messages/messages_en.properties >> $JBOSS_HOME/themes/base/account/messages/messages_en.properties && \
    cat messages/messages_zh_CN.properties >> $JBOSS_HOME/themes/base/account/messages/messages_zh_CN.properties
