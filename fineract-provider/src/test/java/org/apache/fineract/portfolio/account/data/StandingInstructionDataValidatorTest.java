/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.account.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.UnsupportedParameterException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants;
import org.apache.fineract.portfolio.account.data.AccountTransfersDetailDataValidator;
import org.apache.fineract.portfolio.account.data.StandingInstructionDataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StandingInstructionDataValidatorTest {    

    @Mock
    private AccountTransfersDetailDataValidator accountTransfersDetailDataValidator;
    
    private final static FromJsonHelper fromApiJsonHelper = new FromJsonHelper();
    private StandingInstructionDataValidator standingInstructionDataValidator;

    @BeforeEach
    public void setUp() {
        this.standingInstructionDataValidator = new StandingInstructionDataValidator(fromApiJsonHelper, 
            this.accountTransfersDetailDataValidator);
    }

    @Nested
    class ValidateForCreate {

        @Nested
        class UnconditionedBehavior {
            @Test
            void throwExceptionWhenJsonIsBlankOrNull(){
                final JsonCommand command = createJsonCommand(null);
                assertThrowsException(InvalidJsonException.class, command);
            }

            @Test
            void throwExceptionWhenJsonHasAnInvalidParam() {
                final JsonObject json = getBaseJsonObjectWithInvalidParam();
                final JsonCommand command = createJsonCommand(json);
                assertThrowsException(UnsupportedParameterException.class, command);
            }

            @Test
            void shouldCallAccountTransfersDetailDataValidator() {
                final JsonObject json = getBaseJsonObject();
                final JsonCommand command = createJsonCommand(json);
                standingInstructionDataValidator.validateForCreate(command);

                verify(accountTransfersDetailDataValidator, times(1))
                    .validate(
                        any(JsonCommand.class),
                        any(DataValidatorBuilder.class));
            }

            @Test
            void throwErrorWhenTransferTypeIsMissing() {
                assertThrowsBlankValidationError(AccountDetailConstants.transferTypeParamName);
            }

            @Test
            void throwErrorWhenNameIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.nameParamName);
            }

            @Test
            void throwErrorWhenPriorityIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.priorityParamName);
            }

            @Test
            void throwErrorWhenInstructionTypeIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void throwErrorWhenStatusIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.statusParamName);
            }

            @Test
            void throwErrorWhenValidFromIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.validFromParamName);
            }

            @Test 
            void throwErrorWhenValidTillIsBeforeValidFrom() {
                final JsonObject json = getBaseJsonObject();
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command, 
                    StandingInstructionApiConstants.validTillParamName,
                    "validation.msg.standinginstruction.validTill.is.less.than.date");
            }

            @Test
            void throwErrorWhenRecurrenceTypeIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.recurrenceTypeParamName);
            }
        }
        
        @Nested
        class AccountTransfer {
            
            @Test
            void throwExceptionWithEqualAccountsAndEqualOffices() {
                final JsonObject json = getBaseJsonObject("en", "dd MMMM yyyy", 1, 1, 1, 2, 1, 1, 1, 2, 1,
                    "BASE TEST", 1, 1, 1, "08 May 2026", "07 May 2027", 1,
                    new BigDecimal(10.00), 2, 1, "08 May", "dd MMMM");

                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command, 
                    AccountDetailConstants.toAccountIdParamName,
                    "validation.msg.standinginstruction.toAccountId.transfer.to.same.account.not.allowed");
            }
        }
    }

    private JsonCommand createJsonCommand(final JsonObject jsonObject) {
        final String json = jsonObject == null ? "" : jsonObject.toString();
        final JsonElement parsedCommand = fromApiJsonHelper.parse(json);

        return JsonCommand.from(json, parsedCommand, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    private JsonObject getBaseJsonObjectWithoutParam(final String paramName) {
        final JsonObject jsonObject = getBaseJsonObject();
        jsonObject.remove(paramName);
        return jsonObject;
    }

    private JsonObject getBaseJsonObjectWithInvalidParam() {
        final JsonObject jsonObject = getBaseJsonObject();
        jsonObject.addProperty("invalidParam", "invalidValue");

        return jsonObject;
    }

    private JsonObject getBaseJsonObject() {   
        final JsonObject jsonObject = new JsonObject();
        return getBaseJsonObject("en", "dd MMMM yyyy", 1, 1, 1, 2, 1, 1, 2, 2, 1,
            "BASE TEST", 1, 1, 1, "08 May 2026", "07 May 2026", 1,
            new BigDecimal(10.00), 2, 1, "08 May", "dd MMMM");
    }

    private JsonObject getBaseJsonObject(final String locale, final String dateFormat, final Integer fromOfficeId,
        final Integer fromClientId, final Integer fromAccountId, final Integer fromAccountType, final Integer toOfficeId,
        final Integer toClientId, final Integer toAccountId, final Integer toAccountType, final Integer transferType,
        final String name, final Integer priority, final Integer instructionType, final Integer status,
        final String validFrom, final String validTill, final Integer recurrenceType, final BigDecimal amount,
        final Integer recurrenceFrequency, final Integer recurrenceInterval, final String recurrenceOnMonthDay,
        final String monthDayFormat) {

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(AccountDetailConstants.localeParamName, locale);
        jsonObject.addProperty(AccountDetailConstants.dateFormatParamName, dateFormat);
        jsonObject.addProperty(AccountDetailConstants.fromOfficeIdParamName, fromOfficeId);
        jsonObject.addProperty(AccountDetailConstants.fromClientIdParamName, fromClientId);
        jsonObject.addProperty(AccountDetailConstants.fromAccountIdParamName, fromAccountId);
        jsonObject.addProperty(AccountDetailConstants.fromAccountTypeParamName, fromAccountType);
        jsonObject.addProperty(AccountDetailConstants.toOfficeIdParamName, toOfficeId);
        jsonObject.addProperty(AccountDetailConstants.toClientIdParamName, toClientId);
        jsonObject.addProperty(AccountDetailConstants.toAccountIdParamName, toAccountId);
        jsonObject.addProperty(AccountDetailConstants.toAccountTypeParamName, toAccountType);
        jsonObject.addProperty(AccountDetailConstants.transferTypeParamName, transferType);
        jsonObject.addProperty(StandingInstructionApiConstants.nameParamName, name);
        jsonObject.addProperty(StandingInstructionApiConstants.priorityParamName, priority);
        jsonObject.addProperty(StandingInstructionApiConstants.instructionTypeParamName, instructionType);
        jsonObject.addProperty(StandingInstructionApiConstants.statusParamName, status);
        jsonObject.addProperty(StandingInstructionApiConstants.validFromParamName, validFrom);
        jsonObject.addProperty(StandingInstructionApiConstants.validTillParamName, validTill);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, recurrenceType);
        jsonObject.addProperty(StandingInstructionApiConstants.amountParamName, amount);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, recurrenceFrequency);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, recurrenceInterval);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, recurrenceOnMonthDay);
        jsonObject.addProperty(StandingInstructionApiConstants.monthDayFormatParamName, monthDayFormat);
        return jsonObject;
    }

    private void assertThrowsBlankValidationError(final String parameter) {
        final String expectedCode = String.format("validation.msg.standinginstruction.%s.cannot.be.blank", parameter);
        assertThrowsBlankValidationError(parameter, expectedCode);
    }

    private void assertThrowsBlankValidationError(final String parameter, final String expectedCode) {
        final JsonObject json = getBaseJsonObjectWithoutParam(parameter);
        final JsonCommand command = createJsonCommand(json);

        assertThrowsValidationError(command, parameter, expectedCode);
    }

    private void assertThrowsValidationError(final JsonCommand command, final String parameter,
        final String expectedCode) {

        PlatformApiDataValidationException ex = assertThrows(
            PlatformApiDataValidationException.class, () -> {
                this.standingInstructionDataValidator.validateForCreate(command);
        });

        boolean hasError = ex.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));

        assertEquals(true, hasError);
    }
    
    private void assertThrowsException(Class<? extends Throwable> exceptionClass, JsonCommand command) {
        assertThrows(exceptionClass, () -> this.standingInstructionDataValidator.validateForCreate(command));
    }
}