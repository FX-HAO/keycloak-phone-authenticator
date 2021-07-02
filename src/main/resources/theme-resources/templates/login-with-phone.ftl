<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">

        <script src="https://cdn.jsdelivr.net/npm/vue"></script>
        <script src="https://unpkg.com/axios/dist/axios.min.js"></script>
        <#if captchaKey?has_content >
            <script src="https://www.recaptcha.net/recaptcha/api.js" nonce="{NONCE}" async defer></script>
        </#if>


        <style>
            [v-cloak] > * { display:none; }
            [v-cloak]::before { content: "loading..."; }
        </style>

        <div id="vue-app">
            <div v-cloak>

                <div id="kc-form" <#if realm.password && social.providers??>class="${properties.kcContentWrapperClass!}"</#if>>
                    <div id="kc-form-wrapper" <#if realm.password && social.providers??>class="${properties.kcFormSocialAccountContentClass!} ${properties.kcFormSocialAccountClass!}"</#if>>
                        <#if realm.password>
                            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">

                                <div class="${properties.kcFormGroupClass!}">
                                    <div class="${properties.kcLabelWrapperClass!}">
                                        <ul class="nav nav-pills nav-justified">
                                            <li role="presentation" v-bind:class="{ active: usernameOrPhone }" v-on:click="usernameOrPhone = true"><a href="#"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></a></li>
                                            <li role="presentation" v-bind:class="{ active: !usernameOrPhone }" v-on:click="usernameOrPhone = false"><a href="#">${msg("phone")}</a></li>
                                        </ul>
                                    </div>
                                </div>

                                <div v-if="usernameOrPhone">
                                    <div class="${properties.kcFormGroupClass!}">
                                        <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                                        <#if usernameEditDisabled??>
                                            <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" type="text" disabled />
                                        <#else>
                                            <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off"
                                                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                                            />

                                            <#if messagesPerField.existsError('username','password')>
                                                <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                                            </#if>
                                        </#if>
                                    </div>

                                    <div class="${properties.kcFormGroupClass!}">
                                        <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>

                                        <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off"
                                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                                        />
                                    </div>
                                </div>

                                <div v-show="!usernameOrPhone">
                                    <div class="${properties.kcFormGroupClass!}">
                                        <div class="${properties.kcLabelWrapperClass!}">
                                            <label for="phoneNumber" class="${properties.kcLabelClass!}">${msg("phoneNumber")}</label>
                                        </div>
                                        <div class="${properties.kcInputWrapperClass!}">
                                            <input type="text" id="phoneNumber" name="phoneNumber" v-model="phoneNumber" class="${properties.kcInputClass!}" autofocus/>
                                        </div>
                                    </div>

                                    <div class="row">
                                        <div class="${properties.kcFormGroupClass!}">
                                            <div class="${properties.kcLabelWrapperClass!}">
                                                <label for="code" class="${properties.kcLabelClass!}">${msg("verificationCode")}</label>
                                            </div>
                                            <div class="col-xs-9 col-sm-9 col-md-9 col-lg-9">
                                                <input type="text" id="code" name="code" class="${properties.kcInputClass!}" autofocus/>
                                            </div>
                                            <div class="col-xs-3 col-sm-3 col-md-3 col-lg-3">
                                                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                                       type="button" v-model="sendButtonText" :disabled='sendButtonText !== initSendButtonText' v-on:click="sendVerificationCode()"/>
                                            </div>
                                        </div>
                                    </div>

                                    <#if captchaKey?has_content >
                                        <br/>
                                        <div class="row">
                                            <div class="${properties.kcFormGroupClass!}">
                                                <div class="${properties.kcInputWrapperClass!}">
                                                    <div id="my-recaptcha" class="g-recaptcha" data-sitekey="${captchaKey}"></div>
                                                </div>
                                            </div>
                                        </div>
                                    </#if>
                                </div>

                                <div class="${properties.kcFormGroupClass!}">
                                    <br/>
                                </div>


                                <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                                    <div id="kc-form-options">
                                        <#if realm.rememberMe && !usernameEditDisabled??>
                                            <div class="checkbox">
                                                <label>
                                                    <#if login.rememberMe??>
                                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                                    <#else>
                                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                                    </#if>
                                                </label>
                                            </div>
                                        </#if>
                                    </div>
                                    <div class="${properties.kcFormOptionsWrapperClass!}">
                                        <#if realm.resetPasswordAllowed>
                                            <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                                        </#if>
                                    </div>
                                </div>

                                <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                                    <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                                    <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                                </div>
                            </form>
                        </#if>
                    </div>
                    <#if realm.password && social.providers??>
                        <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                            <hr/>
                            <h4>${msg("identity-provider-login-label")}</h4>

                            <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                                <#list social.providers as p>
                                    <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                                       type="button" href="${p.loginUrl}">
                                        <#if p.iconClasses?has_content>
                                            <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                            <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                                        <#else>
                                            <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                                        </#if>
                                    </a>
                                </#list>
                            </ul>
                        </div>
                    </#if>
                </div>
            </div>
        </div>

        <script type="text/javascript">
            var app = new Vue({
                el: '#vue-app',
                data: {
                    errorMessage: '',
                    freezeSendCodeSeconds: 0,
                    usernameOrPhone: true,
                    phoneNumber: '',
                    sendButtonText: '${msg("sendVerificationCode")}',
                    initSendButtonText: '${msg("sendVerificationCode")}',
                    disableSend: function(seconds) {
                        if (seconds <= 0) {
                            app.sendButtonText = app.initSendButtonText;
                            app.freezeSendCodeSeconds = 0;
                        } else {
                            app.sendButtonText = String(seconds);
                            setTimeout(function() {
                                app.disableSend(seconds - 1);
                            }, 1000);
                        }
                    },
                    sendVerificationCode: function() {
                        <#if captchaKey?has_content >
                        const recaptchaResponse = document.getElementById('g-recaptcha-response').value;
                        if (!recaptchaResponse) {
                            this.errorMessage = '${msg("requireRecaptcha")}';
                            return;
                        }
                        </#if>

                        const phoneNumber = document.getElementById('phoneNumber').value.trim();
                        if (!phoneNumber) {
                            this.errorMessage = '${msg("requirePhoneNumber")}';
                            document.getElementById('phoneNumber').focus();
                            return;
                        }

                        if (this.sendButtonText !== this.initSendButtonText) {
                            return;
                        }

                        this.disableSend(60);

                        const params = new URLSearchParams();
                        params.append('phoneNumber', this.phoneNumber);
                        params.append('kind', '${verificationCodeKind}');
                        <#if captchaKey?has_content >
                        params.append('g-recaptcha-response', recaptchaResponse);
                        </#if>

                        axios
                            .post(window.location.origin + '/auth/realms/${realm.name}/verification_codes', params)
                            .then(res => (console.log(res.status)));
                    }
                }
            });
        </script>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration-container">
                <div id="kc-registration">
                    <span>${msg("noAccount")} <a tabindex="6"
                                                 href="${url.registrationUrl}">${msg("doRegister")}</a></span>
                </div>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
