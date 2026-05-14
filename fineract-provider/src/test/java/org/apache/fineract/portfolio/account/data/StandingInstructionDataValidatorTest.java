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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    private static final String BASE_ERR = "validation.msg.standinginstruction.";
    
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
                assertThrowsException(InvalidJsonException.class, null);
            }

            @Test
            void throwExceptionWhenJsonHasAnInvalidParam() {
                final JsonObject json = getBaseRequest();
                json.addProperty("invalidParam", "invalidValue");
                assertThrowsException(UnsupportedParameterException.class, json);
            }

            @Test
            void shouldCallAccountTransfersDetailDataValidator() {
                final JsonObject json = getAccountTransferRequest();
                final JsonCommand command = createJsonCommand(json);
                standingInstructionDataValidator.validateForCreate(command);

                verify(accountTransfersDetailDataValidator, times(1))
                    .validate(
                        any(JsonCommand.class),
                        any(DataValidatorBuilder.class));
            }

            @Test
            void throwErrorWhenTransferTypeIsMissing() {
                final JsonObject json = getBaseRequest();
                json.remove(AccountDetailConstants.transferTypeParamName);
                assertValidation(json, 
                    AccountDetailConstants.transferTypeParamName, "cannot.be.blank");
            }

            @Test
            void throwErrorWhitTransferTypeInvalidValue() {
                final JsonObject json = getBaseRequest();
                assertValidation(json, 
                    AccountDetailConstants.transferTypeParamName, "is.not.within.expected.range");
            }

            @Test
            void throwErrorWhenNameIsMissing() {
                final JsonObject json = getBaseRequest();
                json.remove(StandingInstructionApiConstants.nameParamName);
                assertValidation(json, 
                    StandingInstructionApiConstants.nameParamName, "cannot.be.blank");
            }

            @Test
            void throwErrorWhenPriorityIsMissing() {
                final JsonObject json = getBaseRequest();
                json.remove(StandingInstructionApiConstants.priorityParamName);
                assertValidation(json, 
                    StandingInstructionApiConstants.priorityParamName, "cannot.be.blank");
            }

            @Test
            void throwErrorWhitPriorityInvalidValue() {
                final JsonObject json = getBaseRequest();
                assertValidation(json, 
                    StandingInstructionApiConstants.priorityParamName, "is.not.within.expected.range");
            }

            @Test
            void throwErrorWhenInstructionTypeIsMissing() {
                final JsonObject json = getBaseRequest();
                json.remove(StandingInstructionApiConstants.instructionTypeParamName);
                assertValidation(json, 
                    StandingInstructionApiConstants.instructionTypeParamName, "cannot.be.blank");
            }

            @Test
            void throwErrorWhitInstructionTypeInvalidValue() {
                final JsonObject json = getBaseRequest();
                assertValidation(json, 
                    StandingInstructionApiConstants.instructionTypeParamName, "is.not.within.expected.range");
            }

            @Test
            void throwErrorWhenStatusIsMissing() {
                final JsonObject json = getBaseRequest();
                json.remove(StandingInstructionApiConstants.statusParamName);
                assertValidation(json, 
                    StandingInstructionApiConstants.statusParamName, "cannot.be.blank");
            }

            @Test
            void throwErrorWhitStatusTypeInvalidValue() {
                final JsonObject json = getBaseRequest();
                assertValidation(json, 
                    StandingInstructionApiConstants.statusParamName, "is.not.within.expected.range");
            }

            @Test
            void throwErrorWhenValidFromIsMissing() {
                final JsonObject json = getBaseRequest();
                json.remove(StandingInstructionApiConstants.validFromParamName);
                assertValidation(json, 
                    StandingInstructionApiConstants.validFromParamName, "cannot.be.blank");
            }

            @Test 
            void throwErrorWhenValidTillIsBeforeValidFrom() {
                assertValidation(getBaseRequest(), 
                    StandingInstructionApiConstants.validTillParamName, "is.less.than.date");
            }

            @Test
            void throwErrorWhenValidTillIsBeforeFirstDailyExecution() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.validFromParamName, "08 May 2026");
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 1); 
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 5);
                json.addProperty(StandingInstructionApiConstants.validTillParamName, "10 May 2026");
                assertValidation(json, 
                    StandingInstructionApiConstants.validTillParamName, "must.not.be.before.first.execution.date");
            }

            @Test
            void throwErrorWhenValidTillIsBeforeFirstWeeklyExecution() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.validFromParamName, "08 May 2026");
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 2);
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 2);
                json.addProperty(StandingInstructionApiConstants.validTillParamName, "15 May 2026");
                assertValidation(json, 
                    StandingInstructionApiConstants.validTillParamName, "must.not.be.before.first.execution.date");
            }

            @Test
            void throwErrorWhenRecurrenceTypeIsMissing() {
                assertThrowsBlankValidationError(StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            void throwErrorWhitRecurrenceTypeInvalidValue() {
                assertThrowsOutOfRangeValidationError(StandingInstructionApiConstants.recurrenceTypeParamName);
            }
        }
        
        @Nested
        class PeriodicRecurrenceType {
            @Test
            void throwErrorWhenRecurrenceFrequencyIsMissing() {
                final JsonObject json = getPeriodicRequest();
                assertThrowsBlankValidationError(json,
                    StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void throwErrorWhitRecurrenceFrequencyInvalidValue() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 4);
                assertThrowsOutOfRangeValidationError(json,
                    StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void throwErrorWhenRecurrenceIntervalIsMissing() {
                final JsonObject json = getPeriodicRequest();
                assertThrowsBlankValidationError(json,
                    StandingInstructionApiConstants.recurrenceIntervalParamName);
            }

            @Test
            void throwErrorWhenRecurrenceFrequencyIsMonthlyAndMonthDayFormatIsMissing() {
                final JsonObject json = getPeriodicRequest();
                assertThrowsBlankValidationError(json,
                    StandingInstructionApiConstants.monthDayFormatParamName);
            }

            @Test
            void throwErrorWhenRecurrenceFrequencyIsMonthlyAndRecurrenceOnMonthDayIsMissing() {
                final JsonObject json = getPeriodicRequest();
                assertThrowsBlankValidationError(json,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName);
            }

            @Test
            void throwErrorWithRecurrenceOnMonthDayInvalidValue() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 Mayo");
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName,
                    "validation.msg.standinginstruction.recurrenceOnMonthDay.invalid.month.day.format");
            }

        }

        @Nested
        class Amount {
            @Test
            void throwErrorWhenAmountIsMissing() {
                final JsonObject json = getPeriodicRequest();
                assertThrowsBlankValidationError(json, StandingInstructionApiConstants.amountParamName);
            }

            @Test
            void throwErrorWhenAmountIsNotPositive() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, new BigDecimal("-10.00"));
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    StandingInstructionApiConstants.amountParamName,
                    "validation.msg.standinginstruction.amount.not.greater.than.zero");
            }

            @Test
            void throwErrorWhenInstructionTypeIsDuesAndAmountIsNotNull() {
                final JsonObject json = getDuesRequest();
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    StandingInstructionApiConstants.amountParamName,
                    "validation.msg.standinginstruction.amount.not.allowed.for.dues.instruction");
            }
        }

        @Nested
        class AccountTransfer {
            @Test
            void throwErrorWhenRecurrenceTypeIsAsPerDues() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "validation.msg.standinginstruction.recurrenceType.as.per.dues.not.allowed.for.account.transfer");
            }

            @Test
            void throwErrorWhenInstructionTypeIsDues() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    StandingInstructionApiConstants.instructionTypeParamName,
                    "validation.msg.standinginstruction.instructionType.dues.not.allowed.for.account.transfer");
            }

            @Test
            void throwErrorWhenInstructionTypeIsNotFixed() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertThrowsOutOfRangeValidationError(json,
                    StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void throwErrorWhenRecurrenceTypeIsNotPeriodic() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertThrowsOutOfRangeValidationError(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            void throwErrorWhenIsNotAnAccountTransfer() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 1);
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    AccountDetailConstants.transferTypeParamName,
                    "validation.msg.standinginstruction.transferType.not.account.transfer");
            }

            @Test
            void throwErrorWithEqualAccountsAndEqualOffices() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);

                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command, 
                    AccountDetailConstants.toAccountIdParamName,
                    "validation.msg.standinginstruction.toAccountId.transfer.to.same.account.not.allowed");
            }

            @Test
            void shouldNotThrowErrorWithValidAccountTransfer() {
                final JsonObject json = getAccountTransferRequest();
                final JsonCommand command = createJsonCommand(json);
                assertDoesNotThrow(() -> { standingInstructionDataValidator.validateForCreate(command); });
            }
        }

        @Nested
        class LoanRepayment {
            @Test
            void throwErrorWhenInstructionTypeIsFixedAndRecurrenceTypeIsAsPerDues() {
                final JsonObject json = getLoanRepaymentRequest();
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "validation.msg.standinginstruction.recurrenceType.as.per.dues.not.allowed.with.fixed.amount");
            }

            @Test
            void throwErrorWhenIsNotALoanRepayment() {
                final JsonObject json = getLoanRepaymentRequest();
                json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
                final JsonCommand command = createJsonCommand(json);
                assertThrowsValidationError(command,
                    AccountDetailConstants.transferTypeParamName,
                    "validation.msg.standinginstruction.transferType.not.loan.repayment");
            }

            @Test
            void shouldNotThrowErrorWithValidLoanRepayment() {
                final JsonObject json = getLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
                final JsonCommand command = createJsonCommand(json);
                assertDoesNotThrow(() -> { standingInstructionDataValidator.validateForCreate(command); });
            }
        }
    }

    @Nested
    class ValidateForUpdate {

    }

    private JsonCommand createJsonCommand(final JsonObject jsonObject) {
        final String json = jsonObject == null ? "" : jsonObject.toString();
        final JsonElement parsedCommand = fromApiJsonHelper.parse(json);

        return JsonCommand.from(json, parsedCommand, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    private JsonObject getRequest(final String locale, final String dateFormat, final Integer fromOfficeId,
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

    private JsonObject getBaseRequest() {   
        return getRequest("en", "dd MMMM yyyy", 1, 1, 1, 1, 1, 1, 1, 1, 4,
            "BASE TEST", 5, 3, 3, "08 May 2026", "07 May 2026", 3,
            new BigDecimal(10.00), 2, 1, "08 May", "dd MMMM");
    }

    private JsonObject getPeriodicRequest() {
        JsonObject json = getBaseRequest();
        json.addProperty(StandingInstructionApiConstants.nameParamName, "PERIODIC TEST");
        json.addProperty(AccountDetailConstants.transferTypeParamName, 1);
        json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 2);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
        json.addProperty(StandingInstructionApiConstants.priorityParamName, 1);
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.statusParamName, 1);
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.validTillParamName, "07 May 2027");
        return json;
    }

    private JsonObject getDuesRequest() {
        JsonObject json = getPeriodicRequest();
        json.addProperty(StandingInstructionApiConstants.nameParamName, "DUES TEST");
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
        return json;
    }

    private JsonObject getLoanRepaymentRequest() {
        JsonObject json = getPeriodicRequest();
        json.addProperty(StandingInstructionApiConstants.nameParamName, "LOAN REPAYMENT TEST");
        json.addProperty(AccountDetailConstants.transferTypeParamName, 2);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        return json;
    }

    private JsonObject getAccountTransferRequest() {
        JsonObject json = getPeriodicRequest(); 
        json.addProperty(StandingInstructionApiConstants.nameParamName, "ACCOUNT TRANSFER TEST");
        json.addProperty(AccountDetailConstants.toAccountIdParamName, 2);
        return json;
    }

    private String getValidationError(final String parameter, final String reason) {
        return String.format("validation.msg.standinginstruction.%s.%s", parameter, reason);
    }

    private void assertThrowsOutOfRangeValidationError(final String parameter) {
        final String expectedCode = getValidationError(parameter, "is.not.within.expected.range");
        final JsonObject json = getBaseRequest();
        final JsonCommand command = createJsonCommand(json);

        assertThrowsValidationError(command, parameter, expectedCode);
    }

    private void assertThrowsOutOfRangeValidationError(final JsonObject json, final String parameter) {
        final String expectedCode = getValidationError(parameter, "is.not.within.expected.range");
        final JsonCommand command = createJsonCommand(json);

        assertThrowsValidationError(command, parameter, expectedCode);
    }

    private void assertThrowsBlankValidationError(final String parameter) {
        final String expectedCode = getValidationError(parameter, "cannot.be.blank");
        final JsonObject json = getBaseRequest();
        json.remove(parameter);
        final JsonCommand command = createJsonCommand(json);
        assertThrowsValidationError(command, parameter, expectedCode);
    }

    private void assertThrowsBlankValidationError(final JsonObject json, final String parameter) {
        final String expectedCode = getValidationError(parameter, "cannot.be.blank");
        json.remove(parameter);
        final JsonCommand command = createJsonCommand(json);

        assertThrowsValidationError(command, parameter, expectedCode);
    }

    private void assertValidation(final JsonObject json, final String parameter, final String reason) {
        final String expectedCode = BASE_ERR + parameter + "." + reason;
        final JsonCommand command = createJsonCommand(json);
        
        PlatformApiDataValidationException ex = assertThrows(PlatformApiDataValidationException.class, () -> {
            this.standingInstructionDataValidator.validateForCreate(command);
        });

        boolean hasError = ex.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));

        assertEquals(true, hasError);
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
    
    private void assertThrowsException(Class<? extends Throwable> exceptionClass, JsonObject json) {
        final JsonCommand command = createJsonCommand(json);
        assertThrows(exceptionClass, () -> 
            this.standingInstructionDataValidator.validateForCreate(command));
    }
}