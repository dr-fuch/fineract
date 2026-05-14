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
    private static final String BASE_ERROR = "validation.msg.standinginstruction.";
    private static final String BLANK_ERROR = "cannot.be.blank";
    private static final String RANGE_ERROR = "is.not.within.expected.range";
    
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
                assertBlank(AccountDetailConstants.transferTypeParamName);
            }

            @Test
            void throwErrorWhitTransferTypeInvalidValue() {
                assertRange(AccountDetailConstants.transferTypeParamName);
            }

            @Test
            void throwErrorWhenNameIsMissing() {
                assertBlank(StandingInstructionApiConstants.nameParamName);
            }

            @Test
            void throwErrorWhenPriorityIsMissing() {
                assertBlank(StandingInstructionApiConstants.priorityParamName);
            }

            @Test
            void throwErrorWhitPriorityInvalidValue() {
                assertRange(StandingInstructionApiConstants.priorityParamName);
            }

            @Test
            void throwErrorWhenInstructionTypeIsMissing() {
                assertBlank(StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void throwErrorWhitInstructionTypeInvalidValue() {
                assertRange(StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void throwErrorWhenStatusIsMissing() {
                assertBlank(StandingInstructionApiConstants.statusParamName);
            }

            @Test
            void throwErrorWhitStatusTypeInvalidValue() {
                assertRange(StandingInstructionApiConstants.statusParamName);
            }

            @Test
            void throwErrorWhenValidFromIsMissing() {
                assertBlank(StandingInstructionApiConstants.validFromParamName);
            }

            @Test 
            void throwErrorWhenValidTillIsBeforeValidFrom() {
                assertValidation(getBaseRequest(), 
                    StandingInstructionApiConstants.validTillParamName, "is.less.than.date");
            }

            @Test
            void throwErrorWhenRecurrenceTypeIsMissing() {
                assertBlank(StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            void throwErrorWhitRecurrenceTypeInvalidValue() {
                assertRange(StandingInstructionApiConstants.recurrenceTypeParamName);
            }
        }
        
        @Nested
        class PeriodicRecurrenceType {
            @Test
            void throwErrorWhenRecurrenceFrequencyIsMissing() {
                final JsonObject json = getPeriodicRequest();
                json.remove(StandingInstructionApiConstants.recurrenceFrequencyParamName);
                assertBlank(json, 
                    StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void throwErrorWhitRecurrenceFrequencyInvalidValue() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 4);
                assertRange(json, StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void throwErrorWhenRecurrenceIntervalIsMissing() {
                final JsonObject json = getPeriodicRequest();
                json.remove(StandingInstructionApiConstants.recurrenceIntervalParamName);
                assertBlank(json, 
                    StandingInstructionApiConstants.recurrenceIntervalParamName);
            }

            @Test
            void throwErrorWhenRecurrenceFrequencyIsMonthlyAndMonthDayFormatIsMissing() {
                final JsonObject json = getPeriodicRequest();
                json.remove(StandingInstructionApiConstants.monthDayFormatParamName);
                assertBlank(json, 
                    StandingInstructionApiConstants.monthDayFormatParamName);
            }

            @Test
            void throwErrorWhenRecurrenceFrequencyIsMonthlyAndRecurrenceOnMonthDayIsMissing() {
                final JsonObject json = getPeriodicRequest();
                json.remove(StandingInstructionApiConstants.recurrenceOnMonthDayParamName);
                assertBlank(json, 
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName);
            }

            @Test
            void throwErrorWithRecurrenceOnMonthDayInvalidValue() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 Mayo");
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "invalid.month.day.format");
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
            void throwErrorWhenValidTillIsBeforeFirstMonthlyExecutionWithMonthDay() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.validFromParamName, "15 May 2026");
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "10 May");
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 3);
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 1);
                json.addProperty(StandingInstructionApiConstants.validTillParamName, "25 May 2026");
            
                assertValidation(json, 
                    StandingInstructionApiConstants.validTillParamName, "must.not.be.before.first.execution.date");
            }

        }

        @Nested
        class Amount {
            @Test
            void throwErrorWhenAmountIsMissing() {
                final JsonObject json = getPeriodicRequest();
                json.remove(StandingInstructionApiConstants.amountParamName);
                assertBlank(json, 
                    StandingInstructionApiConstants.amountParamName);
            }

            @Test
            void throwErrorWhenAmountIsNotPositive() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, new BigDecimal("-10.00"));
                assertValidation(json,
                    StandingInstructionApiConstants.amountParamName, "not.greater.than.zero");
            }

            @Test
            void throwErrorWhenInstructionTypeIsDuesAndAmountIsNotNull() {
                assertValidation(getDuesRequest(),
                    StandingInstructionApiConstants.amountParamName, "not.allowed.for.dues.instruction");
            }
        }

        @Nested
        class AccountTransfer {
            @Test
            void throwErrorWhenRecurrenceTypeIsAsPerDues() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.for.account.transfer");
            }

            @Test
            void throwErrorWhenInstructionTypeIsDues() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertValidation(json,
                    StandingInstructionApiConstants.instructionTypeParamName, "dues.not.allowed.for.account.transfer");
            }

            @Test
            void throwErrorWhenInstructionTypeIsNotFixed() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertRange(StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void throwErrorWhenRecurrenceTypeIsNotPeriodic() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertRange(StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            void throwErrorWhenIsNotAnAccountTransfer() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 1);
                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, "not.account.transfer");
            }

            @Test
            void throwErrorWithEqualAccountsAndEqualOffices() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);
                assertValidation(json, 
                    AccountDetailConstants.toAccountIdParamName, "transfer.to.same.account.not.allowed");
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
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.with.fixed.amount");
            }

            @Test
            void throwErrorWhenIsNotALoanRepayment() {
                final JsonObject json = getLoanRepaymentRequest();
                json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, "not.loan.repayment");
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

    private void assertValidation(final JsonObject json, final String parameter, final String reason) {
        final String expectedCode = BASE_ERROR + parameter + "." + reason;
        final JsonCommand command = createJsonCommand(json);
        
        PlatformApiDataValidationException ex = assertThrows(PlatformApiDataValidationException.class, () -> {
            this.standingInstructionDataValidator.validateForCreate(command);
        });

        boolean hasError = ex.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));

        assertEquals(true, hasError);
    }
    
    private void assertRange(JsonObject json, String param) {
        assertValidation(json, param, RANGE_ERR);
    }

    private void assertRange(String param) {
        assertValidation(getBaseRequest(), param, RANGE_ERR);
    }

    private void assertBlank(JsonObject json, String param) {
        json.remove(param);
        assertValidation(json, param, BLANK_ERROR);
    }

    private void assertBlank(String param) {
        JsonObject json = getBaseRequest();
        json.remove(param);
        assertValidation(json, param, BLANK_ERROR);
    }

    private void assertThrowsException(Class<? extends Throwable> exceptionClass, JsonObject json) {
        final JsonCommand command = createJsonCommand(json);
        assertThrows(exceptionClass, () -> 
            this.standingInstructionDataValidator.validateForCreate(command));
    }
}