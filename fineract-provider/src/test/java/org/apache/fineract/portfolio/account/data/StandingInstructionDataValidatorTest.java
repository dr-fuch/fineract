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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.stream.Stream;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class StandingInstructionDataValidatorTest {
    private static final String STANDING_INSTRUCTION_MSG_BASE = "validation.msg.standinginstruction.";
    private static final String MSG_CODE_CANNOT_BE_BLANK = "cannot.be.blank";
    private static final String MSG_CODE_INVALID_RANGE = "is.not.within.expected.range";
    
    private static final String invalidParamName = "invalidParam";
    private static final String invalidValue = "invalidValue";
    
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
    class WhenCreatingStandingInstruction {

        @Nested
        class BaseValidation {
            @Test
            void shouldFailWhenRequestBodyIsNull(){
                assertThrowsException(InvalidJsonException.class, null);
            }

            @Test
            void shouldFailWhenRequestContainsUnknownParameter() {
                final JsonObject json = accountTransferRequest();
                json.addProperty(invalidParamName, invalidValue);

                assertThrowsException(UnsupportedParameterException.class, json);
            }

            @Test
            void shouldValidateAccountTransferDetails() {
                final JsonObject json = getAccountTransferRequest();
                standingInstructionDataValidator.validateForCreate(command(json));

                verify(accountTransfersDetailDataValidator, times(1))
                    .validate(
                        any(JsonCommand.class),
                        any(DataValidatorBuilder.class));
            }

            @ParameterizedTest
            @ValueSource(strings = {
                AccountDetailConstants.transferTypeParamName,
                StandingInstructionApiConstants.nameParamName,
                StandingInstructionApiConstants.priorityParamName,
                StandingInstructionApiConstants.instructionTypeParamName,
                StandingInstructionApiConstants.statusParamName,
                StandingInstructionApiConstants.validFromParamName,
                StandingInstructionApiConstants.recurrenceTypeParamName })
            void shouldFailWhenRequiredParameterIsMissing(String parameter) {
                assertBlank(getBaseRequest(), parameter);
            }

            @ParameterizedTest
            @ValueSource(strings = {
                AccountDetailConstants.transferTypeParamName,
                StandingInstructionApiConstants.priorityParamName,
                StandingInstructionApiConstants.instructionTypeParamName,
                StandingInstructionApiConstants.statusParamName,
                StandingInstructionApiConstants.recurrenceTypeParamName
            })
            void shouldFailWhenParameterHasInvalidValue(String parameter) {
                assertRange(getBaseRequest(), parameter);
            }

            @Test 
            void shouldFailWhenValidTillDateIsBeforeValidFromDate() {
                assertValidation(getBaseRequest(), 
                    StandingInstructionApiConstants.validTillParamName, "is.less.than.date");
            }
        }
        
        @Nested
        class PeriodicRecurrenceType {
            @ParameterizedTest
            @ValueSource(strings = {
                StandingInstructionApiConstants.recurrenceFrequencyParamName,
                StandingInstructionApiConstants.recurrenceIntervalParamName,
                StandingInstructionApiConstants.monthDayFormatParamName,
                StandingInstructionApiConstants.recurrenceOnMonthDayParamName
            })
            void shouldFailWhenPeriodicFieldIsMissing(String parameter) {
                assertBlank(getPeriodicRequest(), parameter);
            }

            @Test
            void shouldFailWhenRecurrenceFrequencyHasInvalidValue() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 4);
                assertRange(json, StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void shouldFailWhenRecurrenceOnMonthDayHasInvalidValue() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 Mayo");
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "invalid.month.day.format");
            }

            @ParameterizedTest
            @MethodSource("invalidExecutionDates")
            void shouldFailWhenValidTillDateIsBeforeFirstExecutionDate(String validFrom, String validTill,
                Integer recurrenceFrequency, Integer recurrenceInterval, String recurrenceOnMonthDay) {
                
                final JsonObject json = getPeriodicRequest();
                
                json.addProperty(StandingInstructionApiConstants.validFromParamName, validFrom);
                json.addProperty(StandingInstructionApiConstants.validTillParamName, validTill);
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, recurrenceFrequency);                
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, recurrenceInterval);
                
                if (recurrenceOnMonthDay != null) {
                    json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, recurrenceOnMonthDay);
                }
            
                assertValidation(json,
                    StandingInstructionApiConstants.validTillParamName,
                    "must.not.be.before.first.execution.date");
            }

            private static Stream<Arguments> invalidExecutionDates() {
                return Stream.of(
                    Arguments.of("08 May 2026", "10 May 2026", 0, 5, null),
                    Arguments.of("08 May 2026", "15 May 2026", 1, 2, null),
                    Arguments.of("15 May 2026", "25 May 2026", 2, 1, "10 May")
                );
            }

        }

        @Nested
        class Amount {
            @Test
            void shouldFailWhenAmountIsMissing() {
                assertBlank(getPeriodicRequest(), 
                    StandingInstructionApiConstants.amountParamName);
            }

            @Test
            void shouldFailWhenAmountValueIsNotPositive() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, new BigDecimal("-10.00"));
                assertValidation(json,
                    StandingInstructionApiConstants.amountParamName, "not.greater.than.zero");
            }

            @Test
            void shouldFailWhenInstructionTypeIsDuesAndAmountIsNotNull() {
                assertValidation(getDuesRequest(),
                    StandingInstructionApiConstants.amountParamName, "not.allowed.for.dues.instruction");
            }
        }

        @Nested
        class AccountTransfer {
            @Test
            void shouldFailWhenRecurrenceTypeIsAsPerDues() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.for.account.transfer");
            }

            @Test
            void shouldFailWhenInstructionTypeIsDues() {
                final JsonObject json = getPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertValidation(json,
                    StandingInstructionApiConstants.instructionTypeParamName, "dues.not.allowed.for.account.transfer");
            }

            @Test
            void shouldFailWhenInstructionTypeIsNotFixed() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertRange(json,
                    StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void shouldFailWhenRecurrenceTypeIsNotPeriodic() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertRange(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            void shouldFailWhenIsNotAnAccountTransfer() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 1);
                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, "not.account.transfer");
            }

            @Test
            void shouldFailWithEqualAccountsAndEqualOffices() {
                final JsonObject json = getAccountTransferRequest();
                json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);
                assertValidation(json, 
                    AccountDetailConstants.toAccountIdParamName, "transfer.to.same.account.not.allowed");
            }

            @Test
            void shouldPassWithValidAccountTransfer() {
                assertValidationSuccess(getAccountTransferRequest());
            }
        }

        @Nested
        class LoanRepayment {
            @Test
            void shouldFailWhenInstructionTypeIsFixedAndRecurrenceTypeIsAsPerDues() {
                assertValidation(getLoanRepaymentRequest(),
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.with.fixed.amount");
            }

            @Test
            void shouldFailWhenIsNotALoanRepayment() {
                final JsonObject json = getLoanRepaymentRequest();
                json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, "not.loan.repayment");
            }

            @Test
            void shouldPassWithValidLoanRepayment() {
                final JsonObject json = getLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
                
                assertValidationSuccess(json);
            }
        }
    }

    @Nested
    class WhenUpdatingStandingInstruction {
        @Test
        void throwExceptionWhenJsonIsBlankOrNull() {
            assertThrowsException(InvalidJsonException.class, null);
        }
    }

    private JsonCommand command(JsonObject jsonObject) {
        final String json = jsonObject == null ? "" : jsonObject.toString();
        final JsonElement element = fromApiJsonHelper.parse(json);

        return JsonCommand.from(json, element, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    private JsonObject accountTransferRequest() {
        JsonObject json = new JsonObject();
        json.addProperty(AccountDetailConstants.localeParamName, "en");
        json.addProperty(AccountDetailConstants.dateFormatParamName, "dd MMMM yyyy");
        json.addProperty(AccountDetailConstants.fromOfficeIdParamName, 1);
        json.addProperty(AccountDetailConstants.fromClientIdParamName, 1);
        json.addProperty(AccountDetailConstants.fromAccountIdParamName, 1);
        json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 2);
        json.addProperty(AccountDetailConstants.toOfficeIdParamName, 1);
        json.addProperty(AccountDetailConstants.toClientIdParamName, 1);
        json.addProperty(AccountDetailConstants.toAccountIdParamName, 2);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
        json.addProperty(AccountDetailConstants.transferTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.nameParamName, "BASIC ACCOUNT TRANSFER");
        json.addProperty(StandingInstructionApiConstants.priorityParamName, 1);
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.statusParamName, 1);
        json.addProperty(StandingInstructionApiConstants.validFromParamName, "16 May 2026");
        json.addProperty(StandingInstructionApiConstants.validTillParamName, "16 May 2027");
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.amountParamName, BigDecimal.TEN);
        json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 0);
        json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 1);

        return json;
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
        return getRequest(
            "en", 
            "dd MMMM yyyy", 1, 1, 1, 1, 1, 1, 1, 1, 4,
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
        final String expectedCode = STANDING_INSTRUCTION_MSG_BASE + parameter + "." + reason;
        
        PlatformApiDataValidationException ex = assertThrows(PlatformApiDataValidationException.class, () -> {
            this.standingInstructionDataValidator.validateForCreate(command(json));
        });

        boolean hasError = ex.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));

        assertTrue(hasError);
    }
    
    private void assertValidationSuccess(JsonObject json) {
        assertDoesNotThrow(() ->
            standingInstructionDataValidator.validateForCreate(command(json)));
    }

    private void assertRange(JsonObject json, String param) {
        assertValidation(json, param, MSG_CODE_INVALID_RANGE);
    }

    private void assertBlank(JsonObject json, String param) {
        json.remove(param);
        assertValidation(json, param, MSG_CODE_CANNOT_BE_BLANK);
    }

    private void assertThrowsException(Class<? extends Throwable> exceptionClass, JsonObject json) {
        assertThrows(exceptionClass, () -> 
            this.standingInstructionDataValidator.validateForCreate(command(json)));
    }
}